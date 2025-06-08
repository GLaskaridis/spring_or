/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.icsd.springor.controller;

import com.icsd.springor.DTO.RoomDTO;
import com.icsd.springor.model.Room;
import com.icsd.springor.model.RoomAvailability;
import com.icsd.springor.service.RoomService;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
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

}
