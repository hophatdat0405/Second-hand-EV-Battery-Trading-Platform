// js/profile.js
// Robust profile script — handles personal info, vehicles/pin, transactions.
// Stores data in localStorage keys: sh_users (array) and sh_current (id/email/phone)

(function () {
  // Helpers
  function getUsers() {
    const raw = localStorage.getItem("sh_users");
    return raw ? JSON.parse(raw) : [];
  }
  function setUsers(users) {
    localStorage.setItem("sh_users", JSON.stringify(users));
  }
  function currentUserId() {
    return localStorage.getItem("sh_current"); // email or phone (as used in auth)
  }
  function findCurrentUser() {
    const id = currentUserId();
    if (!id) return null;
    const users = getUsers();
    return (
      users.find(
        (u) =>
          (u.email && u.email.toLowerCase() === id.toLowerCase()) ||
          (u.phone && u.phone === id)
      ) || null
    );
  }
  function saveUser(user) {
    const users = getUsers();
    const idx = users.findIndex((u) => u.id === user.id);
    if (idx >= 0) users[idx] = user;
    else users.push(user);
    setUsers(users);
  }
  function formatVND(x) {
    if (!x && x !== 0) return "-";
    try {
      return new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
      }).format(Number(x));
    } catch (e) {
      return x + " VND";
    }
  }

  // DOM ready
  document.addEventListener("DOMContentLoaded", () => {
    const user = findCurrentUser();
    if (!user) {
      // not logged in -> redirect to login
      console.warn("No current user. Redirecting to login.");
      location.href = "login.html";
      return;
    }

    // Profile fields
    const pfName = document.getElementById("pfName");
    const pfEmail = document.getElementById("pfEmail");
    const pfPhone = document.getElementById("pfPhone");
    const pfAddress = document.getElementById("pfAddress");
    const profileForm = document.getElementById("profileForm");

    // Items area
    const addItemBtn = document.getElementById("addItemBtn");
    const addForm = document.getElementById("addForm");
    const saveItemBtn = document.getElementById("saveItemBtn");
    const cancelItemBtn = document.getElementById("cancelItemBtn");
    const itemsList = document.getElementById("itemsList");

    // Transactions
    const txList = document.getElementById("txList");

    // Initialize form values
    pfName.value = user.name || "";
    pfEmail.value = user.email || "";
    pfPhone.value = user.phone || "";
    pfAddress.value = user.address || "";

    // Save profile
    profileForm?.addEventListener("submit", (e) => {
      e.preventDefault();
      user.name = pfName.value.trim();
      user.phone = pfPhone.value.trim();
      user.address = pfAddress.value.trim();
      saveUser(user);
      alert("Cập nhật thông tin thành công.");
    });

    // Toggle add form
    addItemBtn?.addEventListener("click", () => {
      if (!addForm) return;
      addForm.classList.toggle("hidden");
      // clear edit state
      delete saveItemBtn.dataset.edit;
      // clear inputs
      ["itemTitle", "itemPrice", "itemYear", "itemKM", "itemCondition"].forEach(
        (id) => {
          const el = document.getElementById(id);
          if (el) el.value = "";
        }
      );
      document.getElementById("itemType").value = "car";
    });

    cancelItemBtn?.addEventListener("click", () => {
      if (!addForm) return;
      addForm.classList.add("hidden");
      delete saveItemBtn.dataset.edit;
    });

    // Render items & tx
    function renderItems() {
      itemsList.innerHTML = "";
      user.vehicles = user.vehicles || [];
      if (user.vehicles.length === 0) {
        itemsList.innerHTML =
          '<div class="text-sm text-gray-500">Bạn chưa có mục nào. Bấm "Thêm" để tạo.</div>';
        return;
      }
      user.vehicles.forEach((it, idx) => {
        const card = document.createElement("div");
        card.className =
          "p-4 border rounded flex justify-between items-start gap-3 bg-white";
        card.innerHTML = `
          <div>
            <div class="font-bold">${escapeHtml(
              it.title || "-"
            )} <span class="text-sm text-gray-500">(${escapeHtml(
          it.type || ""
        )})</span></div>
            <div class="text-sm text-gray-600">Năm: ${escapeHtml(
              it.year || "-"
            )} • KM: ${escapeHtml(it.km || "-")} • ${escapeHtml(
          it.condition || "-"
        )}</div>
            <div class="text-sm mt-2 font-semibold">${formatVND(it.price)}</div>
          </div>
          <div class="flex flex-col gap-2">
            <button data-i="${idx}" class="editBtn px-3 py-1 border rounded text-sm">Sửa</button>
            <button data-i="${idx}" class="delBtn px-3 py-1 border rounded text-sm text-red-600">Xóa</button>
          </div>
        `;
        itemsList.appendChild(card);
      });

      // attach handlers
      itemsList.querySelectorAll(".delBtn").forEach((btn) => {
        btn.addEventListener("click", (e) => {
          const i = Number(e.currentTarget.dataset.i);
          if (!confirm("Bạn có chắc muốn xóa mục này?")) return;
          user.vehicles.splice(i, 1);
          saveUser(user);
          renderItems();
        });
      });
      itemsList.querySelectorAll(".editBtn").forEach((btn) => {
        btn.addEventListener("click", (e) => {
          const i = Number(e.currentTarget.dataset.i);
          // populate form
          const it = user.vehicles[i];
          document.getElementById("itemTitle").value = it.title || "";
          document.getElementById("itemType").value = it.type || "car";
          document.getElementById("itemPrice").value = it.price || "";
          document.getElementById("itemYear").value = it.year || "";
          document.getElementById("itemKM").value = it.km || "";
          document.getElementById("itemCondition").value = it.condition || "";
          saveItemBtn.dataset.edit = i;
          addForm.classList.remove("hidden");
        });
      });
    }

    function renderTx() {
      txList.innerHTML = "";
      user.transactions = user.transactions || [];
      if (user.transactions.length === 0) {
        txList.innerHTML =
          '<div class="text-sm text-gray-500">Chưa có lịch sử giao dịch.</div>';
        return;
      }
      user.transactions.forEach((t) => {
        const el = document.createElement("div");
        el.className = "p-3 border rounded bg-white";
        el.innerHTML = `
          <div class="flex justify-between"><div class="font-semibold">${escapeHtml(
            t.title || "Giao dịch"
          )}</div><div class="text-sm">${escapeHtml(t.date || "")}</div></div>
          <div class="text-sm text-gray-600">${escapeHtml(t.note || "")}</div>
          <div class="mt-1 font-bold">${formatVND(t.amount)}</div>
        `;
        txList.appendChild(el);
      });
    }

    // Save or update item
    saveItemBtn?.addEventListener("click", (e) => {
      e.preventDefault?.();
      const title = (document.getElementById("itemTitle").value || "").trim();
      const type = document.getElementById("itemType").value || "car";
      const price = (document.getElementById("itemPrice").value || "").trim();
      const year = (document.getElementById("itemYear").value || "").trim();
      const km = (document.getElementById("itemKM").value || "").trim();
      const cond = (
        document.getElementById("itemCondition").value || ""
      ).trim();

      if (!title) {
        alert("Vui lòng nhập tên mục");
        return;
      }

      const item = { title, type, price, year, km, condition: cond };
      user.vehicles = user.vehicles || [];

      if (saveItemBtn.dataset.edit !== undefined) {
        const idx = Number(saveItemBtn.dataset.edit);
        user.vehicles[idx] = item;
        delete saveItemBtn.dataset.edit;
      } else {
        user.vehicles.push(item);
      }

      saveUser(user);
      renderItems();
      addForm.classList.add("hidden");
      // clear inputs
      ["itemTitle", "itemPrice", "itemYear", "itemKM", "itemCondition"].forEach(
        (id) => {
          const el = document.getElementById(id);
          if (el) el.value = "";
        }
      );
    });

    // Utility: escape HTML to avoid injection in innerHTML
    function escapeHtml(str) {
      if (!str && str !== 0) return "";
      return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
    }

    // ensure sample tx if empty (optional)
    if (!user.transactions || user.transactions.length === 0) {
      user.transactions = [
        {
          title: "Đặt cọc mẫu",
          date: new Date().toLocaleDateString(),
          amount: 2000000,
          note: "Giao dịch demo",
        },
      ];
      saveUser(user);
    }

    // initial render
    renderItems();
    renderTx();

    // expose some functions for debugging (optional)
    window.__sh_profile = { user, renderItems, renderTx, saveUser };
  });
})();
