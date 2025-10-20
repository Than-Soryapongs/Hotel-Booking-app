package com.system.hotel_room_booking.service;

import com.system.hotel_room_booking.exception.ResourceNotFoundException;
import com.system.hotel_room_booking.exception.RoomNotAvailableException;
import com.system.hotel_room_booking.model.dto.request.CreateBookingRequest;
import com.system.hotel_room_booking.model.dto.request.UpdateBookingRequest;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final DiscountRepository discountRepository;
    private final BookingDiscountRepository bookingDiscountRepository;

    /**
     * Get the currently authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    /**
     * Create a new booking
     */
    public BookingResponse createBooking(CreateBookingRequest request) {
        User user = getCurrentUser();
        log.info("Creating booking for user: {} and room: {}", user.getId(), request.getRoomId());

        // Validate dates
        validateBookingDates(request.getCheckInDate(), request.getCheckOutDate());

        // Get room
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", request.getRoomId()));

        // Check room status
        if (!room.getIsActive() || room.getStatus() != RoomStatus.AVAILABLE) {
            throw new RoomNotAvailableException("Room is not available for booking");
        }

        // Check room capacity
        if (request.getNumberOfGuests() > room.getCapacity()) {
            throw new IllegalArgumentException("Number of guests exceeds room capacity");
        }

        // Check for conflicting bookings
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                request.getRoomId(),
                request.getCheckInDate(),
                request.getCheckOutDate()
        );

        if (!conflicts.isEmpty()) {
            throw new RoomNotAvailableException("Room is not available for the selected dates");
        }

        // Calculate prices
        long numberOfNights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal totalPrice = room.getBasePrice().multiply(BigDecimal.valueOf(numberOfNights));

        BigDecimal discountAmount = BigDecimal.ZERO;
        Discount discount = null;

        // Apply discount if code provided
        if (request.getDiscountCode() != null && !request.getDiscountCode().isEmpty()) {
            discount = discountRepository.findValidDiscountByCode(request.getDiscountCode(), LocalDateTime.now())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid or expired discount code"));

            discountAmount = calculateDiscount(discount, totalPrice);
        }
        BigDecimal finalPrice = totalPrice.subtract(discountAmount);

        // Generate confirmation number
        String confirmationNumber = generateConfirmationNumber();

        // Create booking - pending payment confirmation
        Booking booking = Booking.builder()
                .confirmationNumber(confirmationNumber)
                .user(user)
                .room(room)
                .status(BookingStatus.PENDING)
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .numberOfGuests(request.getNumberOfGuests())
                .totalPrice(totalPrice)
                .discountAmount(discountAmount)
                .finalPrice(finalPrice)
                .specialRequests(request.getSpecialRequests())
                .appliedDiscounts(new HashSet<>())
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // Save discount application if used
        if (discount != null) {
            BookingDiscount bookingDiscount = BookingDiscount.builder()
                    .booking(savedBooking)
                    .discount(discount)
                    .discountAmount(discountAmount)
                    .discountCode(request.getDiscountCode())
                    .appliedAt(LocalDateTime.now())
                    .build();
            bookingDiscountRepository.save(bookingDiscount);

            // Update discount usage count
            discount.setCurrentUsageCount(discount.getCurrentUsageCount() + 1);
            discountRepository.save(discount);
        }

        // Update room status to RESERVED
        room.setStatus(RoomStatus.RESERVED);
        roomRepository.save(room);

        log.info("Booking created successfully with confirmation: {}", confirmationNumber);

        return mapToBookingResponse(savedBooking);
    }

    /**
     * Update booking
     */
    public BookingResponse updateBooking(Long bookingId, UpdateBookingRequest request) {
        User user = getCurrentUser();
        log.info("Updating booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        // Check ownership
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not authorized to update this booking");
        }

        // Update status
        if (request.getStatus() != null) {
            updateBookingStatus(booking, request.getStatus());
        }

        // Update dates if provided and booking is still pending/confirmed
        if (booking.getStatus() == BookingStatus.PENDING || booking.getStatus() == BookingStatus.CONFIRMED) {
            if (request.getCheckInDate() != null || request.getCheckOutDate() != null) {
                LocalDate newCheckIn = request.getCheckInDate() != null ? request.getCheckInDate() : booking.getCheckInDate();
                LocalDate newCheckOut = request.getCheckOutDate() != null ? request.getCheckOutDate() : booking.getCheckOutDate();

                validateBookingDates(newCheckIn, newCheckOut);

                // Check for conflicts with new dates
                List<Booking> conflicts = bookingRepository.findConflictingBookings(
                        booking.getRoom().getId(),
                        newCheckIn,
                        newCheckOut
                ).stream()
                        .filter(b -> !b.getId().equals(bookingId))
                        .collect(Collectors.toList());

                if (!conflicts.isEmpty()) {
                    throw new RoomNotAvailableException("Room is not available for the new dates");
                }

                booking.setCheckInDate(newCheckIn);
                booking.setCheckOutDate(newCheckOut);

                // Recalculate prices
                recalculateBookingPrices(booking);
            }
        }

        if (request.getNumberOfGuests() != null) {
            if (request.getNumberOfGuests() > booking.getRoom().getCapacity()) {
                throw new IllegalArgumentException("Number of guests exceeds room capacity");
            }
            booking.setNumberOfGuests(request.getNumberOfGuests());
        }

        if (request.getSpecialRequests() != null) {
            booking.setSpecialRequests(request.getSpecialRequests());
        }

        if (request.getCheckInTime() != null) {
            booking.setCheckInTime(request.getCheckInTime());
        }

        if (request.getCheckOutTime() != null) {
            booking.setCheckOutTime(request.getCheckOutTime());
        }

        if (request.getCancellationReason() != null) {
            booking.setCancellationReason(request.getCancellationReason());
        }

        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Booking updated successfully: {}", bookingId);

        return mapToBookingResponse(updatedBooking);
    }

    /**
     * Get booking by ID
     */
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId) {
        log.info("Fetching booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        return mapToBookingResponse(booking);
    }

    /**
     * Get booking by confirmation number
     */
    @Transactional(readOnly = true)
    public BookingResponse getBookingByConfirmation(String confirmationNumber) {
        log.info("Fetching booking by confirmation: {}", confirmationNumber);

        Booking booking = bookingRepository.findByConfirmationNumber(confirmationNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "confirmationNumber", confirmationNumber));

        return mapToBookingResponse(booking);
    }

    /**
     * Get user bookings
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings() {
        User user = getCurrentUser();
        log.info("Fetching bookings for user: {}", user.getId());

        return bookingRepository.findByUserId(user.getId()).stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cancel booking
     */
    public BookingResponse cancelBooking(Long bookingId, String reason) {
        User user = getCurrentUser();
        log.info("Cancelling booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        // Check ownership
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not authorized to cancel this booking");
        }

        // Can only cancel pending or confirmed bookings
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Cannot cancel booking with status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancellationReason(reason);

        // Update room status back to AVAILABLE
        Room room = booking.getRoom();
        room.setStatus(RoomStatus.AVAILABLE);
        roomRepository.save(room);

        Booking cancelledBooking = bookingRepository.save(booking);
        log.info("Booking cancelled successfully: {}", bookingId);

        return mapToBookingResponse(cancelledBooking);
    }

    /**
     * Check-in
     */
    public BookingResponse checkIn(Long bookingId) {
        log.info("Checking in booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Only confirmed bookings can be checked in");
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckInTime(LocalDateTime.now());

        // Update room status
        Room room = booking.getRoom();
        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);

        Booking checkedInBooking = bookingRepository.save(booking);
        log.info("Booking checked in successfully: {}", bookingId);

        return mapToBookingResponse(checkedInBooking);
    }

    /**
     * Check-out
     */
    public BookingResponse checkOut(Long bookingId) {
        log.info("Checking out booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new IllegalArgumentException("Only checked-in bookings can be checked out");
        }

        booking.setStatus(BookingStatus.CHECKED_OUT);
        booking.setCheckOutTime(LocalDateTime.now());

        // Update room status
        Room room = booking.getRoom();
        room.setStatus(RoomStatus.CLEANING);
        roomRepository.save(room);

        Booking checkedOutBooking = bookingRepository.save(booking);
        log.info("Booking checked out successfully: {}", bookingId);

        return mapToBookingResponse(checkedOutBooking);
    }

    // Helper methods

    private void validateBookingDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn.isAfter(checkOut)) {
            throw new IllegalArgumentException("Check-in date must be before check-out date");
        }

        if (checkIn.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Check-in date cannot be in the past");
        }

        long numberOfNights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (numberOfNights < 1) {
            throw new IllegalArgumentException("Booking must be for at least one night");
        }
    }

    private BigDecimal calculateDiscount(Discount discount, BigDecimal totalPrice) {
        BigDecimal discountAmount;

        if (discount.getPercentageValue() != null) {
            discountAmount = totalPrice.multiply(discount.getPercentageValue()).divide(BigDecimal.valueOf(100));
        } else {
            discountAmount = discount.getFixedAmount();
        }

        // Apply max discount limit if set
        if (discount.getMaxDiscountAmount() != null && discountAmount.compareTo(discount.getMaxDiscountAmount()) > 0) {
            discountAmount = discount.getMaxDiscountAmount();
        }

        return discountAmount;
    }

    private void recalculateBookingPrices(Booking booking) {
        long numberOfNights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
        BigDecimal totalPrice = booking.getRoom().getBasePrice().multiply(BigDecimal.valueOf(numberOfNights));
        BigDecimal finalPrice = totalPrice.subtract(booking.getDiscountAmount());

        booking.setTotalPrice(totalPrice);
        booking.setFinalPrice(finalPrice);
    }

    private void updateBookingStatus(Booking booking, BookingStatus newStatus) {
        BookingStatus oldStatus = booking.getStatus();

        // Validate status transition
        if (!isValidStatusTransition(oldStatus, newStatus)) {
            throw new IllegalArgumentException(
                    String.format("Cannot transition from %s to %s", oldStatus, newStatus)
            );
        }

        booking.setStatus(newStatus);

        // Handle room status changes
        if (newStatus == BookingStatus.CANCELLED) {
            booking.getRoom().setStatus(RoomStatus.AVAILABLE);
        } else if (newStatus == BookingStatus.CHECKED_IN) {
            booking.getRoom().setStatus(RoomStatus.OCCUPIED);
        } else if (newStatus == BookingStatus.CHECKED_OUT) {
            booking.getRoom().setStatus(RoomStatus.CLEANING);
        }
    }

    private boolean isValidStatusTransition(BookingStatus from, BookingStatus to) {
        // Define valid transitions
        Map<BookingStatus, Set<BookingStatus>> validTransitions = Map.of(
                BookingStatus.PENDING, Set.of(BookingStatus.CONFIRMED, BookingStatus.CANCELLED),
                BookingStatus.CONFIRMED, Set.of(BookingStatus.CHECKED_IN, BookingStatus.CANCELLED, BookingStatus.NO_SHOW),
                BookingStatus.CHECKED_IN, Set.of(BookingStatus.CHECKED_OUT),
                BookingStatus.CHECKED_OUT, Set.of(BookingStatus.REFUNDED)
        );

        return validTransitions.getOrDefault(from, Collections.emptySet()).contains(to);
    }

    private String generateConfirmationNumber() {
        return "BK" + System.currentTimeMillis() + new Random().nextInt(1000);
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .confirmationNumber(booking.getConfirmationNumber())
                .user(mapToUserSummary(booking.getUser()))
                .room(mapToRoomSummary(booking.getRoom()))
                .status(booking.getStatus())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .numberOfGuests(booking.getNumberOfGuests())
                .totalPrice(booking.getTotalPrice())
                .discountAmount(booking.getDiscountAmount())
                .finalPrice(booking.getFinalPrice())
                .specialRequests(booking.getSpecialRequests())
                .checkInTime(booking.getCheckInTime())
                .checkOutTime(booking.getCheckOutTime())
                .cancelledAt(booking.getCancelledAt())
                .cancellationReason(booking.getCancellationReason())
                .appliedDiscounts(mapToBookingDiscountResponses(booking.getAppliedDiscounts()))
                .review(booking.getReview() != null ? mapToReviewResponse(booking.getReview()) : null)
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    private UserSummaryResponse mapToUserSummary(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(null) // phoneNumber not in User entity
                .build();
    }

    private RoomSummaryResponse mapToRoomSummary(Room room) {
        return RoomSummaryResponse.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .type(room.getType())
                .floor(room.getFloor())
                .basePrice(room.getBasePrice())
                .build();
    }

    private List<BookingDiscountResponse> mapToBookingDiscountResponses(Set<BookingDiscount> discounts) {
        if (discounts == null) return new ArrayList<>();

        return discounts.stream()
                .map(bd -> BookingDiscountResponse.builder()
                        .id(bd.getId())
                        .discountCode(bd.getDiscountCode())
                        .discountAmount(bd.getDiscountAmount())
                        .appliedAt(bd.getAppliedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private ReviewResponse mapToReviewResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .user(mapToUserSummary(review.getUser()))
                .room(mapToRoomSummary(review.getRoom()))
                .overallRating(review.getOverallRating())
                .cleanlinessRating(review.getCleanlinessRating())
                .comfortRating(review.getComfortRating())
                .serviceRating(review.getServiceRating())
                .valueForMoneyRating(review.getValueForMoneyRating())
                .locationRating(review.getLocationRating())
                .comment(review.getComment())
                .isVerified(review.getIsVerified())
                .isPublished(review.getIsPublished())
                .publishedAt(review.getPublishedAt())
                .managementResponse(review.getManagementResponse())
                .respondedAt(review.getRespondedAt())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
