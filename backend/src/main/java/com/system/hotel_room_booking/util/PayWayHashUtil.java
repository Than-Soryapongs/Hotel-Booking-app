package com.system.hotel_room_booking.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Utility class for generating HMAC SHA512 hash for ABA PayWay API
 * Based on PayWay documentation requirements
 */
@Slf4j
public class PayWayHashUtil {

    private static final String HMAC_SHA512 = "HmacSHA512";

    /**
     * Generate hash for PayWay Purchase API request
     * Concatenates values in specific order and generates Base64 encoded HMAC SHA512 hash
     * 
     * Order as per PayWay specification:
     * req_time, merchant_id, tran_id, amount, firstname, lastname, email, phone, 
     * type, payment_option, return_url, cancel_url, continue_success_url, 
     * return_params, lifetime, skip_success_page
     * 
     * @param reqTime Request time in YYYYMMDDHHmmss format
     * @param merchantId Merchant ID
     * @param tranId Transaction ID
     * @param amount Amount
     * @param firstname First name (can be empty string)
     * @param lastname Last name (can be empty string)
     * @param email Email (can be empty string)
     * @param phone Phone (can be empty string)
     * @param type Type (can be empty string)
     * @param paymentOption Payment option (can be empty string)
     * @param returnUrl Return URL (can be empty string)
     * @param cancelUrl Cancel URL (can be empty string)
     * @param continueSuccessUrl Continue success URL (can be empty string)
     * @param returnParams Return params (can be empty string)
     * @param lifetime Lifetime (can be empty string)
     * @param skipSuccessPage Skip success page (can be empty string)
     * @param publicKey Public key for HMAC
     * @return Base64 encoded hash
     */
    public static String generatePurchaseHash(
            String reqTime,
            String merchantId,
            String tranId,
            String amount,
            String firstname,
            String lastname,
            String email,
            String phone,
            String type,
            String paymentOption,
            String returnUrl,
            String cancelUrl,
            String continueSuccessUrl,
            String returnParams,
            String lifetime,
            String skipSuccessPage,
            String publicKey) {
        
        // Concatenate values in exact order specified by PayWay
        StringBuilder dataToHash = new StringBuilder();
        dataToHash.append(nvl(reqTime))
                  .append(nvl(merchantId))
                  .append(nvl(tranId))
                  .append(nvl(amount))
                  .append(nvl(firstname))
                  .append(nvl(lastname))
                  .append(nvl(email))
                  .append(nvl(phone))
                  .append(nvl(type))
                  .append(nvl(paymentOption))
                  .append(nvl(returnUrl))
                  .append(nvl(cancelUrl))
                  .append(nvl(continueSuccessUrl))
                  .append(nvl(returnParams))
                  .append(nvl(lifetime))
                  .append(nvl(skipSuccessPage));

        log.debug("Data to hash: {}", dataToHash);
        
        return generateHash(dataToHash.toString(), publicKey);
    }

    /**
     * Generate hash for PayWay callback verification
     * 
     * @param tranId Transaction ID
     * @param reqTime Request time
     * @param status Status
     * @param hash Hash from callback
     * @param publicKey Public key
     * @return Base64 encoded hash
     */
    public static String generateCallbackHash(
            String tranId,
            String reqTime,
            String status,
            String hash,
            String publicKey) {
        
        StringBuilder dataToHash = new StringBuilder();
        dataToHash.append(nvl(tranId))
                  .append(nvl(reqTime))
                  .append(nvl(status))
                  .append(nvl(hash));

        return generateHash(dataToHash.toString(), publicKey);
    }

    /**
     * Verify callback hash from PayWay
     * 
     * @param tranId Transaction ID
     * @param reqTime Request time
     * @param status Status
     * @param receivedHash Hash received from PayWay
     * @param publicKey Public key
     * @return true if hash is valid
     */
    public static boolean verifyCallbackHash(
            String tranId,
            String reqTime,
            String status,
            String receivedHash,
            String publicKey) {
        
        StringBuilder dataToHash = new StringBuilder();
        dataToHash.append(nvl(tranId))
                  .append(nvl(reqTime))
                  .append(nvl(status));

        String calculatedHash = generateHash(dataToHash.toString(), publicKey);
        boolean isValid = calculatedHash.equals(receivedHash);
        
        if (!isValid) {
            log.warn("Hash verification failed. Calculated: {}, Received: {}", calculatedHash, receivedHash);
        }
        
        return isValid;
    }

    /**
     * Generate HMAC SHA512 hash and encode as Base64
     * 
     * @param data Data to hash
     * @param key Secret key
     * @return Base64 encoded hash
     */
    private static String generateHash(String data, String key) {
        try {
            Mac sha512Hmac = Mac.getInstance(HMAC_SHA512);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA512);
            sha512Hmac.init(secretKey);
            
            byte[] hashBytes = sha512Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating hash", e);
            throw new RuntimeException("Failed to generate hash", e);
        }
    }

    /**
     * Null-safe string conversion
     * Returns empty string if value is null
     */
    private static String nvl(String value) {
        return value == null ? "" : value;
    }
}
