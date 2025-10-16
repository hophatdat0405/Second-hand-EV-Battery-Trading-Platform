package edu.uth.listingservice.Service;

import edu.uth.listingservice.Model.ProductImage;
import edu.uth.listingservice.Repository.ProductImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductImageServiceImpl implements ProductImageService {

    @Autowired
    private ProductImageRepository productImageRepository;

    @Override
    public List<ProductImage> getAllImages() {
        return productImageRepository.findAll();
    }

    @Override
    public ProductImage getImageById(Long id) {
        return productImageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found with ID: " + id));
    }

    @Override
    public List<ProductImage> getImagesByProductId(Long productId) {
        return productImageRepository.findByProduct_ProductId(productId);
    }

    @Override
    public ProductImage createImage(ProductImage productImage) {
        return productImageRepository.save(productImage);
    }

    @Override
    public ProductImage updateImage(Long id, ProductImage updatedImage) {
        ProductImage existing = getImageById(id);
        existing.setImageUrl(updatedImage.getImageUrl());
        existing.setImageType(updatedImage.getImageType());
        existing.setProduct(updatedImage.getProduct());
        return productImageRepository.save(existing);
    }

    @Override
    public void deleteImage(Long id) {
        productImageRepository.deleteById(id);
    }
}
