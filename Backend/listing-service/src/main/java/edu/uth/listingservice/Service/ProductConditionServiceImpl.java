package edu.uth.listingservice.Service;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Caching;
import edu.uth.listingservice.Model.ProductCondition;
import edu.uth.listingservice.Repository.ProductConditionRepository;

@Service
public class ProductConditionServiceImpl implements ProductConditionService {

    private final ProductConditionRepository conditionRepository;

    public ProductConditionServiceImpl(ProductConditionRepository conditionRepository) {
        this.conditionRepository = conditionRepository;
    }

    @Override
    @Cacheable("productConditions") 
    public List<ProductCondition> getAll() {
        return conditionRepository.findAll();
    }

    @Override
    @Cacheable(value = "productCondition", key = "#id")
    public ProductCondition getById(Long id) {
        return conditionRepository.findById(id).orElse(null);
    }

    @Override
@Caching(evict = { 
        @CacheEvict(value = "productConditions", allEntries = true),
        @CacheEvict(value = "productCondition", allEntries = true) 
    })
    public ProductCondition create(ProductCondition condition) {
        return conditionRepository.save(condition);
    }

    @Override
   @Caching(evict = {
        @CacheEvict(value = "productConditions", allEntries = true),
        @CacheEvict(value = "productCondition", allEntries = true) // <-- BỔ SUNG
    })
    public ProductCondition update(Long id, ProductCondition condition) {
        ProductCondition existing = getById(id);
        if (existing != null) {
            existing.setConditionName(condition.getConditionName());
            return conditionRepository.save(existing);
        }
        return null;
    }

    @Override
   @Caching(evict = { 
        @CacheEvict(value = "productConditions", allEntries = true),
        @CacheEvict(value = "productCondition", allEntries = true) 
    })
    public void delete(Long id) {
        conditionRepository.deleteById(id);
    }
}