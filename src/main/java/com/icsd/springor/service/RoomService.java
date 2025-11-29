package com.icsd.springor.service;

import com.icsd.springor.model.Room;
import com.icsd.springor.model.RoomType;
import com.icsd.springor.repository.RoomRepository;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
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
        //validation logic here
        return true; 
    }

    public Room getRoomById(Long id) {
        return roomRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));
    }

    public Room updateRoom(Long id, Room updatedRoom) {
        Room room = getRoomById(id);
        room.setName(updatedRoom.getName());
        room.setBuilding(updatedRoom.getBuilding());
        room.setCapacity(updatedRoom.getCapacity());
        room.setType(updatedRoom.getType());
        room.setLocation(updatedRoom.getLocation());
        room.setAvailability(updatedRoom.getAvailability());
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
    
    public List<Room> getAvailableRoomsByDayAndTime(DayOfWeek day, LocalTime startTime, LocalTime endTime) {
        List<Room> allRooms = roomRepository.findAll();
        
        return allRooms.stream()
            .filter(room -> isRoomAvailable(room, day, startTime, endTime))
            .collect(Collectors.toList());
    }
    
     private boolean isRoomAvailable(Room room, DayOfWeek day, LocalTime startTime, LocalTime endTime) {
        if (room.getAvailability() == null || room.getAvailability().isEmpty()) {
            return false;
        }
        
        return room.getAvailability().stream()
            .anyMatch(avail -> 
                avail.getDay().equals(day) &&
                !avail.getStartTime().isAfter(startTime) &&
                !avail.getEndTime().isBefore(endTime)
            );
    }
    
    
}