package com.system.hotel_room_booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.hotel_room_booking.exception.ResourceNotFoundException;
import com.system.hotel_room_booking.model.dto.payment.*;
import com.system.hotel_room_booking.model.dto.response.CheckoutResponse;
import com.system.hotel_room_booking.model.entity.*;
import com.system.hotel_room_booking.repository.*;
import com.system.hotel_room_booking.util.PayWayHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Service for handling ABA PayWay payment integration
 * Implements Purchase API with proper hash generation and callback handling
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CartRepository cartRepository;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final ObjectMapper objectMapper;

    @Value("${payway.base-url}")
    private String paywayBaseUrl;
    
    @Value("${payway.purchase-path}")
    private String paywayPurchasePath;
    
    @Value("${payway.merchant-id}")
    private String merchantId;
    
    @Value("${payway.public-key}")
    private String publicKey;
    
    @Value("${payway.return-url}")
    private String returnUrl;
    
    @Value("${payway.cancel-url}")
    private String cancelUrl;
    
    @Value("${payway.continue-success-url}")
    private String continueSuccessUrl;

    /**
     * Initiate payment with ABA PayWay
     * Creates payment record and returns form data for frontend to submit to PayWay
     */
    public CheckoutResponse initiatePayment(Cart cart) {
        try {
            // Generate unique transaction ID
            String transactionId = generateTransactionId();
            String reqTime = generateReqTime();

            User user = cart.getUser();
            
            // Generate hash according to PayWay specification
            // Order: req_time, merchant_id, tran_id, amount, firstname, lastname, email, phone,
            // type, payment_option, return_url, cancel_url, continue_success_url,
            // return_params, lifetime, skip_success_page
            String hash = PayWayHashUtil.generatePurchaseHash(
                reqTime,
                merchantId,
                transactionId,
                cart.getTotalPrice().toString(),
                user.getFirstName() != null ? user.getFirstName() : "",
                user.getLastName() != null ? user.getLastName() : "",
                user.getEmail(),
                "", // phone
                "", // type
                "", // payment_option
                returnUrl,
                cancelUrl,
                continueSuccessUrl,
                "", // return_params
                "", // lifetime
                "1", // skip_success_page
                publicKey
            );

            // PayWay Purchase API endpoint
            String paymentUrl = paywayBaseUrl + paywayPurchasePath;

            // Create payment record
            Payment payment = Payment.builder()
                .transactionId(transactionId)
                .user(user)
                .cart(cart)
                .amount(cart.getTotalPrice())
                .currency("USD")
                .status(PaymentStatus.PENDING)
                .reqTime(reqTime)
                .paywayHash(hash)
                .paymentUrl(paymentUrl)
                .customerName(user.getFirstName() + " " + user.getLastName())
                .customerEmail(user.getEmail())
                .initiatedAt(LocalDateTime.now())
                .build();

            paymentRepository.save(payment);

            // Update cart status
            cart.setStatus(CartStatus.CHECKOUT_PENDING);
            cart.setTransactionId(transactionId);
            cart.setCheckoutInitiatedAt(LocalDateTime.now());
            cartRepository.save(cart);

            log.info("Payment initiated successfully. TransactionId: {}, User: {}", transactionId, user.getEmail());

            // Return checkout response with form data for frontend submission
            return CheckoutResponse.builder()
                .transactionId(transactionId)
                .paymentUrl(paymentUrl)
                .totalAmount(cart.getTotalPrice())
                .currency("USD")
                .merchantId(merchantId)
                .reqTime(reqTime)
                .hash(hash)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .continueSuccessUrl(continueSuccessUrl)
                .message("Payment initiated successfully. Please submit the form to proceed.")
                .success(true)
                .build();

        } catch (Exception e) {
            log.error("Error initiating payment for cart: {}", cart.getId(), e);
            throw new RuntimeException("Failed to initiate payment: " + e.getMessage(), e);
        }
    }

    /**
     * Handle PayWay callback after payment
     * Verifies hash and updates payment status
     */
    public String handlePaymentCallback(AbaPayWayCallbackRequest callback) {
        try {
            log.info("Received PayWay callback: {}", callback);

            // Find payment by transaction ID
            Payment payment = paymentRepository.findByTransactionId(callback.getTranId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionId", callback.getTranId()));

            // Verify callback hash
            boolean isValidHash = PayWayHashUtil.verifyCallbackHash(
                callback.getTranId(),
                callback.getReqTime(),
                callback.getStatus() != null ? callback.getStatus().toString() : "",
                callback.getHash(),
                publicKey
            );

            if (!isValidHash) {
                log.error("Invalid callback hash for transaction: {}", callback.getTranId());
                payment.setStatus(PaymentStatus.FAILED);
                payment.setErrorMessage("Invalid callback signature");
                payment.setFailedAt(LocalDateTime.now());
                paymentRepository.save(payment);
                return "Invalid signature";
            }

            // Store callback data
            payment.setCallbackReceivedAt(LocalDateTime.now());
            payment.setCallbackHash(callback.getHash());
            payment.setCallbackData(objectMapper.writeValueAsString(callback));
            
            if (callback.getPaymentOption() != null) {
                payment.setPaymentMethod(callback.getPaymentOption());
            }

            // Update payment status based on callback status
            Integer status = callback.getStatus();
            if (status != null) {
                switch (status) {
                    case 0: // Success
                        payment.setStatus(PaymentStatus.COMPLETED);
                        payment.setCompletedAt(LocalDateTime.now());
                        handleSuccessfulPayment(payment);
                        log.info("Payment completed successfully: {}", payment.getTransactionId());
                        return "Payment successful";
                        
                    case 1: // Pending
                        payment.setStatus(PaymentStatus.PROCESSING);
                        log.info("Payment processing: {}", payment.getTransactionId());
                        return "Payment processing";
                        
                    case 2: // Failed
                        payment.setStatus(PaymentStatus.FAILED);
                        payment.setFailedAt(LocalDateTime.now());
                        payment.setErrorMessage(callback.getMessage());
                        handleFailedPayment(payment);
                        log.warn("Payment failed: {}", payment.getTransactionId());
                        return "Payment failed";
                        
                    case 3: // Cancelled
                        payment.setStatus(PaymentStatus.CANCELLED);
                        payment.setFailedAt(LocalDateTime.now());
                        handleCancelledPayment(payment);
                        log.info("Payment cancelled: {}", payment.getTransactionId());
                        return "Payment cancelled";
                        
                    default:
                        payment.setStatus(PaymentStatus.FAILED);
                        payment.setErrorMessage("Unknown status: " + status);
                        payment.setFailedAt(LocalDateTime.now());
                        log.warn("Unknown payment status: {} for transaction: {}", status, payment.getTransactionId());
                        return "Unknown status";
                }
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setErrorMessage("No status provided in callback");
                payment.setFailedAt(LocalDateTime.now());
                paymentRepository.save(payment);
                return "Invalid callback: no status";
            }

        } catch (Exception e) {
            log.error("Error handling payment callback", e);
            return "Error processing callback: " + e.getMessage();
        }
    }

    /**
     * Handle successful payment
     * Create bookings from cart items and update cart status
     */
    private void handleSuccessfulPayment(Payment payment) {
        Cart cart = payment.getCart();
        if (cart == null) {
            log.error("No cart associated with payment: {}", payment.getTransactionId());
            return;
        }

        try {
            // Update cart status
            cart.setStatus(CartStatus.COMPLETED);
            cart.setCheckoutCompletedAt(LocalDateTime.now());
            cartRepository.save(cart);

            // Create bookings for each cart item
            for (CartItem item : cart.getItems()) {
                createBookingFromCartItem(item, payment);
            }

            log.info("Successfully created bookings for cart: {}", cart.getId());

        } catch (Exception e) {
            log.error("Error handling successful payment for cart: {}", cart.getId(), e);
            throw new RuntimeException("Failed to process successful payment", e);
        }
    }

    /**
     * Create booking from cart item
     */
    private void createBookingFromCartItem(CartItem item, Payment payment) {
        String confirmationNumber = generateConfirmationNumber();
        
        Booking booking = Booking.builder()
            .confirmationNumber(confirmationNumber)
            .user(payment.getUser())
            .room(item.getRoom())
            .status(BookingStatus.CONFIRMED)
            .checkInDate(item.getCheckInDate())
            .checkOutDate(item.getCheckOutDate())
            .numberOfGuests(item.getNumberOfGuests())
            .totalPrice(item.getPrice())
            .discountAmount(BigDecimal.ZERO)
            .finalPrice(item.getPrice())
            .transactionId(payment.getTransactionId())
            .paidAt(payment.getCompletedAt())
            .build();

        bookingRepository.save(booking);

        // Update payment with booking reference
        payment.setBooking(booking);
        paymentRepository.save(payment);

        // Update room status
        Room room = item.getRoom();
        room.setStatus(RoomStatus.RESERVED);
        roomRepository.save(room);

        log.info("Created booking: {} for room: {}", confirmationNumber, room.getRoomNumber());
    }

    /**
     * Handle failed payment
     */
    private void handleFailedPayment(Payment payment) {
        Cart cart = payment.getCart();
        if (cart != null) {
            cart.setStatus(CartStatus.ACTIVE); // Reset cart to active
            cartRepository.save(cart);
        }
        paymentRepository.save(payment);
    }

    /**
     * Handle cancelled payment
     */
    private void handleCancelledPayment(Payment payment) {
        Cart cart = payment.getCart();
        if (cart != null) {
            cart.setStatus(CartStatus.CANCELLED);
            cartRepository.save(cart);
        }
        paymentRepository.save(payment);
    }

    /**
     * Generate unique transaction ID
     * Format: TXN-{timestamp}-{random}
     */
    private String generateTransactionId() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().substring(0, 8);
        return "TXN-" + timestamp + "-" + random;
    }

    /**
     * Generate request time in PayWay format (YYYYMMDDHHmmss)
     */
    private String generateReqTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return LocalDateTime.now().format(formatter);
    }

    /**
     * Generate booking confirmation number
     */
    private String generateConfirmationNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "BK-" + timestamp.substring(timestamp.length() - 8) + "-" + random;
    }

    /**
     * Get payment by transaction ID
     */
    public Payment getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionId", transactionId));
    }

    // Legacy methods for backward compatibility
    public String handleReturn(String tranId, Integer status, String hash) {
        AbaPayWayCallbackRequest callback = AbaPayWayCallbackRequest.builder()
            .tranId(tranId)
            .status(status)
            .hash(hash)
            .reqTime(generateReqTime())
            .build();
        return handlePaymentCallback(callback);
    }
}
