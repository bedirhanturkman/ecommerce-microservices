package com.example.authservice.service;

import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.CreateCustomerRequest;
import com.example.authservice.dto.CustomerInternalResponse;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AuthService {

    private final RestClient restClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:8081")
                .build();
    }

    public AuthResponse register(RegisterRequest request) {

        CreateCustomerRequest customerRequest = new CreateCustomerRequest(
                request.firstName(),
                request.lastName(),
                request.email(),
                passwordEncoder.encode(request.password()),
                "USER"
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