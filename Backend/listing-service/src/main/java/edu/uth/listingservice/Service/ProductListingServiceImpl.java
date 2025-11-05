// File: edu/uth/listingservice/Service/ProductListingServiceImpl.java
package edu.uth.listingservice.Service;

import org.springframework.data.domain.Page;
import java.util.Date;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import edu.uth.listingservice.Model.ProductImage;
import java.util.ArrayList; 
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; 
import org.springframework.amqp.rabbit.core.RabbitTemplate; 
import edu.uth.listingservice.DTO.ListingEventDTO; 
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
import edu.uth.listingservice.DTO.AdminListingUpdateDTO;
import edu.uth.listingservice.Repository.ProductImageRepository;
import org.springframework.cache.annotation.Cacheable; 
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.hibernate.Hibernate; 
import org.springframework.cache.annotation.CacheEvict; 
import org.springframework.cache.annotation.Caching;   
import org.springframework.cache.CacheManager; 

@Service
public class ProductListingServiceImpl implements ProductListingService {

    // (Giữ nguyên tất cả @Autowired và @Value)
    @Autowired
    private SimpMessagingTemplate messagingTemplate; 
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Value("${app.rabbitmq.exchange}")
    private String listingExchange;
    @Value("${app.rabbitmq.routing-key}")
    private String notificationRoutingKey;
    @Autowired
    private ProductListingRepository listingRepository;
    @Autowired 
    private ProductRepository productRepository;
    @Autowired 
    private ProductSpecificationRepository specRepository;
    @Autowired
    private ProductImageRepository productImageRepository;
    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private CacheManager cacheManager;
    
    @Override
    @Transactional
 
    public void deleteImageFromListing(Long listingId, Long imageId) {
        ProductListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));
        
        Product product = listing.getProduct();

        // Thêm logic xóa cache thủ công
        Long productId = product.getProductId();
        if (productId != null) {
            cacheManager.getCache("productDetails").evictIfPresent(productId);
            // Cũng nên xóa cache danh sách ảnh của sản phẩm đó
            cacheManager.getCache("productImages").evictIfPresent(productId); 
        }
        

        ProductImage imageToRemove = product.getImages().stream()
                .filter(image -> image.getImageId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Image with ID " + imageId + " not found in this product."));
        
        fileStorageService.delete(imageToRemove.getImageUrl());
        product.getImages().remove(imageToRemove);
        productRepository.save(product); // Cascade remove sẽ được xử lý nếu cấu hình đúng
        listing.setUpdatedAt(new Date());
        listingRepository.save(listing);
    }
    



    @Override
    @Transactional
    public void delete(Long id) {
        // 1. Tìm tin đăng
        Optional<ProductListing> listingOpt = listingRepository.findById(id);

        // 2. Nếu không tìm thấy (đã bị xóa), chỉ cần thoát
        if (listingOpt.isEmpty()) {
            return; 
        }
        
        ProductListing listing = listingOpt.get();
        Product product = listing.getProduct();
        Long listingId = listing.getListingId();

        // 3. Xóa tất cả các cache liên quan
        // (CacheManager đã được inject ở lần sửa trước)
        cacheManager.getCache("userListings").clear();
        cacheManager.getCache("adminListings").clear();
        cacheManager.getCache("activeListings").clear();
        cacheManager.getCache("adminSearchListings").clear();
        cacheManager.getCache("userListingPage").clear(); // Xóa cache phân trang

        if (product != null) {
            // Xóa cache chi tiết của chính sản phẩm này
            cacheManager.getCache("productDetails").evictIfPresent(product.getProductId());
            // Xóa cache tin liên quan (vì tin này có thể nằm trong đó)
            cacheManager.getCache("relatedListings").clear(); 
        }
   

        // 4. Thực hiện logic xóa khỏi CSDL (như cũ)
        if (product != null) {
            List<ProductImage> images = productImageRepository.findByProduct_ProductId(product.getProductId());
            for (ProductImage image : images) {
                fileStorageService.delete(image.getImageUrl());
            }
            
            listingRepository.delete(listing);
            productRepository.delete(product); 
        } else {
             listingRepository.delete(listing); // Chỉ xóa listing nếu không có product
        }

        // 5. Gửi thông báo WebSocket (như cũ)
        java.util.Map<String, Object> deleteMessage = new java.util.HashMap<>();
        deleteMessage.put("action", "delete");
        deleteMessage.put("listingId", listingId);
        messagingTemplate.convertAndSend("/topic/admin/listingUpdate", deleteMessage);
    }
    
    @Override
    @Cacheable(value = "userListingPage", key = "#userId + '-' + #listingId + '-' + #pageSize")
    public int findPageForListing(Long userId, Long listingId, int pageSize) {
        
        Optional<Integer> indexOptional = listingRepository
                .findZeroBasedIndexByUserIdAndListingId(userId, listingId);

        if (indexOptional.isPresent()) {
            int index = indexOptional.get(); 
            return index / pageSize;
        }

        return 0;
    }
    
   
    
    @Override
    public List<ProductListing> getAll() { return listingRepository.findAll(); }
    @Override
    public ProductListing getById(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + id));
    }


    @Cacheable(value = "activeListings", key = "#type + '-' + #sortBy + '-' + #page + '-' + #size")
    @Override
    @Transactional(readOnly = true) 
    public Page<ProductListing> getActiveListings(String type, String sortBy, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "listingDate");
        if ("price".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "product.price");
        }
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<ProductListing> listingPage;
        if (type != null && !type.isEmpty() && !"all".equalsIgnoreCase(type)) {
            listingPage = listingRepository.findByStatusAndProductType(ListingStatus.ACTIVE, type, pageable);
        } else {
            listingPage = listingRepository.findByListingStatus(ListingStatus.ACTIVE, pageable);
        }

        // === SỬA LỖI LAZY LOADING (BƯỚC CUỐI) ===
        listingPage.getContent().forEach(listing -> {
            if (listing.getProduct() != null) {
                // 1. "Đánh thức" collection
                Hibernate.initialize(listing.getProduct().getImages());
                
                // 2. (QUAN TRỌNG) Thay thế proxy bằng ArrayList
                // Điều này yêu cầu Product.java phải có hàm setImages()
                List<ProductImage> plainImages = new ArrayList<>(listing.getProduct().getImages());
                listing.getProduct().setImages(plainImages); 
            }
        });
        // ===================================
        
        return listingPage;
    }
    
  @Override
    @Cacheable(value = "relatedListings", key = "#productType + '-' + #excludeProductId")
    @Transactional(readOnly = true) 
    public List<ProductListing> findRandomRelated(String productType, Long excludeProductId, int limit) {
        
        // 2. Lấy danh sách từ repo
        List<ProductListing> listings = listingRepository.findRandomRelatedProducts(productType, excludeProductId, limit);

        // 3. THÊM KHỐI SỬA LỖI LAZY LOADING
        listings.forEach(listing -> {
            if (listing.getProduct() != null) {
                // "Đánh thức" collection
                Hibernate.initialize(listing.getProduct().getImages());
                
                // (QUAN TRỌNG) Thay thế proxy bằng ArrayList
                List<ProductImage> plainImages = new ArrayList<>(listing.getProduct().getImages());
                listing.getProduct().setImages(plainImages);
            }
        });
        // ===================================
        
        // 4. Trả về danh sách đã "đánh thức"
        return listings;
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


    @Override
    @Cacheable(value = "userListings", key = "#userId + '-' + #page + '-' + #size")
    @Transactional(readOnly = true) 
    public Page<ProductListing> getByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        
        Page<ProductListing> listingPage = listingRepository.findByUserId(userId, pageable);

        // === SỬA LỖI LAZY LOADING (BƯỚC CUỐI) ===
        listingPage.getContent().forEach(listing -> {
            if (listing.getProduct() != null) {
                // 1. "Đánh thức" collection
                Hibernate.initialize(listing.getProduct().getImages());
                
                // 2. (QUAN TRỌNG) Thay thế proxy bằng ArrayList
                List<ProductImage> plainImages = new ArrayList<>(listing.getProduct().getImages());
                listing.getProduct().setImages(plainImages);
            }
        });
      
        
        return listingPage;
    }
    
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "adminListings", allEntries = true),
        @CacheEvict(value = "userListings", allEntries = true),
        @CacheEvict(value = "activeListings", allEntries = true),
        @CacheEvict(value = "adminSearchListings", allEntries = true)
    })
    public ProductListing create(ProductListing listing) {
        listing.setListingDate(new Date());
        listing.setUpdatedAt(new Date());
        if (listing.getListingStatus() == null) {
            listing.setListingStatus(ListingStatus.PENDING);
        }
        ProductListing savedListing = listingRepository.save(listing);
        
        if (savedListing.getProduct() != null) {
            Hibernate.initialize(savedListing.getProduct());
        }

     
        
        // Giữ lại WS cho Admin
        AdminListingUpdateDTO updateDTO = new AdminListingUpdateDTO(savedListing);
        messagingTemplate.convertAndSend("/topic/admin/listingUpdate", updateDTO);
        
        return savedListing;
    }

 @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "activeListings", allEntries = true),
        @CacheEvict(value = "userListings", allEntries = true),
        @CacheEvict(value = "adminListings", allEntries = true), 
        @CacheEvict(value = "adminSearchListings", allEntries = true), 
        @CacheEvict(value = "productDetails", key = "#result.product.productId"),
        
        // Các dòng Evict cache này là ĐÚNG, bạn hãy giữ nguyên
        @CacheEvict(value = "productSpecs", key = "'prod-' + #result.product.productId"),
        @CacheEvict(value = "productSpecs", key = "#result.product.specification.specId")
    })
    public ProductListing updateListingDetails(Long listingId, UpdateListingDTO dto) {
        ProductListing listing = getById(listingId);
        Product product = listing.getProduct();
        ProductSpecification spec = product.getSpecification();

        if (listing.getListingStatus() == ListingStatus.SOLD) {
            throw new IllegalStateException("Không thể chỉnh sửa tin đã bán.");
        }

   

        if (listing.getListingStatus() == ListingStatus.ACTIVE) {
             product.setPrice(dto.getPrice());
             product.setDescription(dto.getDescription());
             listing.setPhone(dto.getPhone());
             listing.setLocation(dto.getLocation());
             spec.setWarrantyPolicy(dto.getWarrantyPolicy());
             
        } else { // Trạng thái bây giờ sẽ là PENDING hoặc REJECTED
             // Khối này cập nhật đầy đủ thông tin (cả tên, hãng, v.v.)
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

             // Cập nhật các thông số kỹ thuật (lý do chính gây ra lỗi)
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
        // --- KẾT THÚC SỬA LỖI LOGIC ---
        
        // Phần còn lại của hàm giữ nguyên
        listing.setUpdatedOnce(true); 
        listing.setVerified(false); 
        listing.setListingStatus(ListingStatus.PENDING); // Luôn set về PENDING sau khi sửa
        
        listing.setListingDate(new Date());
        listing.setUpdatedAt(new Date());
        product.setUpdatedAt(new Date());

        productRepository.save(product);
        specRepository.save(spec); // Bây giờ 'spec' đã chứa dữ liệu mới
        ProductListing savedListing = listingRepository.save(listing);

        if (savedListing.getProduct() != null) {
            Hibernate.initialize(savedListing.getProduct());
        }
        
        AdminListingUpdateDTO updateDTO = new AdminListingUpdateDTO(savedListing);
        messagingTemplate.convertAndSend("/topic/admin/listingUpdate", updateDTO);

        return savedListing;
    }

    @Override
    @Transactional
  @Caching(evict = {
        @CacheEvict(value = "activeListings", allEntries = true),
        @CacheEvict(value = "userListings", allEntries = true),
        @CacheEvict(value = "adminListings", allEntries = true), 
        @CacheEvict(value = "adminSearchListings", allEntries = true), 
        @CacheEvict(value = "productDetails", key = "#result.product.productId")
    })
    public ProductListing markAsSold(Long listingId) {
        ProductListing listing = getById(listingId);
        if (listing.getListingStatus() == ListingStatus.SOLD) {
            throw new IllegalStateException("Tin này đã ở trạng thái 'Đã Bán'.");
        }
        listing.setListingStatus(ListingStatus.SOLD);
        listing.setUpdatedAt(new Date());
        ProductListing savedListing = listingRepository.save(listing);

        if (savedListing.getProduct() != null) {
            Hibernate.initialize(savedListing.getProduct());
        }

        ListingEventDTO event = new ListingEventDTO(
            savedListing.getListingId(),
            savedListing.getUserId(),
            savedListing.getProduct().getProductName(),
            "SOLD"
        );
        rabbitTemplate.convertAndSend(listingExchange, notificationRoutingKey, event);

        return savedListing;
    }

    @Override
    @Transactional
    @CacheEvict(value = "productDetails", key = "#result.product.productId")
    public ProductListing addImagesToListing(Long listingId, List<MultipartFile> files) {
        ProductListing listing = getById(listingId);
        Product product = listing.getProduct();
        if (files != null && !files.isEmpty()) {
            List<ProductImage> newImages = new ArrayList<>();
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String imageUrl = fileStorageService.store(file);
                    ProductImage newImage = new ProductImage(null, product, imageUrl);
                    newImages.add(newImage);
                }
            }
            productImageRepository.saveAll(newImages);
        }
        listing.setUpdatedAt(new Date());
        return listingRepository.save(listing); 
    }
}