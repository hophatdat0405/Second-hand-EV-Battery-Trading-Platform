// Thay thế toàn bộ file: /js/main.js
document.addEventListener("DOMContentLoaded", () => {
  const API_BASE_URL = "http://localhost:8080/api";
  const WS_URL = "http://localhost:8080/ws"; // Endpoint WebSocket
  const FAKE_USER_ID = 1;

  const bellContainer = document.getElementById("notification-bell-container");
  if (!bellContainer) return;

  const bellIcon = document.getElementById("notification-bell-icon");
  const badge = document.getElementById("notification-badge");
  const dropdown = document.getElementById("notification-dropdown");
  const notificationList = document.getElementById("notification-list");
  const loadingText = document.getElementById("notification-loading");

  let hasFetchedNotifications = false;
  let stompClient = null; // Biến giữ kết nối WebSocket

  // Hàm callApi (đã sửa lỗi JSON)
  const callApi = async (url, method = "GET", body = null) => {
    const options = {
      method,
      headers: { "Content-Type": "application/json" },
    };
    if (body) {
      options.body = JSON.stringify(body);
    }
    const response = await fetch(url, options);
    if (!response.ok) {
      const errorData = await response
        .json()
        .catch(() => ({ message: `Lỗi HTTP: ${response.statusText}` }));
      throw new Error(
        errorData.message || `Lỗi không xác định (${response.status})`
      );
    }
    if (response.status === 204) {
      // Xử lý No Content
      return null;
    }
    return response.json();
  };

  // 1. Hàm lấy số lượng thông báo chưa đọc (HTTP)
  const fetchUnreadCount = async () => {
    try {
      const count = await callApi(
        `${API_BASE_URL}/notifications/user/${FAKE_USER_ID}/unread-count`
      );
      if (typeof count === "number" && count > 0) {
        badge.classList.remove("hidden");
      } else {
        badge.classList.add("hidden");
      }
    } catch (error) {
      console.error("Lỗi khi lấy số lượng thông báo:", error);
    }
  };

  // 2. Hàm tạo HTML cho từng thông báo
  const createNotificationHTML = (notif) => {
    // Kiểm tra tường minh notif.read hoặc notif.isRead
    const isRead = notif.read === true || notif.isRead === true;
    const unreadClass = !isRead ? "bg-blue-50" : ""; // Dùng class này để tạo màu nền
    return `
      <div class="notification-item-container p-4 border-b hover:bg-gray-50 ${unreadClass} relative group" data-read-status="${!isRead}">
        <a href="${notif.link}" class="notification-link" data-id="${notif.id}">
          <p class="text-sm text-gray-800 pr-6">${notif.message}</p>
          <p class="text-xs text-gray-500 mt-1">${new Date(
            notif.createdAt
          ).toLocaleString("vi-VN")}</p>
        </a>
        <button class="notification-delete-btn absolute top-3 right-3 text-gray-400 hover:text-red-600 opacity-0 group-hover:opacity-100 transition-opacity" data-id="${
          notif.id
        }">
          <i class="fas fa-times"></i>
        </button>
      </div>
    `;
  };

  const fetchNotifications = async () => {
    try {
      const data = await callApi(
        `${API_BASE_URL}/notifications/user/${FAKE_USER_ID}?page=0&size=6`
      );
      const notifications = data.content;
      loadingText.style.display = "none";
      notificationList.innerHTML = "";
      if (notifications.length === 0) {
        notificationList.innerHTML =
          '<p class="text-center text-gray-500 p-4 text-sm">Bạn không có thông báo nào.</p>';
        return;
      }
      notifications.forEach((notif) => {
        notificationList.innerHTML += createNotificationHTML(notif);
      });
      hasFetchedNotifications = true; // Đánh dấu đã tải xong
    } catch (error) {
      console.error("Lỗi khi lấy danh sách thông báo:", error);
      loadingText.textContent = "Không thể tải thông báo.";
      hasFetchedNotifications = false; // Đặt lại nếu lỗi để thử lại
    }
  };

  // 4. Hàm đánh dấu tất cả là đã đọc (HTTP)
  const markAllAsRead = async () => {
    try {
      await callApi(
        `${API_BASE_URL}/notifications/mark-all-as-read/user/${FAKE_USER_ID}`,
        "POST"
      );
      badge.classList.add("hidden");
      notificationList.querySelectorAll(".bg-blue-50").forEach((el) => {
        el.classList.remove("bg-blue-50");
        el.dataset.readStatus = "true";
      });
    } catch (error) {
      console.error("Lỗi khi đánh dấu đã đọc:", error);
    }
  };

  // 5. Gán sự kiện click cho chuông
  bellIcon.addEventListener("click", (e) => {
    e.preventDefault();
    dropdown.classList.toggle("hidden");
    if (!dropdown.classList.contains("hidden")) {
      if (!hasFetchedNotifications) {
        fetchNotifications();
      }
      // KHÔNG gọi markAllAsRead() tự động ở đây nữa
    }
  });

  // 6. Xử lý click trong danh sách thông báo
  notificationList.addEventListener("click", async (e) => {
    // 6.1. Xử lý click vào nút XÓA
    const deleteBtn = e.target.closest(".notification-delete-btn");
    if (deleteBtn) {
      e.preventDefault();
      e.stopPropagation();
      const notifId = deleteBtn.dataset.id;
      const notifItem = deleteBtn.closest(".notification-item-container");
      try {
        await callApi(`${API_BASE_URL}/notifications/${notifId}`, "DELETE");
        notifItem.remove(); // Xóa khỏi DOM
      } catch (error) {
        alert("Không thể xóa thông báo.");
      }
      return;
    }
    // 6.2. Xử lý click vào LINK
    const link = e.target.closest(".notification-link");
    if (link) {
      e.preventDefault();
      const notifId = link.dataset.id;
      const href = link.href;
      const notifItem = link.closest(".notification-item-container");
      try {
        // Chỉ gọi API nếu thông báo này chưa đọc (có màu nền)
        if (notifItem.classList.contains("bg-blue-50")) {
          await callApi(
            `${API_BASE_URL}/notifications/${notifId}/read`,
            "POST"
          );
          notifItem.classList.remove("bg-blue-50"); // Xóa màu nền ngay
          notifItem.dataset.readStatus = "true";
        }
      } finally {
        // Luôn điều hướng
        window.location.href = href;
      }
    }
  });

  // 7. Đóng dropdown nếu click ra ngoài
  document.addEventListener("click", (e) => {
    if (
      !bellContainer.contains(e.target) &&
      !dropdown.classList.contains("hidden")
    ) {
      dropdown.classList.add("hidden");
      // Không reset hasFetchedNotifications nữa
    }
  });

  // 8. Xử lý khi nhận được thông báo real-time
  const onNotificationReceived = (payload) => {
    try {
      const notification = JSON.parse(payload.body);

      badge.classList.remove("hidden"); // Hiện dấu chấm đỏ

      // Nếu dropdown đang mở, thêm vào đầu và xóa placeholder
      if (!dropdown.classList.contains("hidden")) {
        const notificationHTML = createNotificationHTML(notification);
        notificationList.insertAdjacentHTML("afterbegin", notificationHTML);

        const noNotifText = notificationList.querySelector("p");
        if (
          noNotifText &&
          noNotifText.textContent.includes("không có thông báo")
        ) {
          noNotifText.remove();
        }
      }
      // Nếu dropdown đang đóng, đánh dấu là cần fetch lại khi mở
      else {
        hasFetchedNotifications = false;
      }
    } catch (e) {
      console.error("Lỗi xử lý thông báo WebSocket:", e);
    }
  };

  // 9. Kết nối WebSocket
  const connectWebSocket = () => {
    try {
      const socket = new SockJS(WS_URL);
      stompClient = Stomp.over(socket);
      stompClient.debug = null; // Tắt log debug
      stompClient.connect(
        {},
        (frame) => {
          console.log("Đã kết nối WebSocket:", frame);
          stompClient.subscribe(
            `/user/${FAKE_USER_ID}/topic/notifications`,
            onNotificationReceived
          );
        },
        (error) => {
          console.error("Lỗi WebSocket:", error.toString());
          setTimeout(connectWebSocket, 5000 + Math.random() * 5000); // Thử kết nối lại
        }
      );
    } catch (e) {
      console.error("Không thể khởi tạo SockJS:", e);
      setTimeout(connectWebSocket, 10000); // Thử lại sau 10s
    }
  };

  // 10. Khởi chạy
  fetchUnreadCount();
  connectWebSocket();
});
