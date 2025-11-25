/**
 * ===================================================================
 * FILE NÀY DÙNG ĐỂ LẤY FCM TOKEN THẬT VÀ ĐĂNG KÝ VỚI BACKEND
 * (Đã tối ưu: Chỉ gửi lên Server khi Token hoặc User thay đổi)
 * ===================================================================
 */

(function () {
  const firebaseConfig = {
    apiKey: "AIzaSyDYflGsxA3-wABQMM4oQAkagBACWptTROw",
    authDomain: "second-hand-ev-battery.firebaseapp.com",
    projectId: "second-hand-ev-battery",
    storageBucket: "second-hand-ev-battery.firebasestorage.app",
    messagingSenderId: "732419381903",
    appId: "1:732419381903:web:bd8ed62b62c79c6939aefa",
    measurementId: "G-FZ3Q0G3GPZ",
  };

  const VAPID_KEY =
    "BA-ZNvlW57aPh99OgqXJmzZVJ05oLzkK__yfWk9J7V-Xbv1mFB82KyE5TOJXbojKw_zMTAusiV_9l1WVxXC8Gqc";

  const API_BASE_URL = "http://localhost:9000/api";

  function getAuthInfo() {
    const token = localStorage.getItem("token");
    const userString = localStorage.getItem("user");

    if (!token || !userString) {
      console.log("FCM Init: Người dùng chưa đăng nhập, bỏ qua.");
      return null;
    }
    try {
      const user = JSON.parse(userString);
      return {
        jwtToken: token,
        userId: user.id,
      };
    } catch (e) {
      console.error("FCM Init: Lỗi parse user data", e);
      return null;
    }
  }

  const callApi = async (url, method = "POST", body = null) => {
    const options = {
      method,
      headers: {
        "Content-Type": "application/json",
      },
    };
    if (body) {
      options.body = JSON.stringify(body);
    }

    const response = await fetch(`${API_BASE_URL}${url}`, options);
    if (!response.ok) {
      throw new Error(`Lỗi HTTP: ${response.status} ${response.statusText}`);
    }
  };

  async function initializeFCM() {
    const auth = getAuthInfo();
    if (!auth) {
      return;
    }

    try {
      if (firebase.apps.length === 0) {
        firebase.initializeApp(firebaseConfig);
      }
    } catch (e) {
      console.error("FCM Init: Không thể khởi tạo Firebase", e);
      return;
    }

    const messaging = firebase.messaging();

    try {
      // Chỉ xin quyền nếu chưa được cấp (trình duyệt tự xử lý việc này, nhưng check lại cho chắc)
      if (Notification.permission !== "granted") {
        const permission = await Notification.requestPermission();
        if (permission !== "granted") {
          console.warn("FCM Init: Quyền thông báo bị từ chối.");
          return;
        }
      }
    } catch (err) {
      console.error("FCM Init: Lỗi khi hỏi quyền.", err);
      return;
    }

    try {
      const currentToken = await messaging.getToken({ vapidKey: VAPID_KEY });

      if (currentToken) {
        // --- [TỐI ƯU HIỆU NĂNG] ---
        // Kiểm tra xem token này đã được gửi lên server cho user này chưa
        const sentToken = localStorage.getItem("fcmToken_sent");
        const sentUser = localStorage.getItem("fcmUser_sent");

        // Nếu Token GIỐNG token cũ VÀ User GIỐNG user cũ -> KHÔNG GỬI LẠI
        if (currentToken === sentToken && sentUser === auth.userId.toString()) {
          console.log(
            "✅ FCM Init: Token chưa thay đổi, bỏ qua cập nhật Server."
          );
          return;
        }

        console.log(
          "🔄 FCM Init: Token mới hoặc User mới, đang cập nhật Server..."
        );

        // Gửi lên server
        await sendTokenToBackend(currentToken, auth.userId);
      } else {
        console.warn("FCM Init: Không lấy được token.");
      }
    } catch (err) {
      console.error("FCM Init: Lỗi khi lấy token:", err);
    }
  }

  async function sendTokenToBackend(fcmToken, userId) {
    try {
      await callApi("/devices/register", "POST", {
        userId: userId,
        token: fcmToken,
      });

      console.log("✅ FCM Init: Đăng ký token THÀNH CÔNG.");

      // --- [LƯU TRẠNG THÁI ĐÃ GỬI] ---
      // Lưu lại token và userId để lần sau kiểm tra
      localStorage.setItem("fcmToken_sent", fcmToken);
      localStorage.setItem("fcmUser_sent", userId);
    } catch (err) {
      console.error("❌ FCM Init: Đăng ký token THẤT BẠI.", err);

      // Nếu lỗi, xóa cache để lần sau thử lại
      localStorage.removeItem("fcmToken_sent");
      localStorage.removeItem("fcmUser_sent");
    }
  }

  window.addEventListener("load", initializeFCM);
})();
