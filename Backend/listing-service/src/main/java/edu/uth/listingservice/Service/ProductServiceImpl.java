package edu.uth.listingservice.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager; 
import org.springframework.stereotype.Service;

import edu.uth.listingservice.Model.Product;
import edu.uth.listingservice.Repository.ProductRepository;
import org.springframework.transaction.annotation.Transactional; 


@Service
public class ProductServiceImpl implements ProductService {


    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager; 

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }


    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }


    @Override
    @Transactional
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }


    @Override
    @Transactional
    public Product updateProduct(Long id, Product product) {
        // BỔ SUNG: Xóa cache chi tiết trước khi cập nhật
        cacheManager.getCache("productDetails").evictIfPresent(id);
    
    cacheManager.getCache("productSpecs").evictIfPresent("prod-" + id); // <-- THÊM DÒNG NÀY
        product.setProductId(id);
        return productRepository.save(product);
    }


    @Override
    @Transactional
    public void deleteProduct(Long id) {
        // BỔ SUNG: Xóa cache chi tiết trước khi xóa
        cacheManager.getCache("productDetails").evictIfPresent(id);
cacheManager.getCache("productSpecs").evictIfPresent("prod-" + id); // <-- THÊM DÒNG NÀY
        productRepository.deleteById(id);
    }
}
