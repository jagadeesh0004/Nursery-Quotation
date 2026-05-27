package com.nursery.common.utils;

import java.util.Random;

/**
 * Utility for generating and validating OTP codes
 */
public class OtpUtil {

    private static final Random random = new Random();

    /**
     * Generate random 6-digit OTP
     * @return 6-digit OTP as String (e.g., "123456")
     */
    public static String generateOtp() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Validate OTP format (must be exactly 6 digits)
     * @param otp OTP to validate
     * @return true if valid format, false otherwise
     */
    public static boolean isValidOtpFormat(String otp) {
        return otp != null && otp.matches("^[0-9]{6}$");
    }
}

