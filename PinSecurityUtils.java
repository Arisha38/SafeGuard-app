package com.safeguard.womensafety;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class PinSecurityUtils {
    private static final String SALT = "SafeGuardPinSalt_v1";

    public static String hashPin(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((pin + SALT).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
