package com.nursery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

@Configuration
@Slf4j
public class SecurityConfig {

    // FIX #1: Load Supabase credentials from environment variables instead of hardcoding
    @Value("${supabase.jwks-uri}")
    private String jwksUri;

    @Value("${supabase.issuer}")
    private String issuer;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String springIssuerUri;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CorsConfigurationSource corsConfigSource = corsConfigurationSource();

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(request -> corsConfigSource.getCorsConfiguration(request)))

                .authorizeHttpRequests(auth -> auth
                        // Health + public endpoints (NO AUTH)
                        // Note: context-path is /api, so these become /api/ping, /api/health, etc.
                        .requestMatchers("/ping", "/health" ,"/error").permitAll()
                        .requestMatchers("/public/**", "/sharing/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        // Everything else requires auth
                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder()))
                        .authenticationEntryPoint((request, response, authException) -> {
                            String remoteAddr = request.getRemoteAddr();
                            String uri = request.getRequestURI();

                            log.warn("AUTH FAILED | IP: {} | URI: {} | Reason: {}",
                                    remoteAddr, uri, authException.getMessage());

                            response.sendError(401, "Unauthorized");
                        })
                );

        return http.build();
    }
    // FIX #2: CORS configuration - restrict to known origins only
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setExposedHeaders(Arrays.asList("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String resolvedIssuer = firstNonBlank(issuer, springIssuerUri);
        String resolvedJwksUri = firstNonBlank(jwksUri, deriveJwksUri(resolvedIssuer));

        log.info("JWT config | issuer: {} | jwks: {}", resolvedIssuer, resolvedJwksUri);

        if (isBlank(resolvedIssuer) || isBlank(resolvedJwksUri)) {
            throw new IllegalStateException("JWT configuration is incomplete. Set SUPABASE_ISSUER and SUPABASE_JWKS_URI, or SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI.");
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(resolvedJwksUri)
            .jwsAlgorithm(SignatureAlgorithm.ES256)
            .build();

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(resolvedIssuer);
        decoder.setJwtValidator(issuerValidator);

        return new LoggingJwtDecoder(decoder);
    }

    private static String deriveJwksUri(String issuerUri) {
        if (isBlank(issuerUri)) {
            return null;
        }
        String normalized = issuerUri.endsWith("/") ? issuerUri.substring(0, issuerUri.length() - 1) : issuerUri;
        return normalized + "/.well-known/jwks.json";
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class LoggingJwtDecoder implements JwtDecoder {
        private final JwtDecoder delegate;

        private LoggingJwtDecoder(JwtDecoder delegate) {
            this.delegate = delegate;
        }

        @Override
        public Jwt decode(String token) throws JwtException {
            try {
                return delegate.decode(token);
            } catch (JwtException ex) {
                log.warn("JWT decode failed: {}", ex.getMessage());
                throw ex;
            }
        }
    }
}
