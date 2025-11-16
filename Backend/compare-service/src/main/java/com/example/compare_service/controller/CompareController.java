package com.example.compare_service.controller;

import com.example.compare_service.client.ListingClient;
import com.example.compare_service.dto.ProductDetailDTO;
import com.example.compare_service.dto.ProductListingDTO;
import com.example.compare_service.service.CompareService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/compares")
@RequiredArgsConstructor
public class CompareController {

    private final CompareService compareService;
    private final ListingClient listingClient;

    @GetMapping("/listings")
    public List<ProductListingDTO> getListings(@RequestParam(required = false) String type,
                                               @RequestParam(defaultValue = "100") int limit) {
        // get from cache / service
        List<ProductListingDTO> all = compareService.getListings(type, limit);

        // If no specific type requested — return all cached results
        if (type == null || type.trim().isEmpty()) return all;

        // server-side filter (kept minimal, same as before)
        final String canonicalType = mapRequestedTypeToCanonical(type);
        if (canonicalType == null) return all;

        return all.stream()
                .filter(l -> {
                    if (l.getProduct() == null) return false;
                    String pt = l.getProduct().getProductType();
                    return productTypeMatchesCanonical(pt, canonicalType);
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/listings/{id}")
    public ProductListingDTO getListing(@PathVariable Long id) {
        return listingClient.getListingById(id);
    }

    @GetMapping("/product-detail/{productId}")
    public ProductDetailDTO getProductDetail(@PathVariable Long productId) {
        return listingClient.getProductDetail(productId);
    }

    // ---------- helper methods (same as before) ----------
    private String mapRequestedTypeToCanonical(String requestedType) {
        if (requestedType == null) return null;
        String r = requestedType.trim().toLowerCase();
        r = r.replaceAll("[ôóòỏõộồở]+", "o");
        r = r.replaceAll("[\\s_]+", " ");
        // mapping...
        java.util.Map<String, String> MAP = new java.util.HashMap<>();
        MAP.put("oto", "car"); MAP.put("ô tô", "car"); MAP.put("o to", "car"); MAP.put("car", "car"); MAP.put("xe ôtô", "car"); MAP.put("xe oto", "car");
        MAP.put("xemay", "motorbike"); MAP.put("xe máy", "motorbike"); MAP.put("xe may", "motorbike"); MAP.put("motorbike", "motorbike");
        MAP.put("xedap", "bike"); MAP.put("xe đạp", "bike"); MAP.put("xe dap", "bike"); MAP.put("bike", "bike");
        MAP.put("pin", "battery"); MAP.put("battery", "battery");
        if (MAP.containsKey(r)) return MAP.get(r);
        String normalized = r.replaceAll("[^a-z0-9]", "");
        for (java.util.Map.Entry<String, String> e : MAP.entrySet()) {
            String keynorm = e.getKey().replaceAll("[^a-z0-9]", "");
            if (keynorm.equals(normalized)) return e.getValue();
        }
        if (r.contains("oto") || r.contains("ô tô") || r.contains("car")) return "car";
        if (r.contains("xemay") || r.contains("xe máy") || r.contains("motor")) return "motorbike";
        if (r.contains("xedap") || r.contains("xe đạp") || r.contains("bike")) return "bike";
        if (r.contains("pin") || r.contains("battery")) return "battery";
        return null;
    }

    private boolean productTypeMatchesCanonical(String productTypeRaw, String canonicalType) {
        if (canonicalType == null) return true;
        if (productTypeRaw == null) return false;
        String p = productTypeRaw.trim().toLowerCase();
        if ((p.equals("car") || p.equals("oto") || p.contains("car")) && canonicalType.equals("car")) return true;
        if ((p.equals("motorbike") || p.contains("motor")) && canonicalType.equals("motorbike")) return true;
        if ((p.equals("bike") || p.contains("bicycle") || p.contains("xedap")) && canonicalType.equals("bike")) return true;
        if ((p.equals("battery") || p.contains("pin")) && canonicalType.equals("battery")) return true;
        switch (canonicalType) {
            case "car": return p.contains("car") || p.contains("oto") || p.contains("ô tô");
            case "motorbike": return p.contains("motor") || p.contains("xemay") || p.contains("xe may");
            case "bike": return p.contains("bike") || p.contains("xedap");
            case "battery": return p.contains("pin") || p.contains("battery");
            default: return false;
        }
    }
}
