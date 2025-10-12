const productTypeSelector = document.getElementById("productTypeSelector");
const categoryInput = document.getElementById("category");
const commonFieldsWrapper = document.getElementById("commonFieldsWrapper");
const specificFieldsContainer = document.getElementById("specificFields");
const brandField = document.getElementById("brandField");
const yearField = document.getElementById("yearField");
const imagePreview = document.getElementById("imagePreview");
const messageBox = document.getElementById("messageBox");
let selectedType = null;

// Xử lý khi chọn loại sản phẩm (Ô tô, xe máy, pin...)
productTypeSelector.addEventListener("click", (e) => {
  const button = e.target.closest(".product-type-btn");
  if (!button) return;

  // Xóa trạng thái active cũ và set trạng thái mới
  setActiveButton(button);

  selectedType = button.dataset.type;
  categoryInput.value = button.textContent.trim();
  updateFormVisibility(selectedType);
});

// Hàm thiết lập trạng thái active cho nút
function setActiveButton(button) {
  document.querySelectorAll(".product-type-btn").forEach((btn) => {
    btn.classList.remove(
      "bg-green-500",
      "text-white",
      "hover:bg-green-600",
      "border-green-500"
    );
    btn.classList.add("bg-white", "text-gray-700", "border-gray-200");
  });

  if (button) {
    button.classList.add(
      "bg-green-500",
      "text-white",
      "hover:bg-green-600",
      "border-green-500"
    );
    button.classList.remove("bg-white", "text-gray-700", "border-gray-200");
  }
}

// Hàm chính để điều chỉnh form: Ẩn/hiện các trường và thêm các trường kỹ thuật riêng
function updateFormVisibility(type) {
  specificFieldsContainer.innerHTML = "";

  // Lấy các trường cần toggle required
  const brandInput = document.getElementById("brand"); // Đã đổi thành input
  const yearInput = document.getElementById("year");

  // 1. Điều chỉnh hiển thị các trường chung
  if (type === "battery") {
    // HIỆN trường Hãng Sản Xuất
    brandField.classList.remove("hidden");

    // ẨN trường Năm Sản Xuất
    yearField.classList.add("hidden");

    // Thêm lại thuộc tính 'required' cho Brand, và loại bỏ cho Year
    if (brandInput) brandInput.setAttribute("required", "required");
    if (yearInput) yearInput.removeAttribute("required");
  } else if (type === "car" || type === "motorbike" || type === "bike") {
    // HIỆN các trường cho Xe
    brandField.classList.remove("hidden");
    yearField.classList.remove("hidden");

    // Thêm lại thuộc tính 'required' cho cả Brand và Year
    if (brandInput) brandInput.setAttribute("required", "required");
    if (yearInput) yearInput.setAttribute("required", "required");
  } else {
    // Trạng thái ban đầu (null) - set mặc định như 'car' ở dưới
    // Để giữ cho logic này đơn giản, ta sẽ xử lý mặc định trong window.onload
    // Do đó, phần 'else' này chỉ giữ lại để đảm bảo không bị lỗi
    brandField.classList.remove("hidden");
    yearField.classList.remove("hidden");
    if (brandInput) brandInput.setAttribute("required", "required");
    if (yearInput) yearInput.setAttribute("required", "required");
  }

  // 2. Thêm các trường kỹ thuật riêng (Specific Fields)
  let fieldsHTML = "";

  if (type === "car" || type === "motorbike") {
    // Trường chung cho Ô tô và Xe máy điện
    fieldsHTML += `
          <div>
            <label for="range" class="block text-sm font-medium text-gray-700 mb-1">Quãng Đường Đi Được Khi Sạc Đầy (Km) <span class="text-red-500">*</span></label>
            <input type="number" id="range" name="range" min="1" placeholder="300" required class="form-input w-full p-3 border rounded-lg focus:ring-green-500 focus:border-green-500">
          </div>
          <div>
            <label for="mileage" class="block text-sm font-medium text-gray-700 mb-1">Số Km Đã Đi (Km) <span class="text-red-500">*</span></label>
            <input type="number" id="mileage" name="mileage" min="0" placeholder="15000" required class="form-input w-full p-3 border rounded-lg focus:ring-green-500 focus:border-green-500">
          </div>
          <div>
            <label for="battery_capacity" class="block text-sm font-medium text-gray-700 mb-1">Dung Lượng Pin (kWh/Ah) <span class="text-red-500">*</span></label>
            <input type="text" id="battery_capacity" name="battery_capacity" placeholder="30 kWh hoặc 20 Ah" required class="form-input w-full p-3 border rounded-lg focus:ring-green-500 focus:border-green-500">
          </div>
          <div>
            <label for="warranty_status" class="block text-sm font-medium text-gray-700 mb-1">Tình Trạng Bảo Hành <span class="text-red-500">*</span></label>
            <select id="warranty_status" name="warranty_status" required class="form-select w-full p-3 border rounded-lg bg-white focus:ring-green-500 focus:border-green-500">
              <option value="">-- Chọn tình trạng bảo hành --</option>
              <option value="con_bh">Còn bảo hành chính hãng</option>
              <option value="het_bh">Hết bảo hành</option>
              <option value="bh_ngan_han">Bảo hành ngắn hạn (cửa hàng)</option>
            </select>
          </div>
          <div>
            <label for="color" class="block text-sm font-medium text-gray-700 mb-1">Màu Sắc</label>
            <input type="text" id="color" name="color" placeholder="Trắng, Đỏ, Bạc" class="form-input w-full p-3 border rounded-lg focus:ring-green-500 focus:border-green-500">
          </div>
          <div>
            <label for="max_speed" class="block text-sm font-medium text-gray-700 mb-1">Tốc Độ Tối Đa (Km/h) <span class="text-red-500">*</span></label>
            <input type="number" id="max_speed" name="max_speed" min="10" placeholder="120" required class="form-input w-full p-3 border rounded-lg focus:ring-green-500 focus:border-green-500">
          </div>
        `;
  } else if (type === "bike") {
    // Trường riêng cho Xe Đạp Điện (ít thông số hơn)
    fieldsHTML += `
          <div>
            <label for="range" class="block text-sm font-medium text-gray-700 mb-1">Quãng Đường Đi Được Khi Sạc Đầy (Km) <span class="text-red-500">*</span></label>
            <input type="number" id="range" name="range" min="1" placeholder="40" required class="form-input w-full p-3 border rounded-lg focus:ring-green-500 focus:border-green-500">
          </div>
          <div>
            <label for="mileage" class="block text-sm font-medium text-gray-700 mb-1">Số Km Đã Đi (Km) <span class="text-red-500">*</span></label>
            <input type="number" id="mileage" name="mileage" min="0" placeholder="1500" required class="form-input w-full p-3 border rounded-lg focus:ring-green-500 focus:border-green-500">
          </div>
          <div>
            <label for="battery_capacity" class="block text-sm font-medium text-gray-700 mb-1">Dung Lượng Pin (Ah) <span class="text-red-500">*</span></label>
            <input type="text" id="battery_capacity" name="battery_capacity" placeholder="12 Ah" required class="form-input w-full p-3 border rounded-lg focus:ring-green-500 focus:border-green-500">
          </div>
          <div>
            <label for="warranty_status" class="block text-sm font-medium text-gray-700 mb-1">Tình Trạng Bảo Hành</label>
            <select id="warranty_status" name="warranty_status" class="form-select w-full p-3 border rounded-lg bg-white focus:ring-green-500 focus:border-green-500">
              <option value="">-- Chọn tình trạng bảo hành --</option>
              <option value="con_bh">Còn bảo hành</option>
              <option value="het_bh">Hết bảo hành</option>
            </select>
          </div>
          <div>
            <label for="color" class="block text-sm font-medium text-gray-700 mb-1">Màu Sắc</label>
            <input type="text" id="color" name="color" placeholder="Đen, Xanh" class="form-input w-full p-3 border rounded-lg focus:ring-green-500 focus:border-green-500">
          </div>
          <div>
            <label for="max_speed" class="block text-sm font-medium text-gray-700 mb-1">Tốc Độ Tối Đa (Km/h)</label>
            <input type="number" id="max_speed" name="max_speed" min="10" placeholder="40" class="form-input w-full p-3 border rounded-lg focus:ring-green-500 focus:border-green-500">
          </div>
        `;
  } else if (type === "battery") {
    // Trường riêng cho Pin
    fieldsHTML += `
          <div>
            <label for="battery_type" class="block text-sm font-medium text-gray-700 mb-1">Loại Pin <span class="text-red-500">*</span></label>
            <select id="battery_type" name="battery_type" required class="form-select w-full p-3 border rounded-lg bg-white focus:ring-green-500 focus:border-green-500">
              <option value="">-- Chọn loại pin --</option>
              <option value="lithium">Lithium-ion</option>
              <option value="lfp">Lithium-ion Phosphate (LFP)</option>
              <option value="acid">Ắc quy Chì-axit</option>
              <option value="other">Khác</option>
            </select>
          </div>
          <div>
            <label for="battery_capacity_pin" class="block text-sm font-medium text-gray-700 mb-1">Dung Lượng Pin (Ah/kWh) <span class="text-red-500">*</span></label>
            <input type="text" id="battery_capacity_pin" name="battery_capacity_pin" placeholder="60 Ah hoặc 4 kWh" required class="form-input w-full p-3 border rounded-lg focus:ring-green-500 focus:border-green-500">
          </div>
          <div>
            <label for="usage_time" class="block text-sm font-medium text-gray-700 mb-1">Thời Gian Đã Sử Dụng (Tháng) <span class="text-red-500">*</span></label>
            <input type="number" id="usage_time" name="usage_time" min="1" placeholder="18" required class="form-input w-full p-3 border rounded-lg focus:ring-green-500 focus:border-green-500">
          </div>
          <div>
            <label for="compatibility" class="block text-sm font-medium text-gray-700 mb-1">Tương Thích Với Dòng Xe Nào?</label>
            <input type="text" id="compatibility" name="compatibility" placeholder="Ví dụ: Dùng cho VinFast Klara S" class="form-input w-full p-3 border rounded-lg focus:ring-green-500 focus:border-green-500">
          </div>
        `;
  }
  specificFieldsContainer.innerHTML = fieldsHTML;
}

// Xử lý hiển thị ảnh preview
function previewImages() {
  const input = document.getElementById("imageUpload");
  imagePreview.innerHTML = "";

  if (input.files) {
    const filesArray = Array.from(input.files).slice(0, 5); // Giới hạn 5 ảnh
    filesArray.forEach((file) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const imgDiv = document.createElement("div");
        imgDiv.className =
          "relative aspect-w-1 aspect-h-1 overflow-hidden rounded-lg";
        imgDiv.innerHTML = `
                <img src="${e.target.result}" alt="Ảnh sản phẩm" class="w-full h-full object-cover">
              `;
        imagePreview.appendChild(imgDiv);
      };
      reader.readAsDataURL(file);
    });
  }
}

// Xử lý khi Submit Form
function handleFormSubmit(event) {
  event.preventDefault();

  // 1. Kiểm tra loại sản phẩm đã chọn chưa
  if (!selectedType) {
    showMessage(
      "Vui lòng chọn loại sản phẩm (Ô tô/Xe máy/Pin) trước khi đăng tin.",
      "bg-red-100 text-red-700 border-red-400"
    );
    return;
  }

  // 2. Thu thập dữ liệu và Ánh xạ tên trường theo DB
  const formData = new FormData(event.target);
  const data = {};

  // Trường bắt buộc chung
  data["product_type"] = selectedType;
  data["product_name"] = formData.get("title"); // title -> product_name
  data["condition_id"] = formData.get("condition_id"); // condition -> condition_id
  data["price"] = formData.get("price");
  data["description"] = formData.get("description");
  data["phone"] = formData.get("phone");
  data["location"] = formData.get("location");
  data["brand"] = formData.get("brand"); // Brand luôn được gửi

  // Trường chỉ dành cho XE
  if (selectedType !== "battery") {
    data["range"] = formData.get("range"); // TRƯỜNG MỚI
    data["year_of_manufacture"] = formData.get("year_of_manufacture");
    data["mileage"] = formData.get("mileage");

    // Lưu ý: Tên trường trong DB có thể khác
    data["battery_capacity"] = formData.get("battery_capacity"); // Dung lượng Pin (cho Xe)
    data["warranty_status"] = formData.get("warranty_status"); // Tình trạng bảo hành (cho Xe)

    // TRƯỜNG MỚI: Tốc độ tối đa
    data["max_speed"] = formData.get("max_speed");

    data["color"] = formData.get("color");
  } else {
    // Trường chỉ dành cho PIN
    data["battery_type"] = formData.get("battery_type"); // Loại Pin
    data["battery_capacity_pin"] = formData.get("battery_capacity_pin"); // Dung lượng Pin (cho Pin)
    data["usage_time"] = formData.get("usage_time");
    data["compatibility"] = formData.get("compatibility");

    // Đặt các trường không dùng thành null/undefined nếu cần
    data["range"] = null; // TRƯỜNG MỚI: Pin không có quãng đường
    data["year_of_manufacture"] = null;
    data["max_speed"] = null;
    // brand đã được giữ lại
  }

  // Log dữ liệu để minh họa (giả lập gửi dữ liệu)
  console.log("Dữ liệu đăng tin (Mapped to DB schema):", data);

  // 3. Hiển thị thông báo thành công (Thực tế sẽ gửi lên server)
  showMessage(
    "Đăng tin thành công! Dữ liệu đã được thu thập theo cấu trúc DB mới (Hãng Sản Xuất là tự nhập, mặc định là Ô Tô Điện).",
    "bg-green-100 text-green-700 border-green-400"
  );

  // Tùy chọn: Reset form sau khi gửi
  document.getElementById("sellForm").reset();
  categoryInput.value = "Chưa chọn";
  imagePreview.innerHTML = "";
  selectedType = null;
  updateFormVisibility(null);
  document.querySelectorAll(".product-type-btn").forEach((btn) => {
    btn.classList.remove(
      "bg-green-500",
      "text-white",
      "hover:bg-green-600",
      "border-green-500"
    );
    btn.classList.add("bg-white", "text-gray-700", "border-gray-200");
  });
}

// Hàm hiển thị thông báo
function showMessage(message, classes) {
  messageBox.textContent = message;
  messageBox.className = `mt-4 p-4 border rounded-lg ${classes}`;
  messageBox.classList.remove("hidden");
  setTimeout(() => {
    messageBox.classList.add("hidden");
  }, 5000);
}

// Khởi tạo: Đảm bảo form hiển thị đầy đủ khi load (Mặc định là Ô Tô Điện - 'car')
window.onload = function () {
  // 1. Tìm nút "Ô Tô Điện"
  const defaultButton = document.querySelector(
    '.product-type-btn[data-type="car"]'
  );

  if (defaultButton) {
    // 2. Kích hoạt trạng thái active cho nút
    setActiveButton(defaultButton);

    // 3. Set giá trị cho các biến global
    selectedType = "car";
    categoryInput.value = defaultButton.textContent.trim();

    // 4. Cập nhật các trường hiển thị trong form
    updateFormVisibility(selectedType);
  }

  // Đảm bảo padding-top cho navbar
  const navbar = document.querySelector(".navbar");
  document.body.style.paddingTop = "120px";
};
window.addEventListener("load", () => {
  const navbar = document.querySelector(".navbar");

  // Tạo khoảng trống cho body để navbar không bị che khuất
  document.body.style.paddingTop = "120px"; // Điều chỉnh độ cao padding-top tùy thuộc vào chiều cao của navbar
});
