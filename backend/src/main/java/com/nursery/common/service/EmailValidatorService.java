package com.nursery.common.service;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service for validating email addresses to detect temporary/dummy emails
 * Uses a free disposable email domains list - NO API KEY REQUIRED
 */
@Slf4j
@Service
public class EmailValidatorService {

    // Common temporary/disposable email domains (FREE - no API needed)
    private static final Set<String> DISPOSABLE_DOMAINS = new HashSet<>();

    static {
        // Populate with common disposable email domains
        // This is a comprehensive but not exhaustive list
        DISPOSABLE_DOMAINS.addAll(Set.of(
            // 10 minute emails
            "10minutemail.com", "10minute.com", "10minmail.com",
            "10min.com", "guerrillamail.info", "guerrillamail.com",

            // Temp mail services
            "tempmail.com", "temp-mail.org", "mytrashmail.com",
            "throwaway.email", "trashmail.com", "mailinator.com",
            "maildrop.cc", "yopmail.com", "guerrillamail.net",

            // More disposable domains
            "temp.email", "tempmails.com", "sharklasers.com",
            "grr.la", "guerrillamail.org", "pokemail.net",
            "spam4.me", "trashmail.ws", "fakeinbox.com",
            "tempemails.com", "guerrillamail.de", "spam.la",
            "tempail.com", "throwawaymail.com", "10minutemail.org",
            "mailnesia.com", "mintemail.com", "temp.pm",
            "spoofmail.com", "guerrillamail.tv", "getairmail.com",
            "guerrillamail.biz", "tempemailer.com", "fakeemail.com",
            "temp-mail.io", "trash-mail.com", "disposable.email",
            "mailnesia.com", "tempor.email", "grr.la",

            // Additional common ones
            "mailinator.net", "spam.org", "trashmail.com",
            "mailinator.info", "maildrop.net", "meltmail.com",
            "minmail.com", "sharklasers.com", "spam123.com",
            "mail.tm", "temp.email.com", "mail.fake.com",
            "throwaway.me", "tempmail.net", "10minutemail.info",
            "trashmail.net", "guerrillamail.co", "spam.la",
            "temp.mailbox.com", "mailinator.org", "mailbox.org",
            "mintemail.org", "spam.email", "tempmail.info",
            "guerrillamail.xyz", "mail.tempmail.com", "spam4u.com"
        ));
    }

    /**
     * Validate if email is legitimate (not temporary/disposable)
     * @param email Email address to validate
     * @return true if valid permanent email, false if temporary/dummy
     */
    public boolean isValidEmail(String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                log.warn("Email validation: Empty email");
                return false;
            }

            email = email.trim().toLowerCase();

            // Check email format
            if (!isValidEmailFormat(email)) {
                log.warn("Email validation: Invalid email format: {}", email);
                return false;
            }

            // Extract domain
            String domain = extractDomain(email);
            if (domain == null) {
                log.warn("Email validation: Could not extract domain from: {}", email);
                return false;
            }

            // Check against disposable domains list
            if (DISPOSABLE_DOMAINS.contains(domain)) {
                log.warn("Email validation: Disposable email detected: {} (domain: {})", email, domain);
                return false;
            }

            log.info("Email validation: Valid email: {} (domain: {})", email, domain);
            return true;

        } catch (Exception e) {
            log.error("Email validation error: {}", e.getMessage());
            // On error, allow email (fail open)
            return true;
        }
    }

    /**
     * Check email format using regex
     * @param email Email to validate
     * @return true if format is valid
     */
    private boolean isValidEmailFormat(String email) {
        // Standard email regex pattern
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }

    /**
     * Extract domain from email address
     * @param email Email address
     * @return Domain name (e.g., "gmail.com") or null if invalid
     */
    private String extractDomain(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex == email.length() - 1) {
            return null;
        }
        return email.substring(atIndex + 1).toLowerCase();
    }

    /**
     * Get validation status message for error display
     * @param email Email address
     * @return User-friendly error message
     */
    public String getValidationMessage(String email) {
        if (!isValidEmail(email)) {
            String domain = extractDomain(email);
            if (DISPOSABLE_DOMAINS.contains(domain)) {
                return "Temporary email addresses are not allowed. Please use your personal or work email.";
            }
            return "Invalid email address format.";
        }
        return "Email is valid";
    }

    /**
     * Add custom domain to blocked list (for future use)
     * @param domain Domain to block
     */
    public void blockDomain(String domain) {
        if (domain != null && !domain.isEmpty()) {
            DISPOSABLE_DOMAINS.add(domain.toLowerCase());
            log.info("Blocked domain: {}", domain);
        }
    }

    /**
     * Check if a domain is in the disposable list
     * @param domain Domain to check
     * @return true if domain is disposable
     */
    public boolean isDisposableDomain(String domain) {
        return domain != null && DISPOSABLE_DOMAINS.contains(domain.toLowerCase());
    }
}
