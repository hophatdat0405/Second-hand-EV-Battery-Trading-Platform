package edu.uth.notification_service.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;

import edu.uth.notification_service.Model.Notification;

// ⛔ Đã xóa 'import com.google.firebase.messaging.Notification;'

@Service
public class FCMService {

    @Autowired
    private FirebaseMessaging firebaseMessaging;

    @Autowired
    private UserDeviceService userDeviceService;

    // Hàm này nhận Model "Notification" của bạn (từ CSDL)
    public void sendPushNotification(Notification notification) {
        
        // (Đây là logic 1-Nhiều đã nâng cấp)
        List<String> deviceTokens = userDeviceService.getTokensByUserId(notification.getUserId());

        if (deviceTokens == null || deviceTokens.isEmpty()) {
            System.err.println("Không tìm thấy Device Token nào trong CSDL cho User ID: " + notification.getUserId() + ". Bỏ qua gửi FCM.");
            return;
        }
        
        if (deviceTokens.size() > 500) {
             System.err.println("User " + notification.getUserId() + " có quá nhiều token. Chỉ gửi cho 500 token đầu tiên.");
             deviceTokens = deviceTokens.subList(0, 500);
        }

        // 1. ⛔ ĐÃ XÓA BỎ đối tượng 'fcmNotification'
        // Chúng ta sẽ đưa title và body vào 'data'

        // 2. Tạo MulticastMessage (CHỈ DÙNG DATA)
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(deviceTokens) 
                // ⛔ ĐÃ XÓA DÒNG: .setNotification(fcmNotification)
                
                // ✅ THÊM TITLE VÀ BODY VÀO DATA
                .putData("title", "Bạn có thông báo mới!") // <-- THAY ĐỔI
                .putData("body", notification.getMessage())     // <-- THAY ĐỔI

                // DỮ LIỆU CŨ CỦA BẠN
                .putData("link", notification.getLink())
                .putData("notificationId", notification.getId().toString())

                // DỮ LIỆU MỚI
                .putData("image", "http://127.0.0.1:5501/images/acer.webp") 
                .putData("badge", "http://127.0.0.1:5501/images/hp.webp")
                
                .build();

        // 3. Gửi thông báo
        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            System.out.println("Gửi FCM (Data-only) thành công: " + response.getSuccessCount() + " messages");
        } catch (FirebaseMessagingException e) {
            System.err.println("Lỗi khi gửi FCM: " + e.getMessage());
        }
    }
}