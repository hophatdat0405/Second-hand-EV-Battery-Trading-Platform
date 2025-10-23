// Thay thế toàn bộ file: admin-listings.js
document.addEventListener("DOMContentLoaded", () => {
  // --- PHẦN GIẢ LẬP QUYỀN ADMIN ---
  function checkAdminAccess() {
    const adminUser = localStorage.getItem("admin_user");
    if (!adminUser) {
      alert("Bạn không có quyền truy cập trang này..."); // Giữ thông báo ngắn gọn
      localStorage.setItem(
        "admin_user",
        JSON.stringify({ id: "admin01", role: "ADMIN" })
      );
      alert("Đã tạo phiên admin giả. Vui lòng tải lại trang.");
      return false;
    }
    const user = JSON.parse(adminUser);
    if (user.role === "ADMIN") {
      document.getElementById("auth-gate").classList.remove("hidden");
      return true;
    } else {
      alert("Tài khoản không phải là Admin.");
      return false;
    }
  }
  if (!checkAdminAccess()) {
    return;
  }

  // --- CẤU HÌNH VÀ BIẾN ---
  const ADMIN_API_URL = "http://localhost:8080/api/admin/listings";
  const PUBLIC_API_URL = "http://localhost:8080/api/listings";
  const WS_URL = "http://localhost:8080/ws";
  const BACKEND_ORIGIN = "http://localhost:8080";
  let currentPage = 0;
  let totalPages = 0;
  let currentStatus = "PENDING";
  let currentListingId = null;
  let debounceTimer;
  let stompClient = null;
  const PAGE_SIZE = 15; // Định nghĩa size cố định

  // --- LẤY DOM ELEMENTS ---
  const filterContainer = document.getElementById("filterContainer");
  const tableBody = document.getElementById("listingsTableBody");
  const tableStatus = document.getElementById("tableStatus");
  const paginationContainer = document.getElementById("paginationContainer");
  const rejectModal = document.getElementById("rejectModal");
  const rejectReasonInput = document.getElementById("rejectReason");
  const viewDetailsModal = document.getElementById("viewDetailsModal");
  const searchForm = document.getElementById("searchForm");
  const searchInput = document.getElementById("searchInput");

  // --- HÀM CALL API (HTTP) ---
  const callApi = async (url, method = "GET", body = null) => {
    const options = { method, headers: { "Content-Type": "application/json" } };
    if (body) {
      options.body = JSON.stringify(body);
    }
    const response = await fetch(url, options);
    if (!response.ok) {
      const errorData = await response
        .json()
        .catch(() => ({ message: `Lỗi HTTP: ${response.statusText}` }));
      throw new Error(
        errorData.message || `Lỗi không xác định (${response.status})`
      );
    }
    return response.status === 204 ? null : response.json();
  };

  // --- HÀM LẤY DỮ LIỆU BAN ĐẦU (HTTP) ---
  const fetchListings = async (page) => {
    tableBody.innerHTML = "";
    tableStatus.textContent = "Đang tải dữ liệu...";
    tableStatus.style.display = "block";
    const searchQuery = searchInput.value.trim();
    const isSearching = searchQuery.length > 0;

    try {
      let data;
      let apiUrl = "";

      if (isSearching) {
        // Sắp xếp kết quả tìm kiếm theo MỚI NHẤT (giữ nguyên)
        const sortParam = "sort=listingDate,desc";
        apiUrl = `${ADMIN_API_URL}/search?query=${searchQuery}&page=${page}&size=${PAGE_SIZE}&${sortParam}`;
      } else {
        // Sắp xếp theo logic MỚI
        let sortParam = "sort=listingDate,desc"; // Mặc định: Mới nhất cho các tab khác
        if (currentStatus === "PENDING") {
          // ✅✅✅ THAY ĐỔI DUY NHẤT TẠI ĐÂY ✅✅✅
          // Sắp xếp theo ngày TẠO (createdAt) của sản phẩm, cũ nhất trước
          sortParam = "sort=product.createdAt,asc";
        }
        apiUrl = `${ADMIN_API_URL}?status=${currentStatus}&page=${page}&size=${PAGE_SIZE}&${sortParam}`;
      }

      data = await callApi(apiUrl);
      renderListings(data.content);
      currentPage = data.number;
      totalPages = data.totalPages;
      renderPagination();
    } catch (error) {
      tableStatus.textContent = `Lỗi khi tải dữ liệu: ${error.message}`;
    }
  };

  // --- HÀM RENDER ---
  const renderListings = (listings) => {
    if (!listings || listings.length === 0) {
      tableStatus.textContent = "Không có tin đăng nào phù hợp.";
      tableStatus.style.display = "block";
      tableBody.innerHTML = "";
      return;
    }
    tableStatus.style.display = "none";
    tableBody.innerHTML = listings.map(createListingRowHTML).join("");
  };

  const getStatusBadge = (status) => {
    const statuses = {
      PENDING: "bg-yellow-100 text-yellow-800",
      ACTIVE: "bg-green-100 text-green-800",
      SOLD: "bg-gray-100 text-gray-800",
      REJECTED: "bg-red-100 text-red-800",
    };
    return `<span class="px-2 py-1 text-xs font-semibold rounded-full ${
      statuses[status] || ""
    }">${status}</span>`;
  };

  const createListingRowHTML = (listing) => {
    const {
      listingId,
      product,
      userId,
      listingDate,
      listingStatus,
      verified,
      adminNotes,
    } = listing;

    // Lấy ngày tạo gốc (createdAt) từ product
    const createdAt = product ? product.createdAt : listingDate;

    const productNameHTML =
      product && product.productName
        ? product.productName
        : `<i class="text-gray-500">Đang cập nhật...</i>`;

    const reasonHTML =
      listingStatus === "REJECTED" && adminNotes
        ? `<td class="px-6 py-4 text-xs italic text-red-700 max-w-xs truncate" title="${adminNotes}">${adminNotes}</td>`
        : `<td class="px-6 py-4 text-gray-400">---</td>`;

    let actionsHTML = "";
    if (listingStatus === "PENDING") {
      actionsHTML = `
            <button data-id="${listingId}" data-action="approve" class="action-btn bg-green-500 text-white px-3 py-1 rounded hover:bg-green-600 text-xs">Duyệt</button>
            <button data-id="${listingId}" data-action="reject" class="action-btn bg-red-500 text-white px-3 py-1 rounded hover:bg-red-600 text-xs">Từ chối</button>
        `;
    } else if (listingStatus === "ACTIVE") {
      actionsHTML = verified
        ? `<span class="px-3 py-1 text-xs font-bold text-blue-800 bg-blue-100 rounded-full">Đã kiểm định</span>`
        : `<button data-id="${listingId}" data-action="verify" class="action-btn bg-blue-500 text-white px-3 py-1 rounded hover:bg-blue-600 text-xs">Gắn nhãn KĐ</button>`;
    }

    return `
        <tr class="border-b" data-listing-id="${listingId}"> <td class="px-6 py-4 font-medium">${productNameHTML} ${
      verified ? "⭐" : ""
    }</td>
            <td class="px-6 py-4">${userId}</td>
            <td class="px-6 py-4">${new Date(
              createdAt // Hiển thị ngày tạo gốc
            ).toLocaleString("vi-VN")}</td>
            <td class="px-6 py-4 status-cell">${getStatusBadge(
              listingStatus
            )}</td>
            <td class="px-4 py-3">
                <button data-id="${listingId}" data-action="view" class="action-btn text-blue-600 hover:underline text-xs font-semibold">Xem</button>
            </td>
            ${reasonHTML} <td class="px-6 py-4 text-center space-x-2 actions-cell">${
      actionsHTML || "---"
    }</td>
        </tr>
    `;
  };

  const renderPagination = () => {
    if (totalPages <= 1) {
      paginationContainer.innerHTML = "";
      return;
    }
    let paginationHTML = "";
    paginationHTML += `<button data-page="${
      currentPage - 1
    }" class="pagination-btn px-4 py-2 rounded-md bg-white border text-gray-700 hover:bg-gray-100 ${
      currentPage === 0 ? "opacity-50 cursor-not-allowed" : ""
    }" ${currentPage === 0 ? "disabled" : ""}>Trước</button>`;
    for (let i = 0; i < totalPages; i++) {
      const activeClass =
        i === currentPage
          ? "bg-blue-700 text-white border-blue-700"
          : "bg-white text-gray-700 border-gray-300 hover:bg-gray-100";
      paginationHTML += `<button data-page="${i}" class="pagination-btn px-4 py-2 rounded-md border ${activeClass}">${
        i + 1
      }</button>`;
    }
    paginationHTML += `<button data-page="${
      currentPage + 1
    }" class="pagination-btn px-4 py-2 rounded-md bg-white border text-gray-700 hover:bg-gray-100 ${
      currentPage >= totalPages - 1 ? "opacity-50 cursor-not-allowed" : ""
    }" ${currentPage >= totalPages - 1 ? "disabled" : ""}>Sau</button>`;
    paginationContainer.innerHTML = paginationHTML;
  };

  const productTypeMap = {
    car: "Ô Tô Điện",
    motorbike: "Xe Máy Điện",
    bike: "Xe Đạp Điện",
    battery: "Pin Đã Qua Sử Dụng",
  };
  const formatPrice = (price) => {
    if (!price || price === 0) return "Thương lượng";
    // Luôn hiển thị giá đầy đủ, định dạng VNĐ
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(price);
  };

  const populateViewModal = (listing) => {
    const { product, phone, location, listingStatus, listingDate } = listing;
    if (!product) {
      console.error(
        "Lỗi: Dữ liệu chi tiết bị thiếu thông tin sản phẩm.",
        listing
      );
      alert("Không thể tải chi tiết: thiếu thông tin sản phẩm.");
      return;
    }
    const spec = product.specification;
    const createdAt = product.createdAt || listingDate; // Ưu tiên createdAt

    document.getElementById("viewTitle").textContent = product.productName;
    document.getElementById("viewPrice").textContent = formatPrice(
      product.price
    );
    document.getElementById("viewProductType").textContent =
      productTypeMap[product.productType] || "Không xác định";
    document.getElementById("viewBrand").textContent = spec.brand || "N/A";
    document.getElementById("viewLocation").textContent = location;
    document.getElementById("viewPhone").textContent = phone;
    document.getElementById("viewDate").textContent = new Date(
      createdAt // Hiển thị ngày tạo gốc
    ).toLocaleString("vi-VN");
    document.getElementById("viewStatus").innerHTML =
      getStatusBadge(listingStatus);
    document.getElementById("viewDescription").textContent =
      product.description;

    document.getElementById("viewImages").innerHTML =
      product.images && product.images.length > 0
        ? product.images
            .map(
              (img) =>
                `<div class="aspect-square overflow-hidden rounded-md"><img src="${BACKEND_ORIGIN}${img.imageUrl}" class="w-full h-full object-cover"></div>`
            )
            .join("")
        : '<p class="text-sm text-gray-500 col-span-full">Không có hình ảnh.</p>';

    const createSpecItem = (label, value) =>
      value
        ? `<div><strong class="text-gray-600">${label}:</strong> <span>${value}</span></div>`
        : "";
    let specsHTML = [
      createSpecItem("Bảo hành", spec.warrantyPolicy),
      createSpecItem("Loại Pin", spec.batteryType),
      createSpecItem("Thời gian sạc", spec.chargeTime),
      createSpecItem("Số lần sạc", spec.chargeCycles),
    ].join("");
    if (product.productType !== "battery") {
      specsHTML += [
        createSpecItem(
          "Quãng đường",
          spec.rangePerCharge ? `${spec.rangePerCharge} km` : null
        ),
        createSpecItem(
          "Số km đã đi",
          spec.mileage ? `${spec.mileage} km` : null
        ),
        createSpecItem("Dung lượng pin", spec.batteryCapacity),
        createSpecItem("Màu sắc", spec.color),
        createSpecItem(
          "Tốc độ tối đa",
          spec.maxSpeed ? `${spec.maxSpeed} km/h` : null
        ),
      ].join("");
    } else {
      specsHTML += [
        createSpecItem("Dung lượng", spec.batteryCapacity),
        createSpecItem("Thời gian đã dùng", spec.batteryLifespan),
        createSpecItem("Tương thích xe", spec.compatibleVehicle),
      ].join("");
    }
    document.getElementById("viewSpecs").innerHTML = specsHTML;
    viewDetailsModal.classList.remove("hidden");
  };

  const updateTableRow = (listingData) => {
    const row = tableBody.querySelector(
      `tr[data-listing-id="${listingData.listingId}"]`
    );
    const isSearching = searchInput.value.trim().length > 0;
    const currentFilterStatus =
      filterContainer.querySelector(".active")?.dataset.status;

    // Logic sắp xếp mới cho PENDING
    const isPendingTab = currentStatus === "PENDING" && !isSearching;

    const statusMatchesFilter =
      isSearching ||
      !currentFilterStatus ||
      (listingData.listingStatus &&
        listingData.listingStatus.toUpperCase() ===
          currentFilterStatus.toUpperCase());

    if (row) {
      if (statusMatchesFilter) {
        const newRowHTML = createListingRowHTML(listingData);
        row.outerHTML = newRowHTML;

        // Nếu là tab PENDING, không sắp xếp lại, chỉ cập nhật
        if (isPendingTab) {
          const updatedRow = tableBody.querySelector(
            `tr[data-listing-id="${listingData.listingId}"]`
          );
          if (updatedRow) {
            updatedRow.classList.add("row-updated");
            setTimeout(() => updatedRow.classList.remove("row-updated"), 1000);
          }
        } else {
          // Các tab khác có thể tải lại để sắp xếp (hoặc logic phức tạp hơn)
          // Tạm thời chỉ cập nhật
          const updatedRow = tableBody.querySelector(
            `tr[data-listing-id="${listingData.listingId}"]`
          );
          if (updatedRow) {
            updatedRow.classList.add("row-updated");
            setTimeout(() => updatedRow.classList.remove("row-updated"), 1000);
          }
        }
      } else {
        // Tin bị chuyển trạng thái (ví dụ PENDING -> ACTIVE)
        row.style.transition = "opacity 0.5s ease-out";
        row.style.opacity = "0";
        setTimeout(() => {
          row.remove();
          checkEmptyTable();
        }, 500);
      }
    } else {
      // Tin mới (ví dụ PENDING mới)
      if (statusMatchesFilter && !isSearching) {
        const newRowHTML = createListingRowHTML(listingData);

        if (isPendingTab) {
          // Thêm vào *cuối* danh sách vì nó là tin mới nhất (sắp xếp cũ nhất trước)
          tableBody.insertAdjacentHTML("beforeend", newRowHTML);
        } else {
          // Thêm vào *đầu* danh sách vì nó là tin mới nhất
          tableBody.insertAdjacentHTML("afterbegin", newRowHTML);
        }

        const newRow = tableBody.querySelector(
          `tr[data-listing-id="${listingData.listingId}"]`
        );
        if (newRow) {
          newRow.classList.add("row-updated");
          setTimeout(() => newRow.classList.remove("row-updated"), 1000);
        }
        checkEmptyTable();
      }
    }
  };

  const removeTableRow = (listingId) => {
    const row = tableBody.querySelector(`tr[data-listing-id="${listingId}"]`);
    if (row) {
      console.log("Xóa hàng real-time:", listingId);
      row.style.transition = "opacity 0.5s ease-out";
      row.style.opacity = "0";
      setTimeout(() => {
        row.remove();
        checkEmptyTable();
      }, 500);
    } else {
      console.log("Không tìm thấy hàng để xóa:", listingId);
    }
  };

  const checkEmptyTable = () => {
    if (tableBody.children.length === 0) {
      tableStatus.textContent = "Không có tin đăng nào phù hợp.";
      tableStatus.style.display = "block";
    } else {
      tableStatus.style.display = "none";
    }
  };

  // --- XỬ LÝ SỰ KIỆN ---
  searchForm.addEventListener("submit", (e) => e.preventDefault());
  searchInput.addEventListener("input", () => {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      const query = searchInput.value.trim();
      if (query.length > 0) {
        const activeFilter = filterContainer.querySelector(".active");
        if (activeFilter) activeFilter.classList.remove("active");
      } else {
        let activeFilter = filterContainer.querySelector(".active");
        if (!activeFilter) {
          activeFilter = filterContainer.querySelector(
            `[data-status="${currentStatus}"]`
          );
          if (activeFilter) activeFilter.classList.add("active");
          else {
            const firstFilter = filterContainer.querySelector(".filter-btn");
            if (firstFilter) {
              firstFilter.classList.add("active");
              currentStatus = firstFilter.dataset.status;
            }
          }
        } else {
          currentStatus = activeFilter.dataset.status;
        }
      }
      fetchListings(0);
    }, 400);
  });

  filterContainer.addEventListener("click", (e) => {
    const button = e.target.closest(".filter-btn");
    if (!button) return;
    searchInput.value = "";
    const currentActive = filterContainer.querySelector(".active");
    if (currentActive) currentActive.classList.remove("active");
    button.classList.add("active");
    currentStatus = button.dataset.status;
    fetchListings(0);
  });

  paginationContainer.addEventListener("click", (e) => {
    const button = e.target.closest(".pagination-btn");
    if (button && !button.disabled) {
      fetchListings(parseInt(button.dataset.page));
    }
  });

  tableBody.addEventListener("click", async (e) => {
    const button = e.target.closest(".action-btn");
    if (!button) return;
    const id = button.dataset.id;
    const action = button.dataset.action;

    try {
      if (action === "view") {
        const listingDetails = await callApi(`${PUBLIC_API_URL}/${id}`);
        populateViewModal(listingDetails);
      } else if (action === "approve") {
        if (confirm("Bạn có chắc muốn DUYỆT tin đăng này?")) {
          button.textContent = "Đang...";
          button.disabled = true;

          const updatedListing = await callApi(
            `${ADMIN_API_URL}/${id}/approve`,
            "POST"
          );
          console.log("Hành động Approve thành công, cập nhật UI ngay.");
          updateTableRow(updatedListing);
        }
      } else if (action === "reject") {
        currentListingId = id;
        rejectModal.classList.remove("hidden");
        rejectReasonInput.value = "";
        rejectReasonInput.focus();
      } else if (action === "verify") {
        if (confirm("Bạn có chắc muốn GẮN NHÃN KIỂM ĐỊNH cho tin này?")) {
          const updatedListing = await callApi(
            `${ADMIN_API_URL}/${id}/verify`,
            "POST"
          );
          alert("Đã gắn nhãn kiểm định thành công!");

          console.log("Hành động Verify thành công, cập nhật UI ngay.");
          updateTableRow(updatedListing);
        }
      }
    } catch (error) {
      alert(`Thao tác thất bại: ${error.message}`);
      // Reset lại nút nếu lỗi
      if (action === "approve" || action === "reject") {
        const originalButton = tableBody.querySelector(
          `button[data-id="${id}"][data-action="${action}"]`
        );
        if (originalButton) {
          originalButton.textContent =
            action === "approve" ? "Duyệt" : "Từ chối";
          originalButton.disabled = false;
        }
      }
    }
  });

  document
    .getElementById("cancelRejectBtn")
    .addEventListener("click", () => rejectModal.classList.add("hidden"));

  document
    .getElementById("confirmRejectBtn")
    .addEventListener("click", async () => {
      const reason = rejectReasonInput.value.trim();
      if (!reason) {
        alert("Vui lòng nhập lý do từ chối.");
        return;
      }
      const rejectButton = document.getElementById("confirmRejectBtn");
      const originalText = rejectButton.textContent;
      try {
        rejectButton.textContent = "Đang xử lý...";
        rejectButton.disabled = true;

        const updatedListing = await callApi(
          `${ADMIN_API_URL}/${currentListingId}/reject`,
          "POST",
          {
            reason,
          }
        );
        rejectModal.classList.add("hidden");

        console.log("Hành động Reject thành công, cập nhật UI ngay.");
        updateTableRow(updatedListing);
      } catch (error) {
        alert(`Lỗi: ${error.message}`);
      } finally {
        rejectButton.textContent = originalText;
        rejectButton.disabled = false;
      }
    });

  document
    .getElementById("closeViewModalBtn")
    .addEventListener("click", () => viewDetailsModal.classList.add("hidden"));

  document.getElementById("logoutBtn").addEventListener("click", () => {
    localStorage.removeItem("admin_user");
    alert("Đã đăng xuất.");
    window.location.reload();
  });

  // --- LOGIC WEBSOCKET ---
  const onAdminUpdateReceived = (payload) => {
    try {
      const messageData = JSON.parse(payload.body);
      console.log("Nhận được cập nhật qua WebSocket:", messageData);

      if (messageData.action === "delete" && messageData.listingId) {
        removeTableRow(messageData.listingId);
      } else if (messageData.listingId && messageData.listingStatus) {
        if (
          !(messageData.product && messageData.product.productName) &&
          messageData.listingStatus === "PENDING"
        ) {
          console.warn("WS: Tin PENDING mới bị thiếu dữ liệu, gọi API...");

          // Thử gọi API để lấy dữ liệu đầy đủ
          setTimeout(() => {
            callApi(`${PUBLIC_API_URL}/${messageData.listingId}`)
              .then((fullListingData) => {
                if (fullListingData) {
                  console.log(
                    "API: Lấy full data cho tin PENDING thành công, cập nhật UI."
                  );
                  // Cập nhật listingData với dữ liệu đầy đủ
                  updateTableRow(fullListingData);
                }
              })
              .catch((err) => {
                console.error("API: Lỗi khi lấy full data cho WS update:", err);
                // Vẫn cập nhật với dữ liệu bị thiếu
                updateTableRow(messageData);
              });
          }, 500);
        } else {
          console.log(
            "WS: Dữ liệu đầy đủ, hoặc là tin đã được xử lý (approve/reject). Cập nhật UI."
          );
          updateTableRow(messageData);
        }
      } else {
        console.warn(
          "Nhận được tin nhắn WebSocket không hợp lệ. Nội dung:",
          messageData
        );
      }
    } catch (e) {
      console.error("Lỗi xử lý thông báo WebSocket:", e);
      console.error("Nội dung thô (raw) gây lỗi:", payload.body);
    }
  };

  const connectWebSocketForAdmin = () => {
    try {
      const socket = new SockJS(WS_URL);
      stompClient = Stomp.over(socket);
      stompClient.debug = null;
      stompClient.connect(
        {},
        (frame) => {
          console.log("Admin đã kết nối WebSocket:", frame);
          stompClient.subscribe(
            `/topic/admin/listingUpdate`,
            onAdminUpdateReceived
          );
        },
        (error) => {
          console.error("Lỗi WebSocket Admin:", error.toString());
          setTimeout(connectWebSocketForAdmin, 7000 + Math.random() * 3000);
        }
      );
    } catch (e) {
      console.error("Không thể khởi tạo SockJS cho Admin:", e);
      setTimeout(connectWebSocketForAdmin, 10000);
    }
  };

  // --- KHỞI CHẠY ---
  fetchListings(0);
  connectWebSocketForAdmin();
});
