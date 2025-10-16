package edu.uth.listingservice.Service;

import java.util.Date;
import java.util.List;

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
@Service
public class ProductListingServiceImpl implements ProductListingService {

    @Autowired
    private ProductListingRepository listingRepository;
      @Autowired private ProductRepository productRepository;
    @Autowired private ProductSpecificationRepository specRepository;

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
    public List<ProductListing> getByUserId(Long userId) {
        return listingRepository.findByUserId(userId);
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

    @Override
    public void delete(Long id) {
        listingRepository.deleteById(id);
    }

    @Override
    public List<ProductListing> getActiveListings(String type, String sortBy, int limit) {
        // Mặc định sắp xếp theo ngày đăng mới nhất
        Sort sort = Sort.by(Sort.Direction.DESC, "listingDate");

        // Nếu người dùng chọn "Giá tốt", đổi lại sắp xếp theo giá tăng dần
        if ("price".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "product.price");
        }

        // Tạo đối tượng Pageable để giới hạn số lượng kết quả
        Pageable pageable = PageRequest.of(0, limit, sort);

        // Nếu có lọc theo loại sản phẩm (car, bike,...)
        if (type != null && !type.isEmpty() && !"all".equalsIgnoreCase(type)) {
            return listingRepository.findByStatusAndProductType(ListingStatus.PENDING, type, pageable);
        }

        // Nếu không, lấy tất cả các loại
        return listingRepository.findByListingStatus(ListingStatus.PENDING, pageable);
    }
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

        // YÊU CẦU: Không cho cập nhật tin đã bán.
        if (listing.getListingStatus() == ListingStatus.SOLD) {
            throw new IllegalStateException("Không thể chỉnh sửa tin đã bán.");
        }
        
        // YÊU CẦU: Tin ACTIVE chỉ được sửa 1 lần duy nhất.
        if (listing.getListingStatus() == ListingStatus.ACTIVE && listing.isUpdatedOnce()) {
            throw new IllegalStateException("Tin đăng này đã được chỉnh sửa một lần và không thể sửa thêm.");
        }

        // YÊU CẦU: Nếu là PENDING, cho phép sửa tất cả các trường.
        if (listing.getListingStatus() == ListingStatus.PENDING) {
            product.setProductName(dto.getProductName());
            spec.setBrand(dto.getBrand());
        }

        // YÊU CẦU: Luôn cho phép chỉnh sửa các trường này (giá, mô tả, SĐT, địa chỉ, bảo hành)
        // ngay cả khi tin đang ACTIVE.
        product.setPrice(dto.getPrice());
        product.setDescription(dto.getDescription());
        listing.setPhone(dto.getPhone());
        listing.setLocation(dto.getLocation());
        spec.setWarrantyPolicy(dto.getWarrantyPolicy());
        
        // YÊU CẦU: Khi sửa một tin đang ACTIVE, tự động chuyển nó về PENDING và đánh dấu đã sửa.
        if (listing.getListingStatus() == ListingStatus.ACTIVE) {
            listing.setListingStatus(ListingStatus.PENDING); // Tự động chuyển về chờ duyệt
            listing.setUpdatedOnce(true); // Đánh dấu đã sửa 1 lần
        }

        // Cập nhật thời gian và lưu lại tất cả thay đổi.
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
        // Kiểm tra để đảm bảo tin chưa được bán trước đó.
        if (listing.getListingStatus() == ListingStatus.SOLD) {
            throw new IllegalStateException("Tin này đã ở trạng thái 'Đã Bán'.");
        }
        listing.setListingStatus(ListingStatus.SOLD);
        listing.setUpdatedAt(new Date());
        return listingRepository.save(listing);
    }
}