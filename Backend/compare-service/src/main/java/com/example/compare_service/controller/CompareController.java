package com.example.compare_service.controller;

import com.example.compare_service.client.ListingClient;
import com.example.compare_service.dto.ProductDetailDTO;
import com.example.compare_service.dto.ProductListingDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/compares")
@RequiredArgsConstructor
public class CompareController {

    private final ListingClient listingClient;

    /**
     * Map các biến thể request (tiếng VN/EN/không dấu) sang canonical type mà listing-service hiểu.
     * Nếu không map được, trả null => gọi listing-service không có filter (server-side fallback).
     */
    private String mapRequestedTypeToCanonical(String requestedType) {
        if (requestedType == null) return null;
        String r = requestedType.trim().toLowerCase();

        // Normalize common Vietnamese variants
        r = r.replaceAll("[ôóòỏõộồở]+", "o"); // nhẹ, optional
        r = r.replaceAll("[\\s_]+", " ");

        // canonical set: car, motorbike, bike, battery
        // mapping từ nhiều biến thể sang canonical value
        Map<String, String> MAP = new HashMap<>();
        // ô tô
        MAP.put("oto", "car");
        MAP.put("ô tô", "car");
        MAP.put("o to", "car");
        MAP.put("car", "car");
        MAP.put("xe ôtô", "car");
        MAP.put("xe oto", "car");

        // xe máy / motorbike
        MAP.put("xemay", "motorbike");
        MAP.put("xe máy", "motorbike");
        MAP.put("xe may", "motorbike");
        MAP.put("motorbike", "motorbike");
        MAP.put("motorcycle", "motorbike");
        MAP.put("xe máy điện", "motorbike");

        // xe đạp / bike
        MAP.put("xedap", "bike");
        MAP.put("xe đạp", "bike");
        MAP.put("xe dap", "bike");
        MAP.put("bike", "bike");
        MAP.put("bicycle", "bike");

        // pin / battery
        MAP.put("pin", "battery");
        MAP.put("battery", "battery");

        // Try direct matches first
        if (MAP.containsKey(r)) return MAP.get(r);

        // Try fuzzy: remove diacritics and non-alphanum
        String normalized = r.replaceAll("[^a-z0-9]", "");
        for (Map.Entry<String, String> e : MAP.entrySet()) {
            String keynorm = e.getKey().replaceAll("[^a-z0-9]", "");
            if (keynorm.equals(normalized)) return e.getValue();
        }

        // Last chance: check contains words
        if (r.contains("oto") || r.contains("ô tô") || r.contains("car")) return "car";
        if (r.contains("xemay") || r.contains("xe máy") || r.contains("motor")) return "motorbike";
        if (r.contains("xedap") || r.contains("xe đạp") || r.contains("bike") || r.contains("bicycle")) return "bike";
        if (r.contains("pin") || r.contains("battery")) return "battery";

        // không xác định
        return null;
    }

    @GetMapping("/listings")
    public List<ProductListingDTO> getListings(@RequestParam(required = false) String type,
                                               @RequestParam(defaultValue = "100") int limit) {
        // Map request type sang canonical => truyền canonical xuống listing-service
        String canonicalType = mapRequestedTypeToCanonical(type);

        List<ProductListingDTO> all;
        try {
            // Nếu canonicalType != null -> cố gắng cho listing-service filter (nếu nó hỗ trợ)
            all = listingClient.getAllListings(canonicalType, "date", limit);
            if (all == null) all = Collections.emptyList();
        } catch (Exception ex) {
            // nếu Feign lỗi, trả empty
            ex.printStackTrace();
            return Collections.emptyList();
        }

        // Nếu user không truyền type -> trả tất cả (đã là all)
        if (canonicalType == null) {
            return all;
        }

        // Server-side filter an toàn: chỉ lấy những listing có product.productType tương thích với canonicalType
        return all.stream()
                .filter(l -> {
                    if (l.getProduct() == null) return false;
                    String pt = l.getProduct().getProductType();
                    // nếu productType null, có thể xét specification.brand/name -> nhưng ưu tiên productType
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

    /**
     * Kiểm tra product.productType (có thể là 'car','motorbike','bike','battery', hoặc các biến thể)
     * so với canonical type ('car','motorbike','bike','battery').
     * So sánh chặt: ưu tiên equality / canonical match.
     */
    private boolean productTypeMatchesCanonical(String productTypeRaw, String canonicalType) {
        if (canonicalType == null) return true; // no filter
        if (productTypeRaw == null) return false;

        String p = productTypeRaw.trim().toLowerCase();

        // Normalize few known synonyms
        if (p.equals("car") || p.equals("oto") || p.equals("o to") || p.contains("car")) {
            return canonicalType.equals("car");
        }
        if (p.equals("motorbike") || p.equals("motorbike") || p.equals("motorcycle") || p.equals("motorbike")) {
            return canonicalType.equals("motorbike");
        }
        if (p.equals("bike") || p.equals("bicycle") || p.equals("xedap") || p.contains("bike") || p.contains("bicycle")) {
            return canonicalType.equals("bike");
        }
        if (p.equals("battery") || p.contains("battery") || p.contains("pin")) {
            return canonicalType.equals("battery");
        }

        // fallback: try contains but require canonical word present
        switch (canonicalType) {
            case "car":
                return p.contains("car") || p.contains("oto") || p.contains("ô tô");
            case "motorbike":
                return p.contains("motor") || p.contains("motorbike") || p.contains("xe may") || p.contains("xemay");
            case "bike":
                return p.contains("bike") || p.contains("bicycle") || p.contains("xedap") || p.contains("xe dap");
            case "battery":
                return p.contains("battery") || p.contains("pin");
            default:
                return false;
        }
    }
}
