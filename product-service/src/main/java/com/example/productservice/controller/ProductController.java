package com.example.productservice.controller;

import com.example.productservice.dto.CreateProductRequest;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.security.PermissionConstants;
import com.example.productservice.service.ProductService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize(PermissionConstants.HAS_ROLE_ADMIN_OR_SELLER)
    public ProductResponse createProduct(@RequestBody CreateProductRequest request) {
        return productService.createProduct(request);
    }

    @GetMapping
    @PreAuthorize(PermissionConstants.HAS_ROLE_USER_OR_ADMIN_OR_SELLER)
    public List<ProductResponse> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    @PreAuthorize(PermissionConstants.HAS_ROLE_USER_OR_ADMIN_OR_SELLER)
    public ProductResponse getProductById(@PathVariable String id) {
        return productService.getProductById(id);
    }
}