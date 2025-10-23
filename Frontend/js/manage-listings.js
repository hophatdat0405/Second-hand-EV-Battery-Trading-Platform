// Thay thế toàn bộ file: manage-listings.js
document.addEventListener("DOMContentLoaded", () => {
  const API_BASE_URL = "http://localhost:8080/api";
  const WS_URL = "http://localhost:8080/ws"; // Endpoint WebSocket
  const BACKEND_ORIGIN = "http://localhost:8080";
  const FAKE_USER_ID = 1;
  const PAGE_SIZE = 12;

  const urlParams = new URLSearchParams(window.location.search);
  const highlightListingId = urlParams.get("listing_id");

  const listingsContainer = document.getElementById("listingsContainer");
  const loadingText = document.getElementById("loadingText");
  const paginationContainer = document.getElementById("paginationContainer");
  const editModal = document.getElementById("editModal");
  const editForm = document.getElementById("editForm");
  const confirmModal = document.getElementById("confirmModal");
  const currentImagesPreview = document.getElementById("currentImagesPreview");
  const newImagesPreview = document.getElementById("newImagesPreview");
  const editImageUpload = document.getElementById("editImageUpload");
  const viewDetailsModal = document.getElementById("viewDetailsModal");
  const cancelEditBtn = document.getElementById("cancelEditBtn");

  let userListingsPageContent = [];
  let currentEditingListing = null;
  let currentPage = 0;
  let totalPages = 0;
  let currentAction = null;
  let currentTargetId = null;
  const newFilesDataTransfer = new DataTransfer();
  let stompClient = null;

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
  const getStatusBadge = (status, isLarge = false) => {
    const statuses = {
      PENDING: "bg-yellow-100 text-yellow-800",
      ACTIVE: "bg-green-100 text-green-800",
      SOLD: "bg-gray-100 text-gray-800",
      REJECTED: "bg-red-100 text-red-800",
    };
    const sizeClass = isLarge ? "px-3 py-1 text-sm" : "px-2 text-xs";
    return `<span class="${sizeClass} inline-flex leading-5 font-semibold rounded-full ${
      statuses[status] || "bg-gray-100 text-gray-800"
    }">${status}</span>`;
  };
  async function callApi(endpoint, method = "GET", body = null) {
    const options = { method, headers: {} };
    if (body) {
      options.body = JSON.stringify(body);
      options.headers["Content-Type"] = "application/json";
    }
    const response = await fetch(`${API_BASE_URL}${endpoint}`, options);
    if (!response.ok) {
      const errorData = await response
        .json()
        .catch(() => ({ message: `Lỗi HTTP: ${response.statusText}` }));
      throw new Error(
        errorData.message || `Lỗi không xác định (${response.status})`
      );
    }
    if (response.status === 204) {
      return null;
    }
    return response.json();
  }

  const createListingCardHTML = (listing) => {
    // Safety check for product data (especially from WS)
    if (!listing.product) {
      console.warn("Dữ liệu listing thiếu product, không thể render:", listing);
      // Optionally return a placeholder or an empty string
      return ``;
    }

    const {
      product,
      updatedOnce, // <-- Logic này đã có ở đây
      listingStatus,
      listingId,
      adminNotes,
      updatedAt,
    } = listing;

    const imageUrl =
      product.images && product.images.length > 0
        ? `${BACKEND_ORIGIN}${product.images[0].imageUrl}`
        : "https://placehold.co/300x200/e2e8f0/4a5568?text=No+Image";

    // ✅ Logic ẩn nút sửa của bạn vẫn còn ở đây
    const canEdit = !updatedOnce && listingStatus !== "SOLD";
    const showMarkAsSoldButton = listingStatus !== "REJECTED";
    const isAlreadySold = listingStatus === "SOLD";

    const reasonHtml =
      listingStatus === "REJECTED" && adminNotes
        ? `<p class="text-xs text-red-700 mt-2 cursor-pointer font-semibold action-btn" data-action="view-reason" data-reason="${String(
            adminNotes
          ).replace(/"/g, "&quot;")}
         ">
           <i class="fas fa-info-circle"></i> Xem lý do từ chối
         </p>`
        : "";

    return `
      <div class="bg-white rounded-lg shadow-md overflow-hidden flex" data-listing-id="${listingId}">
        <img src="${imageUrl}" alt="${
      product.productName || "N/A" // Add fallback
    }" class="w-48 h-auto object-cover hidden sm:block">
        <div class="p-4 flex flex-col justify-between flex-grow">
          <div>
            <div class="flex justify-between items-start"> <h3 class="text-lg font-bold text-gray-800">${
              product.productName || "[Không có tiêu đề]" // Add fallback
            }</h3> ${getStatusBadge(listingStatus)} </div>
            <p class="text-red-600 font-semibold mt-1">${formatPrice(
              product.price
            )}</p>
            <p class="text-sm text-gray-500 mt-2">Ngày đăng tin: ${new Date(
              updatedAt
            ).toLocaleDateString("vi-VN")}</p>
            ${reasonHtml}
          </div>
          <div class="flex justify-end space-x-2 mt-4">
            <button data-action="view" data-id="${listingId}" class="action-btn bg-white border border-gray-300 text-gray-700 px-3 py-1 text-sm rounded-md hover:bg-gray-50">Xem chi tiết</button>
            ${
              showMarkAsSoldButton
                ? `<button
                    data-action="markSold"
                    data-id="${listingId}"
                    class="action-btn text-white px-3 py-1 text-sm rounded-md ${
                      isAlreadySold
                        ? "bg-gray-400 cursor-not-allowed"
                        : "bg-blue-500 hover:bg-blue-600"
                    }"
                    ${isAlreadySold ? "disabled" : ""}>
                    ${isAlreadySold ? "Đã Bán" : "Xác nhận bán"}
                   </button>`
                : ""
            }
            ${
              canEdit // <-- Logic của bạn được áp dụng ở đây
                ? `<button data-action="edit" data-id="${listingId}" class="action-btn bg-gray-700 text-white px-3 py-1 text-sm rounded-md hover:bg-gray-800">Chỉnh sửa</button>`
                : ""
            }
            <button data-action="delete" data-id="${listingId}" class="action-btn bg-red-600 text-white px-3 py-1 text-sm rounded-md hover:bg-red-700">Xóa</button>
          </div>
        </div>
      </div>`;
  };

  const renderListings = (listings) => {
    loadingText.style.display = "none";
    listingsContainer.innerHTML =
      listings.length > 0
        ? listings
            .map(createListingCardHTML)
            .filter((html) => html)
            .join("") // Filter out empty strings
        : "<p class='text-center text-gray-500'>Bạn chưa có tin đăng nào.</p>";
  };

  const renderPagination = () => {
    if (totalPages <= 1) {
      paginationContainer.innerHTML = "";
      return;
    }
    let paginationHTML = "";
    paginationHTML += `<button data-page="${
      currentPage - 1
    }" class="pagination-btn px-4 py-2 rounded-md bg-white border" ${
      currentPage === 0 ? "disabled" : ""
    }>Trước</button>`;
    for (let i = 0; i < totalPages; i++) {
      paginationHTML += `<button data-page="${i}" class="pagination-btn px-4 py-2 rounded-md border ${
        i === currentPage ? "active" : "bg-white"
      }">${i + 1}</button>`;
    }
    paginationHTML += `<button data-page="${
      currentPage + 1
    }" class="pagination-btn px-4 py-2 rounded-md bg-white border" ${
      currentPage >= totalPages - 1 ? "disabled" : ""
    }>Sau</button>`;
    paginationContainer.innerHTML = paginationHTML;
  };

  const updateListingCard = (listingData) => {
    const cardElement = listingsContainer.querySelector(
      `[data-listing-id="${listingData.listingId}"]`
    );
    if (cardElement) {
      console.log("Cập nhật real-time cho card:", listingData.listingId);
      const newCardHTML = createListingCardHTML(listingData);
      if (newCardHTML) {
        // Only replace if HTML is valid
        cardElement.outerHTML = newCardHTML;
      } else {
        console.error("Không thể tạo HTML mới cho listing:", listingData);
        cardElement.style.border = "2px solid red"; // Highlight error
        return; // Stop further processing for this card
      }

      const index = userListingsPageContent.findIndex(
        (l) => l.listingId == listingData.listingId
      );
      if (index > -1) {
        userListingsPageContent[index] = listingData;
      }

      const updatedCard = listingsContainer.querySelector(
        `[data-listing-id="${listingData.listingId}"]`
      );
      if (updatedCard) {
        updatedCard.classList.add(
          "bg-yellow-100",
          "transition",
          "duration-500"
        );
        setTimeout(() => {
          if (updatedCard) updatedCard.classList.remove("bg-yellow-100");
        }, 1000);
      }
    } else {
      console.log(
        "Không tìm thấy card để cập nhật:",
        listingData.listingId,
        ". Tải lại trang."
      );
      fetchUserListings(FAKE_USER_ID, currentPage);
    }
  };

  const highlightCard = () => {
    if (highlightListingId) {
      setTimeout(() => {
        const cardToHighlight = document.querySelector(
          `[data-listing-id="${highlightListingId}"]`
        );
        if (cardToHighlight) {
          console.log("Tìm thấy thẻ để highlight:", cardToHighlight);
          cardToHighlight.classList.add("listing-highlight");
          requestAnimationFrame(() => {
            cardToHighlight.scrollIntoView({
              behavior: "smooth",
              block: "center",
            });
          });
          setTimeout(() => {
            if (cardToHighlight)
              cardToHighlight.classList.remove("listing-highlight");
          }, 2500);
        } else {
          console.warn(
            "Không tìm thấy thẻ với ID:",
            highlightListingId,
            "trong DOM sau khi chờ."
          );
        }
      }, 150);

      const url = new URL(window.location);
      url.searchParams.delete("listing_id");
      window.history.replaceState({}, "", url);
    }
  };
  const fetchUserListings = async (userId, page = 0) => {
    try {
      loadingText.style.display = "block";
      const data = await callApi(
        `/listings/user/${userId}?page=${page}&size=${PAGE_SIZE}`
      );
      userListingsPageContent = data.content;
      currentPage = data.number;
      totalPages = data.totalPages;
      renderListings(userListingsPageContent);
      renderPagination();

      highlightCard();
    } catch (error) {
      loadingText.textContent = `Lỗi khi tải tin đăng: ${error.message}`;
    }
  };
  const openConfirmModal = (action, id) => {
    currentAction = action;
    currentTargetId = id;
    const title = document.getElementById("confirmModalTitle");
    const text = document.getElementById("confirmModalText");
    const btn = document.getElementById("confirmBtn");
    if (action === "delete") {
      title.textContent = "Xác nhận xóa tin đăng?";
      text.textContent =
        "Hành động này không thể hoàn tác. Bạn có chắc chắn muốn xóa vĩnh viễn tin này không?";
      btn.className =
        "bg-red-600 text-white px-4 py-2 rounded-md hover:bg-red-700";
    } else if (action === "markSold") {
      title.textContent = "Xác nhận đã bán sản phẩm?";
      text.textContent =
        'Sau khi xác nhận, tin sẽ được chuyển sang trạng thái "Đã Bán" và không thể chỉnh sửa. Bạn có chắc chắn?';
      btn.className =
        "bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700";
    }
    confirmModal.classList.remove("hidden");
  };
  const closeModal = (modal) => modal.classList.add("hidden");
  const executeAction = async () => {
    closeModal(confirmModal);
    try {
      if (currentAction === "delete") {
        await callApi(`/listings/${currentTargetId}`, "DELETE");
        alert("Đã xóa tin đăng thành công.");
        if (userListingsPageContent.length === 1 && currentPage > 0) {
          fetchUserListings(FAKE_USER_ID, currentPage - 1);
        } else {
          fetchUserListings(FAKE_USER_ID, currentPage);
        }
      } else if (currentAction === "markSold") {
        const updatedListing = await callApi(
          `/listings/${currentTargetId}/mark-as-sold`,
          "PUT"
        );
        alert('Đã cập nhật trạng thái thành "Đã Bán".');
        updateListingCard(updatedListing);
      }
    } catch (error) {
      alert(`Lỗi: ${error.message}`);
    }
  };

  const renderSpecificFields = (type, spec) => {
    const container = document.getElementById("editSpecificFields");
    const createInput = (label, name, value, type = "text") =>
      `<div><label for="edit_${name}" class="block text-sm font-medium text-gray-700">${label}</label><input type="${type}" id="edit_${name}" name="${name}" value="${
        value || ""
      }" class="mt-1 block w-full form-input border-gray-300 rounded-md shadow-sm"></div>`;
    let fieldsHTML = [
      createInput("Loại Pin", "batteryType", spec.batteryType),
      createInput("Thời Gian Sạc", "chargeTime", spec.chargeTime),
      createInput("Số Lần Sạc", "chargeCycles", spec.chargeCycles, "number"),
    ].join("");
    if (type !== "battery") {
      fieldsHTML += [
        createInput(
          "Quãng Đường (Km)",
          "rangePerCharge",
          spec.rangePerCharge,
          "number"
        ),
        createInput("Số Km Đã Đi", "mileage", spec.mileage, "number"),
        createInput("Dung Lượng Pin", "batteryCapacity", spec.batteryCapacity),
        createInput("Màu Sắc", "color", spec.color),
        createInput(
          "Tốc Độ Tối Đa (Km/h)",
          "maxSpeed",
          spec.maxSpeed,
          "number"
        ),
      ].join("");
    } else {
      fieldsHTML += [
        createInput("Dung Lượng", "batteryCapacity", spec.batteryCapacity),
        createInput(
          "Thời Gian Đã Dùng",
          "batteryLifespan",
          spec.batteryLifespan
        ),
        createInput(
          "Tương Thích Xe",
          "compatibleVehicle",
          spec.compatibleVehicle
        ),
      ].join("");
    }
    container.innerHTML = fieldsHTML;
  };
  const openEditModal = (listing) => {
    currentEditingListing = listing;
    const { product, phone, location, listingStatus } = listing;
    const spec = product.specification;
    editForm.reset();
    newFilesDataTransfer.items.clear();
    newImagesPreview.innerHTML = "";
    document.getElementById("editListingId").value = listing.listingId;
    document.getElementById("editProductType").value =
      productTypeMap[product.productType] || "Không xác định";
    document.getElementById("editProductName").value =
      product.productName || "";
    document.getElementById("editBrand").value = spec.brand || "";
    document.getElementById("editPrice").value = product.price || "";
    document.getElementById("editDescription").value =
      product.description || "";
    document.getElementById("editPhone").value = phone || "";
    document.getElementById("editLocation").value = location || "";
    document.getElementById("editWarranty").value = spec.warrantyPolicy || "";
    renderCurrentImages(product.images);
    renderSpecificFields(product.productType, spec);
    const allFields = editForm.querySelectorAll("input, textarea, select");

    // Chỉ khóa các trường khi tin đang "ACTIVE"
    if (listingStatus === "ACTIVE") {
      const nonEditableFieldIDs = [
        "editProductType",
        "editProductName",
        "editBrand",
      ];
      allFields.forEach((field) => {
        let isSpecField = field.id.startsWith("edit_");
        if (
          field.type !== "hidden" &&
          (nonEditableFieldIDs.includes(field.id) || isSpecField)
        ) {
          field.disabled = true;
        } else {
          field.disabled = false;
        }
      });
    } else {
      // Trạng thái PENDING hoặc REJECTED sẽ rơi vào đây
      allFields.forEach((field) => {
        if (field.type !== "hidden") field.disabled = false;
      });
    }
    // Luôn luôn khóa trường "Loại sản phẩm"
    document.getElementById("editProductType").disabled = true;

    editModal.classList.remove("hidden");
  };
  const renderCurrentImages = (images) => {
    currentImagesPreview.innerHTML = "";
    if (images && images.length > 0) {
      images.forEach((img) => {
        const imgContainer = document.createElement("div");
        imgContainer.className =
          "image-container relative aspect-square overflow-hidden rounded-md";
        imgContainer.innerHTML = `
                <img src="${BACKEND_ORIGIN}${img.imageUrl}" class="w-full h-full object-cover">
                <div class="delete-img-btn" data-image-id="${img.imageId}">×</div>
            `;
        currentImagesPreview.appendChild(imgContainer);
      });
    } else {
      currentImagesPreview.innerHTML = `<p class="text-sm text-gray-500 col-span-full">Chưa có ảnh nào.</p>`;
    }
  };
  const openViewDetailsModal = (listing) => {
    const { product, phone, location, listingStatus, listingDate } = listing;
    const spec = product.specification;
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
      listingDate
    ).toLocaleDateString("vi-VN");
    document.getElementById("viewStatus").innerHTML = getStatusBadge(
      listingStatus,
      true
    );
    document.getElementById("viewDescription").textContent =
      product.description;
    const viewImagesContainer = document.getElementById("viewImages");
    viewImagesContainer.innerHTML = "";
    if (product.images && product.images.length > 0) {
      product.images.forEach((img) => {
        viewImagesContainer.innerHTML += `<div class="aspect-square overflow-hidden rounded-md"><img src="${BACKEND_ORIGIN}${img.imageUrl}" class="w-full h-full object-cover"></div>`;
      });
    } else {
      viewImagesContainer.innerHTML =
        '<p class="text-sm text-gray-500 col-span-full">Không có hình ảnh.</p>';
    }
    const viewSpecsContainer = document.getElementById("viewSpecs");
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
        createSpecItem("Tương thích Xe", spec.compatibleVehicle),
      ].join("");
    }
    viewSpecsContainer.innerHTML = specsHTML;
    viewDetailsModal.classList.remove("hidden");
  };

  const handleFileSelect = (e) => {
    Array.from(e.target.files).forEach((file) =>
      newFilesDataTransfer.items.add(file)
    );
    e.target.files = newFilesDataTransfer.files;
    renderNewImagesPreview();
  };
  const renderNewImagesPreview = () => {
    newImagesPreview.innerHTML = "";
    Array.from(newFilesDataTransfer.files).forEach((file, index) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const imgContainer = document.createElement("div");
        imgContainer.className =
          "image-container relative aspect-square overflow-hidden rounded-md";
        imgContainer.innerHTML = `
                <img src="${e.target.result}" class="w-full h-full object-cover">
                <div class="delete-img-btn" data-file-index="${index}">×</div>
            `;
        newImagesPreview.appendChild(imgContainer);
      };
      reader.readAsDataURL(file);
    });
  };
  const handleDeleteNewImage = (e) => {
    const deleteButton = e.target.closest(".delete-img-btn[data-file-index]");
    if (!deleteButton) return;
    const indexToRemove = parseInt(deleteButton.dataset.fileIndex, 10);
    newFilesDataTransfer.items.remove(indexToRemove);
    editImageUpload.files = newFilesDataTransfer.files;
    renderNewImagesPreview();
  };

  const handleFormSubmit = async (e) => {
    e.preventDefault();
    const submitButton = editForm.querySelector('button[type="submit"]');
    const originalButtonText = submitButton.querySelector("span").textContent;
    submitButton.disabled = true;
    submitButton.querySelector("span").textContent = "Đang xử lý...";
    const listingId = document.getElementById("editListingId").value;
    try {
      const imageFiles = newFilesDataTransfer.files;
      if (imageFiles && imageFiles.length > 0) {
        submitButton.querySelector("span").textContent = "Đang thêm ảnh...";
        const imageFormData = new FormData();
        for (const file of imageFiles) {
          imageFormData.append("images", file);
        }
        const imageResponse = await fetch(
          `${API_BASE_URL}/listings/${listingId}/add-images`,
          { method: "POST", body: imageFormData }
        );
        if (!imageResponse.ok) throw new Error("Thêm ảnh mới thất bại.");
      }
      submitButton.querySelector("span").textContent = "Đang lưu thông tin...";
      const formData = new FormData(e.target);
      const data = Object.fromEntries(formData.entries());
      data.price = parseInt(String(data.price).replace(/\D/g, "")) || 0;
      [
        "chargeCycles",
        "rangePerCharge",
        "mileage",
        "maxSpeed",
        "yearOfManufacture",
      ].forEach((key) => {
        if (data[key]) data[key] = parseInt(data[key], 10);
        else delete data[key];
      });

      const updatedListing = await callApi(
        `/listings/${listingId}/update-details`,
        "PUT",
        data
      );

      closeModal(editModal);
      alert(
        "Cập nhật tin đăng thành công! Tin của bạn đã được gửi để duyệt lại."
      );

      updateListingCard(updatedListing);
    } catch (error) {
      alert(`Lỗi khi cập nhật: ${error.message}`);
    } finally {
      submitButton.disabled = false;
      submitButton.querySelector("span").textContent = originalButtonText;
    }
  };
  const handleDeleteExistingImage = async (e) => {
    const deleteButton = e.target.closest(".delete-img-btn[data-image-id]");
    if (!deleteButton) return;
    const imageId = deleteButton.dataset.imageId;
    const listingId = currentEditingListing.listingId;
    if (!imageId || !listingId) return;
    if (confirm("Bạn có chắc chắn muốn xóa ảnh này không?")) {
      try {
        await callApi(`/listings/${listingId}/delete-image/${imageId}`, "POST");
        deleteButton.parentElement.remove();
        currentEditingListing.product.images =
          currentEditingListing.product.images.filter(
            (img) => img.imageId != imageId
          );
        if (currentImagesPreview.children.length === 0) {
          currentImagesPreview.innerHTML = `<p class="text-sm text-gray-500 col-span-full">Chưa có ảnh nào.</p>`;
        }
      } catch (error) {
        alert(`Lỗi khi xóa ảnh: ${error.message}`);
      }
    }
  };

  listingsContainer.addEventListener("click", (e) => {
    const button = e.target.closest(".action-btn");
    if (!button) return;
    const action = button.dataset.action;

    if (action === "view-reason") {
      const reason = button.dataset.reason;
      alert(`Lý do từ chối từ Admin:\n\n${reason}`);
      return;
    }

    const id = button.dataset.id;
    const listing = userListingsPageContent.find((l) => l.listingId == id);
    if (!listing) return;

    currentEditingListing = listing;

    if (action === "edit") {
      // ✅✅✅ THÊM CẢNH BÁO TẠI ĐÂY ✅✅✅
      const confirmMessage =
        "Bạn có chắc chắn muốn chỉnh sửa tin này?\n\n" +
        "Lưu ý: Bạn chỉ có thể chỉnh sửa tin đăng một lần duy nhất và tin sẽ được gửi đi để duyệt lại.";
      if (confirm(confirmMessage)) {
        openEditModal(listing);
      }
    } else if (action === "view") {
      openViewDetailsModal(listing);
    } else {
      openConfirmModal(action, id);
    }
  });

  paginationContainer.addEventListener("click", (e) => {
    const button = e.target.closest(".pagination-btn");
    if (button && !button.disabled) {
      const page = parseInt(button.dataset.page, 10);
      fetchUserListings(FAKE_USER_ID, page);
    }
  });

  document.getElementById("confirmBtn").addEventListener("click", () => {
    executeAction();
  });

  document
    .getElementById("cancelConfirmBtn")
    .addEventListener("click", () => closeModal(confirmModal));
  document
    .getElementById("closeViewModalBtn")
    .addEventListener("click", () => closeModal(viewDetailsModal));
  editForm.addEventListener("submit", handleFormSubmit);
  currentImagesPreview.addEventListener("click", handleDeleteExistingImage);
  newImagesPreview.addEventListener("click", handleDeleteNewImage);
  editImageUpload.addEventListener("change", handleFileSelect);
  cancelEditBtn.addEventListener("click", () => closeModal(editModal));

  // --- LOGIC WEBSOCKET ---
  const onListingUpdateReceived = (payload) => {
    try {
      const listingData = JSON.parse(payload.body);
      console.log("Nhận được cập nhật listing trực tiếp:", listingData);

      if (listingData && listingData.listingId && listingData.listingStatus) {
        // Check again for product data, just in case
        if (!listingData.product) {
          console.warn(
            "WS: Dữ liệu update thiếu product, gọi API",
            listingData
          );
          // Optionally add a small delay before calling API
          setTimeout(() => {
            callApi(`/listings/${listingData.listingId}`)
              .then((fullData) => {
                if (fullData) updateListingCard(fullData);
              })
              .catch((err) =>
                console.error("WS fallback API call failed:", err)
              );
          }, 300); // 300ms delay
          return; // Don't update with incomplete data
        }
        // Data is likely complete, update the card
        updateListingCard(listingData);
      } else {
        console.warn("WS: Nhận được dữ liệu update không hợp lệ", listingData);
      }
    } catch (e) {
      console.error("Lỗi xử lý thông báo WebSocket:", e);
      console.error("Payload body causing error:", payload.body);
    }
  };

  const connectWebSocket = () => {
    try {
      const socket = new SockJS(WS_URL);
      stompClient = Stomp.over(socket);
      stompClient.debug = null;
      stompClient.connect(
        {},
        (frame) => {
          console.log("User đã kết nối WebSocket:", frame);

          // Subscribe to the NEW topic for listing updates
          stompClient.subscribe(
            `/user/${FAKE_USER_ID}/topic/listingUpdates`,
            onListingUpdateReceived
          );

          // Keep subscribing to the OLD topic for general notifications (bell)
          // but this page doesn't need to act on them directly.
          stompClient.subscribe(
            `/user/${FAKE_USER_ID}/topic/notifications`,
            (payload) => {
              console.log("Received notification (for bell):", payload.body);
              // No action needed on this page for bell notifications
            }
          );
        },
        (error) => {
          console.error("Lỗi WebSocket User:", error.toString());
          setTimeout(connectWebSocket, 6000 + Math.random() * 4000);
        }
      );
    } catch (e) {
      console.error("Không thể khởi tạo SockJS cho User:", e);
      setTimeout(connectWebSocket, 10000);
    }
  };

  // --- KHỞI CHẠY ---
  const initializePage = async () => {
    let pageToLoad = 0;
    if (highlightListingId) {
      try {
        const page = await callApi(
          `/listings/user/${FAKE_USER_ID}/find-page?listingId=${highlightListingId}&size=${PAGE_SIZE}`
        );
        pageToLoad = page !== null ? page : 0;
      } catch (error) {
        console.error(
          "Không tìm thấy trang cho tin đăng, tải trang đầu tiên.",
          error
        );
        pageToLoad = 0;
      }
    }
    fetchUserListings(FAKE_USER_ID, pageToLoad);
  };

  initializePage();
  connectWebSocket();
});
