package edu.uth.listingservice.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.uth.listingservice.Model.ProductCondition;
import edu.uth.listingservice.Model.ProductSpecification;
import edu.uth.listingservice.Repository.ProductConditionRepository;
import edu.uth.listingservice.Repository.ProductSpecificationRepository;

@Service
public class ProductSpecificationServiceImpl implements ProductSpecificationService {

    @Autowired
    private ProductSpecificationRepository specificationRepository;
    
    @Autowired
    private ProductConditionRepository conditionRepository;

    @Override
    public List<ProductSpecification> getAll() {
        return specificationRepository.findAll();
    }

    @Override
    public ProductSpecification getById(Long id) {
        return specificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Specification not found with ID: " + id));
    }

    @Override
    public ProductSpecification getByProductId(Long productId) {
        return specificationRepository.findByProduct_ProductId(productId);
    }

    @Override
    public ProductSpecification create(ProductSpecification specification) {
        // Kiểm tra và lấy đối tượng ProductCondition đã được quản lý (managed)
        if (specification.getCondition() != null && specification.getCondition().getConditionId() != null) {
            Long conditionId = specification.getCondition().getConditionId();
            ProductCondition managedCondition = conditionRepository.findById(conditionId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Condition với ID: " + conditionId));
            specification.setCondition(managedCondition);
        }
        
        return specificationRepository.save(specification);
    }

    @Override
    public ProductSpecification update(Long id, ProductSpecification updatedSpec) {
        ProductSpecification existingSpec = getById(id);

        // Cập nhật ProductCondition
        if (updatedSpec.getCondition() != null && updatedSpec.getCondition().getConditionId() != null) {
            Long conditionId = updatedSpec.getCondition().getConditionId();
            ProductCondition managedCondition = conditionRepository.findById(conditionId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Condition với ID: " + conditionId));
            existingSpec.setCondition(managedCondition);
        } else {
            existingSpec.setCondition(null);
        }

        // Cập nhật các trường thông tin khác
        existingSpec.setProduct(updatedSpec.getProduct());
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

        return specificationRepository.save(existingSpec);
    }

    @Override
    public void delete(Long id) {
        specificationRepository.deleteById(id);
    }
}
