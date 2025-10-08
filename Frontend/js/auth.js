// assets/js/auth.js
// Simple client-side auth demo using localStorage (NOT for production)

function getUsers() {
  const raw = localStorage.getItem("sh_users");
  return raw ? JSON.parse(raw) : [];
}
function setUsers(users) {
  localStorage.setItem("sh_users", JSON.stringify(users));
}

function findUserByEmailOrPhone(id) {
  const users = getUsers();
  return users.find((u) => u.email === id || u.phone === id);
}

function registerHandler(evt) {
  evt.preventDefault();
  const name = document.getElementById("name").value.trim();
  const email = document.getElementById("email").value.trim().toLowerCase();
  const phone = document.getElementById("phone").value.trim();
  const pw = document.getElementById("password").value;
  const pw2 = document.getElementById("password2").value;

  if (!name || (!email && !phone) || !pw) {
    return alert("Vui lòng điền đầy đủ thông tin");
  }
  if (pw !== pw2) return alert("Mật khẩu không khớp");

  const users = getUsers();
  if (users.some((u) => u.email === email && email !== ""))
    return alert("Email đã được sử dụng");
  if (users.some((u) => u.phone === phone && phone !== ""))
    return alert("SĐT đã được sử dụng");

  const user = {
    id: Date.now(),
    name,
    email,
    phone,
    password: pw,
    address: "",
    vehicles: [],
    transactions: [],
  };
  users.push(user);
  setUsers(users);
  localStorage.setItem("sh_current", user.email || user.phone);
  alert("Đăng ký thành công!");
  location.href = "profile.html";
}

function loginHandler(evt) {
  evt.preventDefault();
  const id = document.getElementById("idOrPhone").value.trim().toLowerCase();
  const pw = document.getElementById("loginPass").value;
  const user = findUserByEmailOrPhone(id);
  if (!user) return alert("Không tìm thấy tài khoản");
  if (user.password !== pw) return alert("Mật khẩu sai");
  localStorage.setItem("sh_current", user.email || user.phone);
  location.href = "profile.html";
}

// simulated social login
function socialLogin(provider) {
  // create or get demo user
  const users = getUsers();
  const demoEmail = provider + "_demo@example.com";
  let user = users.find((u) => u.email === demoEmail);
  if (!user) {
    user = {
      id: Date.now(),
      name: provider + " User",
      email: demoEmail,
      phone: "",
      password: "social",
      address: "",
      vehicles: [],
      transactions: [],
    };
    users.push(user);
    setUsers(users);
  }
  localStorage.setItem("sh_current", user.email);
  location.href = "profile.html";
}

/* attach events if elements exist */
document.addEventListener("DOMContentLoaded", () => {
  const reg = document.getElementById("registerForm");
  if (reg) reg.addEventListener("submit", registerHandler);

  const log = document.getElementById("loginForm");
  if (log) log.addEventListener("submit", loginHandler);

  const gbtn = document.getElementById("googleBtn");
  if (gbtn) gbtn.addEventListener("click", () => socialLogin("google"));
  const fbbtn = document.getElementById("fbBtn");
  if (fbbtn) fbbtn.addEventListener("click", () => socialLogin("facebook"));

  const gLogin = document.getElementById("googleLogin");
  if (gLogin) gLogin.addEventListener("click", () => socialLogin("google"));
  const fLogin = document.getElementById("fbLogin");
  if (fLogin) fLogin.addEventListener("click", () => socialLogin("facebook"));
});
