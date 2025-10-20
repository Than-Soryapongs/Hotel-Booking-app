package com.system.hotel_room_booking.controller;

import com.system.hotel_room_booking.model.dto.request.AddToCartRequest;
import com.system.hotel_room_booking.model.dto.response.CartResponse;
import com.system.hotel_room_booking.model.dto.response.CheckoutResponse;
import com.system.hotel_room_booking.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for shopping cart operations
 * Handles cart management, item operations, discount application, and checkout initiation
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management APIs")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get active cart", description = "Retrieve the current user's active shopping cart")
    public ResponseEntity<CartResponse> getCart() {
        return ResponseEntity.ok(cartService.getOrCreateActiveCart());
    }

    @PostMapping("/items")
    @Operation(summary = "Add room to cart", description = "Add a room with dates and guest count to the cart")
    public ResponseEntity<CartResponse> addToCart(@Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addToCart(request));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove item from cart", description = "Remove a specific item from the cart")
    public ResponseEntity<CartResponse> removeItem(@PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(itemId));
    }

    @PostMapping("/discount/{code}")
    @Operation(summary = "Apply discount code", description = "Apply a discount code to the cart")
    public ResponseEntity<CartResponse> applyDiscount(@PathVariable String code) {
        return ResponseEntity.ok(cartService.applyDiscount(code));
    }

    @DeleteMapping("/discount")
    @Operation(summary = "Remove discount", description = "Remove applied discount from the cart")
    public ResponseEntity<CartResponse> removeDiscount() {
        return ResponseEntity.ok(cartService.removeDiscount());
    }

    @DeleteMapping("/clear")
    @Operation(summary = "Clear cart", description = "Remove all items from the cart")
    public ResponseEntity<Void> clearCart() {
        cartService.clearCart();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/checkout")
    @Operation(summary = "Initiate checkout", description = "Initiate checkout process with ABA PayWay payment integration")
    public ResponseEntity<CheckoutResponse> checkout() {
        return ResponseEntity.ok(cartService.checkout());
    }
}
