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
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

@Configuration
@Slf4j
public class SecurityConfig {

    // ✅ FIX #1: Load Supabase credentials from environment variables instead of hardcoding
    @Value("${supabase.jwks-uri}")
    private String jwksUri;

    @Value("${supabase.issuer}")
    private String issuer;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CorsConfigurationSource corsConfigSource = corsConfigurationSource();

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(request -> corsConfigSource.getCorsConfiguration(request)))

                .authorizeHttpRequests(auth -> auth
                        // ✅ Health + public endpoints (NO AUTH)
                        .requestMatchers("/api/ping", "/api/health").permitAll()
                        .requestMatchers("/api/public/**", "/api/sharing/**").permitAll()

                        // ✅ Everything else requires auth
                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder()))
                        .authenticationEntryPoint((request, response, authException) -> {
                            String remoteAddr = request.getRemoteAddr();
                            String uri = request.getRequestURI();

                            log.warn("🔴 AUTH FAILED | IP: {} | URI: {} | Reason: {}",
                                    remoteAddr, uri, authException.getMessage());

                            response.sendError(401, "Unauthorized");
                        })
                );

        return http.build();
    }
    // ✅ FIX #2: CORS configuration - restrict to known origins only
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
        log.info("🔐 JWT config | issuer: {} | jwks: {}", issuer, jwksUri);

        // ✅ FIX #1: Use injected credentials from environment variables
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri)
            .jwsAlgorithm(SignatureAlgorithm.ES256)
            .build();

        // ✅ Set issuer validator to match Supabase's iss claim
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
        decoder.setJwtValidator(issuerValidator);

        return new LoggingJwtDecoder(decoder);
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
                log.warn("🔴 JWT decode failed: {}", ex.getMessage());
                throw ex;
            }
        }
    }
}