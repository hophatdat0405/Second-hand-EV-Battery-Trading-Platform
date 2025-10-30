package edu.uth.listingservice.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.uth.listingservice.DTO.ProductDetailDTO;
import edu.uth.listingservice.DTO.UserDTO;
import edu.uth.listingservice.Model.Product;
import edu.uth.listingservice.Model.ProductImage;
import edu.uth.listingservice.Model.ProductListing;
import edu.uth.listingservice.Model.ProductSpecification;
import edu.uth.listingservice.Repository.ProductImageRepository;
import edu.uth.listingservice.Repository.ProductListingRepository;
import edu.uth.listingservice.Repository.ProductRepository;
import edu.uth.listingservice.Repository.ProductSpecificationRepository;

@Service
public class ProductDetailService {

    @Autowired private ProductRepository productRepository;
    @Autowired private ProductImageRepository imageRepository;
    @Autowired private ProductSpecificationRepository specificationRepository;
    @Autowired private ProductListingRepository listingRepository;
    //  Không cần conditionRepository nữa

    public ProductDetailDTO getProductDetail(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        List<ProductImage> images = imageRepository.findByProduct_ProductId(productId);
        ProductSpecification spec = specificationRepository.findByProduct_ProductId(productId);
        ProductListing listing = listingRepository.findByProduct_ProductId(productId);

        //  Không cần lấy ProductCondition riêng nữa vì nó đã có trong 'spec'

        //  Tạm tạo UserDTO để trả ra, sau này kết nối User Service thật
        UserDTO seller = null;
        if (listing != null && listing.getUserId() != null) {
            seller = new UserDTO();
            seller.setId(listing.getUserId());
            seller.setName("Seller Name"); // Tạm thời, sau này sẽ lấy từ User Service
            seller.setEmail("seller@example.com"); // Tạm thời
        }

        //  Dùng constructor của DTO mới để tạo đối tượng trả về một cách gọn gàng
        return new ProductDetailDTO(product, images, spec, listing, seller);
    }
}
