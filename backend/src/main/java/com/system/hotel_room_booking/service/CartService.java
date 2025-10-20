package com.system.hotel_room_booking.service;

import com.system.hotel_room_booking.exception.ResourceNotFoundException;
import com.system.hotel_room_booking.exception.RoomNotAvailableException;
import com.system.hotel_room_booking.model.dto.request.AddToCartRequest;
import com.system.hotel_room_booking.model.dto.response.*;
import com.system.hotel_room_booking.model.entity.*;
import com.system.hotel_room_booking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing shopping cart operations
 * Handles add to cart, remove items, apply discounts, and checkout initiation
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final RoomRepository roomRepository;
    private final RoomAvailabilityRepository roomAvailabilityRepository;
    private final UserRepository userRepository;
    private final DiscountService discountService;
    private final PaymentService paymentService;

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    /**
     * Get or create active cart for current user
     */
    public CartResponse getOrCreateActiveCart() {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                        .user(user)
                        .status(CartStatus.ACTIVE)
                        .subtotal(BigDecimal.ZERO)
                        .discountAmount(BigDecimal.ZERO)
                        .totalPrice(BigDecimal.ZERO)
                        .build();
                    return cartRepository.save(newCart);
                });
        return mapToResponse(cart);
    }

    /**
     * Add room to cart with date range and guest count
     */
    public CartResponse addToCart(AddToCartRequest request) {
        User user = getCurrentUser();
        
        // Validate dates
        validateDates(request.getCheckInDate(), request.getCheckOutDate());

        // Find room
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", request.getRoomId()));

        // Check if room is available
        if (!room.getIsActive() || room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new RoomNotAvailableException("Room is not available");
        }

        // Check room availability for the date range
        boolean unavailableExists = roomAvailabilityRepository
                .findByRoomIdAndDateRange(room.getId(), request.getCheckInDate(), request.getCheckOutDate().minusDays(1))
                .stream()
                .anyMatch(ra -> Boolean.FALSE.equals(ra.getIsAvailable()));
        
        if (unavailableExists) {
            throw new RoomNotAvailableException("Room is not available for the selected dates");
        }

        // Get or create active cart
        Cart cart = cartRepository.findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                        .user(user)
                        .status(CartStatus.ACTIVE)
                        .subtotal(BigDecimal.ZERO)
                        .discountAmount(BigDecimal.ZERO)
                        .totalPrice(BigDecimal.ZERO)
                        .build();
                    return cartRepository.save(newCart);
                });

        // Calculate price based on number of nights
        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal itemPrice = room.getBasePrice().multiply(BigDecimal.valueOf(nights));

        // Create cart item
        CartItem item = CartItem.builder()
                .cart(cart)
                .room(room)
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .numberOfGuests(request.getNumberOfGuests())
                .price(itemPrice)
                .build();
        
        cartItemRepository.save(item);
        cart.getItems().add(item);

        // Recalculate cart totals
        recalculateCartTotals(cart);
        cartRepository.save(cart);

        log.info("Added room {} to cart for user {}", room.getRoomNumber(), user.getEmail());

        return mapToResponse(cart);
    }

    /**
     * Remove item from cart
     */
    public CartResponse removeItem(Long itemId) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", user.getId()));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", itemId));
        
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Item does not belong to your cart");
        }

        // Remove item
        cart.getItems().removeIf(ci -> ci.getId().equals(itemId));
        cartItemRepository.delete(item);

        // Recalculate totals
        recalculateCartTotals(cart);
        cartRepository.save(cart);

        log.info("Removed item {} from cart", itemId);

        return mapToResponse(cart);
    }

    /**
     * Apply discount code to cart
     */
    public CartResponse applyDiscount(String discountCode) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", user.getId()));

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot apply discount to empty cart");
        }

        // Validate and apply discount
        BigDecimal discountAmount = discountService.calculateDiscount(discountCode, cart.getSubtotal());
        
        cart.setAppliedDiscountCode(discountCode);
        cart.setDiscountAmount(discountAmount);
        
        // Recalculate totals
        recalculateCartTotals(cart);
        cartRepository.save(cart);

        log.info("Applied discount {} to cart", discountCode);

        return mapToResponse(cart);
    }

    /**
     * Remove discount from cart
     */
    public CartResponse removeDiscount() {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", user.getId()));

        cart.setAppliedDiscountCode(null);
        cart.setDiscountAmount(BigDecimal.ZERO);
        cart.setDiscountAppliedAt(null);

        recalculateCartTotals(cart);
        cartRepository.save(cart);

        log.info("Removed discount from cart");

        return mapToResponse(cart);
    }

    /**
     * Initiate checkout process
     * Creates payment and returns payment URL
     */
    public CheckoutResponse checkout() {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", user.getId()));

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        // Validate all items are still available
        for (CartItem item : cart.getItems()) {
            validateRoomAvailability(item);
        }

        // Initiate payment through PaymentService
        CheckoutResponse response = paymentService.initiatePayment(cart);

        log.info("Checkout initiated for cart {}, transaction: {}", cart.getId(), response.getTransactionId());

        return response;
    }

    /**
     * Clear cart (remove all items)
     */
    public void clearCart() {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", user.getId()));

        cart.getItems().clear();
        cartItemRepository.deleteAll(cart.getItems());

        cart.setSubtotal(BigDecimal.ZERO);
        cart.setDiscountAmount(BigDecimal.ZERO);
        cart.setTotalPrice(BigDecimal.ZERO);
        cart.setAppliedDiscountCode(null);
        cart.setDiscountAppliedAt(null);

        cartRepository.save(cart);

        log.info("Cleared cart for user {}", user.getEmail());
    }

    /**
     * Recalculate cart totals (subtotal, discount, total)
     */
    private void recalculateCartTotals(Cart cart) {
        // Calculate subtotal from all items
        BigDecimal subtotal = cart.getItems().stream()
            .map(CartItem::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setSubtotal(subtotal);

        // Recalculate discount if applied
        if (cart.getAppliedDiscountCode() != null && !cart.getAppliedDiscountCode().isEmpty()) {
            try {
                BigDecimal discountAmount = discountService.calculateDiscount(
                    cart.getAppliedDiscountCode(), 
                    subtotal
                );
                cart.setDiscountAmount(discountAmount);
            } catch (Exception e) {
                log.warn("Failed to recalculate discount, removing it: {}", e.getMessage());
                cart.setAppliedDiscountCode(null);
                cart.setDiscountAmount(BigDecimal.ZERO);
                cart.setDiscountAppliedAt(null);
            }
        } else {
            cart.setDiscountAmount(BigDecimal.ZERO);
        }

        // Calculate final total
        BigDecimal total = subtotal.subtract(cart.getDiscountAmount());
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }
        cart.setTotalPrice(total);
    }

    /**
     * Validate room availability for cart item
     */
    private void validateRoomAvailability(CartItem item) {
        Room room = item.getRoom();
        
        if (!room.getIsActive() || room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new RoomNotAvailableException(
                "Room " + room.getRoomNumber() + " is no longer available"
            );
        }

        boolean unavailableExists = roomAvailabilityRepository
                .findByRoomIdAndDateRange(
                    room.getId(), 
                    item.getCheckInDate(), 
                    item.getCheckOutDate().minusDays(1)
                )
                .stream()
                .anyMatch(ra -> Boolean.FALSE.equals(ra.getIsAvailable()));
        
        if (unavailableExists) {
            throw new RoomNotAvailableException(
                "Room " + room.getRoomNumber() + " is not available for the selected dates"
            );
        }
    }

    /**
     * Validate check-in and check-out dates
     */
    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn.isAfter(checkOut)) {
            throw new IllegalArgumentException("Check-in date must be before check-out date");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Check-in date cannot be in the past");
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights < 1) {
            throw new IllegalArgumentException("Booking must be for at least one night");
        }
    }

    /**
     * Map Cart entity to CartResponse DTO
     */
    private CartResponse mapToResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
            .map(ci -> CartItemResponse.builder()
                .id(ci.getId())
                .roomId(ci.getRoom().getId())
                .roomNumber(ci.getRoom().getRoomNumber())
                .checkInDate(ci.getCheckInDate())
                .checkOutDate(ci.getCheckOutDate())
                .numberOfGuests(ci.getNumberOfGuests())
                .price(ci.getPrice())
                .build())
            .collect(Collectors.toList());
        
        return CartResponse.builder()
                .id(cart.getId())
                .status(cart.getStatus())
                .subtotal(cart.getSubtotal())
                .discountAmount(cart.getDiscountAmount())
                .discountCode(cart.getAppliedDiscountCode())
                .totalPrice(cart.getTotalPrice())
                .items(items)
                .itemCount(items.size())
                .build();
    }
}
