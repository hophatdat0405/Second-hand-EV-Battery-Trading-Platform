//============Tạo khoảng cách với header========================
window.addEventListener("load", () => {
  const navbar = document.querySelector(".navbar");

  // Tạo khoảng trống cho body để navbar không bị che khuất
  document.body.style.paddingTop = "80px"; // Điều chỉnh độ cao padding-top tùy thuộc vào chiều cao của navbar
});

//============Tương tác để xổ ra thông số ============================

// compare.js - tương tác: accordion + tab mở panel tương ứng + cuộn
document.addEventListener("DOMContentLoaded", () => {
  const toggles = document.querySelectorAll(".accordion-toggle");
  const panels = document.querySelectorAll(".accordion-panel");
  const tabs = document.querySelectorAll(".compare-tabs .tab");

  function closeAllPanels() {
    panels.forEach((p) => {
      p.style.maxHeight = null;
    });
    toggles.forEach((t) => t.classList.remove("open"));
  }

  // Accordion toggle: giữ hành vi click thông thường (mở/đóng) và đóng các panel khác
  toggles.forEach((btn) => {
    btn.addEventListener("click", (e) => {
      const item = btn.parentElement;
      const panel = item.querySelector(".accordion-panel");
      const isOpen = panel.style.maxHeight && panel.style.maxHeight !== "0px";

      // đóng tất cả trước
      closeAllPanels();

      if (!isOpen) {
        panel.style.maxHeight = panel.scrollHeight + "px";
        btn.classList.add("open");
        // cuộn tới item để nhìn rõ phần nội dung vừa mở
        item.scrollIntoView({ behavior: "smooth", block: "start" });
      } else {
        panel.style.maxHeight = null;
        btn.classList.remove("open");
      }
    });
  });

  // Tab click: mở panel tương ứng (data-target -> data-id)
  tabs.forEach((tab) => {
    tab.addEventListener("click", () => {
      // cập nhật trạng thái active tab
      tabs.forEach((t) => t.classList.remove("active"));
      tab.classList.add("active");

      const target = tab.dataset.target;
      if (!target) return;

      const item = document.querySelector(
        `.accordion-item[data-id="${target}"]`
      );
      if (!item) return;

      const panel = item.querySelector(".accordion-panel");
      const toggleBtn = item.querySelector(".accordion-toggle");
      const isOpen = panel.style.maxHeight && panel.style.maxHeight !== "0px";

      // đóng tất cả trước
      closeAllPanels();

      // nếu trước đó panel đóng — mở nó; nếu đã mở — đóng (tùy chọn)
      if (!isOpen) {
        panel.style.maxHeight = panel.scrollHeight + "px";
        toggleBtn.classList.add("open");
        // cuộn nhẹ để panel nằm gần đầu view
        item.scrollIntoView({ behavior: "smooth", block: "start" });
      } else {
        panel.style.maxHeight = null;
        toggleBtn.classList.remove("open");
      }
    });
  });

  // nút thêm xe: placeholder
  document.querySelectorAll(".choose-plus").forEach((btn) => {
    btn.addEventListener("click", (e) => {
      e.preventDefault();
      alert(
        "Chức năng chọn xe: bạn có thể mở modal chọn xe hoặc dẫn đến trang chọn xe ở đây."
      );
    });
  });
});
