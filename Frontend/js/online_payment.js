document.addEventListener("DOMContentLoaded", function () {
  if (window.AOS) AOS.init({ duration: 800, once: true });
  if (window.lucide) lucide.createIcons();

  const confirmBtn = document.getElementById("confirmOnlinePay");

  confirmBtn.addEventListener("click", async function () {
    const cartIdsStr = localStorage.getItem("cartIds");
    const totalStr = localStorage.getItem("total");
    const name = localStorage.getItem("cName");
    const phone = localStorage.getItem("cPhone");
    const email = localStorage.getItem("cEmail");
    const address = localStorage.getItem("cAddress");
    const userId = localStorage.getItem("userId"); // ✅ LẤY USERID
    const selectedMethod = document.querySelector('input[name="method"]:checked');

    if (!cartIdsStr || !name || !phone || !email || !address) {
      alert("⚠️ Thiếu thông tin khách hàng hoặc giỏ hàng! Vui lòng quay lại trang thanh toán.");
      window.location.href = "payment.html";
      return;
    }
    if (!selectedMethod) {
      alert("⚠️ Vui lòng chọn phương thức thanh toán (MoMo hoặc VNPay)!");
      return;
    }

    const cartIds = cartIdsStr.split(",").map(id => parseInt(id.trim()));
    const totalAmount = parseFloat(totalStr) || 0;
    const method = selectedMethod.value.toLowerCase();

    if (!userId) {
      alert("⚠️ Không tìm thấy userId, vui lòng đăng nhập lại!");
      window.location.href = "/login.html";
      return;
    }

    alert(`💳 Đang khởi tạo giao dịch ${method.toUpperCase()}...`);

    try {
      const res = await fetch("http://localhost:8083/api/payments/create", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          type: "order",
          cartIds: cartIds,
          totalAmount: totalAmount,
          paymentMethod: method,
          userId: parseInt(userId), // ✅ THÊM DÒNG NÀY
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

      if (data && data.redirectUrl) {
        localStorage.setItem("transactionId", data.transactionId);
        window.location.href = data.redirectUrl;
      } else {
        alert("❌ Không nhận được URL thanh toán từ server!");
      }
    } catch (error) {
      console.error("🚨 Lỗi khi tạo giao dịch:", error);
      alert("❌ Không thể kết nối đến máy chủ! Kiểm tra server backend ở cổng 8083.");
    }
  });
});
