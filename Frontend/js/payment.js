// JS cho trang thanh toán
document.addEventListener("DOMContentLoaded", function () {
  AOS.init({ duration: 800, once: true });
  lucide.createIcons();

  const form = document.getElementById("paymentForm");
  form.addEventListener("submit", function (e) {
    e.preventDefault();

    const name = document.getElementById("name").value.trim();
    const phone = document.getElementById("phone").value.trim();
    const email = document.getElementById("email").value.trim();
    const address = document.getElementById("address").value.trim();
    const method = document.querySelector("input[name='payment']:checked");

    if (!name || !phone || !email || !address) {
      alert("⚠️ Vui lòng nhập đầy đủ thông tin khách hàng!");
      return;
    }

    if (!method) {
      alert("⚠️ Vui lòng chọn phương thức thanh toán!");
      return;
    }

    // Lưu vào localStorage để hiển thị lại sau
    localStorage.setItem("cName", name);
    localStorage.setItem("cPhone", phone);
    localStorage.setItem("cEmail", email);
    localStorage.setItem("cAddress", address);
    localStorage.setItem("cMethod", method.value);

    if (method.value === "cash") {
      alert(`✅ Cảm ơn ${name}, bạn đã chọn thanh toán tiền mặt khi nhận.`);
      window.location.href = "payment_success.html";   // sang trang cảm ơn luôn
    } else {
      alert(`💳 Cảm ơn ${name}, bạn đã chọn thanh toán online.`);
      window.location.href = "online_payment.html";    // sang trang thanh toán online
    }
  });
});
