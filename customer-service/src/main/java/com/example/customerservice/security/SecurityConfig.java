package com.example.customerservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /*
     * JWT imzalamada kullanılan secret key doğrudan kod içine yazılmaz.
     * Değer Config Server üzerinden alınır.
     *
     * Auth Service token üretirken bu secret'ı kullanır.
     * Customer Service de token imzasını aynı secret ile doğrular.
     */
    @Value("${jwt.secret}")
    private String secretKey;

    /*
     * Customer Service güvenlik kurallarını tanımlar.
     *
     * - CSRF devre dışıdır.
     * - Session tutulmaz.
     * - Internal endpoint'ler token olmadan erişilebilir.
     * - Actuator health ve info endpoint'leri token olmadan erişilebilir.
     * - Diğer bütün endpoint'ler JWT gerektirir.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info"
                        ).permitAll()

                        .requestMatchers(
                                "/api/v1/customers/internal",
                                "/api/v1/customers/by-email/**"
                        ).permitAll()

                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                )

                .build();
    }

    /*
     * Gelen JWT token'ın HMAC SHA-256 imzasını doğrular.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        return NimbusJwtDecoder
                .withSecretKey(secretKeySpec)
                .build();
    }

    /*
     * Token içindeki "role" claim'ini Spring Security authority yapısına çevirir.
     *
     * Örnek:
     * role = USER
     * authority = ROLE_USER
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter authenticationConverter =
                new JwtAuthenticationConverter();

        authenticationConverter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        authenticationConverter.setPrincipalClaimName("sub");

        return authenticationConverter;
    }
}