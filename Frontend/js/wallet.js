// ============================================================
// ✅ wallet.js - đồng bộ userId giữa localhost và 127.0.0.1
// ============================================================

const API_BASE = "http://localhost:8089/api/wallet"; // ⚙️ URL của wallet-service
const TX_API = `${API_BASE}/transactions/user`;
const BAL_API = `${API_BASE}/user`;

// ============================================================
// 🧠 1️⃣ Lấy user từ localStorage hoặc window.name (đồng bộ host)
// ============================================================

let currentUser = null;

try {
  const stored = localStorage.getItem("user");

  if (stored) {
    currentUser = JSON.parse(stored);
  } else if (window.name && window.name.startsWith("{")) {
    try {
      currentUser = JSON.parse(window.name);
      localStorage.setItem("user", window.name);
      if (currentUser.userId || currentUser.id) {
        localStorage.setItem("userId", currentUser.userId || currentUser.id);
      }
      console.log("✅ [wallet.js] Đồng bộ user từ window.name:", currentUser);
    } catch (err) {
      console.warn("⚠️ [wallet.js] window.name không hợp lệ, reset:", err);
      window.name = "";
    }
  } else {
    console.log("⚠️ [wallet.js] Không tìm thấy user trong localStorage hoặc window.name");
  }

  // Đồng bộ ngược lại (đảm bảo host khác cũng đọc được)
  if (currentUser && !window.name) {
    window.name = JSON.stringify(currentUser);
  }
} catch (err) {
  console.warn("⚠️ [wallet.js] Không thể đọc user:", err);
}

// ============================================================
// 👤 2️⃣ Xác định userId hiện tại (ưu tiên userId hợp lệ)
// ============================================================

const userId = Number(
  currentUser?.userId ||
  currentUser?.id ||
  localStorage.getItem("userId")
);

console.log("👤 [wallet.js] userId hiện tại:", userId);

// ============================================================
// 🚫 3️⃣ Nếu chưa đăng nhập → chuyển về login
// ============================================================

if (!userId || isNaN(userId) || userId <= 0) {
  alert("⚠️ Bạn cần đăng nhập trước!");
  window.location.href = "login.html";
}

// ============================================================
// 🪙 4️⃣ Load số dư ví
// ============================================================

async function loadBalance() {
  try {
    const res = await fetch(`${BAL_API}/${userId}`);
    if (!res.ok) throw new Error("Không thể lấy số dư ví");
    const balance = await res.text();

    const balanceEl = document.getElementById("balance");
    if (balanceEl) {
      balanceEl.textContent = Number(balance).toLocaleString("vi-VN") + " ₫";
    }
  } catch (err) {
    console.error("❌ [wallet.js] Lỗi tải số dư:", err);
    const balanceEl = document.getElementById("balance");
    if (balanceEl) balanceEl.textContent = "❌ Lỗi tải số dư";
  }
}

// ============================================================
// 📜 5️⃣ Load lịch sử giao dịch
// ============================================================

async function loadTransactions() {
  try {
    const res = await fetch(`${TX_API}/${userId}`);
    if (!res.ok) throw new Error("Không thể tải lịch sử giao dịch");
    const data = await res.json();

    const tbody = document.getElementById("txBody");
    if (!tbody) return;

    tbody.innerHTML = "";

    if (!data || data.length === 0) {
      tbody.innerHTML = `<tr><td colspan="4">Chưa có giao dịch</td></tr>`;
      return;
    }

    data.forEach((tx) => {
      const tr = document.createElement("tr");
      const type = tx.txType === "CREDIT" ? "Nhận" : "Trừ";
      const amount =
        (tx.txType === "CREDIT" ? "+" : "-") +
        Number(tx.amount).toLocaleString("vi-VN") +
        " ₫";

      tr.innerHTML = `
        <td>${new Date(tx.createdAt).toLocaleString("vi-VN")}</td>
        <td class="${tx.txType.toLowerCase()}">${type}</td>
        <td>${amount}</td>
        <td>${tx.description || "-"}</td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {
    console.error("❌ [wallet.js] Lỗi tải lịch sử:", err);
    const tbody = document.getElementById("txBody");
    if (tbody)
      tbody.innerHTML = `<tr><td colspan="4">❌ Lỗi tải dữ liệu</td></tr>`;
  }
}

// ============================================================
// 🔁 6️⃣ Làm mới dữ liệu
// ============================================================

document.getElementById("refreshBtn")?.addEventListener("click", () => {
  loadBalance();
  loadTransactions();
});

// ============================================================
// 💳 7️⃣ Nút “Nạp tiền” → chuyển sang deposit.html
// ============================================================

document.getElementById("depositBtn")?.addEventListener("click", () => {
  window.location.href = "deposit.html";
});

// ============================================================
// 🚀 8️⃣ Khi trang tải xong
// ============================================================

document.addEventListener("DOMContentLoaded", () => {
  loadBalance();
  loadTransactions();
});
