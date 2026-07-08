package com.example.authservice.service;

import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.CreateCustomerRequest;
import com.example.authservice.dto.CustomerInternalResponse;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Qualifier;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE = "USER";

    private final RestClient restClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder restClientBuilder,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.restClient = restClientBuilder
                .baseUrl("http://customer-service")
                .build();
    }

    public AuthResponse register(RegisterRequest request) {

        String role = request.role() == null || request.role().isBlank()
                ? DEFAULT_ROLE
                : request.role();

        CreateCustomerRequest customerRequest = new CreateCustomerRequest(
                request.firstName(),
                request.lastName(),
                request.email(),
                passwordEncoder.encode(request.password()),
                role
        );

        CustomerInternalResponse savedCustomer = restClient.post()
                .uri("/api/v1/customers/internal")
                .body(customerRequest)
                .retrieve()
                .body(CustomerInternalResponse.class);

        String token = jwtService.generateToken(
                savedCustomer.email(),
                savedCustomer.role()
        );

        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {

        CustomerInternalResponse customer = restClient.get()
                .uri("/api/v1/customers/by-email/{email}", request.email())
                .retrieve()
                .body(CustomerInternalResponse.class);

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                customer.password()
        );

        if (!passwordMatches) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtService.generateToken(
                customer.email(),
                customer.role()
        );

        return new AuthResponse(token);
    }
}