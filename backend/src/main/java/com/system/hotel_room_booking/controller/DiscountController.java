package com.system.hotel_room_booking.controller;

import com.system.hotel_room_booking.model.dto.request.CreateDiscountRequest;
import com.system.hotel_room_booking.model.dto.response.DiscountResponse;
import com.system.hotel_room_booking.service.DiscountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/discounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Discount Management", description = "APIs for managing discount codes and promotions")
public class DiscountController {

    private final DiscountService discountService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Create a discount code",
        description = "Create a new discount code with specified rules and limitations. Admin access required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Discount created successfully",
            content = @Content(schema = @Schema(implementation = DiscountResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid discount data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    public ResponseEntity<DiscountResponse> createDiscount(
            @Valid @RequestBody CreateDiscountRequest request) {
        log.info("REST request to create discount: {}", request.getCode());
        DiscountResponse response = discountService.createDiscount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Update a discount code",
        description = "Update an existing discount code. Admin access required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Discount updated successfully",
            content = @Content(schema = @Schema(implementation = DiscountResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid discount data"),
        @ApiResponse(responseCode = "404", description = "Discount not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    public ResponseEntity<DiscountResponse> updateDiscount(
            @Parameter(description = "Discount ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody CreateDiscountRequest request) {
        log.info("REST request to update discount: {}", id);
        DiscountResponse response = discountService.updateDiscount(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Get discount by ID",
        description = "Retrieve detailed information about a specific discount. Admin access required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Discount found",
            content = @Content(schema = @Schema(implementation = DiscountResponse.class))),
        @ApiResponse(responseCode = "404", description = "Discount not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    public ResponseEntity<DiscountResponse> getDiscountById(
            @Parameter(description = "Discount ID", required = true)
            @PathVariable Long id) {
        log.info("REST request to get discount: {}", id);
        DiscountResponse response = discountService.getDiscountById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/code/{code}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Get discount by code",
        description = "Retrieve discount information by code"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Discount found",
            content = @Content(schema = @Schema(implementation = DiscountResponse.class))),
        @ApiResponse(responseCode = "404", description = "Discount not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<DiscountResponse> getDiscountByCode(
            @Parameter(description = "Discount code", required = true)
            @PathVariable String code) {
        log.info("REST request to get discount by code: {}", code);
        DiscountResponse response = discountService.getDiscountByCode(code);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Get all discounts",
        description = "Retrieve a paginated list of all discount codes. Admin access required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved discounts",
            content = @Content(schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    public ResponseEntity<Page<DiscountResponse>> getAllDiscounts(
            @PageableDefault(size = 20, sort = "code") Pageable pageable) {
        log.info("REST request to get all discounts");
        Page<DiscountResponse> response = discountService.getAllDiscounts(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    @Operation(
        summary = "Get active discounts",
        description = "Retrieve all currently valid and active discount codes"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved active discounts")
    })
    public ResponseEntity<List<DiscountResponse>> getActiveDiscounts() {
        log.info("REST request to get active discounts");
        List<DiscountResponse> response = discountService.getActiveDiscounts();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate/{code}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Validate discount code",
        description = "Validate a discount code for a specific order amount"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Discount code is valid",
            content = @Content(schema = @Schema(implementation = DiscountResponse.class))),
        @ApiResponse(responseCode = "400", description = "Discount code is invalid or expired"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<DiscountResponse> validateDiscountCode(
            @Parameter(description = "Discount code", required = true)
            @PathVariable String code,
            @Parameter(description = "Order amount to validate against", required = true)
            @RequestParam BigDecimal orderAmount) {
        log.info("REST request to validate discount code: {} for amount: {}", code, orderAmount);
        DiscountResponse response = discountService.validateDiscountCode(code, orderAmount);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Deactivate a discount",
        description = "Deactivate a discount code (soft delete). Admin access required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Discount deactivated successfully",
            content = @Content(schema = @Schema(implementation = DiscountResponse.class))),
        @ApiResponse(responseCode = "404", description = "Discount not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    public ResponseEntity<DiscountResponse> deactivateDiscount(
            @Parameter(description = "Discount ID", required = true)
            @PathVariable Long id) {
        log.info("REST request to deactivate discount: {}", id);
        DiscountResponse response = discountService.deactivateDiscount(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Delete a discount",
        description = "Permanently delete a discount code. Cannot delete active discounts. Admin access required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Discount deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot delete active discount"),
        @ApiResponse(responseCode = "404", description = "Discount not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    public ResponseEntity<Void> deleteDiscount(
            @Parameter(description = "Discount ID", required = true)
            @PathVariable Long id) {
        log.info("REST request to delete discount: {}", id);
        discountService.deleteDiscount(id);
        return ResponseEntity.noContent().build();
    }
}
