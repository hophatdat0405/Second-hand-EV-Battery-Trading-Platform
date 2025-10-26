// File: edu/uth/listingservice/Service/ProductListingServiceImpl.java
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
import edu.uth.listingservice.DTO.AdminListingUpdateDTO;
import edu.uth.listingservice.Repository.ProductImageRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.hibernate.Hibernate; // Import Hibernate

@Service
public class ProductListingServiceImpl implements ProductListingService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ProductListingRepository listingRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductSpecificationRepository specRepository;
    @Autowired
    private ProductImageRepository productImageRepository;

@Autowired
private FileStorageService fileStorageService;

    // --- Keep these methods unchanged ---
    @Override
    public List<ProductListing> getAll() { return listingRepository.findAll(); }
    @Override
    public ProductListing getById(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + id));
    }
    @Override
    @Transactional
    public ProductListing create(ProductListing listing) {
        listing.setListingDate(new Date());
        listing.setUpdatedAt(new Date());
        if (listing.getListingStatus() == null) {
            listing.setListingStatus(ListingStatus.PENDING);
        }
        ProductListing savedListing = listingRepository.save(listing);
        // Force load lazy-loaded data
        if (savedListing.getProduct() != null) {
            Hibernate.initialize(savedListing.getProduct());
        }
        // Send DTO to Admin
        AdminListingUpdateDTO updateDTO = new AdminListingUpdateDTO(savedListing);
        messagingTemplate.convertAndSend("/topic/admin/listingUpdate", updateDTO);
        return savedListing;
    }
    @Override
    public ProductListing update(Long id, ProductListing updated) { // This method seems unused? Keep as is for now.
        ProductListing existing = getById(id);
        existing.setListingStatus(updated.getListingStatus());
        existing.setProduct(updated.getProduct());
        existing.setUserId(updated.getUserId());
        existing.setUpdatedAt(new Date());
        return listingRepository.save(existing);
    }
   // ✅ THAY ĐỔI HÀM NÀY
    @Override
    public Page<ProductListing> getActiveListings(String type, String sortBy, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "listingDate");
        if ("price".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "product.price");
        }
        // Sử dụng page và size thay vì "0, limit"
        Pageable pageable = PageRequest.of(page, size, sort);
        
        if (type != null && !type.isEmpty() && !"all".equalsIgnoreCase(type)) {
            // Trả về đối tượng Page đầy đủ (không .getContent())
            return listingRepository.findByStatusAndProductType(ListingStatus.ACTIVE, type, pageable);
        }
        // Trả về đối tượng Page đầy đủ
        return listingRepository.findByListingStatus(ListingStatus.ACTIVE, pageable);
    }
    @Override
    public List<ProductListing> findRandomRelated(String productType, Long excludeProductId, int limit) {
        return listingRepository.findRandomRelatedProducts(productType, excludeProductId, limit);
    }
    // --- End of unchanged methods ---

   @Override
    @Transactional
    public ProductListing updateListingDetails(Long listingId, UpdateListingDTO dto) {
        ProductListing listing = getById(listingId);
        Product product = listing.getProduct();
        ProductSpecification spec = product.getSpecification();

        if (listing.getListingStatus() == ListingStatus.SOLD) {
            throw new IllegalStateException("Không thể chỉnh sửa tin đã bán.");
        }

        // --- Update logic (remains the same) ---
         if (listing.getListingStatus() == ListingStatus.ACTIVE || listing.getListingStatus() == ListingStatus.REJECTED) {
             product.setPrice(dto.getPrice());
             product.setDescription(dto.getDescription());
             listing.setPhone(dto.getPhone());
             listing.setLocation(dto.getLocation());
             spec.setWarrantyPolicy(dto.getWarrantyPolicy());
        } else { // PENDING
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
        // --- End Update logic ---

        // ✅✅✅ THÊM DÒNG NÀY DƯỚI ĐÂY ✅✅✅
        listing.setUpdatedOnce(true); // Đánh dấu là đã chỉnh sửa 1 lần
        
        listing.setVerified(false); // Bất kỳ chỉnh sửa nào cũng xóa trạng thái đã kiểm định
        listing.setListingStatus(ListingStatus.PENDING); // Luôn set về PENDING để duyệt lại
        
        listing.setListingDate(new Date());
        listing.setUpdatedAt(new Date());
        product.setUpdatedAt(new Date());

        productRepository.save(product);
        specRepository.save(spec);
        ProductListing savedListing = listingRepository.save(listing);

        // Force load lazy-loaded data
        if (savedListing.getProduct() != null) {
            Hibernate.initialize(savedListing.getProduct());
        }

        // Send DTO to Admin
        AdminListingUpdateDTO updateDTO = new AdminListingUpdateDTO(savedListing);
        messagingTemplate.convertAndSend("/topic/admin/listingUpdate", updateDTO);

        // Send full object to the user for instant update on their management page
        messagingTemplate.convertAndSendToUser(
            String.valueOf(savedListing.getUserId()),
            "/topic/listingUpdates", // NEW TOPIC
            savedListing
        );

        return savedListing;
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
        ProductListing savedListing = listingRepository.save(listing);

        // Force load lazy-loaded data
        if (savedListing.getProduct() != null) {
            Hibernate.initialize(savedListing.getProduct());
        }

        // Send full object to the user for instant update on their management page
        messagingTemplate.convertAndSendToUser(
            String.valueOf(savedListing.getUserId()),
            "/topic/listingUpdates", // NEW TOPIC
            savedListing
        );

        return savedListing;
    }

    // --- Keep these methods unchanged ---
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
                    ProductImage newImage = new ProductImage(null, product, imageUrl);
                    newImages.add(newImage);
                }
            }
            productImageRepository.saveAll(newImages);
        }
        listing.setUpdatedAt(new Date());
        return listingRepository.save(listing); // No WS needed here as UI updates via form submit
    }
    @Override
    @Transactional
    public void deleteImageFromListing(Long listingId, Long imageId) {
        ProductListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));
        Product product = listing.getProduct();
        ProductImage imageToRemove = product.getImages().stream()
                .filter(image -> image.getImageId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Image with ID " + imageId + " not found in this product."));
        fileStorageService.delete(imageToRemove.getImageUrl());
        product.getImages().remove(imageToRemove);
        productRepository.save(product);
        listing.setUpdatedAt(new Date());
        listingRepository.save(listing); // No WS needed here as UI updates via form submit
    }
    @Override
    public Page<ProductListing> getByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        return listingRepository.findByUserId(userId, pageable);
    }
    @Override
    @Transactional
    public void delete(Long id) {
        ProductListing listing = getById(id);
        Product product = listing.getProduct();
        Long listingId = listing.getListingId();
        List<ProductImage> images = productImageRepository.findByProduct_ProductId(product.getProductId());
        for (ProductImage image : images) {
            fileStorageService.delete(image.getImageUrl());
        }
        listingRepository.delete(listing);
        productRepository.delete(product); // Assuming Cascade is set correctly or handled here
        // Send delete notification to Admin
        java.util.Map<String, Object> deleteMessage = new java.util.HashMap<>();
        deleteMessage.put("action", "delete");
        deleteMessage.put("listingId", listingId);
        messagingTemplate.convertAndSend("/topic/admin/listingUpdate", deleteMessage);
        // Maybe send a delete message to the user's listingUpdates topic too? Optional.
    }
    @Override
    public int findPageForListing(Long userId, Long listingId, int pageSize) {
        List<ProductListing> allListings = listingRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        int index = -1;
        for (int i = 0; i < allListings.size(); i++) {
            if (allListings.get(i).getListingId().equals(listingId)) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            return index / pageSize;
        }
        return 0;
    }
    // --- End of unchanged methods ---
}