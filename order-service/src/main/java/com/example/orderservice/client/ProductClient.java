package com.example.orderservice.client;

import com.example.orderservice.dto.ProductResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(
            RestClient.Builder restClientBuilder,
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder loadBalancedRestClientBuilder,
            @Value("${service-discovery.mode:eureka}") String serviceDiscoveryMode,
            @Value("${product-service.base-url:http://product-service}") String productServiceBaseUrl
    ) {
        RestClient.Builder serviceRestClientBuilder = "dns".equalsIgnoreCase(serviceDiscoveryMode)
                ? restClientBuilder
                : loadBalancedRestClientBuilder;
        this.restClient = serviceRestClientBuilder
                .baseUrl(productServiceBaseUrl)
                .build();
    }

    public ProductResponse getProductById(
            String productId,
            String authorizationHeader
    ) {
        ProductResponse product = restClient.get()
                .uri("/api/v1/products/{id}", productId)
                .header("Authorization", authorizationHeader)
                .retrieve()
                .body(ProductResponse.class);

        if (product == null) {
            throw new RuntimeException("Product not found");
        }

        return product;
    }
}
