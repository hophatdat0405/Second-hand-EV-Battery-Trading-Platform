// ============================================================
// ✅ wallet.js - Đã nâng cấp giao diện & đồng bộ User
// ============================================================

document.addEventListener("DOMContentLoaded", async () => {
  const API_BASE = "http://localhost:9000/api/wallet";
  const TX_API = `${API_BASE}/transactions/user`;
  const BAL_API = `${API_BASE}/user`;

  // Cấu hình Toast thông báo góc phải
  const Toast = Swal.mixin({
    toast: true,
    position: "top-end",
    showConfirmButton: false,
    timer: 2000,
    timerProgressBar: true,
  });

  // ============================================================
  // 🧠 1️⃣ Lấy & Đồng bộ User (Logic giữ nguyên)
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
    }
    
    // Đồng bộ ngược lại
    if (currentUser && !window.name) {
      window.name = JSON.stringify(currentUser);
    }
  } catch (err) {
    console.warn("⚠️ [wallet.js] Không thể đọc user:", err);
  }

  // ============================================================
  // 👤 2️⃣ Xác định userId & Kiểm tra đăng nhập
  // ============================================================

  const userId = Number(
    currentUser?.userId ||
    currentUser?.id ||
    localStorage.getItem("userId")
  );

  console.log("👤 [wallet.js] userId hiện tại:", userId);

  if (!userId || isNaN(userId) || userId <= 0) {
    await Swal.fire({
      icon: "warning",
      title: "Chưa đăng nhập",
      text: "Bạn cần đăng nhập để xem ví điện tử.",
      confirmButtonText: "Đăng nhập ngay",
      allowOutsideClick: false
    });
    window.location.href = "login.html";
    return;
  }

  // ============================================================
  // 🛠 3️⃣ Helper Functions (Format tiền & Ngày)
  // ============================================================

  const formatMoney = (amount) => {
    return Number(amount).toLocaleString("vi-VN") + " ₫";
  };

  const formatTime = (dateString) => {
    if (!dateString) return "-";
    try {
      return new Date(dateString).toLocaleString("vi-VN", {
        year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit', hour12: false
      });
    } catch { return dateString; }
  };

  // ============================================================
  // 🪙 4️⃣ Load Số Dư
  // ============================================================

  async function loadBalance() {
    const balanceEl = document.getElementById("balance");
    if (!balanceEl) return;

    // Loading state nhẹ
    balanceEl.innerHTML = `<span style="font-size:0.8em; color:gray;">...</span>`;

    try {
      const res = await fetch(`${BAL_API}/${userId}`);
      if (!res.ok) throw new Error("Lỗi API");
      
      const balanceText = await res.text();
      const balance = parseFloat(balanceText) || 0;

      balanceEl.textContent = formatMoney(balance);
      balanceEl.style.color = "#2ecc71"; // Màu xanh lá
    } catch (err) {
      console.error("❌ [wallet.js] Lỗi tải số dư:", err);
      balanceEl.innerHTML = `<span style="color:red; font-size:0.8em;">Lỗi</span>`;
    }
  }

  // ============================================================
  // 📜 5️⃣ Load Lịch Sử Giao Dịch
  // ============================================================

  async function loadTransactions() {
    const tbody = document.getElementById("txBody");
    if (!tbody) return;

    // Loading state cho bảng
    tbody.innerHTML = `<tr><td colspan="4" style="text-align:center; padding:20px; color:gray;">Đang tải lịch sử giao dịch...</td></tr>`;

    try {
      const res = await fetch(`${TX_API}/${userId}`);
      if (!res.ok) throw new Error("Lỗi API");
      
      const data = await res.json();

      if (!data || data.length === 0) {
        tbody.innerHTML = `<tr><td colspan="4" style="text-align:center; padding:20px;">Chưa có giao dịch nào.</td></tr>`;
        return;
      }

      // Render bảng
      tbody.innerHTML = data.map(tx => {
        const isCredit = tx.txType === "CREDIT";
        const typeLabel = isCredit ? "Nhận tiền" : "Thanh toán";
        const typeClass = isCredit ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700";
        const amountSign = isCredit ? "+" : "-";
        const amountColor = isCredit ? "color: #2ecc71;" : "color: #e74c3c;";

        return `
          <tr>
            <td style="white-space: nowrap;">${formatTime(tx.createdAt)}</td>
            <td>
              <span style="padding: 4px 8px; border-radius: 4px; font-size: 0.85em; font-weight: bold;" 
                    class="${typeClass}">
                ${typeLabel}
              </span>
            </td>
            <td style="font-weight: bold; ${amountColor}">
              ${amountSign} ${formatMoney(tx.amount).replace(" ₫", "")}
            </td>
            <td style="color: #555;">${tx.description || "-"}</td>
          </tr>
        `;
      }).join("");

    } catch (err) {
      console.error("❌ [wallet.js] Lỗi tải lịch sử:", err);
      tbody.innerHTML = `<tr><td colspan="4" style="text-align:center; color:red; padding:20px;">Không thể tải dữ liệu lịch sử.</td></tr>`;
    }
  }

  // ============================================================
  // 🔁 6️⃣ Hàm Làm Mới Tổng Hợp
  // ============================================================

  async function refreshData() {
    // Gọi song song 2 API để tiết kiệm thời gian
    await Promise.allSettled([loadBalance(), loadTransactions()]);
  }

  // ============================================================
  // 🎮 7️⃣ Sự Kiện Buttons
  // ============================================================

  // Nút Refresh
  const refreshBtn = document.getElementById("refreshBtn");
  if (refreshBtn) {
    refreshBtn.addEventListener("click", async () => {
      refreshBtn.disabled = true;
      // Hiệu ứng xoay icon nếu có
      const icon = refreshBtn.querySelector("i") || refreshBtn.querySelector("span");
      if(icon) icon.style.transition = "transform 0.5s";
      if(icon) icon.style.transform = "rotate(360deg)";

      await refreshData();
      
      Toast.fire({ icon: "success", title: "Đã cập nhật ví" });
      
      if(icon) icon.style.transform = "none";
      refreshBtn.disabled = false;
    });
  }

  // Nút Nạp tiền
  const depositBtn = document.getElementById("depositBtn");
  if (depositBtn) {
    depositBtn.addEventListener("click", () => {
      window.location.href = "deposit.html";
    });
  }

  // ============================================================
  // 🚀 8️⃣ Khởi chạy lần đầu
  // ============================================================
  await refreshData();
});