package com.system.hotel_room_booking.util;

import lombok.experimental.UtilityClass;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * Utility class for PayWay payment operations
 */
@UtilityClass
public class PayWayUtil {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Generate unique transaction ID
     */
    public static String generateTransactionId() {
        return System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).replaceAll("-", "");
    }

    /**
     * Get current UTC time in PayWay format (YYYYMMDDHHmmss)
     */
    public static String getCurrentReqTime() {
        return ZonedDateTime.now().format(DATE_TIME_FORMATTER);
    }

    /**
     * Generate HMAC SHA512 hash
     * 
     * @param data The data to hash
     * @param key The secret key
     * @return Base64 encoded hash
     */
    public static String generateHash(String data, String key) {
        try {
            Mac sha512Hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            sha512Hmac.init(secretKey);
            byte[] hash = sha512Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate hash", e);
        }
    }

    /**
     * Validate PayWay callback hash
     * 
     * @param tranId Transaction ID
     * @param status Payment status
     * @param receivedHash Hash received from PayWay
     * @param publicKey Your public key
     * @return true if hash is valid
     */
    public static boolean validateCallbackHash(String tranId, String status, String receivedHash, String publicKey) {
        try {
            String dataToHash = tranId + status;
            String calculatedHash = generateHash(dataToHash, publicKey);
            return calculatedHash.equals(receivedHash);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get payment status text from status code
     * 
     * @param statusCode PayWay status code
     * @return Status text
     */
    public static String getPaymentStatusText(Integer statusCode) {
        if (statusCode == null) return "UNKNOWN";
        return switch (statusCode) {
            case 0 -> "SUCCESS";
            case 1 -> "PENDING";
            case 2 -> "FAILED";
            case 3 -> "CANCELLED";
            default -> "UNKNOWN";
        };
    }

    /**
     * Build hash string from values (concatenate all values)
     * 
     * @param values Values to concatenate
     * @return Concatenated string
     */
    public static String buildHashString(String... values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            sb.append(value != null ? value : "");
        }
        return sb.toString();
    }

    /**
     * Null-safe string converter
     * 
     * @param value Value to convert
     * @return Empty string if null, otherwise the value
     */
    public static String nullSafe(String value) {
        return value != null ? value : "";
    }
}
