package com.system.hotel_room_booking.controller;

import com.system.hotel_room_booking.model.dto.payment.AbaPayWayCallbackRequest;
import com.system.hotel_room_booking.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for ABA PayWay payment callbacks and payment status checks
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "ABA PayWay payment callback and status APIs")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/return")
    @Operation(summary = "PayWay return callback", 
               description = "Callback endpoint for PayWay after payment completion")
    public ResponseEntity<Map<String, String>> handleReturn(
            @RequestParam("tran_id") String tranId,
            @RequestParam(value = "req_time", required = false) String reqTime,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "hash", required = false) String hash,
            @RequestParam(value = "payment_option", required = false) String paymentOption,
            @RequestParam(value = "return_params", required = false) String returnParams,
            @RequestParam(value = "message", required = false) String message
    ) {
        log.info("Received PayWay return callback: tranId={}, status={}", tranId, status);
        
        AbaPayWayCallbackRequest callback = AbaPayWayCallbackRequest.builder()
            .tranId(tranId)
            .reqTime(reqTime)
            .status(status)
            .hash(hash)
            .paymentOption(paymentOption)
            .returnParams(returnParams)
            .message(message)
            .build();

        String result = paymentService.handlePaymentCallback(callback);
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", result,
            "transactionId", tranId
        ));
    }

    @GetMapping("/cancel")
    @Operation(summary = "PayWay cancel callback", 
               description = "Callback endpoint when user cancels payment")
    public ResponseEntity<Map<String, String>> handleCancel(
            @RequestParam(value = "tran_id", required = false) String tranId
    ) {
        log.info("Received PayWay cancel callback: tranId={}", tranId);
        
        if (tranId != null && !tranId.isEmpty()) {
            AbaPayWayCallbackRequest callback = AbaPayWayCallbackRequest.builder()
                .tranId(tranId)
                .status(3) // Cancelled status
                .build();
            
            String result = paymentService.handlePaymentCallback(callback);
            
            return ResponseEntity.ok(Map.of(
                "status", "cancelled",
                "message", result,
                "transactionId", tranId
            ));
        }
        
        return ResponseEntity.ok(Map.of(
            "status", "cancelled",
            "message", "Payment cancelled by user"
        ));
    }

    @GetMapping("/status/{transactionId}")
    @Operation(summary = "Get payment status", 
               description = "Check the status of a payment by transaction ID")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable String transactionId) {
        var payment = paymentService.getPaymentByTransactionId(transactionId);
        
        return ResponseEntity.ok(Map.of(
            "transactionId", payment.getTransactionId(),
            "status", payment.getStatus().toString(),
            "amount", payment.getAmount(),
            "currency", payment.getCurrency(),
            "paymentMethod", payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "N/A",
            "initiatedAt", payment.getInitiatedAt().toString(),
            "completedAt", payment.getCompletedAt() != null ? payment.getCompletedAt().toString() : null
        ));
    }
}
