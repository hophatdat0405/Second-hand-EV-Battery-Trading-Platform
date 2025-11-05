// File: edu/uth/notificationservice/Service/NotificationService.java
package edu.uth.notification_service.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uth.notification_service.Model.Notification;
import edu.uth.notification_service.Repository.NotificationRepository;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // === BỔ SUNG: Dịch vụ gửi FCM ===
    @Autowired
    private FCMService fcmService; // <-- TIÊM FCM SERVICE VÀO

    @Transactional
    public Notification createNotification(Long userId, String message, String link) {
        // 1. Tạo và Lưu thông báo vào CSDL
        Notification notification = new Notification(userId, message, link);
        Notification savedNotification = notificationRepository.save(notification);

        // 2. "ĐẨY" thông báo qua WebSocket (cho chuông thông báo)
        messagingTemplate.convertAndSendToUser(
            String.valueOf(userId),
            "/topic/notifications",
            savedNotification
        );

        // 3. === GỌI FCM ĐỂ GỬI PUSH NOTIFICATION ===
        try {
            fcmService.sendPushNotification(savedNotification);
        } catch (Exception e) {
            // Ghi log lỗi nhưng không để nó làm hỏng giao dịch
            // (Không ném 'throws' ra ngoài để tránh rollback)
            System.err.println("Lỗi khi gọi fcmService.sendPushNotification: " + e.getMessage());
        }
        
        return savedNotification;
    }

    // ... (Các hàm còn lại: getNotificationsForUser, markAsRead, ... giữ nguyên) ...
    public Page<Notification> getNotificationsForUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public long getUnreadNotificationCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            // (Sửa lỗi từ lần trước nếu bạn chưa sửa)
            // Đảm bảo Model 'Notification' của bạn có hàm setIsRead()
            notification.setRead(true); 
            notificationRepository.save(notification);
        });
    }

    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }
    
    @Transactional
    public void deleteAllForUser(Long userId) {
        notificationRepository.deleteAllByUserId(userId);
    }
}