package com.system.hotel_room_booking.service;

import com.system.hotel_room_booking.exception.ResourceNotFoundException;
import com.system.hotel_room_booking.exception.RoomNotAvailableException;
import com.system.hotel_room_booking.model.dto.request.CreateRoomRequest;
import com.system.hotel_room_booking.model.dto.request.UpdateRoomRequest;
import com.system.hotel_room_booking.model.dto.response.*;
import com.system.hotel_room_booking.model.entity.*;
import com.system.hotel_room_booking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;
    private final AmenityRepository amenityRepository;
    private final BookingRepository bookingRepository;

    /**
     * Create a new room
     */
    public RoomResponse createRoom(CreateRoomRequest request) {
        log.info("Creating new room with room number: {}", request.getRoomNumber());

        // Check if room number already exists
        if (roomRepository.findByRoomNumber(request.getRoomNumber()).isPresent()) {
            throw new IllegalArgumentException("Room with number " + request.getRoomNumber() + " already exists");
        }

        // Build room entity
        Room room = Room.builder()
                .roomNumber(request.getRoomNumber())
                .type(request.getType())
                .status(request.getStatus())
                .basePrice(request.getBasePrice())
                .capacity(request.getCapacity())
                .bedCount(request.getBedCount())
                .size(request.getSize())
                .floor(request.getFloor())
                .description(request.getDescription())
                .isActive(request.getIsActive())
                .images(new HashSet<>())
                .amenities(new HashSet<>())
                .build();

        // Add amenities if provided
        if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
            Set<Amenity> amenities = new HashSet<>(amenityRepository.findAllById(request.getAmenityIds()));
            room.setAmenities(amenities);
        }

        Room savedRoom = roomRepository.save(room);
        log.info("Room created successfully with ID: {}", savedRoom.getId());

        return mapToRoomResponse(savedRoom);
    }

    /**
     * Update an existing room
     */
    public RoomResponse updateRoom(Long roomId, UpdateRoomRequest request) {
        log.info("Updating room with ID: {}", roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));

        // Update fields if provided
        if (request.getRoomNumber() != null) {
            // Check if new room number already exists (excluding current room)
            roomRepository.findByRoomNumber(request.getRoomNumber())
                    .ifPresent(existingRoom -> {
                        if (!existingRoom.getId().equals(roomId)) {
                            throw new IllegalArgumentException("Room with number " + request.getRoomNumber() + " already exists");
                        }
                    });
            room.setRoomNumber(request.getRoomNumber());
        }

        if (request.getType() != null) room.setType(request.getType());
        if (request.getStatus() != null) room.setStatus(request.getStatus());
        if (request.getBasePrice() != null) room.setBasePrice(request.getBasePrice());
        if (request.getCapacity() != null) room.setCapacity(request.getCapacity());
        if (request.getBedCount() != null) room.setBedCount(request.getBedCount());
        if (request.getSize() != null) room.setSize(request.getSize());
        if (request.getFloor() != null) room.setFloor(request.getFloor());
        if (request.getDescription() != null) room.setDescription(request.getDescription());
        if (request.getIsActive() != null) room.setIsActive(request.getIsActive());

        // Update amenities if provided
        if (request.getAmenityIds() != null) {
            Set<Amenity> amenities = new HashSet<>(amenityRepository.findAllById(request.getAmenityIds()));
            room.setAmenities(amenities);
        }

        Room updatedRoom = roomRepository.save(room);
        log.info("Room updated successfully: {}", roomId);

        return mapToRoomResponse(updatedRoom);
    }

    /**
     * Get room by ID
     */
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long roomId) {
        log.info("Fetching room with ID: {}", roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));

        return mapToRoomResponse(room);
    }

    /**
     * Get all rooms with pagination
     */
    @Transactional(readOnly = true)
    public Page<RoomResponse> getAllRooms(Pageable pageable) {
        log.info("Fetching all rooms with pagination");

        return roomRepository.findAll(pageable)
                .map(this::mapToRoomResponse);
    }

    /**
     * Search available rooms for given dates
     */
    @Transactional(readOnly = true)
    public List<RoomResponse> searchAvailableRooms(LocalDate checkIn, LocalDate checkOut, RoomType type, Integer minCapacity) {
        log.info("Searching available rooms from {} to {}", checkIn, checkOut);

        // Validate dates
        if (checkIn.isAfter(checkOut)) {
            throw new IllegalArgumentException("Check-in date must be before check-out date");
        }

        if (checkIn.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Check-in date cannot be in the past");
        }

        List<Room> availableRooms;

        if (type != null) {
            availableRooms = roomRepository.findAvailableRoomsByType(type, checkIn, checkOut);
        } else {
            availableRooms = roomRepository.findAvailableRooms(checkIn, checkOut);
        }

        // Filter by capacity if specified
        if (minCapacity != null) {
            availableRooms = availableRooms.stream()
                    .filter(room -> room.getCapacity() >= minCapacity)
                    .collect(Collectors.toList());
        }

        log.info("Found {} available rooms", availableRooms.size());

        return availableRooms.stream()
                .map(this::mapToRoomResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get rooms by price range
     */
    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.info("Fetching rooms with price range: {} - {}", minPrice, maxPrice);

        List<Room> rooms = roomRepository.findByPriceRange(minPrice, maxPrice);

        return rooms.stream()
                .map(this::mapToRoomResponse)
                .collect(Collectors.toList());
    }

    /**
     * Delete a room (soft delete)
     */
    public void deleteRoom(Long roomId) {
        log.info("Deleting room with ID: {}", roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));

        // Check if room has active bookings
        List<Booking> confirmedBookings = bookingRepository.findByRoomIdAndStatus(roomId, BookingStatus.CONFIRMED);
        List<Booking> checkedInBookings = bookingRepository.findByRoomIdAndStatus(roomId, BookingStatus.CHECKED_IN);

        if (!confirmedBookings.isEmpty() || !checkedInBookings.isEmpty()) {
            throw new RoomNotAvailableException("Cannot delete room with active bookings");
        }

        room.setIsActive(false);
        room.setStatus(RoomStatus.MAINTENANCE);
        roomRepository.save(room);

        log.info("Room soft deleted successfully: {}", roomId);
    }

    /**
     * Get room statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getRoomStatistics() {
        Map<String, Long> stats = new HashMap<>();
        
        for (RoomStatus status : RoomStatus.values()) {
            Long count = roomRepository.countByStatus(status);
            stats.put(status.name(), count);
        }

        return stats;
    }

    /**
     * Map Room entity to RoomResponse DTO
     */
    private RoomResponse mapToRoomResponse(Room room) {
        return RoomResponse.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .type(room.getType())
                .status(room.getStatus())
                .basePrice(room.getBasePrice())
                .capacity(room.getCapacity())
                .bedCount(room.getBedCount())
                .size(room.getSize())
                .floor(room.getFloor())
                .description(room.getDescription())
                .isActive(room.getIsActive())
                .images(mapToRoomImageResponses(room.getImages()))
                .amenities(mapToAmenityResponses(room.getAmenities()))
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    private List<RoomImageResponse> mapToRoomImageResponses(Set<RoomImage> images) {
        if (images == null) return new ArrayList<>();

        return images.stream()
                .map(image -> RoomImageResponse.builder()
                        .id(image.getId())
                        .imageUrl(image.getImageUrl())
                        .caption(image.getCaption())
                        .displayOrder(image.getDisplayOrder())
                        .isPrimary(image.getIsPrimary())
                        .build())
                .collect(Collectors.toList());
    }

    private Set<AmenityResponse> mapToAmenityResponses(Set<Amenity> amenities) {
        if (amenities == null) return new HashSet<>();

        return amenities.stream()
                .map(amenity -> AmenityResponse.builder()
                        .id(amenity.getId())
                        .name(amenity.getName())
                        .category(amenity.getCategory())
                        .icon(amenity.getIcon())
                        .description(amenity.getDescription())
                        .isActive(amenity.getIsActive())
                        .build())
                .collect(Collectors.toSet());
    }
}
