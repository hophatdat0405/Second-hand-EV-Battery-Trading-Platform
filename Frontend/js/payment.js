// === JS cho trang thanh toán (tích hợp Spring Boot + Cart + Ví EV) ===
document.addEventListener("DOMContentLoaded", async function () {
  if (window.AOS) AOS.init({ duration: 800, once: true });
  if (window.lucide) lucide.createIcons();

  const form = document.getElementById("paymentForm");
  const orderSummary = document.querySelector(".order-summary");
  const PAYMENT_API = "http://localhost:8083/api/payments/create";

  // ====== Lấy cartIds & tổng tiền từ URL ======
  const urlParams = new URLSearchParams(window.location.search);
  const cartIdsParam = urlParams.get("cartIds");
  const totalParam = urlParams.get("total") || "0";

  const cartIds = cartIdsParam
    ? cartIdsParam.split(",").map((id) => id.trim()).filter(Boolean)
    : [];

  if (cartIds.length === 0) {
    orderSummary.innerHTML = `<p class="text-red-600">Không có sản phẩm nào được chọn để thanh toán!</p>`;
    return;
  }

  // ====== Hiển thị danh sách sản phẩm trong giỏ ======
  let itemsHtml = "";
  try {
    for (const id of cartIds) {
      const res = await fetch(`http://localhost:8082/api/carts/${id}`);
      if (!res.ok) continue;
      const cart = await res.json();

      itemsHtml += `
        <div class="flex justify-between text-gray-700">
          <span>${cart.productName || cart.productname}</span>
          <span class="font-bold text-green-600">
            ${Number(cart.price).toLocaleString("vi-VN")} đ
          </span>
        </div>
      `;
    }

    const totalClean = totalParam.replace(/[^\d]/g, "");
    const totalFormatted = Number(totalClean).toLocaleString("vi-VN") + " đ";

    orderSummary.innerHTML = `
      <h3 class="font-semibold text-lg mb-2">Thông tin đơn hàng</h3>
      <div class="space-y-2">${itemsHtml}</div>
      <div class="flex justify-between text-lg font-bold mt-4 border-t pt-3">
        <span>Tổng cộng</span>
        <span class="text-green-600">${totalFormatted}</span>
      </div>
    `;
  } catch (err) {
    console.error("⚠️ Lỗi khi tải giỏ hàng:", err);
    orderSummary.innerHTML = `<p class="text-red-600">Không thể tải dữ liệu giỏ hàng.</p>`;
  }

  // ====== Khi người dùng nhấn "Xác nhận thanh toán" ======
  form.addEventListener("submit", async function (e) {
    e.preventDefault();

    const name = document.getElementById("name").value.trim();
    const phone = document.getElementById("phone").value.trim();
    const email = document.getElementById("email").value.trim();
    const address = document.getElementById("address").value.trim();
    const methodEl = document.querySelector("input[name='payment']:checked");

    if (!name || !phone || !email || !address) {
      alert("⚠️ Vui lòng nhập đầy đủ thông tin khách hàng!");
      return;
    }
    if (!methodEl) {
      alert("⚠️ Vui lòng chọn phương thức thanh toán!");
      return;
    }

    const method = methodEl.value.toLowerCase();
    const totalAmount = Number(totalParam.replace(/[^\d]/g, ""));
    const user = JSON.parse(localStorage.getItem("user"));
    const userId = user?.userId || user?.id;

    if (!userId) {
      alert("⚠️ Bạn cần đăng nhập trước khi thanh toán!");
      window.location.href = "login.html";
      return;
    }

    // ✅ Lưu thông tin để các trang sau dùng
    localStorage.setItem("cartIds", cartIds.join(","));
    localStorage.setItem("cName", name);
    localStorage.setItem("cPhone", phone);
    localStorage.setItem("cEmail", email);
    localStorage.setItem("cAddress", address);
    localStorage.setItem("cMethod", method);
    localStorage.setItem("total", totalAmount);

    // ====== 1️⃣ Nếu chọn Ví EV ======
    if (method === "ev-wallet") {
      try {
        const payRes = await fetch(PAYMENT_API, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            cartIds,
            totalAmount,
            paymentMethod: "evwallet",
            customer: { fullName: name, phone, email, address },
            type: "order",
            userId,
            amount: totalAmount,
          }),
        });

        if (!payRes.ok) throw new Error("Không thể tạo giao dịch Ví EV");
        const payData = await payRes.json();
        console.log("✅ Payment EV Wallet:", payData);

        if (payData.status === "SUCCESS") {
          window.location.href = payData.redirectUrl;
        } else {
          alert("Thanh toán ví EV không thành công: " + payData.status);
        }
      } catch (err) {
        console.error("❌ Lỗi Ví EV:", err);
        alert("Thanh toán ví EV thất bại!");
      }
      return;
    }



    // ====== 2️⃣ Nếu chọn thanh toán online ======
    if (method === "online") {
      alert("💳 Chuyển sang bước chọn cổng thanh toán (MoMo hoặc VNPay).");
      window.location.href = `online_payment.html?cartIds=${cartIds.join(",")}&total=${encodeURIComponent(
        totalParam
      )}`;
      return;
    }

    // ====== 3️⃣ Nếu chọn COD ======
    if (method === "cod") {
      alert("✅ Đặt hàng thành công! Vui lòng thanh toán khi nhận hàng.");
      window.location.href = "payment_success.html?status=SUCCESS";
      return;
    }
  });
});
