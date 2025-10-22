document.addEventListener("DOMContentLoaded", async function () {
  const params = new URLSearchParams(window.location.search);
  const transactionId = params.get("transactionId");

  if (!transactionId) {
    alert("❌ Không tìm thấy transactionId trong URL!");
    return;
  }

  console.log("🔍 Đang lấy thông tin cho transactionId:", transactionId);

  try {
    // === 1️⃣ Lấy thông tin từ Transaction-Service ===
    const res = await fetch(`http://localhost:8080/api/payments/info/${transactionId}`);
    if (!res.ok) throw new Error(`Lỗi khi gọi API, mã ${res.status}`);

    const data = await res.json();
    console.log("📦 Dữ liệu nhận được từ backend:", data);

    // === 2️⃣ Gán dữ liệu khách hàng ===
    document.getElementById("cName").textContent = data.fullName || "Không rõ";
    document.getElementById("cPhone").textContent = data.phone || "Không rõ";
    document.getElementById("cEmail").textContent = data.email || "Không rõ";
    document.getElementById("cAddress").textContent = data.address || "Không rõ";
    document.getElementById("cMethod").textContent =
      (data.method || "Khác").toUpperCase();

    // === 3️⃣ Gán dữ liệu sản phẩm từ SQL ===
    document.getElementById("productName").textContent = data.productName || "Không có";
    document.getElementById("productPrice").textContent =
      data.price ? `${Number(data.price).toLocaleString()} đ` : "0 đ";
    document.getElementById("totalPrice").textContent =
      data.totalAmount ? `${Number(data.totalAmount).toLocaleString()} đ` : "0 đ";

    // === 4️⃣ Ngày ký ===
    document.getElementById("signDate").textContent =
      new Date().toLocaleDateString("vi-VN");
  } catch (err) {
    console.error("⚠️ Không thể load thông tin hợp đồng:", err);
    alert("Không thể tải dữ liệu từ server!");
  }

  // === 5️⃣ Xử lý vẽ chữ ký ===
  const canvas = document.getElementById("signCanvas");
  const ctx = canvas.getContext("2d");
  let drawing = false;

  canvas.addEventListener("mousedown", (e) => {
    drawing = true;
    ctx.beginPath();
    ctx.moveTo(e.offsetX, e.offsetY);
  });
  canvas.addEventListener("mousemove", (e) => {
    if (drawing) {
      ctx.lineWidth = 3;
      ctx.lineCap = "round";
      ctx.strokeStyle = "#000";
      ctx.lineTo(e.offsetX, e.offsetY);
      ctx.stroke();
    }
  });
  canvas.addEventListener("mouseup", () => (drawing = false));
  canvas.addEventListener("mouseleave", () => (drawing = false));

  window.clearSign = () => ctx.clearRect(0, 0, canvas.width, canvas.height);

  // === 6️⃣ Xuất PDF (ẩn nút và khung khi chụp) ===
    window.downloadContract = async function () {
    const { jsPDF } = window.jspdf;
    const page = document.querySelector("#contractPage");
    const PDF_SCALE = 3;

    // 🎯 Chỉ ẩn phần canvas & nút Xóa chữ ký, KHÔNG ẩn con dấu
    const signCanvas = document.querySelector("#signCanvas");
    const clearButton = document.querySelector(".signature button.ghost");

    const originalCanvasBorder = signCanvas.style.border;
    const originalDisplayClear = clearButton.style.display;

    signCanvas.style.border = "none";
    clearButton.style.display = "none";

    // 📸 Chụp nội dung trang
    const canvasPDF = await html2canvas(page, {
        scale: PDF_SCALE,
        useCORS: true,
        logging: false
    });

    const imgData = canvasPDF.toDataURL("image/jpeg", 1.0);
    const pdf = new jsPDF("p", "mm", "a4");

    const imgWidth = 210;
    const imgHeight = (canvasPDF.height * imgWidth) / canvasPDF.width;
    pdf.addImage(imgData, "JPEG", 0, 0, imgWidth, Math.min(297, imgHeight));
    pdf.save(`HopDong_${transactionId}.pdf`);

    // 🔄 Khôi phục lại
    signCanvas.style.border = originalCanvasBorder;
    clearButton.style.display = originalDisplayClear;
    };

});
