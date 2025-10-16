package edu.uth.listingservice.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.uth.listingservice.Model.Product;
import edu.uth.listingservice.Repository.ProductRepository;


@Service
public class ProductServiceImpl implements ProductService {


@Autowired
private ProductRepository productRepository;


@Override
public List<Product> getAllProducts() {
return productRepository.findAll();
}


@Override
public Product getProductById(Long id) {
return productRepository.findById(id).orElse(null);
}


@Override
public Product createProduct(Product product) {
return productRepository.save(product);
}


@Override
public Product updateProduct(Long id, Product product) {
product.setProductId(id);
return productRepository.save(product);
}


@Override
public void deleteProduct(Long id) {
productRepository.deleteById(id);
}
}
