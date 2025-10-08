//============Tạo khoảng cách với header========================
window.addEventListener("load", () => {
  const navbar = document.querySelector(".navbar");

  // Tạo khoảng trống cho body để navbar không bị che khuất
  document.body.style.paddingTop = "80px"; // Điều chỉnh độ cao padding-top tùy thuộc vào chiều cao của navbar
});

//============click nút trái tim========================
(function () {
  // Chọn tất cả các nút like
  const likeButtons = document.querySelectorAll(".btn-like");

  likeButtons.forEach((btn) => {
    // đảm bảo icon có class fa-heart; nếu không, bỏ qua
    const icon = btn.querySelector("i.fa-heart");

    // nếu không có icon thì skip
    if (!icon) return;

    // Khởi tạo ARIA nếu chưa
    if (!btn.hasAttribute("aria-pressed"))
      btn.setAttribute("aria-pressed", "false");

    // Nếu icon đang là 'fas' (solid) thì coi là đã like
    if (icon.classList.contains("fas")) {
      btn.classList.add("active");
      btn.setAttribute("aria-pressed", "true");
    } else {
      // đảm bảo outline nếu ban đầu là chưa like
      icon.classList.remove("fas");
      icon.classList.add("far");
      btn.classList.remove("active");
      btn.setAttribute("aria-pressed", "false");
    }

    btn.addEventListener("click", (e) => {
      const isActive = btn.classList.toggle("active");

      if (isActive) {
        // set solid red heart
        icon.classList.remove("far");
        icon.classList.add("fas");
        btn.setAttribute("aria-pressed", "true");
        btn.setAttribute("aria-label", "Bỏ thích");
      } else {
        // set outline white heart
        icon.classList.remove("fas");
        icon.classList.add("far");
        btn.setAttribute("aria-pressed", "false");
        btn.setAttribute("aria-label", "Thích");
      }

      // (tùy chọn) nếu muốn lưu trạng thái về server hoặc localStorage, làm ở đây
      // ex: localStorage.setItem('liked_'+id, isActive)
    });
  });
})();
