document.addEventListener("DOMContentLoaded", function () {
  AOS.init({ duration: 800, once: true });
  lucide.createIcons();

  document.getElementById("confirmOnlinePay").addEventListener("click", function () {
    alert("✅ Thanh toán online thành công!");
    // Sau khi thanh toán, chuyển sang ký hợp đồng
    window.location.href = "contract.html";
  });
});
