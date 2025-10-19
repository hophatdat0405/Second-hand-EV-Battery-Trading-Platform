package edu.uth.listingservice.Service;
import org.springframework.data.domain.Page;
import java.util.Date;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import edu.uth.listingservice.Model.ProductImage;

import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable; 
import org.springframework.data.domain.Sort;       
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uth.listingservice.Model.ListingStatus;
import edu.uth.listingservice.Model.ProductListing;
import edu.uth.listingservice.Model.Product;
import edu.uth.listingservice.Model.ProductSpecification;


import edu.uth.listingservice.Repository.ProductListingRepository;
import edu.uth.listingservice.Repository.ProductSpecificationRepository;
import edu.uth.listingservice.Repository.ProductRepository;
import edu.uth.listingservice.DTO.UpdateListingDTO;
import edu.uth.listingservice.Repository.ProductImageRepository;
@Service
public class ProductListingServiceImpl implements ProductListingService {

    @Autowired
    private ProductListingRepository listingRepository;
      @Autowired private ProductRepository productRepository;
    @Autowired private ProductSpecificationRepository specRepository;
@Autowired
private ProductImageRepository productImageRepository; // Tiêm repository cho ảnh

@Autowired
private FileStorageService fileStorageService; // Tận dụng service đã có
    @Override
    public List<ProductListing> getAll() {
        return listingRepository.findAll();
    }

    @Override
    public ProductListing getById(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + id));
    }


    @Override
    public ProductListing create(ProductListing listing) {
        listing.setListingDate(new Date());
        listing.setUpdatedAt(new Date());
        return listingRepository.save(listing);
    }

    @Override
    public ProductListing update(Long id, ProductListing updated) {
        ProductListing existing = getById(id);
        
        existing.setListingStatus(updated.getListingStatus());
        existing.setProduct(updated.getProduct());
        existing.setUserId(updated.getUserId());
        existing.setUpdatedAt(new Date());
        return listingRepository.save(existing);
    }

   


   // --- BẮT ĐẦU PHẦN SỬA LỖI ---
    @Override
    public List<ProductListing> getActiveListings(String type, String sortBy, int limit) {
        Sort sort = Sort.by(Sort.Direction.DESC, "listingDate");
        if ("price".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "product.price");
        }
        Pageable pageable = PageRequest.of(0, limit, sort);
        
        // ✅ ĐÂY LÀ PHẦN SỬA LỖI: Thêm lại .getContent() để chuyển Page -> List
        if (type != null && !type.isEmpty() && !"all".equalsIgnoreCase(type)) {
            // Lấy Page từ repo rồi chuyển thành List bằng .getContent()
            return listingRepository.findByStatusAndProductType(ListingStatus.ACTIVE, type, pageable).getContent();
        }
        
        // Lấy Page từ repo rồi chuyển thành List bằng .getContent()
        return listingRepository.findByListingStatus(ListingStatus.ACTIVE, pageable).getContent();
    }
    // --- KẾT THÚC PHẦN SỬA LỖI ---

    @Override
    public List<ProductListing> findRandomRelated(String productType, Long excludeProductId, int limit) {
        return listingRepository.findRandomRelatedProducts(productType, excludeProductId, limit);
    }

@Override
    @Transactional
    public ProductListing updateListingDetails(Long listingId, UpdateListingDTO dto) {
        ProductListing listing = getById(listingId);
        Product product = listing.getProduct();
        ProductSpecification spec = product.getSpecification();

        if (listing.getListingStatus() == ListingStatus.SOLD) {
            throw new IllegalStateException("Không thể chỉnh sửa tin đã bán.");
        }
        if (listing.isUpdatedOnce()) {
            throw new IllegalStateException("Tin đăng này đã được chỉnh sửa và không thể sửa thêm.");
        }

        // --- LOGIC MỚI: Xử lý các trạng thái có thể sửa ---
        
        // 1. Nếu là ACTIVE, chỉ cập nhật các trường được phép.
        if (listing.getListingStatus() == ListingStatus.ACTIVE) {
            product.setPrice(dto.getPrice());
            product.setDescription(dto.getDescription());
            listing.setPhone(dto.getPhone());
            listing.setLocation(dto.getLocation());
            spec.setWarrantyPolicy(dto.getWarrantyPolicy());
        
        // 2. Nếu là PENDING hoặc REJECTED, cho phép cập nhật tất cả.
        } else if (listing.getListingStatus() == ListingStatus.PENDING || listing.getListingStatus() == ListingStatus.REJECTED) {
            product.setProductName(dto.getProductName());
            spec.setBrand(dto.getBrand());
            product.setPrice(dto.getPrice());
            product.setDescription(dto.getDescription());
            listing.setPhone(dto.getPhone());
            listing.setLocation(dto.getLocation());
            spec.setWarrantyPolicy(dto.getWarrantyPolicy());

            spec.setBatteryType(dto.getBatteryType());
            spec.setChargeTime(dto.getChargeTime());
            spec.setChargeCycles(dto.getChargeCycles());

            if (!"battery".equals(product.getProductType())) {
                spec.setRangePerCharge(dto.getRangePerCharge());
                spec.setMileage(dto.getMileage());
                spec.setBatteryCapacity(dto.getBatteryCapacity());
                spec.setColor(dto.getColor());
                spec.setMaxSpeed(dto.getMaxSpeed());
            } else {
                spec.setCompatibleVehicle(dto.getCompatibleVehicle());
                spec.setBatteryLifespan(dto.getBatteryLifespan());
                spec.setBatteryCapacity(dto.getBatteryCapacity());
            }
        }

        // 3. Cập nhật các thông tin chung sau khi chỉnh sửa
        listing.setUpdatedOnce(true);      // Đánh dấu đã sửa 1 lần
        listing.setListingStatus(ListingStatus.PENDING); // Luôn chuyển về PENDING
        listing.setListingDate(new Date());  // Cập nhật ngày đăng thành ngày mới nhất
        listing.setUpdatedAt(new Date());
        product.setUpdatedAt(new Date());

        productRepository.save(product);
        specRepository.save(spec);
        return listingRepository.save(listing);
    }
    @Override
    @Transactional
    public ProductListing markAsSold(Long listingId) {
        ProductListing listing = getById(listingId);
        if (listing.getListingStatus() == ListingStatus.SOLD) {
            throw new IllegalStateException("Tin này đã ở trạng thái 'Đã Bán'.");
        }
        listing.setListingStatus(ListingStatus.SOLD);
        listing.setUpdatedAt(new Date());
        return listingRepository.save(listing);
    }

@Override
@Transactional
public ProductListing addImagesToListing(Long listingId, List<MultipartFile> files) {
    ProductListing listing = getById(listingId);
    Product product = listing.getProduct();

    if (files != null && !files.isEmpty()) {
        List<ProductImage> newImages = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String imageUrl = fileStorageService.store(file);
                ProductImage newImage = new ProductImage(null, product, imageUrl, "product_image");
                newImages.add(newImage);
            }
        }
        productImageRepository.saveAll(newImages);
    }
    
    // Đánh dấu tin đã được sửa (nếu cần) và trả về
    listing.setUpdatedAt(new Date());
    return listingRepository.save(listing);
}



@Override
@Transactional
public void deleteImageFromListing(Long listingId, Long imageId) {
    // 1. Lấy listing -> product
    ProductListing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));
    Product product = listing.getProduct();

    // 2. Tìm đối tượng ảnh cần xóa trong danh sách ảnh của product
    // Điều này đảm bảo chúng ta chỉ xóa ảnh thuộc đúng sản phẩm đó
    ProductImage imageToRemove = product.getImages().stream()
            .filter(image -> image.getImageId().equals(imageId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Image with ID " + imageId + " not found in this product."));

    // 3. Xóa file vật lý trước
    fileStorageService.delete(imageToRemove.getImageUrl());

    // 4. Xóa đối tượng ảnh khỏi danh sách của product
    // Đây là bước quan trọng nhất để kích hoạt 'orphanRemoval'
    product.getImages().remove(imageToRemove);

    // 5. Lưu lại product. JPA sẽ tự động xóa record trong bảng Product_Images.
    // Không cần gọi productImageRepository.deleteById() nữa.
    productRepository.save(product);

    // 6. Cập nhật thời gian của listing (tùy chọn nhưng nên có)
    listing.setUpdatedAt(new Date());
    listingRepository.save(listing);
}
@Override
public Page<ProductListing> getByUserId(Long userId, int page, int size) {
    // Tạo đối tượng Pageable để lấy đúng trang, với kích thước 12 và sắp xếp theo ngày đăng mới nhất
    Pageable pageable = PageRequest.of(page, size, Sort.by("listingDate").descending());
    return listingRepository.findByUserId(userId, pageable);
}
@Override
    @Transactional
    public void delete(Long id) {
        ProductListing listing = getById(id);
        Product product = listing.getProduct();
        
        List<ProductImage> images = productImageRepository.findByProduct_ProductId(product.getProductId());
        for (ProductImage image : images) {
            fileStorageService.delete(image.getImageUrl());
        }
        // Do cấu hình Cascade và orphanRemoval, chỉ cần xóa product là các ảnh và spec liên quan sẽ tự xóa
        // Tuy nhiên, xóa listing riêng để đảm bảo
        listingRepository.delete(listing);
        productRepository.delete(product);
    }
}