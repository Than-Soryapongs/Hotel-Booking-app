package com.system.hotel_room_booking.repository;

import com.system.hotel_room_booking.model.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {
    
    Optional<Amenity> findByName(String name);
    
    List<Amenity> findByCategory(String category);
    
    List<Amenity> findByIsActiveTrue();
    
    List<Amenity> findByCategoryAndIsActiveTrue(String category);
}
