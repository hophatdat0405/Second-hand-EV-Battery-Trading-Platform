document.addEventListener("DOMContentLoaded", () => {
  const API_BASE_URL = "http://localhost:8080/api";
  const BACKEND_ORIGIN = "http://localhost:8080";

  const tabsContainer = document.querySelector(".product-tabs");
  const productsGrid = document.querySelector(".products-grid");
  const viewMoreLink = document.querySelector(".view-more a");

  let currentFilterParams = { type: "all", sortBy: "date" };

  // --- FUNCTIONS ---

  const fetchAndDisplayProducts = async ({
    type = "all",
    sortBy = "date",
    limit = 4, // Tên biến 'limit' ở đây không quan trọng
  } = {}) => {
    productsGrid.innerHTML = `<p class="loading-text">Đang tải dữ liệu...</p>`;
    try {
      // ✅ SỬA LỖI Ở ĐÂY:
      // Gửi 'size: limit' thay vì 'limit' để khớp với API
      const params = new URLSearchParams({ sortBy, size: limit });

      if (type !== "all") {
        params.append("type", type);
      }

      const response = await fetch(
        `${API_BASE_URL}/listings?${params.toString()}`
      );
      if (!response.ok) throw new Error(`Lỗi mạng: ${response.statusText}`);

      const pageData = await response.json();
      const listings = pageData.content;

      productsGrid.innerHTML = "";

      if (listings.length === 0) {
        productsGrid.innerHTML = `<p class="not-found-text">Không tìm thấy tin đăng nào.</p>`;
        return;
      }

      listings.forEach((listing) => {
        productsGrid.insertAdjacentHTML(
          "beforeend",
          createProductCardHTML(listing)
        );
      });
    } catch (error) {
      console.error("Không thể tải dữ liệu sản phẩm:", error);
      productsGrid.innerHTML = `<p class="error-text">Đã xảy ra lỗi khi tải dữ liệu. Vui lòng thử lại sau.</p>`;
    }
  };

  /**
   * Hàm cập nhật đường dẫn cho nút "Xem thêm"
   */
  const updateViewMoreLink = () => {
    if (!viewMoreLink) return;

    const params = new URLSearchParams();
    if (currentFilterParams.type !== "all") {
      params.append("type", currentFilterParams.type);
    }
    if (currentFilterParams.sortBy !== "date") {
      params.append("sortBy", currentFilterParams.sortBy);
    }

    // Tạo ra URL mới, ví dụ: /product-all.html?type=car
    viewMoreLink.href = `/product-all.html?${params.toString()}`;
  };

  const createProductCardHTML = (listing) => {
    const product = listing.product;
    const spec = product.specification;

    const imageUrl =
      product.images && product.images.length > 0
        ? `${BACKEND_ORIGIN}${product.images[0].imageUrl}`
        : `https://via.placeholder.com/300x200.png?text=${product.productType.toUpperCase()}`;
    const image = `<img src="${imageUrl}" alt="${product.productName}" class="product-image">`;

    return `
            <div class="product-card">
                <div class="favorite-icon" title="Thêm vào yêu thích"><i class="fas fa-heart"></i></div>
                ${image}
                <div class="product-info">
                    <h4>${product.productName}</h4>
                    <p class="brand">${spec?.brand || "N/A"}</p>
                    <p class="price">${formatPrice(product.price)}</p>
                    <div class="details">
                        <span><i class="fas fa-map-marker-alt"></i> ${
                          listing.location || "N/A"
                        }</span>
                        <span><i class="fas fa-calendar-alt"></i> ${
                          spec?.yearOfManufacture || "N/A"
                        }</span>
                        <span><i class="fas fa-bolt"></i> ${
                          spec?.batteryCapacity || "N/A"
                        }</span>
                        ${
                          product.productType !== "battery"
                            ? `<span><i class="fas fa-road"></i> ${
                                spec?.mileage || 0
                              } km</span>`
                            : ""
                        }
                    </div>
                    <div class="progress-bar-container">
                        <span class="progress-text">${
                          spec?.condition?.conditionName || "N/A"
                        }</span>
                    </div>
                    <div class="product-actions">
                        <a href="/product_detail.html?id=${
                          product.productId
                        }" class="btn-green-sm">Xem chi tiết</a>
                        <a href="#" class="contact-link">Mua ngay</a>
                    </div>
                </div>
            </div>
        `;
  };

  const formatPrice = (price) => {
    if (!price || price === 0) return "Thương lượng";
    // Luôn hiển thị giá đầy đủ, định dạng VNĐ
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(price);
  };
  // --- EVENT LISTENERS ---
  tabsContainer.addEventListener("click", (e) => {
    const target = e.target;
    if (!target.classList.contains("tab-btn")) return;

    tabsContainer.querySelector(".active").classList.remove("active");
    target.classList.add("active");

    const filterText = target.textContent.trim();
    let params = { type: "all", sortBy: "date" };

    switch (filterText) {
      case "Ô tô điện":
        params.type = "car";
        break;
      case "Xe đạp điện":
        params.type = "bike";
        break;
      case "Xe máy điện":
        params.type = "motorbike";
        break;
      case "Pin":
        params.type = "battery";
        break;
      case "Giá tốt":
        params.sortBy = "price";
        params.type = currentFilterParams.type; // Giữ lại type cũ khi sort theo giá
        break;
      case "Tất cả":
        params.type = "all";
        params.sortBy = "date"; // Reset về sort theo ngày
        break;
      case "Mới nhất":
        params.type = currentFilterParams.type; // Giữ lại type
        params.sortBy = "date"; // Sort theo ngày
        break;
    }

    // Cập nhật trạng thái lọc và link "Xem thêm"
    currentFilterParams = params;
    updateViewMoreLink();
    fetchAndDisplayProducts({ ...params, limit: 4 }); // Luôn chỉ lấy 4 sản phẩm
  });

  // --- INITIAL LOAD ---
  fetchAndDisplayProducts(); // <-- Tự động gọi với limit = 4 (mặc định)
  updateViewMoreLink(); // Cập nhật link cho lần đầu tải trang
});
