package com.example.authservice.service;

import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.CreateCustomerRequest;
import com.example.authservice.dto.CustomerCredentialsResponse;
import com.example.authservice.dto.CustomerRegistrationResponse;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE = "USER";
    private static final String INTERNAL_API_KEY_HEADER =
            "X-Internal-Api-Key";

    private final RestClient restClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder restClientBuilder,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${internal.api-key}") String internalApiKey
    ) {
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.restClient = restClientBuilder
                .baseUrl("http://customer-service")
                .defaultHeader(
                        INTERNAL_API_KEY_HEADER,
                        requireInternalApiKey(internalApiKey)
                )
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

        CustomerRegistrationResponse savedCustomer = restClient.post()
                .uri("/internal/api/v1/customers")
                .body(customerRequest)
                .retrieve()
                .body(CustomerRegistrationResponse.class);

        String token = jwtService.generateToken(
                savedCustomer.email(),
                savedCustomer.role()
        );

        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {

        CustomerCredentialsResponse customer = restClient.get()
                .uri(
                        "/internal/api/v1/customers/credentials/by-email/{email}",
                        request.email()
                )
                .retrieve()
                .body(CustomerCredentialsResponse.class);

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                customer.passwordHash()
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

    private static String requireInternalApiKey(String internalApiKey) {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            throw new IllegalStateException(
                    "INTERNAL_API_KEY must be configured"
            );
        }

        return internalApiKey;
    }
}
