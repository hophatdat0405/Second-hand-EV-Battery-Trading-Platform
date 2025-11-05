// File: edu/uth/listingservice/Service/ProductSpecificationServiceImpl.java
package edu.uth.listingservice.Service;

import java.util.List;
import java.util.Optional; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uth.listingservice.Model.ProductCondition;
import edu.uth.listingservice.Model.ProductSpecification;
import edu.uth.listingservice.Repository.ProductConditionRepository;
import edu.uth.listingservice.Repository.ProductSpecificationRepository;


import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CacheEvict;

@Service
public class ProductSpecificationServiceImpl implements ProductSpecificationService {

    @Autowired
    private ProductSpecificationRepository specificationRepository;
    @Autowired
    private CacheManager cacheManager; 
    @Autowired
    private ProductConditionRepository conditionRepository;

    // (Hàm getAll, getById, getByProductId giữ nguyên)
    @Override
    public List<ProductSpecification> getAll() {
        return specificationRepository.findAll();
    }
    @Override
    @Cacheable(value = "productSpecs", key = "#id")
    public ProductSpecification getById(Long id) {
        return specificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Specification not found with ID: " + id));
    }
    @Override
    @Cacheable(value = "productSpecs", key = "'prod-' + #productId")
    public ProductSpecification getByProductId(Long productId) {
        return specificationRepository.findByProduct_ProductId(productId);
    }


    @Override
    @Transactional
 
    @Caching(evict = {
        @CacheEvict(value = "productSpecs", allEntries = true), // Xóa cache danh sách
        @CacheEvict(value = "productDetails", key = "#result.product.productId")
    })
    public ProductSpecification create(ProductSpecification specification) {
        // 1. Xử lý logic nghiệp vụ (gắn Condition)
        if (specification.getCondition() != null && specification.getCondition().getConditionId() != null) {
            Long conditionId = specification.getCondition().getConditionId();
            ProductCondition managedCondition = conditionRepository.findById(conditionId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Condition với ID: " + conditionId));
            specification.setCondition(managedCondition);
        } else {
            specification.setCondition(null);
        }
        
        // 2. Lưu vào DB
        ProductSpecification savedSpec = specificationRepository.save(specification);

    

        return savedSpec;
    }

    @Override
    @Transactional
    
    @Caching(evict = {
        // Xóa cache chi tiết của sản phẩm
        @CacheEvict(value = "productDetails", key = "#result.product.productId"),
        // Xóa 2 cache của chính spec này
        @CacheEvict(value = "productSpecs", key = "#id"),
        @CacheEvict(value = "productSpecs", key = "'prod-' + #result.product.productId")
    })
    public ProductSpecification update(Long id, ProductSpecification updatedSpec) {
        ProductSpecification existingSpec = specificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Specification not found with ID: " + id));

        

        // 1. Xử lý logic nghiệp vụ (gắn Condition)
        if (updatedSpec.getCondition() != null && updatedSpec.getCondition().getConditionId() != null) {
            Long conditionId = updatedSpec.getCondition().getConditionId();
            ProductCondition managedCondition = conditionRepository.findById(conditionId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Condition với ID: " + conditionId));
            existingSpec.setCondition(managedCondition);
        } else {
            existingSpec.setCondition(null);
        }

        // 2. Cập nhật các trường thông tin khác (giữ nguyên)
        existingSpec.setYearOfManufacture(updatedSpec.getYearOfManufacture());
        existingSpec.setBrand(updatedSpec.getBrand());
        existingSpec.setMileage(updatedSpec.getMileage());
        existingSpec.setBatteryCapacity(updatedSpec.getBatteryCapacity());
        existingSpec.setBatteryType(updatedSpec.getBatteryType());
        existingSpec.setBatteryLifespan(updatedSpec.getBatteryLifespan());
        existingSpec.setCompatibleVehicle(updatedSpec.getCompatibleVehicle());
        existingSpec.setWarrantyPolicy(updatedSpec.getWarrantyPolicy());
        existingSpec.setMaxSpeed(updatedSpec.getMaxSpeed());
        existingSpec.setRangePerCharge(updatedSpec.getRangePerCharge());
        existingSpec.setColor(updatedSpec.getColor());
        existingSpec.setChargeTime(updatedSpec.getChargeTime());
        existingSpec.setChargeCycles(updatedSpec.getChargeCycles());

   

        // 4. Lưu
        return specificationRepository.save(existingSpec);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // (Với hàm delete, dùng CacheManager thủ công là CHẤP NHẬN ĐƯỢC
        // vì @CacheEvict không thể lấy productId từ biến `existingSpec`)

        // 1. Tìm spec để lấy productId TRƯỚC KHI XÓA
        Optional<ProductSpecification> specOpt = specificationRepository.findById(id);
        
        if (specOpt.isEmpty()) {
            return; // Không tìm thấy, không làm gì cả
        }
        
        ProductSpecification existingSpec = specOpt.get();
        Long productId = existingSpec.getProduct().getProductId();

        // 2. Xóa
        specificationRepository.deleteById(id);

        // 3. Xóa cache liên quan (Chuyển xuống sau khi xóa DB)
        // (Vẫn còn race condition, nhưng tốt hơn là để ở trước)
        cacheManager.getCache("productDetails").evictIfPresent(productId);
        cacheManager.getCache("productSpecs").clear(); // Dùng clear() cho an toàn
    }
}