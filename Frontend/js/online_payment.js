// === JS cho trang thanh toán online (VNPay / MoMo demo hoàn chỉnh) ===
document.addEventListener("DOMContentLoaded", function () {
  AOS.init({ duration: 800, once: true });
  lucide.createIcons();

  const confirmBtn = document.getElementById("confirmOnlinePay");

  confirmBtn.addEventListener("click", async function () {
    const cartId = localStorage.getItem("cartId");
    const name = localStorage.getItem("cName");
    const phone = localStorage.getItem("cPhone");
    const email = localStorage.getItem("cEmail");
    const address = localStorage.getItem("cAddress");
    const selectedMethod = document.querySelector('input[name="method"]:checked');

    // ===== 1️⃣ Kiểm tra dữ liệu =====
    if (!cartId || !name || !phone || !email || !address) {
      alert("⚠️ Thiếu thông tin khách hàng! Vui lòng quay lại trang thanh toán.");
      window.location.href = "payment.html";
      return;
    }
    if (!selectedMethod) {
      alert("⚠️ Vui lòng chọn phương thức thanh toán (MoMo hoặc VNPay)!");
      return;
    }

    const method = selectedMethod.value.toLowerCase(); // momo hoặc vnpay
    alert(`💳 Đang khởi tạo giao dịch ${method.toUpperCase()}...`);

    // ===== 2️⃣ Gọi API backend để tạo giao dịch =====
    try {
      const res = await fetch("http://localhost:8080/api/payments/create", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          cartId: cartId,
          paymentMethod: method,
          customer: {
            fullName: name,
            phone: phone,
            email: email,
            address: address
          }
        })
      });

      if (!res.ok) throw new Error("❌ Lỗi phản hồi từ server!");
      const data = await res.json();
      console.log("📦 Phản hồi từ backend:", data);

      // ===== 3️⃣ Nếu có redirectUrl thì chuyển hướng đến cổng thanh toán =====
      if (data && data.redirectUrl) {
        localStorage.setItem("transactionId", data.transactionId);

        // ⚙️ Redirect trực tiếp sang cổng sandbox của VNPay hoặc MoMo
        window.location.href = data.redirectUrl;
      } else {
        alert("❌ Không nhận được URL thanh toán từ server!");
      }
    } catch (error) {
      console.error("🚨 Lỗi khi tạo giao dịch:", error);
      alert("❌ Không thể kết nối đến máy chủ! Kiểm tra server backend ở cổng 8080.");
    }
  });
});
