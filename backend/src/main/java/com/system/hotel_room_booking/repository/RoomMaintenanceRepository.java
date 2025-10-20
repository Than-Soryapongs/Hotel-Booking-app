package com.system.hotel_room_booking.repository;

import com.system.hotel_room_booking.model.entity.MaintenanceStatus;
import com.system.hotel_room_booking.model.entity.RoomMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RoomMaintenanceRepository extends JpaRepository<RoomMaintenance, Long> {
    
    List<RoomMaintenance> findByRoomId(Long roomId);
    
    List<RoomMaintenance> findByStatus(MaintenanceStatus status);
    
    List<RoomMaintenance> findByAssignedToId(Long userId);
    
    @Query("SELECT rm FROM RoomMaintenance rm WHERE rm.scheduledDate BETWEEN :start AND :end")
    List<RoomMaintenance> findByScheduledDateBetween(@Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);
    
    @Query("SELECT rm FROM RoomMaintenance rm WHERE rm.room.id = :roomId AND rm.status = :status")
    List<RoomMaintenance> findByRoomIdAndStatus(@Param("roomId") Long roomId, 
                                                @Param("status") MaintenanceStatus status);
}
