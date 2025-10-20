package com.system.hotel_room_booking.service;

import com.system.hotel_room_booking.exception.InvalidDiscountException;
import com.system.hotel_room_booking.exception.ResourceNotFoundException;
import com.system.hotel_room_booking.model.dto.request.CreateDiscountRequest;
import com.system.hotel_room_booking.model.dto.response.DiscountResponse;
import com.system.hotel_room_booking.model.entity.Discount;
import com.system.hotel_room_booking.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DiscountService {

    private final DiscountRepository discountRepository;

    /**
     * Create a new discount
     */
    public DiscountResponse createDiscount(CreateDiscountRequest request) {
        log.info("Creating discount with code: {}", request.getCode());

        // Check if code already exists
        if (discountRepository.findByCode(request.getCode()).isPresent()) {
            throw new IllegalArgumentException("Discount code already exists: " + request.getCode());
        }

        // Validate discount type
        validateDiscountValues(request);

        // Validate dates
        if (request.getValidFrom().isAfter(request.getValidUntil())) {
            throw new IllegalArgumentException("Valid from date must be before valid until date");
        }

        // Create discount
        Discount discount = Discount.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .percentageValue(request.getPercentageValue())
                .fixedAmount(request.getFixedAmount())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .maxUsageCount(request.getMaxUsageCount())
                .currentUsageCount(0)
                .maxUsagePerUser(request.getMaxUsagePerUser())
                .minOrderAmount(request.getMinOrderAmount())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .isActive(request.getIsActive())
                .termsAndConditions(request.getTermsAndConditions())
                .build();

        Discount savedDiscount = discountRepository.save(discount);
        log.info("Discount created successfully: {}", savedDiscount.getCode());

        return mapToDiscountResponse(savedDiscount);
    }

    /**
     * Update a discount
     */
    public DiscountResponse updateDiscount(Long discountId, CreateDiscountRequest request) {
        log.info("Updating discount: {}", discountId);

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount", "id", discountId));

        // Update fields
        if (request.getName() != null) discount.setName(request.getName());
        if (request.getDescription() != null) discount.setDescription(request.getDescription());
        if (request.getType() != null) discount.setType(request.getType());
        if (request.getPercentageValue() != null) discount.setPercentageValue(request.getPercentageValue());
        if (request.getFixedAmount() != null) discount.setFixedAmount(request.getFixedAmount());
        if (request.getValidFrom() != null) discount.setValidFrom(request.getValidFrom());
        if (request.getValidUntil() != null) discount.setValidUntil(request.getValidUntil());
        if (request.getMaxUsageCount() != null) discount.setMaxUsageCount(request.getMaxUsageCount());
        if (request.getMaxUsagePerUser() != null) discount.setMaxUsagePerUser(request.getMaxUsagePerUser());
        if (request.getMinOrderAmount() != null) discount.setMinOrderAmount(request.getMinOrderAmount());
        if (request.getMaxDiscountAmount() != null) discount.setMaxDiscountAmount(request.getMaxDiscountAmount());
        if (request.getIsActive() != null) discount.setIsActive(request.getIsActive());
        if (request.getTermsAndConditions() != null) discount.setTermsAndConditions(request.getTermsAndConditions());

        // Validate updated values
        if (request.getPercentageValue() != null || request.getFixedAmount() != null) {
            validateDiscountTypeValues(discount);
        }

        Discount updatedDiscount = discountRepository.save(discount);
        log.info("Discount updated successfully: {}", discountId);

        return mapToDiscountResponse(updatedDiscount);
    }

    /**
     * Get discount by ID
     */
    @Transactional(readOnly = true)
    public DiscountResponse getDiscountById(Long discountId) {
        log.info("Fetching discount: {}", discountId);

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount", "id", discountId));

        return mapToDiscountResponse(discount);
    }

    /**
     * Get discount by code
     */
    @Transactional(readOnly = true)
    public DiscountResponse getDiscountByCode(String code) {
        log.info("Fetching discount by code: {}", code);

        Discount discount = discountRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Discount", "code", code));

        return mapToDiscountResponse(discount);
    }

    /**
     * Get all discounts
     */
    @Transactional(readOnly = true)
    public Page<DiscountResponse> getAllDiscounts(Pageable pageable) {
        log.info("Fetching all discounts");

        return discountRepository.findAll(pageable)
                .map(this::mapToDiscountResponse);
    }

    /**
     * Get active discounts
     */
    @Transactional(readOnly = true)
    public List<DiscountResponse> getActiveDiscounts() {
        log.info("Fetching active discounts");

        List<Discount> activeDiscounts = discountRepository.findActiveDiscounts(LocalDateTime.now());

        return activeDiscounts.stream()
                .map(this::mapToDiscountResponse)
                .collect(Collectors.toList());
    }

    /**
     * Validate discount code
     */
    @Transactional(readOnly = true)
    public DiscountResponse validateDiscountCode(String code, BigDecimal orderAmount) {
        log.info("Validating discount code: {} for order amount: {}", code, orderAmount);

        Discount discount = discountRepository.findValidDiscountByCode(code, LocalDateTime.now())
                .orElseThrow(() -> new InvalidDiscountException("Invalid or expired discount code"));

        // Check minimum order amount
        if (discount.getMinOrderAmount() != null && orderAmount.compareTo(discount.getMinOrderAmount()) < 0) {
            throw new InvalidDiscountException(
                    String.format("Minimum order amount of %s required for this discount", discount.getMinOrderAmount())
            );
        }

        log.info("Discount code validated successfully: {}", code);

        return mapToDiscountResponse(discount);
    }

    /**
     * Deactivate discount
     */
    public DiscountResponse deactivateDiscount(Long discountId) {
        log.info("Deactivating discount: {}", discountId);

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount", "id", discountId));

        discount.setIsActive(false);
        Discount deactivatedDiscount = discountRepository.save(discount);

        log.info("Discount deactivated successfully: {}", discountId);

        return mapToDiscountResponse(deactivatedDiscount);
    }

    /**
     * Delete discount
     */
    public void deleteDiscount(Long discountId) {
        log.info("Deleting discount: {}", discountId);

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount", "id", discountId));

        // Check if discount is currently active
        if (discount.getIsActive() && 
            LocalDateTime.now().isBefore(discount.getValidUntil())) {
            throw new IllegalArgumentException("Cannot delete an active discount. Deactivate it first.");
        }

        discountRepository.delete(discount);
        log.info("Discount deleted successfully: {}", discountId);
    }

    /**
     * Calculate the discount amount for a given discount code and order amount
     * @param code the discount code
     * @param orderAmount the order subtotal amount
     * @return the calculated discount amount
     * @throws InvalidDiscountException if the discount code is invalid
     */
    public BigDecimal calculateDiscount(String code, BigDecimal orderAmount) {
        // First validate the discount code (throws exception if invalid)
        validateDiscountCode(code, orderAmount);
        
        // Get the actual discount entity
        Discount discount = discountRepository.findByCode(code)
                .orElseThrow(() -> new InvalidDiscountException("Discount code not found: " + code));
        
        // Double check it's active (validateDiscountCode already checked, but be safe)
        if (!discount.getIsActive()) {
            throw new InvalidDiscountException("Discount code is not active: " + code);
        }
        
        BigDecimal discountAmount = BigDecimal.ZERO;
        
        // Calculate based on discount type
        switch (discount.getType()) {
            case PERCENTAGE:
            case EARLY_BIRD:
            case LAST_MINUTE:
            case SEASONAL:
            case LOYALTY:
                // Calculate percentage discount: orderAmount * (percentageValue / 100)
                discountAmount = orderAmount
                        .multiply(discount.getPercentageValue())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                break;
                
            case FIXED_AMOUNT:
            case PROMOTIONAL_CODE:
                // Use fixed amount discount
                discountAmount = discount.getFixedAmount();
                break;
                
            default:
                throw new InvalidDiscountException("Unknown discount type: " + discount.getType());
        }
        
        // Apply max discount cap if set
        if (discount.getMaxDiscountAmount() != null && 
            discountAmount.compareTo(discount.getMaxDiscountAmount()) > 0) {
            discountAmount = discount.getMaxDiscountAmount();
        }
        
        // Ensure discount doesn't exceed order amount
        if (discountAmount.compareTo(orderAmount) > 0) {
            discountAmount = orderAmount;
        }
        
        log.info("Calculated discount: {} for code: {} on amount: {}", discountAmount, code, orderAmount);
        return discountAmount;
    }

    // Helper methods

    private void validateDiscountValues(CreateDiscountRequest request) {
        switch (request.getType()) {
            case PERCENTAGE:
            case EARLY_BIRD:
            case LAST_MINUTE:
            case SEASONAL:
            case LOYALTY:
                if (request.getPercentageValue() == null || request.getPercentageValue().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Percentage value is required for this discount type");
                }
                break;
            case FIXED_AMOUNT:
            case PROMOTIONAL_CODE:
                if (request.getFixedAmount() == null || request.getFixedAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Fixed amount is required for this discount type");
                }
                break;
        }
    }

    private void validateDiscountTypeValues(Discount discount) {
        switch (discount.getType()) {
            case PERCENTAGE:
            case EARLY_BIRD:
            case LAST_MINUTE:
            case SEASONAL:
            case LOYALTY:
                if (discount.getPercentageValue() == null || discount.getPercentageValue().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Percentage value is required for this discount type");
                }
                break;
            case FIXED_AMOUNT:
            case PROMOTIONAL_CODE:
                if (discount.getFixedAmount() == null || discount.getFixedAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Fixed amount is required for this discount type");
                }
                break;
        }
    }

    private DiscountResponse mapToDiscountResponse(Discount discount) {
        return DiscountResponse.builder()
                .id(discount.getId())
                .code(discount.getCode())
                .name(discount.getName())
                .description(discount.getDescription())
                .type(discount.getType())
                .percentageValue(discount.getPercentageValue())
                .fixedAmount(discount.getFixedAmount())
                .validFrom(discount.getValidFrom())
                .validUntil(discount.getValidUntil())
                .maxUsageCount(discount.getMaxUsageCount())
                .currentUsageCount(discount.getCurrentUsageCount())
                .maxUsagePerUser(discount.getMaxUsagePerUser())
                .minOrderAmount(discount.getMinOrderAmount())
                .maxDiscountAmount(discount.getMaxDiscountAmount())
                .isActive(discount.getIsActive())
                .termsAndConditions(discount.getTermsAndConditions())
                .createdAt(discount.getCreatedAt())
                .updatedAt(discount.getUpdatedAt())
                .build();
    }
}
