/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.icsd.springor.controller;

import static com.google.protobuf.JavaFeaturesProto.java;
import com.icsd.springor.DTO.RoomDTO;
import com.icsd.springor.model.Room;
import com.icsd.springor.model.RoomAvailability;
import com.icsd.springor.service.RoomService;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @PostMapping("/edit/{id}")
    public String updateRoom(@PathVariable Long id, @ModelAttribute RoomDTO roomDTO) {
        Room room = roomDTO.toEntity();
        room.setId(id); // Make sure to set the ID
        roomService.updateRoom(id, room);
        return "redirect:/rooms/list";
    }

    @GetMapping("/add")
    public String showAddRoomForm(Model model) {
        model.addAttribute("room", new Room());
        return "add_room";
    }

    @PostMapping("/add")
    public String addRoom(@ModelAttribute RoomDTO roomDTO, RedirectAttributes redirectAttributes) {
        try {
            Room room = roomDTO.toEntity();
            System.out.println(room);
            roomService.addRoom(room);
            redirectAttributes.addFlashAttribute("message", "Room added successfully");
            return "redirect:/rooms/list";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding room: " + e.getMessage());
            return "redirect:/rooms/add";
        }
    }

    @GetMapping("/list")
    public String listRooms(Model model) {
        List<Room> rooms = roomService.getAllRooms();
        model.addAttribute("rooms", rooms);
        //
        return "room-list";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Room room = roomService.getRoomById(id);
        RoomDTO roomDTO = convertToDTO(room);
        model.addAttribute("room", roomDTO);
        return "edit-room";
    }

//    @PostMapping("/edit/{id}")
//    public String updateRoom(@PathVariable Long id, @ModelAttribute Room room) {
//        roomService.updateRoom(id, room);
//        return "redirect:/rooms/list";
//    }
    @GetMapping("/delete/{id}")
    public String deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return "redirect:/rooms/list";
    }

    private RoomDTO convertToDTO(Room room) {
        RoomDTO dto = new RoomDTO();
        dto.setName(room.getName());
        dto.setBuilding(room.getBuilding());
        dto.setCapacity(room.getCapacity());
        dto.setType(room.getType());
        dto.setLocation(room.getLocation());

        Set<String> availabilitySlots = new HashSet<>();
        if (room.getAvailability() != null) {
            for (RoomAvailability avail : room.getAvailability()) {
                String day = avail.getDay().toString();
                LocalTime start = avail.getStartTime();

                // Determine which slot this is
                int slotNum = 0;
                if (start.equals(LocalTime.of(9, 0))
                        || (start.isAfter(LocalTime.of(8, 30)) && start.isBefore(LocalTime.of(9, 30)))) {
                    slotNum = 1;
                } else if (start.equals(LocalTime.of(12, 0))
                        || (start.isAfter(LocalTime.of(11, 30)) && start.isBefore(LocalTime.of(12, 30)))) {
                    slotNum = 2;
                } else if (start.equals(LocalTime.of(15, 0))
                        || (start.isAfter(LocalTime.of(14, 30)) && start.isBefore(LocalTime.of(15, 30)))) {
                    slotNum = 3;
                } else if (start.equals(LocalTime.of(18, 0))
                        || (start.isAfter(LocalTime.of(17, 30)) && start.isBefore(LocalTime.of(18, 30)))) {
                    slotNum = 4;
                }

                if (slotNum > 0) {
                    availabilitySlots.add(day + "_SLOT" + slotNum);
                }
            }
        }

        dto.setAvailability(availabilitySlots);
        return dto;
    }

    @GetMapping("/api/all")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllRoomsApi() {
        try {
            List<Room> rooms = roomService.getAllRooms();

            List<Map<String, Object>> roomData = rooms.stream()
                    .map(room -> {
                        Map<String, Object> roomInfo = new HashMap<>();
                        roomInfo.put("id", room.getId());
                        roomInfo.put("name", room.getName() != null ? room.getName() : "Αίθουσα " + room.getId());
                        roomInfo.put("building", room.getBuilding() != null ? room.getBuilding() : "Κτήριο A");
                        roomInfo.put("capacity", room.getCapacity() != null ? room.getCapacity() : 50);
                        roomInfo.put("type", room.getType() != null ? room.getType().toString() : "TEACHING");
                        roomInfo.put("location", room.getLocation() != null ? room.getLocation() : "");
                        roomInfo.put("active", true); 
                        return roomInfo;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(roomData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    
    @GetMapping("/api/filter")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getFilteredRoomsApi(
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer minCapacity) {
        try {
            List<Room> rooms = roomService.getAllRooms();

            List<Room> filteredRooms = rooms.stream()
                    .filter(room -> building == null || building.isEmpty()
                    || (room.getBuilding() != null && room.getBuilding().contains(building)))
                    .filter(room -> type == null || type.isEmpty()
                    || (room.getType() != null && room.getType().toString().equals(type)))
                    .filter(room -> minCapacity == null
                    || (room.getCapacity() != null && room.getCapacity() >= minCapacity))
                    .collect(Collectors.toList());

            List<Map<String, Object>> roomData = filteredRooms.stream()
                    .map(room -> {
                        Map<String, Object> roomInfo = new HashMap<>();
                        roomInfo.put("id", room.getId());
                        roomInfo.put("name", room.getName());
                        roomInfo.put("building", room.getBuilding());
                        roomInfo.put("capacity", room.getCapacity());
                        roomInfo.put("type", room.getType().toString());
                        roomInfo.put("location", room.getLocation());
                        return roomInfo;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(roomData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRoomByIdApi(@PathVariable Long id) {
        try {
            Room room = roomService.getRoomById(id);

            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("id", room.getId());
            roomInfo.put("name", room.getName());
            roomInfo.put("building", room.getBuilding());
            roomInfo.put("capacity", room.getCapacity());
            roomInfo.put("type", room.getType().toString());
            roomInfo.put("location", room.getLocation());

            // Add availability info if needed
            if (room.getAvailability() != null) {
                List<Map<String, Object>> availability = room.getAvailability().stream()
                        .map(avail -> {
                            Map<String, Object> availInfo = new HashMap<>();
                            availInfo.put("day", avail.getDay().toString());
                            availInfo.put("startTime", avail.getStartTime().toString());
                            availInfo.put("endTime", avail.getEndTime().toString());
                            return availInfo;
                        })
                        .collect(Collectors.toList());
                roomInfo.put("availability", availability);
            }

            return ResponseEntity.ok(roomInfo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
   
    @GetMapping("/api/available")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAvailableRoomsApi(
            @RequestParam String day,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        try {
            //μετατροπή string σε DayOfWeek και LocalTime
            java.time.DayOfWeek dayOfWeek = DayOfWeek.valueOf(day.toUpperCase());
            LocalTime start = LocalTime.parse(startTime);
            LocalTime end = LocalTime.parse(endTime);
            
            //λήψη διαθέσιμων αιθουσών
            List<Room> availableRooms = roomService.getAvailableRoomsByDayAndTime(dayOfWeek, start, end);
            
            //μετατροπή σε map για json response
            List<Map<String, Object>> roomData = availableRooms.stream()
                    .map(room -> {
                        Map<String, Object> roomInfo = new HashMap<>();
                        roomInfo.put("id", room.getId());
                        roomInfo.put("name", room.getName() != null ? room.getName() : "Αίθουσα " + room.getId());
                        roomInfo.put("building", room.getBuilding() != null ? room.getBuilding() : "Κτήριο A");
                        roomInfo.put("capacity", room.getCapacity() != null ? room.getCapacity() : 50);
                        roomInfo.put("type", room.getType() != null ? room.getType().toString() : "TEACHING");
                        roomInfo.put("location", room.getLocation() != null ? room.getLocation() : "");
                        roomInfo.put("active", true);
                        return roomInfo;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(roomData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>());
        }
    }


}
