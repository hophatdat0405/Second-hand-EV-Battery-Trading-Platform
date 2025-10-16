package edu.uth.listingservice.Service;

import java.util.List;

import org.springframework.stereotype.Service;

import edu.uth.listingservice.Model.ProductCondition;
import edu.uth.listingservice.Repository.ProductConditionRepository;

@Service
public class ProductConditionServiceImpl implements ProductConditionService {

    private final ProductConditionRepository conditionRepository;

    public ProductConditionServiceImpl(ProductConditionRepository conditionRepository) {
        this.conditionRepository = conditionRepository;
    }

    @Override
    public List<ProductCondition> getAll() {
        return conditionRepository.findAll();
    }

    @Override
    public ProductCondition getById(Long id) {
        return conditionRepository.findById(id).orElse(null);
    }

    @Override
    public ProductCondition create(ProductCondition condition) {
        return conditionRepository.save(condition);
    }

    @Override
    public ProductCondition update(Long id, ProductCondition condition) {
        ProductCondition existing = getById(id);
        if (existing != null) {
            existing.setConditionName(condition.getConditionName());
            return conditionRepository.save(existing);
        }
        return null;
    }

    @Override
    public void delete(Long id) {
        conditionRepository.deleteById(id);
    }
}