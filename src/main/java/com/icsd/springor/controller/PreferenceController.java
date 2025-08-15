/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.controller;

import com.icsd.springor.DTO.AssignmentDTO;
import com.icsd.springor.DTO.RoomPreferenceDTO;
import com.icsd.springor.DTO.TimeSlotPreferenceDTO;
import com.icsd.springor.model.Room;
import com.icsd.springor.model.RoomType;
import com.icsd.springor.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/preferences")
public class PreferenceController {
    
    @Autowired
    private AssignmentService assignmentService;
    
    @Autowired
    private TimeSlotPreferenceService timeSlotService;
    
    @Autowired
    private RoomPreferenceService roomPreferenceService;
    
    @Autowired
    private RoomService roomService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CourseScheduleService scheduleService;
    
    // Teacher view - Show all preferences for assignments in a schedule
    @GetMapping("/schedules/{scheduleId}/my-preferences")
    @PreAuthorize("hasRole('TEACHER')")
    public String showMyPreferences(@PathVariable Long scheduleId, 
                                  Model model, 
                                  Authentication authentication) {
        try {
            Long teacherId = userService.getCurrentUserId(authentication);
            
            var schedule = scheduleService.getScheduleById(scheduleId);
            var myAssignments = assignmentService.getAssignmentsByTeacherAndSchedule(teacherId, scheduleId);
            var timeSlotPrefs = timeSlotService.getPreferencesByTeacherAndSchedule(teacherId, scheduleId);
            var roomPrefs = roomPreferenceService.getPreferencesByTeacherAndSchedule(teacherId, scheduleId);
            var availableRooms = roomService.getAllRooms();
            
            model.addAttribute("schedule", schedule);
            model.addAttribute("assignments", myAssignments);
            model.addAttribute("timeSlotPreferences", timeSlotPrefs);
            model.addAttribute("roomPreferences", roomPrefs);
            model.addAttribute("availableRooms", availableRooms);
            model.addAttribute("daysOfWeek", DayOfWeek.values());
            model.addAttribute("timeSlots", getTimeSlotOptions());
            model.addAttribute("roomTypes", RoomType.values());
            
            return "teacher-preferences";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading preferences: " + e.getMessage());
            return "error";
        }
    }
    
    // Teacher view - Detailed preference form for specific assignment
    @GetMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public String showAssignmentPreferences(@PathVariable Long assignmentId, 
                                          Model model, 
                                          Authentication authentication) {
        try {
            Long teacherId = userService.getCurrentUserId(authentication);
            
            // Verify assignment belongs to teacher
            var myAssignments = assignmentService.getAssignmentsByTeacher(teacherId);
            var assignment = myAssignments.stream()
                .filter(a -> a.getId().equals(assignmentId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Assignment not found or access denied"));
            
            var timeSlotPrefs = timeSlotService.getPreferencesByAssignment(assignmentId);
            var roomPrefs = roomPreferenceService.getPreferencesByAssignment(assignmentId);
            
            // Filter rooms by course component type
            var allRooms = roomService.getAllRooms();
            var suitableRooms = allRooms.stream()
                .filter(room -> {
                    if (assignment.getCourseComponent() == com.icsd.springor.model.Course.TeachingHours.CourseComponent.LABORATORY) {
                        return room.getType() == RoomType.LABORATORY;
                    } else {
                        return room.getType() == RoomType.TEACHING;
                    }
                })
                .collect(Collectors.toList());
            
            model.addAttribute("assignment", assignment);
            model.addAttribute("timeSlotPreferences", timeSlotPrefs);
            model.addAttribute("roomPreferences", roomPrefs);
            model.addAttribute("allRooms", allRooms);
            model.addAttribute("suitableRooms", suitableRooms);
            model.addAttribute("daysOfWeek", DayOfWeek.values());
            model.addAttribute("timeSlots", getTimeSlotOptions());
            
            return "assignment-preferences";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading assignment preferences: " + e.getMessage());
            return "error";
        }
    }
    
    // Program Manager view - Overview of all preferences for a schedule
    @GetMapping("/schedules/{scheduleId}/overview")
    @PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public String showPreferencesOverview(@PathVariable Long scheduleId, Model model) {
        try {
            var schedule = scheduleService.getScheduleById(scheduleId);
            var allAssignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            var allTimeSlotPrefs = timeSlotService.getAllPreferencesForSchedule(scheduleId);
            var allRoomPrefs = roomPreferenceService.getAllPreferencesForSchedule(scheduleId);
            
            // Group preferences by assignment for easier viewing
            var timeSlotPrefsByAssignment = allTimeSlotPrefs.stream()
                .collect(Collectors.groupingBy(TimeSlotPreferenceDTO::getAssignmentId));
            var roomPrefsByAssignment = allRoomPrefs.stream()
                .collect(Collectors.groupingBy(RoomPreferenceDTO::getAssignmentId));
            
            model.addAttribute("schedule", schedule);
            model.addAttribute("assignments", allAssignments);
            model.addAttribute("timeSlotPrefsByAssignment", timeSlotPrefsByAssignment);
            model.addAttribute("roomPrefsByAssignment", roomPrefsByAssignment);
            
            return "admin-preferences-overview";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading preferences overview: " + e.getMessage());
            return "error";
        }
    }
    
    // === TIME SLOT PREFERENCES ===
    
    @PostMapping("/assignments/{assignmentId}/time-slots")
    @PreAuthorize("hasRole('TEACHER')")
    public String addTimeSlotPreference(@PathVariable Long assignmentId,
                                      @RequestParam DayOfWeek day,
                                      @RequestParam Integer slot,
                                      @RequestParam Integer weight,
                                      @RequestParam(required = false) String notes,
                                      RedirectAttributes redirectAttributes) {
        try {
            timeSlotService.addTimeSlotPreference(assignmentId, day, slot, weight, notes);
            redirectAttributes.addFlashAttribute("message", "Time slot preference added successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding time preference: " + e.getMessage());
        }
        
        return "redirect:/preferences/assignments/" + assignmentId;
    }
    
    @PostMapping("/time-slots/{preferenceId}")
    @PreAuthorize("hasRole('TEACHER')")
    public String updateTimeSlotPreference(@PathVariable Long preferenceId,
                                         @RequestParam DayOfWeek day,
                                         @RequestParam Integer slot,
                                         @RequestParam Integer weight,
                                         @RequestParam(required = false) String notes,
                                         @RequestParam Long assignmentId,
                                         RedirectAttributes redirectAttributes) {
        try {
            timeSlotService.updatePreference(preferenceId, day, slot, weight, notes);
            redirectAttributes.addFlashAttribute("message", "Time slot preference updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating time preference: " + e.getMessage());
        }
        
        return "redirect:/preferences/assignments/" + assignmentId;
    }
    
    @PostMapping("/time-slots/{preferenceId}/delete")
    @PreAuthorize("hasRole('TEACHER')")
    public String deleteTimeSlotPreference(@PathVariable Long preferenceId,
                                         @RequestParam Long assignmentId,
                                         RedirectAttributes redirectAttributes) {
        try {
            timeSlotService.deletePreference(preferenceId);
            redirectAttributes.addFlashAttribute("message", "Time slot preference deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting time preference: " + e.getMessage());
        }
        
        return "redirect:/preferences/assignments/" + assignmentId;
    }
    
    // === ROOM PREFERENCES ===
    
    @PostMapping("/assignments/{assignmentId}/rooms")
    @PreAuthorize("hasRole('TEACHER')")
    public String addRoomPreference(@PathVariable Long assignmentId,
                                  @RequestParam Long roomId,
                                  @RequestParam Integer weight,
                                  @RequestParam(required = false) String notes,
                                  RedirectAttributes redirectAttributes) {
        try {
            roomPreferenceService.addRoomPreference(assignmentId, roomId, weight, notes);
            redirectAttributes.addFlashAttribute("message", "Room preference added successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding room preference: " + e.getMessage());
        }
        
        return "redirect:/preferences/assignments/" + assignmentId;
    }
    
    @PostMapping("/rooms/{preferenceId}")
    @PreAuthorize("hasRole('TEACHER')")
    public String updateRoomPreference(@PathVariable Long preferenceId,
                                     @RequestParam Integer weight,
                                     @RequestParam(required = false) String notes,
                                     @RequestParam Long assignmentId,
                                     RedirectAttributes redirectAttributes) {
        try {
            roomPreferenceService.updatePreference(preferenceId, weight, notes);
            redirectAttributes.addFlashAttribute("message", "Room preference updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating room preference: " + e.getMessage());
        }
        
        return "redirect:/preferences/assignments/" + assignmentId;
    }
    
    @PostMapping("/rooms/{preferenceId}/delete")
    @PreAuthorize("hasRole('TEACHER')")
    public String deleteRoomPreference(@PathVariable Long preferenceId,
                                     @RequestParam Long assignmentId,
                                     RedirectAttributes redirectAttributes) {
        try {
            roomPreferenceService.deletePreference(preferenceId);
            redirectAttributes.addFlashAttribute("message", "Room preference deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting room preference: " + e.getMessage());
        }
        
        return "redirect:/preferences/assignments/" + assignmentId;
    }
    
    // === REST API ENDPOINTS ===
    
    @GetMapping("/api/assignments/{assignmentId}/time-slots")
    @ResponseBody
    public ResponseEntity<List<TimeSlotPreferenceDTO>> getTimeSlotPreferences(@PathVariable Long assignmentId) {
        try {
            List<TimeSlotPreferenceDTO> preferences = timeSlotService.getPreferencesByAssignment(assignmentId);
            return ResponseEntity.ok(preferences);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/api/assignments/{assignmentId}/rooms")
    @ResponseBody
    public ResponseEntity<List<RoomPreferenceDTO>> getRoomPreferences(@PathVariable Long assignmentId) {
        try {
            List<RoomPreferenceDTO> preferences = roomPreferenceService.getPreferencesByAssignment(assignmentId);
            return ResponseEntity.ok(preferences);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/api/assignments/{assignmentId}/time-slots")
    @ResponseBody
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> addTimeSlotPreferenceAPI(@PathVariable Long assignmentId,
                                                    @RequestBody Map<String, Object> request) {
        try {
            DayOfWeek day = DayOfWeek.valueOf(request.get("day").toString());
            Integer slot = Integer.valueOf(request.get("slot").toString());
            Integer weight = Integer.valueOf(request.get("weight").toString());
            String notes = request.get("notes") != null ? request.get("notes").toString() : null;
            
            TimeSlotPreferenceDTO preference = timeSlotService.addTimeSlotPreference(assignmentId, day, slot, weight, notes);
            return ResponseEntity.ok(preference);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/api/assignments/{assignmentId}/rooms")
    @ResponseBody
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> addRoomPreferenceAPI(@PathVariable Long assignmentId,
                                                @RequestBody Map<String, Object> request) {
        try {
            Long roomId = Long.valueOf(request.get("roomId").toString());
            Integer weight = Integer.valueOf(request.get("weight").toString());
            String notes = request.get("notes") != null ? request.get("notes").toString() : null;
            
            RoomPreferenceDTO preference = roomPreferenceService.addRoomPreference(assignmentId, roomId, weight, notes);
            return ResponseEntity.ok(preference);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/assignments/{assignmentId}/bulk-time-slots")
    @PreAuthorize("hasRole('TEACHER')")
    public String addBulkTimeSlotPreferences(@PathVariable Long assignmentId,
                                           @RequestParam String[] days,
                                           @RequestParam Integer[] slots,
                                           @RequestParam Integer[] weights,
                                           RedirectAttributes redirectAttributes) {
        try {
            int added = 0;
            
            for (int i = 0; i < days.length; i++) {
                try {
                    DayOfWeek day = DayOfWeek.valueOf(days[i]);
                    Integer slot = slots[i];
                    Integer weight = weights[i];
                    
                    timeSlotService.addTimeSlotPreference(assignmentId, day, slot, weight, null);
                    added++;
                } catch (Exception e) {
                    // Skip invalid entries
                    System.out.println("Skipping invalid time slot preference: " + e.getMessage());
                }
            }
            
            redirectAttributes.addFlashAttribute("message", 
                "Added " + added + " time slot preferences successfully");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding bulk time preferences: " + e.getMessage());
        }
        
        return "redirect:/preferences/assignments/" + assignmentId;
    }
    
    @PostMapping("/assignments/{assignmentId}/bulk-rooms")
    @PreAuthorize("hasRole('TEACHER')")
    public String addBulkRoomPreferences(@PathVariable Long assignmentId,
                                       @RequestParam Long[] roomIds,
                                       @RequestParam Integer[] weights,
                                       RedirectAttributes redirectAttributes) {
        try {
            int added = 0;
            
            for (int i = 0; i < roomIds.length; i++) {
                try {
                    Long roomId = roomIds[i];
                    Integer weight = weights[i];
                    
                    roomPreferenceService.addRoomPreference(assignmentId, roomId, weight, null);
                    added++;
                } catch (Exception e) {
                    // Skip invalid entries
                    System.out.println("Skipping invalid room preference: " + e.getMessage());
                }
            }
            
            redirectAttributes.addFlashAttribute("message", 
                "Added " + added + " room preferences successfully");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding bulk room preferences: " + e.getMessage());
        }
        
        return "redirect:/preferences/assignments/" + assignmentId;
    }
    
    // === HELPER METHODS ===
    
    private List<Map<String, Object>> getTimeSlotOptions() {
        return List.of(
            Map.of("value", 0, "label", "Πρωινό (09:00-12:00)"),
            Map.of("value", 1, "label", "Μεσημεριανό (12:00-15:00)"),
            Map.of("value", 2, "label", "Απογευματινό (15:00-18:00)"),
            Map.of("value", 3, "label", "Βραδινό (18:00-21:00)")
        );
    }
}
