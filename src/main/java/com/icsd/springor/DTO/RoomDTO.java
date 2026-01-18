package com.icsd.springor.DTO;

import com.icsd.springor.model.Room;
import com.icsd.springor.model.RoomAvailability;
import com.icsd.springor.model.RoomType;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoomDTO {
    private Long id;
    private String name;
    private String building;
    private Integer capacity;
    private RoomType type;
    private String location;
    private boolean active = true;
    
    //υποστήριξη για checkboxes (παλιά μέθοδος)
    private Set<String> availability = new HashSet<>();
    
    //υποστήριξη για custom ώρες (νέα μέθοδος)
    private List<String> availabilityDays = new ArrayList<>();
    private List<String> availabilityStartTimes = new ArrayList<>();
    private List<String> availabilityEndTimes = new ArrayList<>();

    public RoomDTO(String name, String building, Integer capacity, RoomType type, String location) {
        this.name = name;
        this.building = building;
        this.capacity = capacity;
        this.type = type;
        this.location = location;
    }

    public RoomDTO() {
    }
    
    //getters και setters
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<String> getAvailability() {
        return availability;
    }

    public void setAvailability(Set<String> availability) {
        this.availability = availability;
    }
    
    public List<String> getAvailabilityDays() {
        return availabilityDays;
    }

    public void setAvailabilityDays(List<String> availabilityDays) {
        this.availabilityDays = availabilityDays;
    }

    public List<String> getAvailabilityStartTimes() {
        return availabilityStartTimes;
    }

    public void setAvailabilityStartTimes(List<String> availabilityStartTimes) {
        this.availabilityStartTimes = availabilityStartTimes;
    }

    public List<String> getAvailabilityEndTimes() {
        return availabilityEndTimes;
    }

    public void setAvailabilityEndTimes(List<String> availabilityEndTimes) {
        this.availabilityEndTimes = availabilityEndTimes;
    }

    public Room toEntity() {
        Room room = new Room();
        room.setName(name);
        room.setBuilding(building);
        room.setCapacity(capacity);
        room.setType(type);
        room.setLocation(location);
        room.setActive(active);
        
        Set<RoomAvailability> availabilitySet = new HashSet<>();
        
        //πρώτα έλεγχος για custom ώρες (νέα μέθοδος)
        if (availabilityDays != null && !availabilityDays.isEmpty()) {
            for (int i = 0; i < availabilityDays.size(); i++) {
                if (availabilityDays.get(i) != null && !availabilityDays.get(i).isEmpty()) {
                    RoomAvailability avail = new RoomAvailability();
                    avail.setDay(DayOfWeek.valueOf(availabilityDays.get(i)));
                    
                    //parsing ωρών
                    String startTimeStr = (i < availabilityStartTimes.size()) ? availabilityStartTimes.get(i) : "09:00";
                    String endTimeStr = (i < availabilityEndTimes.size()) ? availabilityEndTimes.get(i) : "21:00";
                    
                    avail.setStartTime(LocalTime.parse(startTimeStr));
                    avail.setEndTime(LocalTime.parse(endTimeStr));
                    
                    availabilitySet.add(avail);
                }
            }
        }
        
        //fallback στα checkboxes αν δεν υπάρχουν custom ώρες
        if (availabilitySet.isEmpty() && availability != null && !availability.isEmpty()) {
            for (String slot : availability) {
                String[] parts = slot.split("_");
                if (parts.length < 2) continue;
                
                DayOfWeek day = DayOfWeek.valueOf(parts[0]);
                int slotNum = Integer.parseInt(parts[1].substring(4));
                
                RoomAvailability avail = new RoomAvailability();
                avail.setDay(day);
                
                //ορισμός ωρών με βάση τον αριθμό slot
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
                    default:
                        continue;
                }
                
                availabilitySet.add(avail);
            }
        }
        
        room.setAvailability(availabilitySet);
        return room;
    }
    
    //στατική μέθοδος για μετατροπή από entity σε DTO
    public static RoomDTO fromEntity(Room room) {
        RoomDTO dto = new RoomDTO();
        dto.setId(room.getId());
        dto.setName(room.getName());
        dto.setBuilding(room.getBuilding());
        dto.setCapacity(room.getCapacity());
        dto.setType(room.getType());
        dto.setLocation(room.getLocation());
        dto.setActive(room.isActive());
        
        //μετατροπή διαθεσιμότητας σε checkboxes
        if (room.getAvailability() != null) {
            Set<String> availSlots = new HashSet<>();
            List<String> days = new ArrayList<>();
            List<String> startTimes = new ArrayList<>();
            List<String> endTimes = new ArrayList<>();
            
            for (RoomAvailability avail : room.getAvailability()) {
                String day = avail.getDay().toString();
                LocalTime start = avail.getStartTime();
                LocalTime end = avail.getEndTime();
                
                //προσθήκη στις custom ώρες
                days.add(day);
                startTimes.add(start.toString());
                endTimes.add(end.toString());
                
                //εύρεση slot για checkboxes
                int slotNum = getSlotNumber(start);
                if (slotNum > 0) {
                    availSlots.add(day + "_SLOT" + slotNum);
                }
            }
            
            dto.setAvailability(availSlots);
            dto.setAvailabilityDays(days);
            dto.setAvailabilityStartTimes(startTimes);
            dto.setAvailabilityEndTimes(endTimes);
        }
        
        return dto;
    }
    
    //βοηθητική μέθοδος για εύρεση αριθμού slot
    private static int getSlotNumber(LocalTime start) {
        if (start.equals(LocalTime.of(9, 0)) || 
            (start.isAfter(LocalTime.of(8, 30)) && start.isBefore(LocalTime.of(9, 30)))) {
            return 1;
        } else if (start.equals(LocalTime.of(12, 0)) || 
                   (start.isAfter(LocalTime.of(11, 30)) && start.isBefore(LocalTime.of(12, 30)))) {
            return 2;
        } else if (start.equals(LocalTime.of(15, 0)) || 
                   (start.isAfter(LocalTime.of(14, 30)) && start.isBefore(LocalTime.of(15, 30)))) {
            return 3;
        } else if (start.equals(LocalTime.of(18, 0)) || 
                   (start.isAfter(LocalTime.of(17, 30)) && start.isBefore(LocalTime.of(18, 30)))) {
            return 4;
        }
        return 0;
    }
}