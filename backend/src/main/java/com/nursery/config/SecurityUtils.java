package com.nursery.config;

import com.nursery.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility class for extracting user information from Supabase JWT tokens.
 *
 * IMPORTANT: This now works with Spring OAuth2 Resource Server which provides
 * Jwt objects, NOT UserDetails objects. The principal is a Jwt, not UserDetails.
 *
 * This fixes the ClassCastException that occurred with the old implementation.
 */
@Slf4j
public class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Get the current authenticated user's ID from the JWT token.
     *
     * Supabase JWT contains "sub" claim with the user's UUID.
     * Throws BusinessException if user is not authenticated.
     *
     * @return User's UUID
     * @throws BusinessException if user is not authenticated
     */
    public static UUID getCurrentUserId() {
        return getCurrentJwt()
                .map(jwt -> {
                    String subject = jwt.getSubject();
                    try {
                        return UUID.fromString(subject);
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid UUID in token subject: {}", subject);
                        throw new BusinessException("Invalid user ID in token");
                    }
                })
                .orElseThrow(() -> new BusinessException("User not authenticated"));
    }

    /**
     * Get the current authenticated user's ID (Optional version).
     *
     * @return Optional containing user's UUID if authenticated
     */
    public static Optional<UUID> getCurrentUserIdOptional() {
        return getCurrentJwt()
                .map(jwt -> {
                    String subject = jwt.getSubject();
                    try {
                        return UUID.fromString(subject);
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid UUID in token subject: {}", subject);
                        return null;
                    }
                });
    }

    /**
     * Get the current authenticated user's email from the JWT token.
     *
     * Supabase JWT contains "email" claim.
     *
     * @return User's email or empty if not present
     */
    public static Optional<String> getCurrentUserEmail() {
        return getCurrentJwt()
                .map(jwt -> jwt.getClaimAsString("email"));
    }

    /**
     * Get the current JWT token from the SecurityContext.
     *
     * This is the key difference from the old implementation:
     * - OLD: auth.getPrincipal() was UserDetails
     * - NEW: auth.getPrincipal() is Jwt
     *
     * @return Optional containing the current Jwt
     */
    public static Optional<Jwt> getCurrentJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof Jwt)) {
            log.debug("Principal is not a Jwt: {}. Type: {}", principal, principal.getClass().getName());
            return Optional.empty();
        }

        return Optional.of((Jwt) principal);
    }

    /**
     * Verify that a user is authenticated.
     *
     * @return true if current user is authenticated
     */
    public static boolean isAuthenticated() {
        return getCurrentJwt().isPresent();
    }

    /**
     * Get custom claim from the current JWT token.
     *
     * @param claimName the name of the claim
     * @return Optional containing the claim value as String
     */
    public static Optional<String> getClaimAsString(String claimName) {
        return getCurrentJwt()
                .map(jwt -> jwt.getClaimAsString(claimName));
    }

    /**
     * Get all claims from the current JWT token.
     * Useful for debugging or accessing custom claims.
     *
     * @return Jwt object containing all claims
     */
    public static Optional<Jwt> getFullToken() {
        return getCurrentJwt();
    }
}
