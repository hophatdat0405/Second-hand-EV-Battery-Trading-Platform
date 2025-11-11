document.addEventListener("DOMContentLoaded", async function () {
  const params = new URLSearchParams(window.location.search);
  const transactionId = params.get("transactionId");

  if (!transactionId) {
    alert("❌ Không tìm thấy transactionId trong URL!");
    return;
  }

  console.log("🔍 Đang lấy thông tin cho transactionId:", transactionId);

  // ============================================================
  // 1️⃣ Lấy dữ liệu thanh toán từ Transaction-Service
  // ============================================================
  let userId, productName, totalAmount;

  try {
    const res = await fetch(`http://localhost:8083/api/payments/info/${transactionId}`);
    if (!res.ok) throw new Error(`Lỗi khi gọi API, mã ${res.status}`);

    const data = await res.json();
    console.log("📦 Dữ liệu nhận được từ backend:", data);

    // Lưu thông tin để gửi qua Contract-Service
    userId = data.userId;
    productName = data.productName;
    totalAmount = data.totalAmount;

    // === Hiển thị thông tin khách hàng ===
    document.getElementById("cName").innerText = data.fullName || "Không rõ";
    document.getElementById("cPhone").innerText = data.phone || "Không rõ";
    document.getElementById("cEmail").innerText = data.email || "Không rõ";
    document.getElementById("cAddress").innerText = data.address || "Không rõ";
    document.getElementById("cMethod").innerText = (data.method || "Khác").toUpperCase();

    // === Hiển thị thông tin sản phẩm ===
    document.getElementById("productName").innerText = data.productName || "Không có";
    document.getElementById("productPrice").innerText =
      data.price ? `${Number(data.price).toLocaleString()} đ` : "0 đ";
    document.getElementById("totalPrice").innerText =
      data.totalAmount ? `${Number(data.totalAmount).toLocaleString()} đ` : "0 đ";

    // === Ngày ký hợp đồng ===
    document.getElementById("signDate").innerText = new Date().toLocaleDateString("vi-VN");

    // Lưu userId vào localStorage để trang lịch sử dùng lại
    if (userId) localStorage.setItem("userId", userId);

  } catch (err) {
    console.error("⚠️ Không thể load thông tin hợp đồng:", err);
    alert("Không thể tải dữ liệu từ server Transaction-Service!");
  }

  // ============================================================
  // 2️⃣ Xử lý chữ ký (canvas)
  // ============================================================
  const canvas = document.getElementById("signCanvas");
  const ctx = canvas.getContext("2d");
  let drawing = false;

  const startDraw = (x, y) => {
    drawing = true;
    ctx.beginPath();
    ctx.moveTo(x, y);
  };
  const draw = (x, y) => {
    if (!drawing) return;
    ctx.lineWidth = 3;
    ctx.lineCap = "round";
    ctx.strokeStyle = "#000";
    ctx.lineTo(x, y);
    ctx.stroke();
  };
  const stopDraw = () => (drawing = false);

  // Chuột
  canvas.addEventListener("mousedown", e => startDraw(e.offsetX, e.offsetY));
  canvas.addEventListener("mousemove", e => draw(e.offsetX, e.offsetY));
  canvas.addEventListener("mouseup", stopDraw);
  canvas.addEventListener("mouseleave", stopDraw);

  // Cảm ứng (mobile)
  canvas.addEventListener("touchstart", e => {
    e.preventDefault();
    const t = e.touches[0];
    startDraw(t.clientX - canvas.offsetLeft, t.clientY - canvas.offsetTop);
  });
  canvas.addEventListener("touchmove", e => {
    e.preventDefault();
    const t = e.touches[0];
    draw(t.clientX - canvas.offsetLeft, t.clientY - canvas.offsetTop);
  });
  canvas.addEventListener("touchend", stopDraw);

  // Xóa chữ ký
  window.clearSign = () => ctx.clearRect(0, 0, canvas.width, canvas.height);

  // ============================================================
  // 3️⃣ Xuất PDF và GỬI hợp đồng sang Contract-Service
  // ============================================================
  window.downloadContract = async function () {
    const { jsPDF } = window.jspdf;
    const page = document.querySelector("#contractPage");
    const PDF_SCALE = 3;

    // Ẩn viền canvas và nút xóa khi chụp
    const signCanvas = document.querySelector("#signCanvas");
    const clearButton = document.querySelector(".signature button.ghost");
    const originalBorder = signCanvas.style.border;
    const originalDisplay = clearButton.style.display;
    signCanvas.style.border = "none";
    clearButton.style.display = "none";

    // Chụp hợp đồng
    const canvasPDF = await html2canvas(page, { scale: PDF_SCALE, useCORS: true, logging: false });
    const imgData = canvasPDF.toDataURL("image/jpeg", 1.0);

    // === Sinh file PDF ===
    const pdf = new jsPDF("p", "mm", "a4");
    const imgWidth = 210;
    const imgHeight = (canvasPDF.height * imgWidth) / canvasPDF.width;
    pdf.addImage(imgData, "JPEG", 0, 0, imgWidth, Math.min(297, imgHeight));

    // === Chuyển PDF blob sang base64 an toàn ===
    const pdfBlob = pdf.output("blob");
    const pdfBase64 = await new Promise((resolve) => {
      const reader = new FileReader();
      reader.onloadend = () => resolve(reader.result.split(",")[1]);
      reader.readAsDataURL(pdfBlob);
    });

    console.log("📎 PDF base64 length:", pdfBase64?.length || 0);


    // Khôi phục giao diện
    signCanvas.style.border = originalBorder;
    clearButton.style.display = originalDisplay;

    // Dữ liệu gửi sang Contract-Service
    const payload = {
      transactionId,
      signature: canvas.toDataURL("image/png"), // chữ ký base64
      userId,                                  // userId của người ký
      productName,
      totalAmount,
      pdfBase64
    };

    console.log("📤 Gửi payload đến Contract-Service:", payload);

    try {
      const resp = await fetch("http://localhost:8081/api/contracts/sign", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });

      const result = await resp.json();
      console.log("📨 Phản hồi từ Contract-Service:", result);

      if (resp.ok) {
        alert("✅ Hợp đồng đã được ký và lưu thành công!");
        const uid = userId || localStorage.getItem("userId");
        window.location.href = `/contract-history.html?userId=${uid}`;
      } else {
        alert("⚠️ Gửi hợp đồng thất bại: " + (result.message || "Lỗi không xác định"));
      }
    } catch (err) {
      console.error("❌ Không thể gửi hợp đồng:", err);
      alert("Không thể gửi dữ liệu đến Contract-Service!");
    }
  };
});
