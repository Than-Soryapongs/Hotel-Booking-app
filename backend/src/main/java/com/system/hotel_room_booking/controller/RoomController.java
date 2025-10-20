package com.system.hotel_room_booking.controller;

import com.system.hotel_room_booking.model.dto.request.CreateRoomRequest;
import com.system.hotel_room_booking.model.dto.request.UpdateRoomRequest;
import com.system.hotel_room_booking.model.dto.response.RoomResponse;
import com.system.hotel_room_booking.model.entity.RoomType;
import com.system.hotel_room_booking.service.RoomService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Room Management", description = "APIs for managing hotel rooms")
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Create a new room",
        description = "Create a new hotel room with specified details and amenities. Admin access required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Room created successfully",
            content = @Content(schema = @Schema(implementation = RoomResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    public ResponseEntity<RoomResponse> createRoom(
            @Valid @RequestBody CreateRoomRequest request) {
        log.info("REST request to create room: {}", request.getRoomNumber());
        RoomResponse response = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Update a room",
        description = "Update an existing room's details. Admin access required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Room updated successfully",
            content = @Content(schema = @Schema(implementation = RoomResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Room not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    public ResponseEntity<RoomResponse> updateRoom(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoomRequest request) {
        log.info("REST request to update room: {}", id);
        RoomResponse response = roomService.updateRoom(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get room by ID",
        description = "Retrieve detailed information about a specific room"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Room found",
            content = @Content(schema = @Schema(implementation = RoomResponse.class))),
        @ApiResponse(responseCode = "404", description = "Room not found")
    })
    public ResponseEntity<RoomResponse> getRoomById(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long id) {
        log.info("REST request to get room: {}", id);
        RoomResponse response = roomService.getRoomById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
        summary = "Get all rooms",
        description = "Retrieve a paginated list of all rooms"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved rooms",
            content = @Content(schema = @Schema(implementation = Page.class)))
    })
    public ResponseEntity<Page<RoomResponse>> getAllRooms(
            @PageableDefault(size = 20, sort = "roomNumber") Pageable pageable) {
        log.info("REST request to get all rooms");
        Page<RoomResponse> response = roomService.getAllRooms(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/available")
    @Operation(
        summary = "Search available rooms",
        description = "Search for rooms available during specified dates with optional filters"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved available rooms"),
        @ApiResponse(responseCode = "400", description = "Invalid date range")
    })
    public ResponseEntity<List<RoomResponse>> searchAvailableRooms(
            @Parameter(description = "Check-in date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            
            @Parameter(description = "Check-out date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            
            @Parameter(description = "Room type filter")
            @RequestParam(required = false) RoomType type,
            
            @Parameter(description = "Minimum capacity required")
            @RequestParam(required = false) Integer minCapacity) {
        log.info("REST request to search available rooms from {} to {}", checkIn, checkOut);
        List<RoomResponse> response = roomService.searchAvailableRooms(checkIn, checkOut, type, minCapacity);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/price-range")
    @Operation(
        summary = "Get rooms by price range",
        description = "Retrieve rooms within a specified price range"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved rooms")
    })
    public ResponseEntity<List<RoomResponse>> getRoomsByPriceRange(
            @Parameter(description = "Minimum price", required = true)
            @RequestParam BigDecimal minPrice,
            
            @Parameter(description = "Maximum price", required = true)
            @RequestParam BigDecimal maxPrice) {
        log.info("REST request to get rooms by price range: {} - {}", minPrice, maxPrice);
        List<RoomResponse> response = roomService.getRoomsByPriceRange(minPrice, maxPrice);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Delete a room",
        description = "Soft delete a room (sets isActive=false). Cannot delete rooms with active bookings. Admin access required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Room deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Room not found"),
        @ApiResponse(responseCode = "409", description = "Room has active bookings"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    public ResponseEntity<Void> deleteRoom(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long id) {
        log.info("REST request to delete room: {}", id);
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Get room statistics",
        description = "Retrieve statistics about room statuses. Admin access required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved statistics"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    public ResponseEntity<Map<String, Long>> getRoomStatistics() {
        log.info("REST request to get room statistics");
        Map<String, Long> statistics = roomService.getRoomStatistics();
        return ResponseEntity.ok(statistics);
    }
}
