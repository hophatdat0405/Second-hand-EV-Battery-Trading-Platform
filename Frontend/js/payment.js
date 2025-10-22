// === JS cho trang thanh toán (tích hợp backend Spring Boot + Cart thật) ===
document.addEventListener("DOMContentLoaded", async function () {
  AOS.init({ duration: 800, once: true });
  lucide.createIcons();

  const form = document.getElementById("paymentForm");
  const orderSummary = document.querySelector(".order-summary");

  // ====== Bước 1: Lấy thông tin giỏ hàng từ backend ======
  const cartId = localStorage.getItem("cartId") || 1; // demo mặc định 1

  try {
    const res = await fetch(`http://localhost:8080/api/cart/${cartId}`);
    if (!res.ok) throw new Error("❌ Không thể lấy dữ liệu giỏ hàng!");
    const cart = await res.json();

    // ✅ Hiển thị sản phẩm ra trang thanh toán
    orderSummary.innerHTML = `
      <h3 class="font-semibold text-lg mb-2">Thông tin đơn hàng</h3>
      <div class="flex justify-between text-gray-700">
        <span>${cart.productName}</span>
        <span class="font-bold text-green-600">${cart.price.toLocaleString()} đ</span>
      </div>
      <div class="flex justify-between text-gray-700 mt-1">
        <span>Số lượng</span>
        <span>${cart.quantity}</span>
      </div>
      <div class="flex justify-between text-lg font-bold mt-3">
        <span>Tổng cộng</span>
        <span class="text-green-600">${(cart.price * cart.quantity).toLocaleString()} đ</span>
      </div>
    `;
  } catch (err) {
    console.error("⚠️ Lỗi khi tải giỏ hàng:", err);
    orderSummary.innerHTML = `
      <p class="text-red-600">Không thể tải dữ liệu giỏ hàng. Vui lòng thử lại!</p>
    `;
  }

  // ====== Bước 2: Khi người dùng nhấn "Xác nhận thanh toán" ======
  form.addEventListener("submit", function (e) {
    e.preventDefault();

    const name = document.getElementById("name").value.trim();
    const phone = document.getElementById("phone").value.trim();
    const email = document.getElementById("email").value.trim();
    const address = document.getElementById("address").value.trim();
    const method = document.querySelector("input[name='payment']:checked");

    if (!name || !phone || !email || !address) {
      alert("⚠️ Vui lòng nhập đầy đủ thông tin khách hàng!");
      return;
    }
    if (!method) {
      alert("⚠️ Vui lòng chọn phương thức thanh toán!");
      return;
    }

    // ✅ Lưu tạm thông tin khách hàng & giỏ hàng để dùng ở bước sau
    localStorage.setItem("cartId", cartId);
    localStorage.setItem("cName", name);
    localStorage.setItem("cPhone", phone);
    localStorage.setItem("cEmail", email);
    localStorage.setItem("cAddress", address);
    localStorage.setItem("cMethod", method.value);

    // ====== Nếu chọn thanh toán online ======
    if (method.value === "online") {
      alert("💳 Vui lòng chọn cổng thanh toán (MOMO hoặc VNPAY).");
      window.location.href = "online_payment.html";
      return;
    }
  });
});
