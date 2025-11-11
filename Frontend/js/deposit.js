// ============================================================
// ✅ deposit.js - Đồng bộ userId giữa localhost và 127.0.0.1
// ============================================================

const API_BASE = "http://localhost:8083/api/payments"; // transaction-service

// ============================================================
// 🧠 1️⃣ Lấy user hiện tại từ localStorage hoặc window.name
// ============================================================

let currentUser = null;

try {
  // Ưu tiên lấy từ localStorage
  const stored = localStorage.getItem("user");

  if (stored) {
    currentUser = JSON.parse(stored);
  } else if (window.name && window.name.startsWith("{")) {
    // Nếu window.name chứa JSON hợp lệ → đồng bộ lại localStorage
    try {
      const parsed = JSON.parse(window.name);
      currentUser = parsed;
      localStorage.setItem("user", window.name);
      if (parsed.userId || parsed.id)
        localStorage.setItem("userId", parsed.userId || parsed.id);
      console.log("✅ [deposit.js] Đồng bộ user từ window.name:", parsed);
    } catch (err) {
      console.warn("⚠️ [deposit.js] window.name không hợp lệ, reset:", err);
      window.name = ""; // tránh lỗi JSON parse lần sau
    }
  }
} catch (err) {
  console.warn("⚠️ [deposit.js] Không thể đọc user:", err);
}

// ============================================================
// 👤 2️⃣ Xác định userId hiện tại (ưu tiên theo thứ tự)
// ============================================================

const USER_ID = Number(
  currentUser?.userId ||
  currentUser?.id ||
  localStorage.getItem("userId")
);

console.log("👤 [deposit.js] USER_ID hiện tại:", USER_ID);

// ============================================================
// 🚫 3️⃣ Nếu chưa đăng nhập → chuyển về login
// ============================================================

if (!USER_ID || isNaN(USER_ID) || USER_ID <= 0) {
  alert("⚠️ Bạn cần đăng nhập trước khi nạp tiền!");
  window.location.href = "login.html";
}

// ============================================================
// 💳 4️⃣ Bắt sự kiện form submit để tạo giao dịch thanh toán
// ============================================================

document.getElementById("depositForm").addEventListener("submit", async (e) => {
  e.preventDefault();

  const amount = document.getElementById("amount").value.trim();
  const method = document.getElementById("method").value.trim();
  const msg = document.getElementById("message");
  const btn = document.getElementById("submitBtn");

  // === Kiểm tra dữ liệu đầu vào ===
  if (!amount || isNaN(amount) || parseFloat(amount) < 1000) {
    msg.textContent = "⚠️ Vui lòng nhập số tiền hợp lệ (tối thiểu 1.000đ).";
    msg.className = "message error";
    return;
  }

  if (!method) {
    msg.textContent = "⚠️ Vui lòng chọn phương thức thanh toán.";
    msg.className = "message error";
    return;
  }

  // === Hiển thị trạng thái đang xử lý ===
  btn.disabled = true;
  msg.textContent = "⏳ Đang tạo giao dịch...";
  msg.className = "message loading";

  try {
    const payload = {
      type: "deposit",              // 🧩 loại giao dịch
      userId: USER_ID,              // 🧩 id người nạp
      amount: parseFloat(amount),   // 🧩 số tiền
      paymentMethod: method         // 🧩 phương thức (vnpay/momo)
    };

    console.log("📤 [deposit.js] Gửi yêu cầu nạp tiền:", payload);

    const res = await fetch(`${API_BASE}/create`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    if (!res.ok)
      throw new Error(`Không thể tạo giao dịch (HTTP ${res.status}).`);

    const data = await res.json();
    console.log("✅ [deposit.js] Phản hồi từ backend:", data);

    if (data.redirectUrl) {
      msg.textContent = "✅ Đang chuyển tới cổng thanh toán...";
      msg.className = "message success";
      setTimeout(() => {
        window.location.href = data.redirectUrl;
      }, 800);
    } else {
      msg.textContent = "❌ Không nhận được URL thanh toán từ máy chủ.";
      msg.className = "message error";
      btn.disabled = false;
    }
  } catch (err) {
    console.error("🚨 [deposit.js] Lỗi:", err);
    msg.textContent = "❌ Lỗi khi tạo giao dịch: " + err.message;
    msg.className = "message error";
    btn.disabled = false;
  }
});
