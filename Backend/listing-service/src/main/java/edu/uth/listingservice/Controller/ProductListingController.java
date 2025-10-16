package edu.uth.listingservice.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.uth.listingservice.Model.ProductListing;
import edu.uth.listingservice.Service.ProductListingService;
import edu.uth.listingservice.DTO.UpdateListingDTO;
@RestController
@RequestMapping("/api/listings")

public class ProductListingController {

    @Autowired
    private ProductListingService listingService;

    @GetMapping
    public List<ProductListing> getFilteredListings(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "4") int limit) {
        return listingService.getActiveListings(type, sortBy, limit);
    }
    
    @GetMapping("/related")
    public List<ProductListing> getRelatedListings(
            @RequestParam String type,
            @RequestParam Long excludeId,
            @RequestParam(defaultValue = "6") int limit) {
        return listingService.findRandomRelated(type, excludeId, limit);
    }
    // @GetMapping
    // public List<ProductListing> getAll() {
    //     return listingService.getAll();
    // }

    @GetMapping("/{id}")
    public ProductListing getById(@PathVariable Long id) {
        return listingService.getById(id);
    }

    @GetMapping("/user/{userId}")
    public List<ProductListing> getByUserId(@PathVariable Long userId) {
        return listingService.getByUserId(userId);
    }

    @PostMapping
    public ProductListing create(@RequestBody ProductListing listing) {
        return listingService.create(listing);
    }

    @PutMapping("/{id}")
    public ProductListing update(@PathVariable Long id, @RequestBody ProductListing listing) {
        return listingService.update(id, listing);
    }
@PutMapping("/{id}/update-details")
    public ProductListing updateDetails(@PathVariable Long id, @RequestBody UpdateListingDTO dto) {
        return listingService.updateListingDetails(id, dto);
    }

    @PutMapping("/{id}/mark-as-sold")
    public ProductListing markAsSold(@PathVariable Long id) {
        return listingService.markAsSold(id);
    }
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        listingService.delete(id);
    }

}
