// ======= Gán dữ liệu vào hợp đồng =======
const name  = localStorage.getItem("cName") || "Nguyễn Văn A";
const phone  = localStorage.getItem("cPhone") || "0123456789";
const email  = localStorage.getItem("cEmail") || "email@example.com";
const address = localStorage.getItem("cAddress") || "Hà Nội";
const method = localStorage.getItem("cMethod") || "online";

const productName = localStorage.getItem("productName") || "VinFast VF8 Plus";
const productYear = localStorage.getItem("productYear") || "2023";
const productKm  = localStorage.getItem("productKm")  || "12.000 km";
const productPrice = localStorage.getItem("productPrice") || "1.200.000.000 đ";
const totalPrice  = localStorage.getItem("totalPrice")  || "1.205.000.000 đ";

document.getElementById("cName").textContent = name;
document.getElementById("cPhone").textContent = phone;
document.getElementById("cEmail").textContent = email;
document.getElementById("cAddress").textContent = address;
document.getElementById("cMethod").textContent = method === "cash" ? "Tiền mặt khi nhận" : "Online";
document.getElementById("productName").textContent = productName;
document.getElementById("productYear").textContent = productYear;
document.getElementById("productKm").textContent = productKm;
document.getElementById("productPrice").textContent = productPrice;
document.getElementById("totalPrice").textContent = totalPrice;
document.getElementById("signDate").textContent = new Date().toLocaleDateString("vi-VN");

// ======= Xử lý chữ ký =======
const canvas = document.getElementById("signCanvas");
const ctx = canvas.getContext("2d");
let drawing = false;
const SIGNATURE_LINE_WIDTH = 3;

// Sự kiện chuột
canvas.addEventListener("mousedown", (e) => { 
    drawing = true; 
    ctx.beginPath(); 
    ctx.moveTo(e.offsetX, e.offsetY); 
});
canvas.addEventListener("mousemove", (e) => { 
    if (drawing) { 
        ctx.lineWidth = SIGNATURE_LINE_WIDTH; 
        ctx.lineCap = "round"; 
        ctx.strokeStyle = "#000"; 
        ctx.lineTo(e.offsetX, e.offsetY); 
        ctx.stroke(); 
    } 
});
canvas.addEventListener("mouseup", () => drawing = false);
canvas.addEventListener("mouseleave", () => drawing = false);

// Sự kiện cảm ứng (mobile/tablet)
canvas.addEventListener("touchstart", (e) => {
    e.preventDefault();
    const rect = canvas.getBoundingClientRect();
    const touch = e.touches[0];
    const x = touch.clientX - rect.left;
    const y = touch.clientY - rect.top;
    drawing = true;
    ctx.beginPath();
    ctx.moveTo(x, y);
});
canvas.addEventListener("touchmove", (e) => {
    e.preventDefault();
    if (drawing) {
        const rect = canvas.getBoundingClientRect();
        const touch = e.touches[0];
        const x = touch.clientX - rect.left;
        const y = touch.clientY - rect.top;
        ctx.lineWidth = SIGNATURE_LINE_WIDTH;
        ctx.lineCap = "round";
        ctx.strokeStyle = "#000";
        ctx.lineTo(x, y);
        ctx.stroke();
    }
});
canvas.addEventListener("touchend", () => drawing = false);

// Xóa chữ ký
function clearSign() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
}
window.clearSign = clearSign;

// ======= Xuất PDF tối ưu (không dư trắng, không tràn trang) =======
async function downloadContract() {
  const { jsPDF } = window.jspdf;
  const page = document.querySelector("#contractPage");
  const PDF_SCALE = 3;

  // 1️⃣ Ẩn các phần không cần chụp
  const clearButton = document.querySelector(".signature button.ghost");
  const toolbar = document.querySelector(".toolbar");
  const signCanvas = document.querySelector("#signCanvas");

  // Lưu lại style gốc
  const originalDisplayClear = clearButton.style.display;
  const originalDisplayToolbar = toolbar.style.display;
  const originalCanvasBorder = signCanvas.style.border;

  // Ẩn nút xóa + toolbar + viền khung ký
  clearButton.style.display = 'none';
  toolbar.style.display = 'none';
  signCanvas.style.border = 'none';

  // 2️⃣ Chụp ảnh nội dung thực tế
  const canvasPDF = await html2canvas(page, {
    scale: PDF_SCALE,
    useCORS: true,
    logging: false
  });

  const imgData = canvasPDF.toDataURL("image/jpeg", 1.0);
  const pdf = new jsPDF("p", "mm", "a4");

  // 3️⃣ Tính toán kích thước vừa khít trang A4
  const imgWidth = 210;
  let imgHeight = (canvasPDF.height * imgWidth) / canvasPDF.width;

  // Nếu cao hơn khổ A4 chút ít thì cắt bớt cho vừa khít
  if (imgHeight > 297) {
    imgHeight = 297;
  }

  // 4️⃣ Thêm ảnh vào PDF (1 trang duy nhất)
  pdf.addImage(imgData, "JPEG", 0, 0, imgWidth, imgHeight);

  // 5️⃣ Xuất file
  pdf.save("HopDongMuaBanXeOTo_Chuan.pdf");

  // 6️⃣ Khôi phục hiển thị gốc
  clearButton.style.display = originalDisplayClear;
  toolbar.style.display = originalDisplayToolbar;
  signCanvas.style.border = originalCanvasBorder;
}
window.downloadContract = downloadContract;

