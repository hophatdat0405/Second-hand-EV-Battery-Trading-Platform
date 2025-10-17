package local.Second_hand_EV_Battery_Trading_Platform.service;

import java.util.Map;

import local.Second_hand_EV_Battery_Trading_Platform.entity.Payment;
import local.Second_hand_EV_Battery_Trading_Platform.model.PaymentRequest;
import local.Second_hand_EV_Battery_Trading_Platform.model.PaymentResponse;

/**
 * Service chịu trách nhiệm xử lý toàn bộ luồng thanh toán:
 *  - Tạo giao dịch mới (VNPay / MoMo)
 *  - Xử lý callback từ cổng thanh toán
 *  - Lấy thông tin giao dịch dựa theo transactionId
 */
public interface PaymentService {

    /**
     * Tạo giao dịch thanh toán mới, lưu vào DB và trả về URL redirect đến cổng thanh toán.
     * @param request đối tượng PaymentRequest từ frontend (bao gồm cartId, customer, paymentMethod)
     * @return PaymentResponse (trạng thái + URL redirect)
     */
    PaymentResponse createPayment(PaymentRequest request);

    /**
     * Xử lý dữ liệu callback trả về từ VNPay hoặc MoMo.
     * @param data dữ liệu callback (có thể đến từ query params hoặc JSON body)
     */
    void handleCallback(Map<String, Object> data);

    /**
     * Truy vấn chi tiết thanh toán theo mã transactionId.
     * @param transactionId mã giao dịch duy nhất (UUID)
     * @return đối tượng Payment (bao gồm cả thông tin khách hàng & giỏ hàng)
     */
    Payment findByTransactionId(String transactionId);
}
