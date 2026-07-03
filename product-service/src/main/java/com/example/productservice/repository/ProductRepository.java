package com.example.productservice.repository;

import com.example.productservice.document.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {

    boolean existsByName(String name);
}