package com.example.like_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.like_service.model.Like;


public interface LikeRepository extends JpaRepository <Like, Long> {
    Optional<Like> findByProductId(Long productId);
    boolean existsByProductId(Long productId);
    void deleteByProductId(Long productId);
}
