document.addEventListener("DOMContentLoaded", async function () {
  // Khởi tạo hiệu ứng và icon
  if (window.AOS) AOS.init({ duration: 800, once: true });
  if (window.lucide) lucide.createIcons();

  // ===== Lấy transactionId hoặc orderId =====
  const urlParams = new URLSearchParams(window.location.search);
  const transactionId = urlParams.get("transactionId") || urlParams.get("orderId");

  if (!transactionId) {
    console.error("❌ Không tìm thấy transactionId hoặc orderId trong URL!");
    return;
  }

  console.log("Transaction ID:", transactionId);

  try {
    // ===== Gọi API lấy thông tin thanh toán =====
    const res = await fetch(`http://localhost:8083/api/payments/info/${transactionId}`);
    if (!res.ok) throw new Error(`Lỗi lấy thông tin thanh toán: ${res.status}`);

    const data = await res.json();
    console.log("📦 Dữ liệu thanh toán:", data);

    // ===== Hiển thị thông tin khách hàng =====
    document.getElementById("customerInfo").classList.remove("hidden");
    document.getElementById("cName").textContent = data.fullName || "Không có";
    document.getElementById("cPhone").textContent = data.phone || "Không có";
    document.getElementById("cEmail").textContent = data.email || "Không có";
    document.getElementById("cAddress").textContent = data.address || "Không có";

    const method = (data.method || "").toLowerCase();
    document.getElementById("cMethod").textContent =
      method === "vnpay" ? "VNPay" :
      method === "momo" ? "MoMo" :
      method === "evwallet" ? "EV Wallet" : "Khác";

    const note = document.getElementById("extraNote");
    note.classList.remove("hidden");

    // ===== Xử lý giao diện theo trạng thái =====
    const successCard = document.getElementById("paymentSuccess");
    const failedCard = document.getElementById("paymentFailed");

    switch (data.status?.toUpperCase()) {
      case "SUCCESS":
        successCard.classList.remove("hidden");
        failedCard.classList.add("hidden");
        note.innerHTML = `
          <p class="text-green-600 font-medium">
            ✅ Bạn đã thanh toán thành công qua ${data.method}.
          </p>
          <p class="text-gray-700 mt-2">
            Vui lòng ký hợp đồng số hóa để hoàn tất giao dịch.
          </p>
        `;

        // ✅ Nếu là thanh toán đơn hàng, chuyển đến trang ký hợp đồng sau 3s
        if (data.type === "order") {
          setTimeout(() => {
            window.location.href = `contract.html?transactionId=${transactionId}`;
          }, 3000);
        }
        break;

      case "CANCELED":
        failedCard.classList.remove("hidden");
        successCard.classList.add("hidden");
        note.innerHTML = `
          <p class="text-yellow-600 font-medium">
            ⚠️ Bạn đã hủy giao dịch. Không có khoản tiền nào bị trừ.
          </p>
        `;
        break;

      case "FAILED":
        failedCard.classList.remove("hidden");
        successCard.classList.add("hidden");
        note.innerHTML = `
          <p class="text-red-600 font-medium">
            ❌ Giao dịch thất bại. Vui lòng thử lại hoặc chọn phương thức khác.
          </p>
        `;
        break;

      case "PENDING":
      default:
        failedCard.classList.remove("hidden");
        successCard.classList.add("hidden");
        note.innerHTML = `
          <p class="text-yellow-600 font-medium">
            ⏳ Giao dịch đang được xử lý. Vui lòng kiểm tra lại sau.
          </p>
        `;
        break;
    }
  } catch (error) {
    console.error("❌ Không thể tải thông tin thanh toán:", error);
    const failedCard = document.getElementById("paymentFailed");
    const note = document.getElementById("extraNote");
    failedCard.classList.remove("hidden");
    note.innerHTML = `
      <p class="text-red-600 font-medium">
        Có lỗi xảy ra khi tải dữ liệu giao dịch. Vui lòng thử lại sau.
      </p>
    `;
  }
});
