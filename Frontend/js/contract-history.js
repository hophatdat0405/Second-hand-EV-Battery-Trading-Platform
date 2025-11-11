document.addEventListener("DOMContentLoaded", async () => {
  const userId =
    localStorage.getItem("userId") ||
    new URLSearchParams(window.location.search).get("userId");

  if (!userId) {
    alert("⚠️ Bạn cần đăng nhập để xem lịch sử hợp đồng.");
    window.location.href = "/login.html";
    return;
  }

  console.log("👤 Đang tải danh sách hợp đồng của userId:", userId);

  const tbody = document.querySelector("#historyTable tbody");

  try {
    const res = await fetch(`http://localhost:8081/api/contracts/user/${userId}`);
    if (!res.ok) throw new Error(`Lỗi khi gọi API: ${res.status}`);
    const data = await res.json();

    if (data.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" style="text-align:center;">Chưa có hợp đồng nào.</td></tr>`;
      return;
    }

    tbody.innerHTML = data
      .map(
        (c) => `
        <tr>
          <td>#${c.id}</td>
          <td>${c.productName || "(Không có dữ liệu)"}</td>
          <td>${c.totalPrice ? Number(c.totalPrice).toLocaleString("vi-VN") + " đ" : "-"}</td>
          <td>${c.signedAt ? new Date(c.signedAt).toLocaleDateString("vi-VN") : "-"}</td>
          <td>
            ${
              c.pdfUrl
                ? `<a href="${c.pdfUrl}" target="_blank" class="btn-view">Xem PDF</a>`
                : `<span class="text-muted">Chưa có</span>`
            }
          </td>
        </tr>`
      )
      .join("");
  } catch (err) {
    console.error("❌ Lỗi khi tải lịch sử hợp đồng:", err);
    tbody.innerHTML = `<tr><td colspan="5" style="color:red;text-align:center;">Không thể tải dữ liệu từ server.</td></tr>`;
  }
});
