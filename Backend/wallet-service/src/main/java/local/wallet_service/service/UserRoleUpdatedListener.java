package local.wallet_service.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import local.wallet_service.model.StaffSalary;
import local.wallet_service.repository.StaffSalaryRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRoleUpdatedListener {

    private final StaffSalaryRepository staffRepo;

    /**
     * 👂 Lắng nghe event MQ từ user-service khi user được gán hoặc gỡ role STAFF
     * Queue: user.role.updated.queue
     */
    @RabbitListener(queues = "user.role.updated.queue")
    public void handleRoleUpdated(Map<String, Object> event) {
        try {
            Long userId = ((Number) event.get("userId")).longValue();
            String role = (String) event.get("role");
            String eventType = (String) event.get("eventType");

            log.info("📩 [WalletService] Nhận event role update: userId={} | role={} | type={}", userId, role, eventType);

            // ===== Khi user được gán role STAFF =====
            if ("STAFF".equalsIgnoreCase(role)) {
                staffRepo.findByUserId(userId).ifPresentOrElse(
                    staff -> {
                        staff.setStatus("ACTIVE");
                        staffRepo.save(staff);
                        log.info("✅ Cập nhật lại StaffSalary thành ACTIVE cho userId={}", userId);
                    },
                    () -> {
                        StaffSalary newStaff = StaffSalary.builder()
                                .userId(userId)
                                .salary(new BigDecimal("5000000")) // 💰 Lương mặc định 5 triệu
                                .payDay(2) // 📅 Mặc định trả ngày 25 hàng tháng
                                .status("ACTIVE")
                                .startDate(LocalDate.now())
                                .build();
                        staffRepo.save(newStaff);
                        log.info("✅ Tạo record StaffSalary mới cho userId={} (role=STAFF)", userId);
                    }
                );
            }

            // ===== Khi user bị gỡ role STAFF =====
            else {
                staffRepo.findByUserId(userId).ifPresent(staff -> {
                    staff.setStatus("INACTIVE");
                    staffRepo.save(staff);
                    log.info("🛑 Vô hiệu hóa StaffSalary cho userId={} (role bị gỡ)", userId);
                });
            }

        } catch (Exception e) {
            log.error("❌ [WalletService] Lỗi khi xử lý event role.updated: {}", e.getMessage(), e);
        }
    }
}
