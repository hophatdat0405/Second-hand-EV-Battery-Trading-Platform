// --- CONFIGURATION ---
const API_BASE_URL = "http://localhost:8080/api";
const FAKE_USER_ID = 1;
let isSubmitting = false;

// THÊM BIẾN TOÀN CỤC ĐỂ LƯU FILE ẢNH
let allImageFiles = [];

// --- MÔ PHỎNG USER SERVICE ---
async function getFakeUserData(userId) {
  console.log(`Đang lấy thông tin giả lập cho User ID: ${userId}...`);
  await new Promise((resolve) => setTimeout(resolve, 500));
  return {
    id: userId,
    name: "Người Dùng Mặc Định",
    email: "user@example.com",
    phone: "0912345678",
    address: "Quận 1, TP.HCM",
  };
}

// --- DOM ELEMENT REFERENCES ---
const productTypeSelector = document.getElementById("productTypeSelector");
const categoryInput = document.getElementById("category");
const specificFieldsContainer = document.getElementById("specificFields");
const sellForm = document.getElementById("sellForm");
const submitButton = sellForm.querySelector('button[type="submit"]');
const imagePreview = document.getElementById("imagePreview");
const imageUploadInput = document.getElementById("imageUpload"); // Thêm
const brandField = document.getElementById("brandField");
const yearField = document.getElementById("yearField");

let selectedType = null;

// --- EVENT LISTENERS ---
productTypeSelector.addEventListener("click", (e) => {
  const button = e.target.closest(".product-type-btn");
  if (!button) return;
  setActiveButton(button);
  selectedType = button.dataset.type;
  categoryInput.value = button.textContent.trim();
  updateFormVisibility(selectedType);
});

sellForm.addEventListener("submit", handleFormSubmit);
window.addEventListener("load", initializeForm);

// THÊM EVENT LISTENER MỚI CHO VIỆC CHỌN FILE VÀ XÓA ẢNH
imageUploadInput.addEventListener("change", handleFileSelection);
imagePreview.addEventListener("click", handleImageDeletion);

// --- CORE FUNCTIONS ---

/**
 * Tải file ảnh lên server và trả về danh sách URL thật
 * @param {File[]} imageFiles - Danh sách file từ input
 * @returns {Promise<string[]>} - Một mảng chứa các URL thật của ảnh
 */
async function uploadImages(imageFiles) {
  const formData = new FormData();
  imageFiles.forEach((file) => {
    // Tên "files" phải khớp với @RequestParam("files") ở backend
    formData.append("files", file);
  });

  // Lưu ý: Không cần set 'Content-Type' header, trình duyệt sẽ tự làm đúng
  const response = await fetch(`${API_BASE_URL}/files/upload`, {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Tải ảnh lên thất bại: ${errorText}`);
  }
  return response.json(); // Trả về một Promise chứa danh sách URL
}

async function handleFormSubmit(event) {
  event.preventDefault();
  if (isSubmitting) return;

  const formData = new FormData(event.target);

  // Kiểm tra các trường bắt buộc
  if (!selectedType) {
    showMessage("Vui lòng chọn loại sản phẩm.", "error");
    return;
  }

  isSubmitting = true;
  const originalButtonText = submitButton.innerHTML;
  submitButton.disabled = true;
  submitButton.innerHTML = `<i class="fas fa-spinner fa-spin mr-2"></i> Đang xử lý...`;

  try {
    // BƯỚC 1: TẢI ẢNH LÊN SERVER TRƯỚC (NẾU CÓ)
    // THAY ĐỔI Ở ĐÂY: Lấy file từ mảng allImageFiles
    const imageFiles = allImageFiles;

    // THÊM VALIDATION: Kiểm tra xem có ảnh nào không
    if (imageFiles.length === 0) {
      showMessage("Vui lòng tải lên ít nhất một hình ảnh.", "error");
      // Đặt lại trạng thái nút submit
      isSubmitting = false;
      submitButton.disabled = false;
      submitButton.innerHTML = originalButtonText;
      return; // Dừng hàm
    }

    let imageUrls = [];
    if (imageFiles.length > 0) {
      submitButton.innerHTML = `<i class="fas fa-spinner fa-spin mr-2"></i> Đang tải ảnh lên...`;
      imageUrls = await uploadImages(imageFiles);
    }

    submitButton.innerHTML = `<i class="fas fa-spinner fa-spin mr-2"></i> Đang lưu thông tin...`;

    // BƯỚC 2: TẠO PRODUCT
    const productData = {
      productName: formData.get("title"),
      productType: selectedType,
      price: parseInt(formData.get("price").replace(/\D/g, ""), 10),
      description: formData.get("description"),
      // status: "available", // <-- ĐÃ XÓA DÒNG NÀY
    };
    const createdProduct = await callApi("/products", "POST", productData);
    const productId = createdProduct.productId;

    // BƯỚC 3: TẠO SPECIFICATION VÀ LISTING
    const specData = {
      product: { productId: productId },
      condition: { conditionId: parseInt(formData.get("condition_id"), 10) },
      brand: formData.get("brand"),
      yearOfManufacture: parseInt(formData.get("year_of_manufacture"), 10),
      warrantyPolicy: formData.get("warranty_status"),
      chargeTime: formData.get("charge_time"),
      chargeCycles: formData.get("charge_cycles")
        ? parseInt(formData.get("charge_cycles"), 10)
        : null,
      batteryType: formData.get("battery_type"),
    };
    if (selectedType !== "battery") {
      specData.mileage = parseInt(formData.get("mileage"), 10);
      specData.batteryCapacity = formData.get("battery_capacity");
      specData.maxSpeed = parseInt(formData.get("max_speed"), 10);
      specData.rangePerCharge = parseInt(formData.get("range"), 10);
      specData.color = formData.get("color");
    } else {
      specData.batteryLifespan = `${formData.get("usage_time")} months`;
      specData.batteryCapacity = formData.get("battery_capacity_pin");
      specData.compatibleVehicle = formData.get("compatibility");
    }
    await callApi("/specifications", "POST", specData);

    const listingData = {
      product: { productId: productId },
      userId: FAKE_USER_ID,
      phone: formData.get("phone"),
      location: formData.get("location"),
    };
    await callApi("/listings", "POST", listingData);

    // BƯỚC 4: TẠO CÁC PRODUCTIMAGE VỚI URL THẬT
    if (imageUrls.length > 0) {
      const imagePromises = imageUrls.map((url) => {
        const imageData = {
          product: { productId: productId },
          imageUrl: url, // <-- SỬ DỤNG URL THẬT
          // imageType: "product_image", // <-- ĐÃ XÓA DÒNG NÀY
        };
        return callApi("/images", "POST", imageData);
      });
      await Promise.all(imagePromises);
    }

    showMessage(
      "Đăng tin thành công! Tin của bạn sẽ sớm được duyệt.",
      "success"
    );
    resetForm();
  } catch (error) {
    console.error("Lỗi khi đăng tin:", error);
    showMessage(`Đã xảy ra lỗi: ${error.message}.`, "error");
  } finally {
    isSubmitting = false;
    submitButton.disabled = false;
    submitButton.innerHTML = originalButtonText;
  }
}

async function callApi(endpoint, method, body = null) {
  const options = {
    method,
    headers: { "Content-Type": "application/json" },
  };
  if (body) {
    options.body = JSON.stringify(body);
  }
  const response = await fetch(`${API_BASE_URL}${endpoint}`, options);
  if (!response.ok) {
    const errorData = await response
      .json()
      .catch(() => ({ message: response.statusText }));
    throw new Error(
      `API Error (${response.status}): ${errorData.message || "Unknown error"}`
    );
  }
  const contentType = response.headers.get("content-type");
  if (contentType && contentType.includes("application/json")) {
    return response.json();
  }
}

// --- UTILITY & UI FUNCTIONS ---
function resetForm() {
  sellForm.reset();
  categoryInput.value = "Chưa chọn";
  imagePreview.innerHTML = "";
  allImageFiles = []; // QUAN TRỌNG: Reset mảng file
  selectedType = null;
  setActiveButton(null);
  updateFormVisibility(null);
  initializeForm();
}

async function initializeForm() {
  try {
    const userData = await getFakeUserData(FAKE_USER_ID);
    const phoneInput = document.getElementById("phone");
    const locationInput = document.getElementById("location");
    if (userData.phone && !phoneInput.value) {
      phoneInput.value = userData.phone;
    }
    if (userData.address && !locationInput.value) {
      locationInput.value = userData.address;
    }
  } catch (error) {
    console.error("Không thể tải thông tin người dùng mặc định:", error);
  }
  const defaultButton = document.querySelector(
    '.product-type-btn[data-type="car"]'
  );
  if (defaultButton) {
    setActiveButton(defaultButton);
    selectedType = "car";
    categoryInput.value = defaultButton.textContent.trim();
    updateFormVisibility(selectedType);
  }
  const navbar = document.querySelector(".navbar");
  if (navbar) {
    document.body.style.paddingTop = `${navbar.offsetHeight}px`;
  }
}

function setActiveButton(button) {
  document.querySelectorAll(".product-type-btn").forEach((btn) => {
    btn.classList.remove("bg-green-500", "text-white", "border-green-500");
    btn.classList.add("bg-white", "text-gray-700", "border-gray-200");
  });
  if (button) {
    button.classList.add("bg-green-500", "text-white", "border-green-500");
    button.classList.remove("bg-white", "text-gray-700", "border-gray-200");
  }
}

function updateFormVisibility(type) {
  specificFieldsContainer.innerHTML = "";
  const brandInput = document.getElementById("brand");
  const yearInput = document.getElementById("year");

  if (type) {
    brandField.classList.remove("hidden");
    yearField.classList.remove("hidden");
    brandInput?.setAttribute("required", "required");
    yearInput?.setAttribute("required", "required");
  } else {
    brandField.classList.add("hidden");
    yearField.classList.add("hidden");
    brandInput?.removeAttribute("required");
    yearInput?.removeAttribute("required");
  }

  let fieldsHTML = "";

  if (type === "car" || type === "motorbike" || type === "bike") {
    fieldsHTML = `
            <div>
                <label for="range" class="block text-sm font-medium text-gray-700 mb-1">Quãng Đường Đi Được Khi Sạc Đầy (Km) <span class="text-red-500">*</span></label>
                <input type="number" id="range" name="range" min="1" placeholder="300" required class="form-input w-full p-3 border rounded-lg">
            </div>
            <div>
                <label for="mileage" class="block text-sm font-medium text-gray-700 mb-1">Số Km Đã Đi <span class="text-red-500">*</span></label>
                <input type="number" id="mileage" name="mileage" min="0" placeholder="15000" required class="form-input w-full p-3 border rounded-lg">
            </div>
            <div>
                <label for="battery_capacity" class="block text-sm font-medium text-gray-700 mb-1">Dung Lượng Pin (kWh/Ah) <span class="text-red-500">*</span></label>
                <input type="text" id="battery_capacity" name="battery_capacity" placeholder="30 kWh hoặc 20 Ah" required class="form-input w-full p-3 border rounded-lg">
            </div>
             <div>
                <label for="battery_type" class="block text-sm font-medium text-gray-700 mb-1">Loại Pin <span class="text-red-500">*</span></label>
                <input type="text" id="battery_type" name="battery_type" placeholder="Ví dụ: Lithium-ion, LFP..." required class="form-input w-full p-3 border rounded-lg">
            </div>
            <div>
                <label for="warranty_status" class="block text-sm font-medium text-gray-700 mb-1">Tình Trạng Bảo Hành <span class="text-red-500">*</span></label>
                <select id="warranty_status" name="warranty_status" required class="form-select w-full p-3 border rounded-lg bg-white">
                    <option value="">-- Chọn --</option>
                    <option value="Còn bảo hành chính hãng">Còn bảo hành chính hãng</option>
                    <option value="Hết bảo hành">Hết bảo hành</option>
                </select>
            </div>
            <div>
                <label for="color" class="block text-sm font-medium text-gray-700 mb-1">Màu Sắc</label>
                <input type="text" id="color" name="color" placeholder="Trắng, Đỏ" class="form-input w-full p-3 border rounded-lg">
            </div>
            <div>
                <label for="max_speed" class="block text-sm font-medium text-gray-700 mb-1">Tốc Độ Tối Đa (Km/h) <span class="text-red-500">*</span></label>
                <input type="number" id="max_speed" name="max_speed" min="10" placeholder="120" required class="form-input w-full p-3 border rounded-lg">
            </div>
            <div>
                <label for="charge_time" class="block text-sm font-medium text-gray-700 mb-1">Thời Gian Sạc</label>
                <input type="text" id="charge_time" name="charge_time" placeholder="Ví dụ: 6-8 giờ" class="form-input w-full p-3 border rounded-lg">
            </div>
            <div>
                <label for="charge_cycles" class="block text-sm font-medium text-gray-700 mb-1">Số Lần Sạc</label>
                <input type="number" id="charge_cycles" name="charge_cycles" min="0" placeholder="Ví dụ: 500" class="form-input w-full p-3 border rounded-lg">
            </div>
        `;
  } else if (type === "battery") {
    fieldsHTML = `
            <div>
                <label for="battery_type" class="block text-sm font-medium text-gray-700 mb-1">Loại Pin <span class="text-red-500">*</span></label>
                <input type="text" id="battery_type" name="battery_type" placeholder="Ví dụ: Lithium-ion, LFP, Ắc quy khô..." required class="form-input w-full p-3 border rounded-lg">
            </div>
            <div>
                <label for="battery_capacity_pin" class="block text-sm font-medium text-gray-700 mb-1">Dung Lượng (Ah/kWh) <span class="text-red-500">*</span></label>
                <input type="text" id="battery_capacity_pin" name="battery_capacity_pin" placeholder="60 Ah hoặc 4 kWh" required class="form-input w-full p-3 border rounded-lg">
            </div>
            <div>
                <label for="usage_time" class="block text-sm font-medium text-gray-700 mb-1">Thời Gian Đã Dùng (Tháng) <span class="text-red-500">*</span></label>
                <input type="number" id="usage_time" name="usage_time" min="1" placeholder="18" required class="form-input w-full p-3 border rounded-lg">
            </div>
            <div>
                <label for="warranty_status" class="block text-sm font-medium text-gray-700 mb-1">Tình Trạng Bảo Hành <span class="text-red-500">*</span></label>
                <select id="warranty_status" name="warranty_status" required class="form-select w-full p-3 border rounded-lg bg-white">
                    <option value="">-- Chọn --</option>
                    <option value="Còn bảo hành chính hãng">Còn bảo hành chính hãng</option>
                    <option value="Hết bảo hành">Hết bảo hành</option>
                </select>
            </div>
            <div>
                <label for="charge_time" class="block text-sm font-medium text-gray-700 mb-1">Thời Gian Sạc</label>
                <input type="text" id="charge_time" name="charge_time" placeholder="Ví dụ: 6-8 giờ" class="form-input w-full p-3 border rounded-lg">
            </div>
            <div>
                <label for="charge_cycles" class="block text-sm font-medium text-gray-700 mb-1">Số Lần Sạc</label>
                <input type="number" id="charge_cycles" name="charge_cycles" min="0" placeholder="Ví dụ: 500" class="form-input w-full p-3 border rounded-lg">
            </div>
            <div>
                <label for="compatibility" class="block text-sm font-medium text-gray-700 mb-1">Tương Thích Với Xe</label>
                <input type="text" id="compatibility" name="compatibility" placeholder="VinFast Klara S" class="form-input w-full p-3 border rounded-lg">
            </div>
        `;
  }
  specificFieldsContainer.innerHTML = fieldsHTML;
}

function handleFileSelection(event) {
  const newFiles = event.target.files;
  if (newFiles.length === 0) {
    // Người dùng bấm "Cancel", không làm gì cả
    return;
  }

  // Thêm file mới vào mảng
  for (const file of newFiles) {
    // (Tùy chọn: Thêm kiểm tra trùng lặp nếu muốn)
    allImageFiles.push(file);
  }

  // Vẽ lại toàn bộ preview
  renderImagePreviews();

  // Reset input để người dùng có thể chọn lại file giống (nếu lỡ xóa)
  event.target.value = null;
}

function renderImagePreviews() {
  imagePreview.innerHTML = ""; // Xóa tất cả preview cũ
  allImageFiles.forEach((file, index) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const previewHtml = `
        <div class="preview-image-container">
          <img src="${e.target.result}" alt="${file.name}" class="w-full h-full object-cover">
          <button type="button" class="delete-image-btn" data-index="${index}" title="Xóa ảnh này">
            &times; 
          </button>
        </div>
      `;
      // Dùng insertAdjacentHTML để không làm ảnh hưởng reader khác
      imagePreview.insertAdjacentHTML("beforeend", previewHtml);
    };
    reader.readAsDataURL(file);
  });
}

/**
 * Xử lý khi người dùng bấm nút "X" trên ảnh preview.
 * Sử dụng event delegation.
 */
function handleImageDeletion(event) {
  const deleteButton = event.target.closest(".delete-image-btn");
  if (!deleteButton) {
    return; // Không phải bấm nút xóa
  }

  const indexToRemove = parseInt(deleteButton.dataset.index, 10);

  // Xóa file khỏi mảng
  allImageFiles.splice(indexToRemove, 1);

  // Vẽ lại preview
  renderImagePreviews();
}

function showMessage(message, type = "success") {
  const messageBox = document.getElementById("messageBox");
  const colorClasses =
    type === "error"
      ? "bg-red-100 text-red-700 border-red-400"
      : "bg-green-100 text-green-700 border-green-400";
  messageBox.textContent = message;
  messageBox.className = `mt-4 p-4 border rounded-lg ${colorClasses}`;
  messageBox.classList.remove("hidden");
  setTimeout(() => messageBox.classList.add("hidden"), 3000);
}
