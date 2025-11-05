// File: edu/uth/notificationservice/EventListener/NotificationEventListener.java
package edu.uth.notification_service.EventListener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import edu.uth.notification_service.DTO.ListingEventDTO; //
import edu.uth.notification_service.Service.NotificationService;

@Component
public class NotificationEventListener {

    @Autowired
    private NotificationService notificationService; 

    private static final String NOTIFICATION_QUEUE = "${app.rabbitmq.queue}";

    // Đọc giá trị base-url từ file properties
    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl; //

    @RabbitListener(queues = NOTIFICATION_QUEUE)
    public void handleListingEvent(ListingEventDTO event) { //
        
        System.out.println("Đã hứng được sự kiện: " + event.getEventType() + " cho User: " + event.getUserId());

        String message;
        String link;
        
        // ✅ ĐỊNH DẠNG TÊN FILE VÀ PARAMETER MỚI
        // Phải khớp với file .html của bạn và code manage-listings.js
        String targetFile = "/edit_news.html"; //
        String parameterName = "?listing_id="; //

        // Xử lý logic dựa trên loại sự kiện
        switch (event.getEventType()) { //
            case "APPROVED":
                message = String.format("Tin đăng '%s' của bạn đã được duyệt.", event.getProductName());
                // ✅ SỬA LẠI LINK
                link = String.format("%s%s%s%d", frontendBaseUrl, targetFile, parameterName, event.getListingId());
                break;
            case "REJECTED":
                message = String.format("Tin đăng '%s' đã bị từ chối.", event.getProductName());
                // ✅ SỬA LẠI LINK
                link = String.format("%s%s%s%d", frontendBaseUrl, targetFile, parameterName, event.getListingId());
                break;
            case "SOLD":
                message = String.format("Sản phẩm '%s' của bạn đã được bán.", event.getProductName());
                // ✅ SỬA LẠI LINK
                link = String.format("%s%s%s%d", frontendBaseUrl, targetFile, parameterName, event.getListingId());
                break;
            case "VERIFIED":
                 message = String.format("Tin đăng '%s' đã được kiểm định.", event.getProductName());
                 // ✅ SỬA LẠI LINK
                 link = String.format("%s%s%s%d", frontendBaseUrl, targetFile, parameterName, event.getListingId());
                 break;
            default:
                System.out.println("Sự kiện không xác định (bỏ qua): " + event.getEventType());
                return; // Bỏ qua
        }

        // Gọi NotificationService để tạo thông báo
        notificationService.createNotification(event.getUserId(), message, link);
    }
}