/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.DTO;

import com.icsd.springor.model.Room;
import com.icsd.springor.model.RoomAvailability;
import com.icsd.springor.model.RoomType;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;


public class RoomDTO {
    private Long id;
    private String name;
    private String building;
    private Integer capacity;
    private RoomType type;
    private String location;
    private Set<String> availability = new HashSet<>();

    public RoomDTO(String name, String building, Integer capacity, RoomType type, String location) {
        this.name = name;
        this.building = building;
        this.capacity = capacity;
        this.type = type;
        this.location = location;
    }

    public RoomDTO() {
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public RoomType getType() {
        return type;
    }

    public void setType(RoomType type) {
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Set<String> getAvailability() {
        return availability;
    }

    public void setAvailability(Set<String> availability) {
        this.availability = availability;
    }
    
    

    public Room toEntity() {
        Room room = new Room();
        room.setName(name);
        room.setId(room.getId());
        room.setBuilding(building);
        room.setCapacity(capacity);
        room.setType(type);
        room.setLocation(location);
        
        Set<RoomAvailability> availabilitySet = new HashSet<>();
        for (String slot : availability) {
            String[] parts = slot.split("_");
            DayOfWeek day = DayOfWeek.valueOf(parts[0]);
            int slotNum = Integer.parseInt(parts[1].substring(4));
            
            RoomAvailability avail = new RoomAvailability();
            avail.setDay(day);
            
            // Set times based on slot number
            switch(slotNum) {
                case 1:
                    avail.setStartTime(LocalTime.of(9, 0));
                    avail.setEndTime(LocalTime.of(12, 0));
                    break;
                case 2:
                    avail.setStartTime(LocalTime.of(12, 0));
                    avail.setEndTime(LocalTime.of(15, 0));
                    break;
                case 3:
                    avail.setStartTime(LocalTime.of(15, 0));
                    avail.setEndTime(LocalTime.of(18, 0));
                    break;
                case 4:
                    avail.setStartTime(LocalTime.of(18, 0));
                    avail.setEndTime(LocalTime.of(21, 0));
                    break;
            }
            
            availabilitySet.add(avail);
        }
        
        room.setAvailability(availabilitySet);
        return room;
    }
}
