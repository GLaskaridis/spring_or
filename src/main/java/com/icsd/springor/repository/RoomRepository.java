package com.icsd.springor.repository;

import com.icsd.springor.model.Room;
import com.icsd.springor.model.RoomType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
    
    List<Room> findByActiveTrue();
    
    List<Room> findByTypeAndActiveTrue(RoomType type);
    
    List<Room> findByCapacityGreaterThanEqualAndActiveTrue(Integer capacity);
    
    List<Room> findByBuildingAndActiveTrue(String building);
    
    List<Room> findByCapacityBetweenAndActiveTrue(Integer minCapacity, Integer maxCapacity);
}