// Tạo file mới: edu/uth/listingservice/Model/Notification.java
package edu.uth.listingservice.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // Người dùng sẽ nhận thông báo

    @Column(nullable = false, length = 500)
    private String message; // Nội dung thông báo

    @Column(nullable = false)
    private String link; // Đường dẫn khi click vào

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false; // Mặc định là chưa đọc

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt = new Date();

    public Notification(Long userId, String message, String link) {
        this.userId = userId;
        this.message = message;
        this.link = link;
    }
}