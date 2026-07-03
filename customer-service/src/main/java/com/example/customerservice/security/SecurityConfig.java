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

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${jwt.secret}") // Burada JWT imzalamada kullanılan secret key, doğrudan kod içine yazılmıyor. Artık Config Server’dan geliyor.
    private String secretKey;  // Yani Auth Service token üretir, Customer Service bu token’ın gerçekten bizim sistemimiz tarafından üretilip üretilmediğini aynı secret ile kontrol eder.

    @Bean
    public SecurityFilterChain securityFilterChain( // Bu metot Customer Service’in güvenlik kurallarını belirliyor. Yani hangi endpoint açık, hangi endpoint token ister, JWT nasıl okunur gibi kararlar burada veriliyor.
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/customers/internal").permitAll()
                        .requestMatchers("/api/v1/customers/by-email/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->  //  Bu servis bir OAuth2 Resource Server olarak çalışacak.
                        oauth2.jwt(jwt ->                                      // JWT token bekleyecek Authorization: Bearer <token> header’ını Spring Security kendisi okuyacak.
                                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter) //  Token geçerliyse Authentication nesnesi oluşturacak.
                        )
                )
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secretKey.getBytes(),
                "HmacSHA256"
        );

        return NimbusJwtDecoder
                .withSecretKey(secretKeySpec)
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter authenticationConverter =
                new JwtAuthenticationConverter();

        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        authenticationConverter.setPrincipalClaimName("sub");

        return authenticationConverter;
    }
}