document.addEventListener("DOMContentLoaded", function () {
  AOS.init({ duration: 800, once: true });
  lucide.createIcons();

  const name = localStorage.getItem("cName");
  const phone = localStorage.getItem("cPhone");
  const email = localStorage.getItem("cEmail");
  const address = localStorage.getItem("cAddress");
  const method = localStorage.getItem("cMethod");

  if (name && phone && email && address && method) {
    document.getElementById("customerInfo").classList.remove("hidden");

    document.getElementById("cName").textContent = name;
    document.getElementById("cPhone").textContent = phone;
    document.getElementById("cEmail").textContent = email;
    document.getElementById("cAddress").textContent = address;

    if (method === "cash") {
      document.getElementById("cMethod").textContent = "Thanh toán tiền mặt khi nhận hàng";
      document.getElementById("extraNote").classList.remove("hidden");
      document.getElementById("extraNote").textContent = "💵 Bạn sẽ thanh toán khi nhận hàng.";
    } else {
      document.getElementById("cMethod").textContent = "Thanh toán Online";
      document.getElementById("extraNote").classList.remove("hidden");
      document.getElementById("extraNote").textContent = "💳 Bạn đã thanh toán online thành công.";
    }
  }

  // 👉 Xóa localStorage sau khi hiển thị để tránh lưu lại khi refresh
  localStorage.removeItem("cName");
  localStorage.removeItem("cPhone");
  localStorage.removeItem("cEmail");
  localStorage.removeItem("cAddress");
  localStorage.removeItem("cMethod");
});
