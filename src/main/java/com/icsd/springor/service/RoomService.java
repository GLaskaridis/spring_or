package com.icsd.springor.service;

import com.icsd.springor.model.Room;
import com.icsd.springor.model.RoomType;
import com.icsd.springor.repository.RoomRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    public Room addRoom(Room room) {
        if (isValidRoom(room)) {
            return roomRepository.save(room);
        }
        throw new IllegalArgumentException("Invalid room data");
    }
    
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    private boolean isValidRoom(Room room) {
        // Implement validation logic here
        return true; // Placeholder
    }

    public Room getRoomById(Long id) {
        return roomRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));
    }

    public Room updateRoom(Long id, Room updatedRoom) {
        Room room = getRoomById(id);
        // Update the fields
        room.setName(updatedRoom.getName());
        room.setBuilding(updatedRoom.getBuilding());
        room.setCapacity(updatedRoom.getCapacity());
        room.setType(updatedRoom.getType());
        room.setLocation(updatedRoom.getLocation());
        room.setAvailability(updatedRoom.getAvailability());
        // Save the updated room
        return roomRepository.save(room);
    }

    public void deleteRoom(Long id) {
        Room room = getRoomById(id);
        roomRepository.delete(room);
    }
    
    public List<Room> getRoomsByType(RoomType type) {
        return roomRepository.findByTypeAndActiveTrue(type);
    }

    public List<Room> getRoomsWithMinCapacity(Integer minCapacity) {
        return roomRepository.findByCapacityGreaterThanEqualAndActiveTrue(minCapacity);
    }
    
    
}