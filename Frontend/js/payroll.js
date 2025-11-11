document.addEventListener("DOMContentLoaded", async () => {
  // Xóa bỏ Lucide và AOS
  // if (window.lucide) lucide.createIcons();
  // if (window.AOS) AOS.init({ duration: 700, once: true });

  const API_BASE = "http://localhost:8089/api/payroll";
  
  // Sửa ID tham chiếu đến <tbody>
  const staffTableBody = document.getElementById("staffTableBody"); 
  const autoPayBtn = document.getElementById("autoPayBtn");

  if (!staffTableBody) {
      console.error("Không tìm thấy phần tử #staffTableBody!");
      return;
  }

  // ======= 1️⃣ Tải danh sách nhân viên (Đã cập nhật CSS) =======
  async function loadStaffs() {
    try {
      const res = await fetch(`${API_BASE}/staff`);
      if (!res.ok) throw new Error("Không thể tải dữ liệu nhân viên");
      const data = await res.json();

      if (data.length === 0) {
        staffTableBody.innerHTML = `<tr><td colspan="6" class="loading">Không có nhân viên nào.</td></tr>`;
        return;
      }
      
      // Sử dụng CSS class và Icon mới
      staffTableBody.innerHTML = data.map((s) => `
        <tr>
          <td>#${s.userId}</td>
          <td>${Number(s.salary || 0).toLocaleString("vi-VN")} đ</td>
          <td>${s.payDay || "-"}</td>
          <td>
            <span class="status ${
              s.status === "ACTIVE" ? "status-active" : "status-paused"
            }">
              ${s.status === "ACTIVE" ? "Hoạt động" : "Tạm dừng"}
            </span>
          </td>
          <td>${s.lastPaid || "-"}</td>
          <td class="text-center">
            <button class="btn-secondary" style="padding: 8px 15px;" onclick="openEditModal(${s.userId}, ${s.salary || 0}, ${s.payDay || 1})">
              <span class='material-icons' style='font-size: 1.1rem;'>edit</span> Sửa
            </button>
            <button style="padding: 8px 15px;" onclick="payNow(${s.userId}, ${s.salary || 0})">
              <span class='material-icons' style='font-size: 1.1rem;'>payment</span> Trả
            </button>
          </td>
        </tr>
      `).join("");
      
      // Xóa bỏ lucide.createIcons();
    } catch (err) {
      console.error(err);
      staffTableBody.innerHTML = `<tr><td colspan="6" class="loading">Lỗi tải dữ liệu</td></tr>`;
    }
  }

  // ======= 2️⃣ Chạy auto payroll (ĐÃ KHÔI PHỤC LOGIC + ALERT) =======
  autoPayBtn.addEventListener("click", async () => {
    if (!confirm("Chạy trả lương tự động ngay bây giờ?")) return;
    try {
      const res = await fetch(`${API_BASE}/run-auto`, { method: "POST" });
      const msg = await res.text();
      alert(msg); // <-- Thông báo của bạn ở đây
      loadStaffs();
    } catch {
      alert("⚠️ Lỗi khi chạy tự động!"); // <-- Thông báo của bạn ở đây
    }
  });

  // ======= 3️⃣ Modal chỉnh sửa (Đã cập nhật logic hiển thị) =======
  const modal = document.getElementById("editModal");
  
  window.openEditModal = (userId, salary, payDay) => {
    document.getElementById("salaryInput").value = salary;
    document.getElementById("payDayInput").value = payDay;
    if (modal) modal.style.display = "flex"; // Sửa logic hiển thị
    window.currentUserId = userId;
  };

  const closeModal = () => {
    if (modal) modal.style.display = "none"; // Sửa logic ẩn
  };

  document.getElementById("closeModal").addEventListener("click", closeModal);
  document.getElementById("cancelEdit").addEventListener("click", closeModal);

  // (ĐÃ KHÔI PHỤC LOGIC + ALERT)
  document.getElementById("editForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const salary = parseFloat(document.getElementById("salaryInput").value);
    const payDay = parseInt(document.getElementById("payDayInput").value);
    const status = "ACTIVE";

    try {
      const res = await fetch(`${API_BASE}/staff/${window.currentUserId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ salary, payDay, status }),
      });
      if (res.ok) {
        alert("✅ Cập nhật thành công!"); // <-- Thông báo của bạn ở đây
        closeModal();
        loadStaffs();
      } else alert("❌ Cập nhật thất bại!"); // <-- Thông báo của bạn ở đây
    } catch {
      alert("⚠️ Không thể kết nối server!"); // <-- Thông báo của bạn ở đây
    }
  });

  // ======= 4️⃣ Trả lương thủ công (ĐÃ KHÔI PHỤC LOGIC + ALERT) =======
  window.payNow = async (userId, amount) => {
    if (!confirm(`Xác nhận trả lương ${amount.toLocaleString("vi-VN")}đ cho user #${userId}?`))
      return;
    try {
      const res = await fetch(`${API_BASE}/run`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          userId,
          amount,
          periodLabel: new Date().toISOString().slice(0, 7),
        }),
      });
      const msg = await res.text();
      alert(msg); // <-- Thông báo của bạn ở đây
      loadStaffs();
    } catch {
      alert("❌ Lỗi khi trả lương!"); // <-- ThôngBáo của bạn ở đây
    }
  };

  // ======= 5️⃣ Gọi lần đầu =======
  loadStaffs();
});