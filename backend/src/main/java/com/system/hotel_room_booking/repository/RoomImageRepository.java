package com.system.hotel_room_booking.repository;

import com.system.hotel_room_booking.model.entity.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {
    
    List<RoomImage> findByRoomIdOrderByDisplayOrderAsc(Long roomId);
    
    Optional<RoomImage> findByRoomIdAndIsPrimaryTrue(Long roomId);
    
    void deleteByRoomId(Long roomId);
}
