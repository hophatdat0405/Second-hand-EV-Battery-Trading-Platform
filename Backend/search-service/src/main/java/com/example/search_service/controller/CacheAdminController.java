package com.example.search_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/cache")
public class CacheAdminController {

    @Autowired
    private CacheManager cacheManager;

    @DeleteMapping("/search-results")
    public ResponseEntity<?> clearSearchCache() {
        if (cacheManager.getCache("searchResults") != null) {
            cacheManager.getCache("searchResults").clear();
        }
        return ResponseEntity.ok().build();
    }
}
