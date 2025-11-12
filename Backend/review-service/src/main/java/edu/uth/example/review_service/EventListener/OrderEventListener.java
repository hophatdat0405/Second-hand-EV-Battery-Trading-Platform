// package edu.uth.reviewservice.EventListener;

// import org.springframework.amqp.rabbit.annotation.RabbitListener;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Component;

// **
//  * TODO: KHI ORDER-SERVICE SẴN SÀNG:
//  * 1. Bỏ comment "@Component"
//  * 2. Bỏ comment "@RabbitListener"
//  * 3. (Và xóa API fake /admin/create-fake-transaction trong ReviewController)
//  **
// @Component
public class OrderEventListener {

    // @Autowired
    // private ReviewService reviewService;

    // **
    //  * Lắng nghe sự kiện từ OrderService khi giao dịch hoàn tất.
    //  * "order.completed.queue" là hàng đợi (Queue) mà bạn
    //  * sẽ bind (liên kết) với exchange của OrderService.
    //  **
    // @RabbitListener(queues = "order.completed.queue")
    // public void handleOrderCompleted(OrderCompletedEventDTO dto) {
    //     System.out.println("Nhận được sự kiện OrderCompleted cho listing: " + dto.getListingId());
    //     try {
    //         reviewService.createReviewableTransaction(dto);
    //     } catch (Exception e) {
    //         System.err.println("Lỗi xử lý sự kiện order completed: " + e.getMessage());
    //         // (Cần xử lý retry hoặc dead-letter-queue sau)
    //     }
    // }
}