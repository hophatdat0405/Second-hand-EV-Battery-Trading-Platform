// File mới: /js/notifications.js
document.addEventListener("DOMContentLoaded", () => {
  const API_BASE_URL = "http://localhost:8080/api";
  const FAKE_USER_ID = 1;
  const PAGE_SIZE = 24; // 24 tin mỗi trang

  const listContainer = document.getElementById("all-notifications-list");
  const paginationContainer = document.getElementById("paginationContainer");

  let currentPage = 0;
  let totalPages = 0;

  const fetchNotifications = async (page = 0) => {
    currentPage = page;
    listContainer.innerHTML = `<p style="text-align: center; padding: 20px">Đang tải...</p>`;

    try {
      const response = await fetch(
        `${API_BASE_URL}/notifications/user/${FAKE_USER_ID}?page=${page}&size=${PAGE_SIZE}`
      );
      if (!response.ok) throw new Error("Lỗi khi tải thông báo");

      const pageData = await response.json();
      const notifications = pageData.content;
      totalPages = pageData.totalPages;

      listContainer.innerHTML = "";

      if (notifications.length === 0) {
        listContainer.innerHTML = `<p style="text-align: center; padding: 20px">Bạn không có thông báo nào.</p>`;
      } else {
        notifications.forEach((notif) => {
          listContainer.innerHTML += createNotificationHTML(notif);
        });
      }

      renderPagination();
    } catch (error) {
      console.error(error);
      listContainer.innerHTML = `<p style="text-align: center; padding: 20px; color: red;">Không thể tải thông báo.</p>`;
    }
  };

  const createNotificationHTML = (notif) => {
    const isRead = notif.read === true || notif.isRead === true;
    const unreadClass = !isRead ? "unread" : "";
    return `
        <a href="${notif.link}" class="notification-item ${unreadClass}">
            <p style="font-size: 0.9rem; color: #1f2937;">${notif.message}</p>
            <p style="font-size: 0.75rem; color: #6b7280; margin-top: 4px;">
                ${new Date(notif.createdAt).toLocaleString("vi-VN")}
            </p>
        </a>
      `;
  };

  const renderPagination = () => {
    paginationContainer.innerHTML = "";
    if (totalPages <= 1) return;

    // Nút "Trước"
    paginationContainer.innerHTML += `
        <button class="pagination-btn" data-page="${currentPage - 1}" ${
      currentPage === 0 ? "disabled" : ""
    }>&lt; Trước</button>`;

    // Nút các trang
    for (let i = 0; i < totalPages; i++) {
      paginationContainer.innerHTML += `
            <button class="pagination-btn ${
              i === currentPage ? "active" : ""
            }" data-page="${i}">${i + 1}</button>`;
    }

    // Nút "Sau"
    paginationContainer.innerHTML += `
        <button class="pagination-btn" data-page="${currentPage + 1}" ${
      currentPage >= totalPages - 1 ? "disabled" : ""
    }>Sau &gt;</button>`;
  };

  paginationContainer.addEventListener("click", (e) => {
    const button = e.target.closest(".pagination-btn");
    if (button && !button.disabled) {
      const page = parseInt(button.dataset.page, 10);
      fetchNotifications(page);
    }
  });

  // Tải lần đầu
  fetchNotifications(0);
});
