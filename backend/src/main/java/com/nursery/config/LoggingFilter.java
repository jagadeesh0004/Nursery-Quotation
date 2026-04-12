package com.nursery.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * ✅ FIX #4: Request/Response Logging Filter
 *
 * Logs all incoming requests and outgoing responses for:
 * - Debugging
 * - Performance monitoring
 * - Security audit trail
 */
@Component
@Slf4j
public class LoggingFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long startTime = Instant.now().toEpochMilli();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String remoteAddr = request.getRemoteAddr();
        String authHeader = request.getHeader("Authorization");
        String authPreview = authHeader != null && authHeader.startsWith("Bearer ")
            ? authHeader.substring(0, Math.min(authHeader.length(), 24)) + "..."
            : (authHeader == null ? "<none>" : "<non-bearer>");
        String authMeta = describeJwtHeader(authHeader);

        log.info(">>> [{}] {} | URI: {} | IP: {} | Auth: {} | Jwt: {}",
            method,
            System.currentTimeMillis(),
            uri + (queryString != null ? "?" + queryString : ""),
            remoteAddr,
            authPreview,
            authMeta);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = Instant.now().toEpochMilli() - startTime;
            int status = response.getStatus();
            String statusLabel = status >= 400 ? "❌" : "✅";

            log.info("<<< [{}] {} | Status: {} {} | Duration: {}ms",
                method,
                System.currentTimeMillis(),
                status,
                statusLabel,
                duration);
        }
    }

    private String describeJwtHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "<none>";
        }

        try {
            String token = authHeader.substring(7);
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return "<invalid-jwt>";
            }

            byte[] decoded = Base64.getUrlDecoder().decode(parts[0]);
            String headerJson = new String(decoded, StandardCharsets.UTF_8);
            Map<String, Object> header = OBJECT_MAPPER.readValue(headerJson, new TypeReference<Map<String, Object>>() {});
            Object alg = header.get("alg");
            Object kid = header.get("kid");
            Object typ = header.get("typ");
            return String.format("alg=%s kid=%s typ=%s", alg, kid, typ);
        } catch (Exception ex) {
            return "<jwt-header-unreadable>";
        }
    }
}
