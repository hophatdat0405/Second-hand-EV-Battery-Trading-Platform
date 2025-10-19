// File mới: edu/uth/listingservice/Controller/AdminListingController.java

package edu.uth.listingservice.Controller;

import edu.uth.listingservice.DTO.AdminRejectDTO;
import edu.uth.listingservice.Model.ListingStatus;
import edu.uth.listingservice.Model.ProductListing;
import edu.uth.listingservice.Service.AdminListingService; // Sẽ tạo service này ở bước 3
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/listings") // URL base cho các API của admin
public class AdminListingController {

    @Autowired
    private AdminListingService adminListingService;

    // 1. API lấy danh sách tin đăng theo trạng thái (VD: lấy các tin PENDING)
    @GetMapping
    public Page<ProductListing> getListingsByStatus(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        ListingStatus listingStatus = ListingStatus.valueOf(status.toUpperCase());
        return adminListingService.getListingsByStatus(listingStatus, page, size);
    }

    // 2. API để duyệt (approve) một tin đăng
    @PostMapping("/{id}/approve")
    public ResponseEntity<ProductListing> approveListing(@PathVariable Long id) {
        ProductListing approvedListing = adminListingService.approveListing(id);
        return ResponseEntity.ok(approvedListing);
    }

    // 3. API để từ chối (reject) một tin đăng
    @PostMapping("/{id}/reject")
    public ResponseEntity<ProductListing> rejectListing(@PathVariable Long id, @RequestBody AdminRejectDTO payload) {
        ProductListing rejectedListing = adminListingService.rejectListing(id, payload.getReason());
        return ResponseEntity.ok(rejectedListing);
    }

    // 4. API để gắn nhãn "Đã kiểm định"
    @PostMapping("/{id}/verify")
    public ResponseEntity<ProductListing> verifyListing(@PathVariable Long id) {
        ProductListing verifiedListing = adminListingService.verifyListing(id);
        return ResponseEntity.ok(verifiedListing);
    }
    @GetMapping("/search")
    public Page<ProductListing> searchListings(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        return adminListingService.searchListings(query, page, size);
    }
}