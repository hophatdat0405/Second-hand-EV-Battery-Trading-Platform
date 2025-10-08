// js/navbar.js
document.addEventListener("DOMContentLoaded", function () {
  // mobile toggle
  const mobileToggle = document.getElementById("mobileToggle");
  const mobileMenu = document.getElementById("mobileMenu");
  mobileToggle?.addEventListener("click", () => {
    if (!mobileMenu) return;
    mobileMenu.classList.toggle("hidden");
  });

  // highlight active link (by pathname or hash)
  const links = document.querySelectorAll(".nav-link, .nav-btn");

  const path = location.pathname.split("/").pop(); // e.g. index.html
  links.forEach((a) => {
    const href = a.getAttribute("href") || "";
    if (href === path || (href.startsWith("#") && location.hash === href)) {
      a.classList.add("active");
    }
  });

  // auth state
  const current = localStorage.getItem("sh_current"); // demo auth key
  const authButtons = document.getElementById("authButtons");
  const profileArea = document.getElementById("profileArea");
  const btnProfile = document.getElementById("btnProfile");
  const btnLogout = document.getElementById("btnLogout");

  function showLoggedInUI(userId) {
    if (authButtons) authButtons.classList.add("hidden");
    if (profileArea) profileArea.classList.remove("hidden");
    if (btnProfile) btnProfile.classList.remove("hidden");
    if (btnLogout) btnLogout.classList.remove("hidden");
  }
  function showLoggedOutUI() {
    if (authButtons) authButtons.classList.remove("hidden");
    if (profileArea) profileArea.classList.add("hidden");
    if (btnProfile) btnProfile.classList.add("hidden");
    if (btnLogout) btnLogout.classList.add("hidden");
  }

  if (current) showLoggedInUI(current);
  else showLoggedOutUI();

  // logout handling
  btnLogout?.addEventListener("click", () => {
    localStorage.removeItem("sh_current");
    // optionally redirect to login page or reload
    location.href = "login.html";
  });

  // make sure mobile auth buttons reflect state too
  const mobileLogin = document.getElementById("mobileLogin");
  const mobileRegister = document.getElementById("mobileRegister");
  if (current) {
    // change mobile buttons to profile + logout
    if (mobileLogin) {
      mobileLogin.textContent = "Hồ sơ";
      mobileLogin.href = "profile.html";
    }
    if (mobileRegister) {
      mobileRegister.textContent = "Đăng xuất";
      mobileRegister.href = "#";
      mobileRegister.addEventListener("click", (e) => {
        e.preventDefault();
        localStorage.removeItem("sh_current");
        location.href = "login.html";
      });
    }
  } else {
    if (mobileLogin) mobileLogin.href = "login.html";
    if (mobileRegister) mobileRegister.href = "register.html";
  }
});
