package local.Second_hand_EV_Battery_Trading_Platform.entity;

import java.time.LocalDateTime;
import java.util.List; // ✅ Bổ sung dòng này

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 Liên kết với Customer (mỗi payment thuộc về 1 khách hàng)
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    private String method; // vnpay / momo
    private Double amount;
    private String status; // PENDING / SUCCESS / FAILED

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "cart_id")
    private Long cartId;

    @ElementCollection
    @Column(name = "cart_id_list")
    private List<Long> cartIdList; // ✅ OK sau khi import java.util.List

    @Column(length = 1000)
    private String productNames;

    @Column
    private Double totalAmount;

}
