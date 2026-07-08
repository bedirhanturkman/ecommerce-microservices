package com.example.orderservice.client;

import com.example.orderservice.dto.ProductResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Qualifier;

@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder restClientBuilder
    ) {
        this.restClient = restClientBuilder
                .baseUrl("http://product-service")
                .build();
    }

    public ProductResponse getProductById(String productId) {
        ProductResponse product = restClient.get()
                .uri("/api/v1/products/{id}", productId)
                .retrieve()
                .body(ProductResponse.class);

        if (product == null) {
            throw new RuntimeException("Product not found");
        }

        return product;
    }
}