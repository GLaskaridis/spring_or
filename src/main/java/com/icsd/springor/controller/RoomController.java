package com.icsd.springor.controller;

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

    @GetMapping("/add")
    public String showAddRoomForm(Model model) {
        model.addAttribute("room", new RoomDTO());
        return "add_room";
    }

    @PostMapping("/add")
    public String addRoom(@ModelAttribute RoomDTO roomDTO, RedirectAttributes redirectAttributes) {
        try {
            Room room = roomDTO.toEntity();
            System.out.println("Αποθήκευση αίθουσας: " + room);
            System.out.println("Διαθεσιμότητα: " + room.getAvailability());
            roomService.addRoom(room);
            redirectAttributes.addFlashAttribute("message", "Η αίθουσα προστέθηκε επιτυχώς!");
            return "redirect:/rooms/list";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Σφάλμα κατά την προσθήκη αίθουσας: " + e.getMessage());
            return "redirect:/rooms/add";
        }
    }

    @GetMapping("/list")
    public String listRooms(Model model) {
        List<Room> rooms = roomService.getAllRooms();
        model.addAttribute("rooms", rooms);
        return "room-list";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Room room = roomService.getRoomById(id);
            RoomDTO roomDTO = RoomDTO.fromEntity(room);
            roomDTO.setId(id);
            model.addAttribute("room", roomDTO);
            return "edit-room";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Η αίθουσα δεν βρέθηκε: " + e.getMessage());
            return "redirect:/rooms/list";
        }
    }

    @PostMapping("/edit/{id}")
    public String updateRoom(@PathVariable Long id, @ModelAttribute RoomDTO roomDTO, RedirectAttributes redirectAttributes) {
        try {
            Room room = roomDTO.toEntity();
            room.setId(id);
            roomService.updateRoom(id, room);
            redirectAttributes.addFlashAttribute("message", "Η αίθουσα ενημερώθηκε επιτυχώς!");
            return "redirect:/rooms/list";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Σφάλμα κατά την ενημέρωση: " + e.getMessage());
            return "redirect:/rooms/edit/" + id;
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteRoom(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            roomService.deleteRoom(id);
            redirectAttributes.addFlashAttribute("message", "Η αίθουσα διαγράφηκε επιτυχώς!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Σφάλμα κατά τη διαγραφή: " + e.getMessage());
        }
        return "redirect:/rooms/list";
    }
    
    //api endpoint για διαγραφή (για ajax calls)
    @DeleteMapping("/api/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteRoomApi(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            roomService.deleteRoom(id);
            response.put("success", true);
            response.put("message", "Η αίθουσα διαγράφηκε επιτυχώς!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Σφάλμα: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
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
                        roomInfo.put("active", room.isActive());
                        
                        //προσθήκη διαθεσιμότητας
                        if (room.getAvailability() != null) {
                            List<Map<String, String>> availability = room.getAvailability().stream()
                                .map(avail -> {
                                    Map<String, String> availInfo = new HashMap<>();
                                    availInfo.put("day", avail.getDay().toString());
                                    availInfo.put("startTime", avail.getStartTime().toString());
                                    availInfo.put("endTime", avail.getEndTime().toString());
                                    return availInfo;
                                })
                                .collect(Collectors.toList());
                            roomInfo.put("availability", availability);
                        }
                        
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
            roomInfo.put("active", room.isActive());

            //προσθήκη διαθεσιμότητας
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
            DayOfWeek dayOfWeek = DayOfWeek.valueOf(day.toUpperCase());
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
                        roomInfo.put("active", room.isActive());
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