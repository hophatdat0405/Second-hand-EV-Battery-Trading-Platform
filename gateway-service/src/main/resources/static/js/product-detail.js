// File: /js/product-detail.js

// ============================================================
// 1. CÁC HÀM DÙNG CHUNG (TOAST & MUA HÀNG) - Đưa lên đầu để tránh lỗi
// ============================================================

// Hàm hiển thị thông báo (Đã sửa: Xóa customClass để hiện giao diện chuẩn)
function showToast(icon, message) {
  // Kiểm tra an toàn: Nếu thư viện chưa load thì dùng alert thường
  if (typeof Swal === "undefined") {
    console.warn("Swal chưa load, dùng alert thay thế.");
    alert(message);
    return;
  }

  const Toast = Swal.mixin({
    toast: true,
    position: "top-end",
    showConfirmButton: false,
    timer: 3000,
    timerProgressBar: true,
    didOpen: (toast) => {
      toast.onmouseenter = Swal.stopTimer;
      toast.onmouseleave = Swal.resumeTimer;
    },
    // ĐÃ XÓA: customClass: { popup: "colored-toast" } -> Để hiện màu mặc định
  });

  Toast.fire({ icon: icon, title: message });
}

// Hàm xử lý Mua Ngay (Đã sửa để giống hệt ảnh bạn gửi)
async function buyNow(productId) {
  try {
    const userId = localStorage.getItem("userId") || null;

    // Kiểm tra đăng nhập
    if (!userId) {
      showToast("warning", "Vui lòng đăng nhập để thêm vào giỏ hàng!");
      return;
    }

    // Gọi API
    const res = await fetch(
      `http://localhost:9000/api/carts/add-by-product/${productId}`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-User-Id": userId,
        },
      }
    );

    // Xử lý kết quả
    if (!res.ok) {
      // BẤT KỂ LỖI GÌ (409 hay 500) ĐỀU HIỆN ICON ĐỎ + TEXT NHƯ HÌNH
      showToast("error", "Thêm thất bại hoặc sản phẩm đã có trong giỏ!");
      return;
    }

    const saved = await res.json();
    console.log("Đã thêm vào giỏ:", saved);

    // THÀNH CÔNG -> Hiện thông báo xanh
    showToast("success", "Đã thêm sản phẩm vào giỏ hàng!");
  } catch (err) {
    console.error("buyNow failed", err);
    // Lỗi mạng/code cũng hiện thông báo tương tự nếu muốn
    showToast("error", "Thêm thất bại hoặc sản phẩm đã có trong giỏ!");
  }
}

// ============================================================
// 2. LOGIC CHÍNH (RENDER TRANG) - Giữ nguyên code gốc của bạn
// ============================================================

document.addEventListener("DOMContentLoaded", () => {
  const API_BASE_URL = "http://localhost:9000/api";
  const BACKEND_ORIGIN = "http://localhost:9000";

  // --- DOM ELEMENT REFERENCES ---
  const pageTitle = document.getElementById("pageTitle");
  const productNameEl = document.getElementById("productName");
  const productCategoryEl = document.getElementById("productCategory");
  const productPriceEl = document.getElementById("productPrice");
  const productStatusContainer = document.getElementById(
    "productStatusContainer"
  );
  const mainProductImage = document.getElementById("mainProductImage");
  const thumbnailGallery = document.getElementById("thumbnailGallery");
  const productDescriptionEl = document.getElementById("productDescription");
  const techSpecsEl = document.getElementById("techSpecs");
  const productLocationEl = document.getElementById("productLocation");
  const contactPhoneNumberEl = document.getElementById("contactPhoneNumber");
  const phoneContactLink = document.getElementById("phoneContact");
  const sellerNameEl = document.getElementById("sellerName");
  const relatedProductsSwiperWrapper = document.getElementById(
    "relatedProductsSwiperWrapper"
  );
  const viewMoreRelatedLink = document.getElementById("viewMoreRelatedLink");

  // NÚT CHAT
  const chatBtn = document.getElementById("chatBtn");

  let relatedSwiper = null;

  const getProductIdFromUrl = () => {
    const params = new URLSearchParams(window.location.search);
    const id = params.get("id");
    if (!id) {
      console.error("Không tìm thấy ID sản phẩm trong URL.");
      showToast("error", "Lỗi: Không tìm thấy ID sản phẩm trong URL.");
      setTimeout(() => {
        window.location.href = "/index.html";
      }, 3000);
    }
    return id;
  };

  const fetchAndRenderProductDetail = async (productId) => {
    try {
      const data = await callApi(API_BASE_URL, `/product-details/${productId}`);
      updatePageContent(data);
      fetchAndRenderRelatedProducts(data.productType, data.productId);
    } catch (error) {
      console.error("Lỗi khi tải chi tiết sản phẩm:", error);
      const errorMessage = error.message || "Lỗi không xác định.";
      showToast("error", errorMessage);
    }
  };

  const fetchAndRenderRelatedProducts = async (productType, excludeId) => {
    try {
      const params = new URLSearchParams({
        type: productType,
        excludeId: excludeId,
        limit: 8,
      });
      const relatedListings = await callApi(
        API_BASE_URL,
        `/listings/related?${params.toString()}`
      );
      relatedProductsSwiperWrapper.innerHTML = "";
      if (relatedListings.length > 0) {
        relatedListings.forEach((listing) => {
          const slideHTML = `<div class="swiper-slide">${createProductCardHTML(
            listing
          )}</div>`;
          relatedProductsSwiperWrapper.insertAdjacentHTML(
            "beforeend",
            slideHTML
          );
        });
        initRelatedSwiper();
      } else {
        relatedProductsSwiperWrapper.innerHTML =
          "<p>Không có sản phẩm liên quan.</p>";
      }
      if (viewMoreRelatedLink) {
        viewMoreRelatedLink.href = `/product-all.html?type=${productType}`;
      }
    } catch (error) {
      console.error("Lỗi khi tải sản phẩm liên quan:", error);
    }
  };

  // --- UPDATE CONTENT (SẢN PHẨM CHÍNH) ---
  const updatePageContent = (data) => {
    pageTitle.textContent = data.productName;
    productNameEl.textContent = data.productName;
    productCategoryEl.textContent = getCategoryLabel(data.productType);
    productPriceEl.textContent = formatPrice(data.price);
    productDescriptionEl.textContent = data.description;
    productLocationEl.textContent = `Khu vực: ${data.location}`;

    if (sellerNameEl && data.seller && data.seller.name) {
      sellerNameEl.innerHTML = `Người bán: <a href="/edit_news.html?id=${data.seller.id}" class="text-green-600 font-bold hover:underline">${data.seller.name}</a>`;
    }

    if (data.phone) {
      contactPhoneNumberEl.textContent = `Gọi Điện: ${data.phone.substring(
        0,
        6
      )} xxx`;
      phoneContactLink.href = `tel:${data.phone}`;
    }

    // Xử lý nút Chat
    if (chatBtn) {
      chatBtn.onclick = () => {
        const myId = localStorage.getItem("userId");
        if (!myId) {
          showToast("warning", "Vui lòng đăng nhập để chat!");
          setTimeout(() => (window.location.href = "/login.html"), 1500);
          return;
        }
        if (data.seller.id == myId) {
          showToast("error", "Bạn không thể chat với chính mình!");
          return;
        }
        const params = new URLSearchParams({
          to: data.seller.id,
          name: data.seller.name,
          pid: data.productId,
          pname: data.productName,
          pprice: data.price,
          pimg:
            data.imageUrls && data.imageUrls.length > 0
              ? data.imageUrls[0]
              : "",
        });
        window.location.href = `/chat.html?${params.toString()}`;
      };
    }

    // 🔥 [SỬA] XỬ LÝ NÚT MUA NGAY (CHECK SOLD) 🔥
    const buyNowBtn = document.getElementById("buyNowBtn");
    if (buyNowBtn) {
      const newBtn = buyNowBtn.cloneNode(true);
      buyNowBtn.parentNode.replaceChild(newBtn, buyNowBtn);

      if (data.listingStatus === "SOLD") {
        // --- TRƯỜNG HỢP ĐÃ BÁN ---
        newBtn.innerHTML = `<i class="fas fa-ban mr-2"></i><span>Đã bán</span>`;
        // Đổi màu xám, bỏ hover, con trỏ not-allowed
        newBtn.className =
          "w-full flex items-center justify-center bg-gray-400 text-white font-bold py-3 px-4 rounded-lg text-lg cursor-not-allowed shadow-none uppercase";
        newBtn.disabled = true; // Khóa nút
      } else {
        // --- TRƯỜNG HỢP CÒN HÀNG ---
        newBtn.innerHTML = `<i class="fas fa-shopping-cart mr-2"></i><span>Mua Ngay</span>`;
        newBtn.className =
          "w-full flex items-center justify-center bg-red-600 text-white font-bold py-3 px-4 rounded-lg text-lg hover:bg-red-700 transition duration-200 shadow-md uppercase";
        newBtn.disabled = false;
        newBtn.onclick = () => {
          buyNow(data.productId);
        };
      }
    }

    // Cập nhật Badge trạng thái
    productStatusContainer.innerHTML = "";
    if (data.verified) productStatusContainer.innerHTML += getVerifiedBadge();

    // Thêm badge ĐÃ BÁN nếu cần
    if (data.listingStatus === "SOLD") {
      productStatusContainer.innerHTML += `<span class="px-3 py-1 text-sm inline-flex items-center leading-5 font-semibold rounded-full bg-gray-200 text-gray-600 border border-gray-300 ml-2">Đã bán</span>`;
    }

    renderThumbnails(data.imageUrls || []);
    renderTechSpecs(data);
  };
  // --- Helper Functions (Giữ nguyên) ---
  const getVerifiedBadge = () => {
    return `<span class="px-3 py-1 text-sm inline-flex items-center leading-5 font-semibold rounded-full bg-blue-100 text-blue-800">
              <i class="fas fa-check-circle" style="font-size: 12px; margin-right: 6px;"></i>Đã Kiểm Định
            </span>`;
  };
  const renderThumbnails = (imageUrls) => {
    if (!imageUrls || imageUrls.length === 0) {
      mainProductImage.src = `https://placehold.co/1000x500/e2e8f0/4a5568?text=No+Image`;
      thumbnailGallery.innerHTML = "";
      return;
    }
    mainProductImage.src = `${BACKEND_ORIGIN}${imageUrls[0]}`;
    thumbnailGallery.innerHTML = "";
    imageUrls.forEach((url, index) => {
      const fullUrl = `${BACKEND_ORIGIN}${url}`;
      const img = document.createElement("img");
      img.src = fullUrl;
      img.alt = `Ảnh sản phẩm ${index + 1}`;
      img.className = `w-full aspect-square object-cover rounded-lg cursor-pointer border-2 transition duration-150 ${
        index === 0
          ? "border-green-500"
          : "border-transparent hover:border-green-500"
      }`;
      img.onclick = () => changeMainImage(fullUrl);
      thumbnailGallery.appendChild(img);
    });
  };
  const changeMainImage = (src) => {
    mainProductImage.src = src;
    document.querySelectorAll("#thumbnailGallery img").forEach((img) => {
      img.classList.toggle("border-green-500", img.src === src);
      img.classList.toggle("border-transparent", img.src !== src);
    });
  };
  const renderTechSpecs = (data) => {
    techSpecsEl.innerHTML = `<h3 class="text-xl font-bold text-gray-800 mb-3"><i class="fas fa-cogs mr-2 text-green-500"></i>Thông Số Kỹ Thuật</h3>`;
    const addSpec = (label, value) => {
      if (value !== null && value !== undefined && value !== "") {
        techSpecsEl.innerHTML += `<div class="detail-item"><span class="detail-label">${label}</span><span class="detail-value">${value}</span></div>`;
      }
    };
    addSpec("Thương hiệu", data.brand);
    addSpec("Năm sản xuất", data.yearOfManufacture);
    addSpec("Tình trạng", data.conditionName);
    addSpec("Dung lượng Pin", data.batteryCapacity);
    addSpec("Loại Pin", data.batteryType);
    if (data.productType !== "battery") {
      addSpec(
        "Số Km đã đi",
        data.mileage ? `${data.mileage.toLocaleString("vi-VN")} km` : null
      );
      addSpec("Tốc độ tối đa", data.maxSpeed ? `${data.maxSpeed} Km/h` : null);
      addSpec(
        "Quãng đường / 1 lần sạc",
        data.rangePerCharge ? `${data.rangePerCharge} km` : null
      );
    } else {
      addSpec("Đã sử dụng", data.batteryLifespan);
      addSpec("Tương thích với", data.compatibleVehicle);
    }
    addSpec("Thời gian sạc", data.chargeTime);
    addSpec("Số lần sạc", data.chargeCycles);
    addSpec("Chính sách bảo hành", data.warrantyPolicy);
    addSpec("Màu sắc", data.color);
  };
  // --- [SỬA] RENDER CARD SẢN PHẨM LIÊN QUAN (CHECK SOLD) ---
  const createProductCardHTML = (listing) => {
    const product = listing.product;
    const spec = product.specification;
    const pid = product.productId;
    // const isLiked = typeof likedSet !== "undefined" && likedSet.has(Number(pid));

    const verifiedBadgeHTML = listing.verified
      ? `<span class="product-card-verified-badge"><i class="fas fa-check-circle"></i> Đã Kiểm Định</span>`
      : "";

    const conditionText = spec?.condition?.conditionName || "Không rõ";
    let conditionPercent = 0;
    if (conditionText.includes("%")) {
      const match = conditionText.match(/(\d+)/);
      if (match) conditionPercent = parseInt(match[1], 10);
    }
    const conditionBarHTML = `
      <div class="condition-bar" title="${conditionText}">
        <div class="condition-bar-fill" style="width: ${conditionPercent}%;"></div>
      </div>
      <span class="condition-text">${conditionText}</span>
    `;
    const imageUrl =
      product.images && product.images.length > 0
        ? `${BACKEND_ORIGIN}${product.images[0].imageUrl}`
        : `https://via.placeholder.com/300x200.png?text=${product.productType.toUpperCase()}`;
    const image = `<img src="${imageUrl}" alt="${product.productName}" class="product-image">`;

    // 🔥 LOGIC NÚT MUA NGAY (LIÊN QUAN) 🔥
    let buyBtnHTML = "";

    if (listing.listingStatus === "SOLD") {
      // Nút xám Đã bán (Không click được)
      buyBtnHTML = `
            <button 
                style="white-space: nowrap; flex: 1; cursor: not-allowed; opacity: 0.7;" 
                class="btn-buy-css bg-gray-400 border-gray-400" 
                disabled
            >
                Đã bán
            </button>`;
    } else {
      // Nút xanh Mua ngay (Bình thường)
      buyBtnHTML = `
            <button 
                style="white-space: nowrap; flex: 1;" 
                class="btn-buy-css btn-buy-now" 
                data-productid="${pid}"
            >
                Mua ngay
            </button>`;
    }

    return `
        <div class="product-card" data-productid="${pid}">
            <button class="favorite-btn" aria-label="Yêu thích" data-productid="${pid}">
                  <i class="far fa-heart"></i>
            </button>  
            ${image}
            <div class="product-info">
                <div class="product-title-wrapper"><h4>${
                  product.productName
                }</h4>${verifiedBadgeHTML}</div>
                <p class="brand">${spec?.brand || "N/A"}</p>
                <p class="price">${formatPrice(product.price)}</p>
                <div class="details">
                    <span><i class="fas fa-map-marker-alt"></i> ${
                      listing.location || "N/A"
                    }</span>
                    <span><i class="fas fa-calendar-alt"></i> ${
                      spec?.yearOfManufacture || "N/A"
                    }</span>
                </div>
                ${conditionBarHTML} 
                <div style="display: flex; align-items: center; gap: 10px; width: 100%;" class="product-actions">
                  <a href="/product_detail.html?id=${pid}" style="white-space: nowrap; flex: 1; text-align: center; display: flex; justify-content: center; align-items: center;" class="btn-green-sm">Xem chi tiết</a>
                  ${buyBtnHTML}
              </div>
            </div>
        </div>
    `;
  };
  const formatPrice = (price) => {
    if (!price || price === 0) return "Thương lượng";
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(price);
  };
  const getCategoryLabel = (type) => {
    const labels = {
      car: "Ô Tô Điện Cũ",
      motorbike: "Xe Máy Điện Cũ",
      bike: "Xe Đạp Điện Cũ",
      battery: "Pin Đã Qua Sử Dụng",
    };
    return labels[type] || "Sản Phẩm";
  };

  async function callApi(serviceUrl, endpoint) {
    const response = await fetch(`${serviceUrl}${endpoint}`);
    if (!response.ok) {
      const errorData = await response.json();
      throw errorData;
    }
    return response.json();
  }

  function initRelatedSwiper() {
    if (relatedSwiper) relatedSwiper.destroy(true, true);
    relatedSwiper = new Swiper("#relatedProductsSwiper", {
      loop: true,
      slidesPerView: "auto",
      spaceBetween: 24,
      navigation: {
        nextEl: ".related-swiper-next",
        prevEl: ".related-swiper-prev",
      },
      breakpoints: {
        640: { slidesPerView: 2 },
        768: { slidesPerView: 3 },
        1024: { slidesPerView: 4 },
      },
    });
  }

  const productId = getProductIdFromUrl();
  if (productId) fetchAndRenderProductDetail(productId);
});

// ============================================================
// 3. EVENT LISTENERS GLOBAL (Cho sản phẩm liên quan)
// ============================================================

// Khi click vào "Mua ngay" ở các sản phẩm liên quan (slider bên dưới)
document.addEventListener("click", (e) => {
  const btn = e.target.closest(".btn-buy-now");
  if (!btn) return;
  const pid = btn.dataset.productid;
  if (!pid) return;

  // Gọi hàm buyNow ở đầu file
  buyNow(pid);
});

// -------------------- LIKE FUNCTIONALITY (Giữ nguyên) --------------------
const LIKE_API_BASE = "http://localhost:9000/api/likes";

let likedSet = new Set(); // sẽ chứa các productId đã like

// Lấy danh sách liked hiện tại từ like-service
const fetchLikedSet = async () => {
  try {
    const userId = localStorage.getItem("userId");
    const headers = {};
    if (userId) headers["X-User-Id"] = userId;

    const res = await fetch(LIKE_API_BASE, { headers });
    if (!res.ok) throw new Error("Không thể lấy liked list");
    const likes = await res.json();
    likedSet = new Set(likes.map((l) => Number(l.productId)));
  } catch (err) {
    console.warn("fetchLikedSet failed", err);
    likedSet = new Set();
  }
};

// Toggle like: nếu đã like -> xóa, nếu chưa -> thêm
async function toggleLike(productId, btnEl) {
  try {
    const pid = Number(productId);
    const userId = localStorage.getItem("userId");
    if (!userId) {
      alert("Vui lòng đăng nhập để thay đổi trạng thái yêu thích.");
      return;
    }

    const headers = { "X-User-Id": userId };

    if (likedSet.has(pid)) {
      // xóa
      const res = await fetch(`${LIKE_API_BASE}/by-product/${pid}`, {
        method: "DELETE",
        headers,
      });
      if (!res.ok && res.status !== 204) {
        const txt = await res.text().catch(() => "");
        throw new Error(txt || "Xóa like thất bại: HTTP " + res.status);
      }
      likedSet.delete(pid);
      btnEl.classList.remove("active");
      const icon = btnEl.querySelector("i");
      if (icon) {
        icon.classList.remove("fas");
        icon.classList.add("far");
      }
      btnEl.setAttribute("aria-label", "Thêm vào yêu thích");
    } else {
      // thêm
      const res = await fetch(`${LIKE_API_BASE}/add-by-product/${pid}`, {
        method: "POST",
        headers,
      });
      if (!res.ok) {
        if (res.status === 409) {
          likedSet.add(pid);
        } else {
          const txt = await res.text().catch(() => "");
          throw new Error(txt || "Thêm like thất bại: HTTP " + res.status);
        }
      } else {
        const saved = await res.json();
        likedSet.add(pid);
      }
      btnEl.classList.add("active");
      const icon = btnEl.querySelector("i");
      if (icon) {
        icon.classList.remove("far");
        icon.classList.add("fas");
      }
      btnEl.setAttribute("aria-label", "Bỏ thích");
    }
  } catch (err) {
    console.error("toggleLike error", err);
    showToast("error", "Không thể thay đổi trạng thái yêu thích.");
  }
}
// Delegated event listener: bắt click cho tất cả .favorite-btn
document.addEventListener("click", async (e) => {
  const favBtn = e.target.closest(".favorite-btn");
  if (!favBtn) return;
  const pid = favBtn.dataset.productid;
  if (!pid) return;
  // disable button tạm để tránh spam
  favBtn.disabled = true;
  try {
    await toggleLike(pid, favBtn);
  } finally {
    favBtn.disabled = false;
  }
});

// INIT: trước khi render products, load likedSet
(async function initPage() {
  await fetchLikedSet();
  try {
    // Hàm này có vẻ là logic từ trang danh sách (product-all),
    // nhưng trong file gốc bạn có để nên tôi giữ nguyên
    if (typeof fetchAndDisplayProducts === "function") {
      await fetchAndDisplayProducts({
        type: currentFilterParams?.type ?? "all",
        sortBy: currentFilterParams?.sortBy ?? "date",
        limit: 4,
      });
    }
  } catch (err) {
    // Ignore error if function not exists
  }
})();
