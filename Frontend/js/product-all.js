document.addEventListener("DOMContentLoaded", () => {
  const API_BASE_URL = "http://localhost:8080/api";
  const BACKEND_ORIGIN = "http://localhost:8080";
  const PAGE_SIZE = 24; // Số sản phẩm mỗi trang

  // --- DOM ELEMENTS ---
  const productsGrid = document.querySelector(".products-grid");
  const sectionTitle = document.getElementById("sectionTitle"); // Lấy qua ID mới
  const paginationContainer = document.getElementById("paginationContainer");

  // --- STATE ---
  let currentPage = 0;
  let totalPages = 0;
  let currentFilter = {
    type: "all",
    sortBy: "date",
  };

  // --- FUNCTIONS ---

  const fetchAndDisplayProducts = async (page = 0) => {
    currentPage = page;
    productsGrid.innerHTML = `<p class="loading-text">Đang tải dữ liệu...</p>`;
    try {
      const params = new URLSearchParams({
        sortBy: currentFilter.sortBy,
        page: currentPage,
        size: PAGE_SIZE,
      });
      if (currentFilter.type !== "all") {
        params.append("type", currentFilter.type);
      }

      const response = await fetch(
        `${API_BASE_URL}/listings?${params.toString()}`
      );
      if (!response.ok) throw new Error(`Lỗi mạng: ${response.statusText}`);

      const pageData = await response.json(); // API giờ trả về Page object
      const listings = pageData.content;

      totalPages = pageData.totalPages;

      productsGrid.innerHTML = "";

      if (listings.length === 0) {
        productsGrid.innerHTML = `<p class="not-found-text">Không tìm thấy tin đăng nào.</p>`;
      } else {
        listings.forEach((listing) => {
          productsGrid.insertAdjacentHTML(
            "beforeend",
            createProductCardHTML(listing)
          );
        });
      }

      renderPagination();
      updateTitle();
    } catch (error) {
      console.error("Không thể tải dữ liệu sản phẩm:", error);
      productsGrid.innerHTML = `<p class="error-text">Đã xảy ra lỗi khi tải dữ liệu.</p>`;
    }
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

  // ✅ HÀM GIÁ ĐÃ SỬA (KHÔNG LÀM TRÒN)
  const formatPrice = (price) => {
    if (!price || price === 0) return "Thương lượng";
    // Luôn hiển thị giá đầy đủ, định dạng VNĐ
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(price);
  };

  const getTypeName = (type) => {
    switch (type) {
      case "car":
        return "Ô tô điện";
      case "bike":
        return "Xe đạp điện";
      case "motorbike":
        return "Xe máy điện";
      case "battery":
        return "Pin";
      default:
        return "Tất cả tin đăng";
    }
  };

  const updateTitle = () => {
    if (sectionTitle) {
      sectionTitle.textContent = getTypeName(currentFilter.type);
    }
  };

  // ✅ HÀM VẼ PHÂN TRANG
  const renderPagination = () => {
    paginationContainer.innerHTML = "";
    if (totalPages <= 1) return;

    // Nút "Trước"
    paginationContainer.innerHTML += `
      <button 
        class="pagination-btn" 
        data-page="${currentPage - 1}" 
        ${currentPage === 0 ? "disabled" : ""}>
        &lt; Trước
      </button>`;

    // Hiển thị các nút trang
    // Logic đơn giản: luôn hiển thị trang đầu, cuối, và các trang lân cận
    let startPage = Math.max(0, currentPage - 1);
    let endPage = Math.min(totalPages - 1, currentPage + 1);

    if (startPage > 0) {
      paginationContainer.innerHTML += `<button class="pagination-btn" data-page="0">1</button>`;
      if (startPage > 1) {
        paginationContainer.innerHTML += `<span class="pagination-dots">...</span>`;
      }
    }

    for (let i = startPage; i <= endPage; i++) {
      paginationContainer.innerHTML += `
          <button 
            class="pagination-btn ${i === currentPage ? "active" : ""}" 
            data-page="${i}">
            ${i + 1}
          </button>`;
    }

    if (endPage < totalPages - 1) {
      if (endPage < totalPages - 2) {
        paginationContainer.innerHTML += `<span class="pagination-dots">...</span>`;
      }
      paginationContainer.innerHTML += `<button class="pagination-btn" data-page="${
        totalPages - 1
      }">${totalPages}</button>`;
    }

    // Nút "Sau"
    paginationContainer.innerHTML += `
      <button 
        class="pagination-btn" 
        data-page="${currentPage + 1}" 
        ${currentPage >= totalPages - 1 ? "disabled" : ""}>
        Sau &gt;
      </button>`;
  };

  // --- EVENT LISTENERS ---
  paginationContainer.addEventListener("click", (e) => {
    const button = e.target.closest(".pagination-btn");
    if (button && !button.disabled) {
      const page = parseInt(button.dataset.page, 10);
      fetchAndDisplayProducts(page);
    }
  });

  // --- INITIAL LOAD ---
  const urlParams = new URLSearchParams(window.location.search);
  currentFilter.type = urlParams.get("type") || "all";
  currentFilter.sortBy = urlParams.get("sortBy") || "date";

  fetchAndDisplayProducts(0); // Luôn bắt đầu từ trang 0
});
