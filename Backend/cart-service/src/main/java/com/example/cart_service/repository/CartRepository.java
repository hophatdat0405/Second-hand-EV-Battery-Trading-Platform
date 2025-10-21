package com.example.cart_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cart_service.model.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {
    // kiểm tra đã tồn tại trong giỏ theo productId chưa
    boolean existsByProductId(Long productId);
}
