package com.example.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.GET;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health", "/swagger-ui/**",
                                "/swagger-ui.html", "/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/api/outbox/**").hasRole("ADMIN")
                        .requestMatchers(PATCH, "/api/orders/*/status").hasRole("ADMIN")
                        .requestMatchers(GET, "/api/orders").hasRole("ADMIN")
                        .requestMatchers(POST, "/api/orders/**")
                        .hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers("/api/orders/**")
                        .hasAnyRole("CUSTOMER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                ));
        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var authorities = new ArrayList<>(scopesConverter.convert(jwt));
            Object realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof Map<?, ?> claims
                    && claims.get("roles") instanceof Collection<?> roles) {
                roles.forEach(role -> authorities.add(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_" + role
                        )
                ));
            }
            return authorities;
        });
        return converter;
    }
}
