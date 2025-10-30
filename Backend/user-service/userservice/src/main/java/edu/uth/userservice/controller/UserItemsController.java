package edu.uth.userservice.controller;

import edu.uth.userservice.model.Item;
import edu.uth.userservice.model.Transaction;
import edu.uth.userservice.security.JwtUtil;
import edu.uth.userservice.service.ItemService;
import edu.uth.userservice.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = {"http://127.0.0.1:5501","http://localhost:3000","http://localhost:5501"})
public class UserItemsController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ItemService itemService;

    @Autowired
    private TransactionService transactionService;

    // helper: extract userId from Authorization header
    private Integer userIdFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        return jwtUtil.extractUserId(token);
    }

    @GetMapping("/items")
    public ResponseEntity<?> myItems(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Integer userId = userIdFromHeader(authHeader);
        if (userId == null) return ResponseEntity.status(401).body("Unauthorized");
        List<Item> list = itemService.findItemsByOwner(userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> myTransactions(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Integer userId = userIdFromHeader(authHeader);
        if (userId == null) return ResponseEntity.status(401).body("Unauthorized");
        List<Transaction> list = transactionService.findTransactionsByUser(userId);
        return ResponseEntity.ok(list);
    }
}
