document.addEventListener("DOMContentLoaded", async () => {
  if (window.lucide) lucide.createIcons();
  if (window.AOS) AOS.init({ duration: 800, once: true });

  const API_BASE = "http://localhost:8089/api/wallet";
  const balanceEl = document.getElementById("platformBalance");
  const platformTable = document.getElementById("platformTxTable");
  const userTable = document.getElementById("userTxTable");

  // ✅ 1️⃣ Lấy số dư ví sàn
  try {
    const res = await fetch(`${API_BASE}/platform/balance`);
    const data = await res.json(); // ← Lấy JSON thay vì text
    const balance = data.balance ?? data?.data?.balance ?? 0;
    balanceEl.textContent = `${Number(balance).toLocaleString("vi-VN")} đ`;
  } catch (e) {
    balanceEl.textContent = "❌ Không lấy được số dư ví sàn";
    console.error(e);
  }


  // ✅ 2️⃣ Lịch sử giao dịch ví sàn
  try {
    const res = await fetch(`${API_BASE}/platform/transactions`);
    const data = await res.json();
    if (!data || data.length === 0) {
      platformTable.innerHTML = `<tr><td colspan="4" class="text-center text-gray-400 py-4">Không có giao dịch nào</td></tr>`;
    } else {
      platformTable.innerHTML = data.map(tx => `
        <tr>
          <td>${new Date(tx.createdAt).toLocaleString("vi-VN")}</td>
          <td>${tx.txType}</td>
          <td class="${tx.txType === 'CREDIT' ? 'amount-credit' : 'amount-debit'}">
            ${Number(tx.amount).toLocaleString("vi-VN")} đ
          </td>
          <td>${tx.description || '-'}</td>
        </tr>
      `).join("");
    }
  } catch (e) {
    console.error("Lỗi tải lịch sử ví sàn:", e);
    platformTable.innerHTML = `<tr><td colspan="4" class="text-center text-red-500 py-4">Lỗi tải dữ liệu</td></tr>`;
  }

  // ✅ 3️⃣ Lịch sử giao dịch của tất cả ví người dùng
  try {
    const res = await fetch(`${API_BASE}/all/transactions`);
    const data = await res.json();
    if (!data || data.length === 0) {
      userTable.innerHTML = `<tr><td colspan="5" class="text-center text-gray-400 py-4">Chưa có giao dịch nào</td></tr>`;
    } else {
      userTable.innerHTML = data.map(tx => `
        <tr>
          <td>${tx.walletRefId}</td>
          <td>${new Date(tx.createdAt).toLocaleString("vi-VN")}</td>
          <td>${tx.txType}</td>
          <td class="${tx.txType === 'CREDIT' ? 'amount-credit' : 'amount-debit'}">
            ${Number(tx.amount).toLocaleString("vi-VN")} đ
          </td>
          <td>${tx.description || '-'}</td>
        </tr>
      `).join("");
    }
  } catch (e) {
    console.error("Lỗi tải giao dịch user:", e);
    userTable.innerHTML = `<tr><td colspan="5" class="text-center text-red-500 py-4">Lỗi tải dữ liệu</td></tr>`;
  }
});
