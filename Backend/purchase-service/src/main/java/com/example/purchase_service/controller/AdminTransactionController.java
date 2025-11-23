package com.example.purchase_service.controller;

import com.example.purchase_service.model.Complaint;
import com.example.purchase_service.model.ComplaintMessage;
import com.example.purchase_service.model.Purchase;
import com.example.purchase_service.mq.ComplaintMqPublisher;
import com.example.purchase_service.service.PurchaseSyncService;
import com.example.purchase_service.repository.ComplaintMessageRepository;
import com.example.purchase_service.repository.ComplaintRepository;
import com.example.purchase_service.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AdminTransactionController
 *
 * Endpoints used by admin-transaction.js frontend:
 * - GET  /api/admin-trans/transactions/all
 * - DELETE /api/admin-trans/transactions/{id}
 * - PUT /api/admin-trans/transactions/{id}
 * - GET  /api/admin-trans/transactions?userId=...
 * - GET  /api/admin-trans/complaints
 * - GET  /api/admin-trans/complaints/purchase/{purchaseId}
 * - GET  /api/admin-trans/complaints/{id}
 * - PUT  /api/admin-trans/complaints/{id}   (admin replies -> publishes MQ to user)
 * - POST /api/admin-trans/sync-purchases
 *
 * Copy-paste this file into: src/main/java/com/example/purchase_service/controller/AdminTransactionController.java
 */
@RestController
@RequestMapping("/api/admin-trans")
@RequiredArgsConstructor
@Slf4j
public class AdminTransactionController {

    private final PurchaseRepository purchaseRepository;
    private final RestTemplate restTemplate;
    private final PurchaseSyncService purchaseSyncService;
    private final ComplaintRepository complaintRepository;
    private final ComplaintMessageRepository complaintMessageRepository;
    private final ComplaintMqPublisher mqPublisher;

    @Value("${user.service.url:http://localhost:8084}")
    private String userServiceUrl;

    @Value("${listing.service.url:http://localhost:8080}")
    private String listingServiceUrl;

    /**
     * Return all transactions (used by admin UI).
     * Path: GET /api/admin-trans/transactions/all
     */
    @GetMapping("/transactions/all")
    public ResponseEntity<List<Purchase>> getAllTransactions() {
        List<Purchase> txs;
        try {
            txs = purchaseRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        } catch (Exception e) {
            // fallback
            txs = purchaseRepository.findAll();
        }
        return ResponseEntity.ok(txs);
    }

    /**
     * Delete transaction by id
     * DELETE /api/admin-trans/transactions/{id}
     */
    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable("id") Long id) {
        if (!purchaseRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Transaction not found"));
        }
        purchaseRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("deletedId", id));
    }

    /**
     * Update a few editable fields on a transaction (status, price, address, productName)
     * PUT /api/admin-trans/transactions/{id}
     */
    @PutMapping("/transactions/{id}")
    public ResponseEntity<?> updateTransaction(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Purchase t = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (body.containsKey("status") && body.get("status") != null) {
            t.setStatus(String.valueOf(body.get("status")));
        }
        if (body.containsKey("productName")) {
            Object v = body.get("productName");
            t.setProductName(v == null ? null : String.valueOf(v));
        }
        if (body.containsKey("price")) {
            Object p = body.get("price");
            if (p instanceof Number) t.setPrice(((Number) p).doubleValue());
            else {
                try { t.setPrice(Double.parseDouble(String.valueOf(p))); } catch (Exception ignored) {}
            }
        }
        if (body.containsKey("address")) {
            Object a = body.get("address");
            t.setAddress(a == null ? null : String.valueOf(a));
        }
        Purchase saved = purchaseRepository.save(t);
        return ResponseEntity.ok(saved);
    }

    /**
     * Admin-only listing endpoint (enriched) — requires userId in query to verify admin role.
     * GET /api/admin-trans/transactions?userId=...
     */
    @GetMapping("/transactions")
    public ResponseEntity<?> listTransactions(@RequestParam("userId") Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "userId required"));
        }

        // verify admin role by calling user-service
        try {
            String rolesUrl = String.format("%s/api/user/%d/roles", userServiceUrl, userId);
            ResponseEntity<List<String>> rolesResp = restTemplate.exchange(
                    rolesUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<String>>() {}
            );

            if (!rolesResp.getStatusCode().is2xxSuccessful() || rolesResp.getBody() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Cannot verify user roles"));
            }
            List<String> roles = rolesResp.getBody().stream()
                    .filter(Objects::nonNull)
                    .map(String::toUpperCase)
                    .collect(Collectors.toList());

            if (roles.stream().noneMatch(r -> "ADMIN".equals(r))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied: Admin only"));
            }
        } catch (Exception ex) {
            log.warn("User-service role check failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied or user-service unreachable", "detail", ex.getMessage()));
        }

        List<Purchase> txs = purchaseRepository.findAll();

        List<Map<String, Object>> out = new ArrayList<>();
        for (Purchase t : txs) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", t.getId());
            item.put("buyerId", t.getUserId());
            item.put("sellerId", t.getSellerId());
            item.put("productId", t.getProductId());
            item.put("productName", t.getProductName());

            // === 🛑 SỬA LỖI TẠI ĐÂY ===
            // Thêm dòng này để trả về URL ảnh cho frontend
            item.put("productImage", t.getProductImage()); 
            // ========================

            item.put("price", t.getPrice());
            item.put("status", t.getStatus());
            item.put("fullName", t.getFullName());
            item.put("phone", t.getPhone());
            item.put("email", t.getEmail());
            item.put("address", t.getAddress());
            item.put("createdAt", t.getCreatedAt());
            long cc = complaintRepository.countByPurchaseId(t.getId());
            item.put("complaintCount", cc);

            // attempt to enrich buyer/seller info without failing if user-service unreachable
            try {
                Long buyerId = t.getUserId();
                if (buyerId != null) {
                    String buyerUrl = String.format("%s/api/user/%d", userServiceUrl, buyerId);
                    ResponseEntity<Map> buyerResp = restTemplate.getForEntity(buyerUrl, Map.class);
                    if (buyerResp.getStatusCode().is2xxSuccessful() && buyerResp.getBody() != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> b = buyerResp.getBody();
                        Object name = Optional.ofNullable(b.get("name")).orElse(Optional.ofNullable(b.get("fullName")).orElse(""));
                        item.put("buyerName", name);
                        item.put("buyerEmail", b.get("email"));
                        item.put("buyerPhone", b.get("phone"));
                        item.put("buyerAddress", b.get("address"));
                    }
                }
            } catch (Exception ignored) {}

            try {
                Long sellerId = t.getSellerId();
                if (sellerId != null) {
                    String sellerUrl = String.format("%s/api/user/%d", userServiceUrl, sellerId);
                    ResponseEntity<Map> sellerResp = restTemplate.getForEntity(sellerUrl, Map.class);
                    if (sellerResp.getStatusCode().is2xxSuccessful() && sellerResp.getBody() != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> s = sellerResp.getBody();
                        item.put("sellerName", Optional.ofNullable(s.get("name")).orElse(""));
                        item.put("sellerEmail", s.get("email"));
                        item.put("sellerPhone", s.get("phone"));
                    }
                }
            } catch (Exception ignored) {}

            out.add(item);
        }

        return ResponseEntity.ok(out);
    }

    /**
     * ADMIN: list all complaints (admin only)
     * GET /api/admin-trans/complaints?userId=...
     */
    @GetMapping("/complaints")
    public ResponseEntity<?> adminListComplaints(@RequestParam("userId") Long userId) {
        if (userId == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "userId required"));
        // verify admin role
        try {
            String rolesUrl = String.format("%s/api/user/%d/roles", userServiceUrl, userId);
            ResponseEntity<List<String>> rolesResp = restTemplate.exchange(
                    rolesUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<String>>() {}
            );
            if (!rolesResp.getStatusCode().is2xxSuccessful() || rolesResp.getBody() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Cannot verify user roles"));
            }
            List<String> roles = rolesResp.getBody().stream().filter(Objects::nonNull).map(String::toUpperCase).collect(Collectors.toList());
            if (roles.stream().noneMatch(r -> "ADMIN".equals(r))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied: Admin only"));
            }
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied or user-service unreachable", "detail", ex.getMessage()));
        }

        List<Complaint> list = complaintRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(list);
    }

    /**
     * Admin: get complaints for a specific purchase (admin only)
     * GET /api/admin-trans/complaints/purchase/{purchaseId}?userId=...
     */
    @GetMapping("/complaints/purchase/{purchaseId}")
    public ResponseEntity<?> getComplaintsForPurchase(@PathVariable("purchaseId") Long purchaseId, @RequestParam("userId") Long userId) {
        if (userId == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "userId required"));
        
        boolean isAdmin = false;
        boolean isSeller = false;

        // 1. Kiểm tra xem có phải Admin không
        try {
            String rolesUrl = String.format("%s/api/user/%d/roles", userServiceUrl, userId);
            ResponseEntity<List<String>> rolesResp = restTemplate.exchange(
                    rolesUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<String>>() {}
            );
            if (rolesResp.getStatusCode().is2xxSuccessful() && rolesResp.getBody() != null) {
                List<String> roles = rolesResp.getBody().stream().filter(Objects::nonNull).map(String::toUpperCase).collect(Collectors.toList());
                if (roles.stream().anyMatch(r -> "ADMIN".equals(r))) {
                    isAdmin = true;
                }
            }
        } catch (Exception ex) {
            log.warn("Kiểm tra vai trò Admin thất bại (không nghiêm trọng): {}", ex.getMessage());
        }

        // 2. Nếu không phải Admin, kiểm tra xem có phải Người bán (Seller) của giao dịch này không
        if (!isAdmin) {
            try {
                Purchase p = purchaseRepository.findById(purchaseId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch (purchase) với ID: " + purchaseId));
                
                if (p.getSellerId() != null && p.getSellerId().equals(userId)) {
                    isSeller = true;
                }
            } catch (Exception ex) {
                 log.warn("Kiểm tra Người bán thất bại: {}", ex.getMessage());
                 // Ném lỗi 403 nếu không tìm thấy Purchase hoặc ID không khớp
                 return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied: Lỗi khi xác thực người bán.", "detail", ex.getMessage()));
            }
        }

        // 3. Nếu không phải Admin VÀ cũng không phải Người bán -> Chặn
        if (!isAdmin && !isSeller) {
             return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Bạn không có quyền xem khiếu nại này (403 Forbidden)."));
        }

        // 4. Nếu là Admin hoặc Người bán -> Cho phép
        List<Complaint> list = complaintRepository.findByPurchaseIdOrderByCreatedAtDesc(purchaseId);
        return ResponseEntity.ok(list);
    }
    /**
     * Admin: get single complaint by id (admin only)
     * GET /api/admin-trans/complaints/{id}?userId=...
     */
    @GetMapping("/complaints/{id}")
    public ResponseEntity<?> getComplaint(@PathVariable("id") Long id, @RequestParam("userId") Long userId) {
        if (userId == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "userId required"));
        // verify admin
        try {
            String rolesUrl = String.format("%s/api/user/%d/roles", userServiceUrl, userId);
            ResponseEntity<List<String>> rolesResp = restTemplate.exchange(
                    rolesUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<String>>() {}
            );
            if (!rolesResp.getStatusCode().is2xxSuccessful() || rolesResp.getBody() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Cannot verify user roles"));
            }
            List<String> roles = rolesResp.getBody().stream().filter(Objects::nonNull).map(String::toUpperCase).collect(Collectors.toList());
            if (roles.stream().noneMatch(r -> "ADMIN".equals(r))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied: Admin only"));
            }
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied or user-service unreachable", "detail", ex.getMessage()));
        }

        Complaint c = complaintRepository.findById(id).orElse(null);
        if (c == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Complaint not found"));
        return ResponseEntity.ok(c);
    }

    /**
     * Admin updates complaint (reply). When admin replies we persist adminResponse and also create a ComplaintMessage
     * and publish to user via MQ so user SSE receives it.
     *
     * PUT /api/admin-trans/complaints/{id}
     * Body: { "adminUserId": 123, "adminResponse": "text", "status": "RESOLVED" }
     */
    @PutMapping("/complaints/{id}")
    public ResponseEntity<?> updateComplaintStatus(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Long adminUserId = body.containsKey("adminUserId") ? (body.get("adminUserId") instanceof Number ? ((Number) body.get("adminUserId")).longValue() : null) : null;
        if (adminUserId == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "adminUserId required as query body to verify admin"));

        // verify admin role
        try {
            String rolesUrl = String.format("%s/api/user/%d/roles", userServiceUrl, adminUserId);
            ResponseEntity<List<String>> rolesResp = restTemplate.exchange(
                    rolesUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<String>>() {}
            );
            if (!rolesResp.getStatusCode().is2xxSuccessful() || rolesResp.getBody() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Cannot verify user roles"));
            }
            List<String> roles = rolesResp.getBody().stream().filter(Objects::nonNull).map(String::toUpperCase).collect(Collectors.toList());
            if (roles.stream().noneMatch(r -> "ADMIN".equals(r))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied: Admin only"));
            }
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied or user-service unreachable", "detail", ex.getMessage()));
        }

        Complaint c = complaintRepository.findById(id).orElse(null);
        if (c == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Complaint not found"));

        if (body.containsKey("status") && body.get("status") != null) {
            c.setStatus(String.valueOf(body.get("status")));
        }
        if (body.containsKey("adminResponse") && body.get("adminResponse") != null) {
            String adminResp = String.valueOf(body.get("adminResponse"));
            c.setAdminResponse(adminResp);
            c.setAdminUserId(adminUserId);
            c.setRepliedAt(java.time.LocalDateTime.now());
            if (!body.containsKey("status")) c.setStatus("RESOLVED");

            // create message record — đảm bảo createdAt không null
            ComplaintMessage am = ComplaintMessage.builder()
                    .complaintId(c.getId())
                    .sender("ADMIN")
                    .senderName("Admin#" + adminUserId)
                    .content(adminResp)
                    .createdAt(java.time.LocalDateTime.now()) // <-- quan trọng
                    .build();
            ComplaintMessage savedAm = complaintMessageRepository.save(am);

            // publish to user via MQ
            Long userId = null;
            if (c.getPurchaseId() != null) {
                Optional<Purchase> opt = purchaseRepository.findById(c.getPurchaseId());
                if (opt.isPresent()) userId = opt.get().getUserId();
            }
            Map<String, Object> pay = new HashMap<>();
            pay.put("type", "complaint.message");
            pay.put("complaintId", c.getId());
            pay.put("messageId", savedAm.getId());
            pay.put("purchaseId", c.getPurchaseId());
            pay.put("userId", userId);
            pay.put("senderName", "Admin#" + adminUserId);
            pay.put("content", adminResp);
            // guard against null createdAt
            String createdAtStr = (savedAm.getCreatedAt() != null) ? savedAm.getCreatedAt().toString() : java.time.LocalDateTime.now().toString();
            pay.put("createdAt", createdAtStr);
            try { mqPublisher.publishToUser(pay); } catch (Exception ignored) {}
        }

        Complaint saved = complaintRepository.save(c);
        return ResponseEntity.ok(saved);
    }


    /**
     * Trigger manual sync (optional)
     */
    @PostMapping("/sync-purchases")
    public ResponseEntity<?> syncPurchases() {
        try {
            List<Purchase> synced = purchaseSyncService.syncAllPurchases();
            return ResponseEntity.ok(Map.of("syncedCount", synced.size()));
        } catch (Exception ex) {
            log.error("Sync purchases failed", ex);
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }

    // Giả định: AdminTransactionController.java (Thêm vào)
    @GetMapping("/purchase/by-seller-listing/{listingId}")
    public ResponseEntity<Purchase> getPurchaseBySellerListing(
            @PathVariable("listingId") Long listingId, 
            @RequestParam("sellerId") Long sellerId) 
    {
        // 🛑 LƯU Ý: Frontend (manage-listings.js) sử dụng Listing ID (listingId) 
        // để truyền vào. Ta giả định Listing ID = Product ID (có thể sai) 
        // hoặc tìm kiếm dựa trên Product ID.

        Optional<Purchase> purchaseOpt = purchaseRepository.findAll().stream()
                // Chỉ tìm kiếm Purchase đã Hoàn thành (completed) hoặc đang chờ (waiting)
                .filter(p -> p.getStatus().equals("completed") || p.getStatus().equals("waiting_delivery"))
                // Lọc theo sellerId (Người bán)
                .filter(p -> p.getSellerId() != null && p.getSellerId().equals(sellerId))
                // Lọc theo ProductId (giả định Listing ID = Product ID)
                .filter(p -> p.getProductId() != null && p.getProductId().equals(listingId)) 
                .findFirst();

        if (purchaseOpt.isEmpty()) {
            // Nếu không tìm thấy theo SellerId + ProductId, trả về 404.
            log.warn("404: Purchase not found for listingId {} and sellerId {}", listingId, sellerId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Lỗi tải thông tin giao dịch (ListingId=" + listingId + ")");
        }
        return ResponseEntity.ok(purchaseOpt.get());
    }
}