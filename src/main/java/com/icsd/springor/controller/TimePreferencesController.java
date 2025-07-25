/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.controller;

import com.icsd.springor.DTO.AssignmentDTO;
import com.icsd.springor.DTO.TeacherPreferenceDTO;
import com.icsd.springor.model.TeacherPreference;
import com.icsd.springor.model.RoomType;
import com.icsd.springor.service.AssignmentService;
import com.icsd.springor.service.TeacherPreferenceService;
import com.icsd.springor.service.RoomService;
import com.icsd.springor.service.UserService;
import com.icsd.springor.service.CourseScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/time-preferences")
public class TimePreferencesController {
    
    @Autowired
    private AssignmentService assignmentService;
    
    @Autowired
    private TeacherPreferenceService preferenceService;
    
    @Autowired
    private RoomService roomService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CourseScheduleService scheduleService;
    
    // Teacher interface - Show assigned courses and allow time/room preferences
    @GetMapping("/my-assignments/{scheduleId}")
    public String showMyAssignments(@PathVariable Long scheduleId, 
                                  Model model, 
                                  Authentication authentication) {
        try {
            Long teacherId = userService.getCurrentUserId(authentication);
            
            var schedule = scheduleService.getScheduleById(scheduleId);
            var myAssignments = assignmentService.getAssignmentsByTeacherAndSchedule(teacherId, scheduleId);
            var myPreferences = preferenceService.getPreferencesByTeacherAndSchedule(teacherId, scheduleId);
            var rooms = roomService.getAllRooms();
            
            model.addAttribute("schedule", schedule);
            model.addAttribute("assignments", myAssignments);
            model.addAttribute("preferences", myPreferences);
            model.addAttribute("rooms", rooms);
            model.addAttribute("preferenceTypes", TeacherPreference.PreferenceType.values());
            model.addAttribute("roomTypes", RoomType.values());
            model.addAttribute("daysOfWeek", DayOfWeek.values());
            model.addAttribute("timeSlots", getTimeSlots());
            
            return "teacher-time-preferences";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading assignments: " + e.getMessage());
            return "error";
        }
    }
    
    // Create detailed time and room preference for an assignment
    @PostMapping("/create")
    public String createTimePreference(@RequestParam Long assignmentId,
                                     @RequestParam TeacherPreference.PreferenceType type,
                                     @RequestParam(required = false) DayOfWeek preferredDay,
                                     @RequestParam(required = false) Integer preferredSlot,
                                     @RequestParam(required = false) Long preferredRoomId,
                                     @RequestParam(required = false) RoomType preferredRoomType,
                                     @RequestParam(required = false) Integer minCapacity,
                                     @RequestParam(required = false) Integer maxCapacity,
                                     @RequestParam(required = false) Integer preferenceWeight,
                                     @RequestParam(required = false) String notes,
                                     @RequestParam Long scheduleId,
                                     RedirectAttributes redirectAttributes) {
        try {
            TeacherPreferenceDTO preferenceDTO = new TeacherPreferenceDTO();
            preferenceDTO.setAssignmentId(assignmentId);
            preferenceDTO.setType(type);
            preferenceDTO.setPreferredDay(preferredDay);
            preferenceDTO.setPreferredSlot(preferredSlot);
            
            if (preferredSlot != null) {
                preferenceDTO.setPreferredSlot(preferredSlot);
            }
            
            preferenceDTO.setPreferredRoomId(preferredRoomId);
            preferenceDTO.setPreferredRoomType(preferredRoomType);
            preferenceDTO.setMinCapacity(minCapacity);
            preferenceDTO.setMaxCapacity(maxCapacity);
            preferenceDTO.setPreferenceWeight(preferenceWeight != null ? preferenceWeight : 5);
            preferenceDTO.setNotes(notes);
            
            preferenceService.createPreference(preferenceDTO);
            redirectAttributes.addFlashAttribute("message", "Time preference created successfully");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating preference: " + e.getMessage());
        }
        
        return "redirect:/time-preferences/my-assignments/" + scheduleId;
    }
    
    // Update existing preference
    @PostMapping("/update/{preferenceId}")
    public String updateTimePreference(@PathVariable Long preferenceId,
                                     @RequestParam TeacherPreference.PreferenceType type,
                                     @RequestParam(required = false) DayOfWeek preferredDay,
                                     @RequestParam(required = false) Integer preferredSlot,
                                     @RequestParam(required = false) String preferredStartTime,
                                     @RequestParam(required = false) String preferredEndTime,
                                     @RequestParam(required = false) Long preferredRoomId,
                                     @RequestParam(required = false) RoomType preferredRoomType,
                                     @RequestParam(required = false) Integer minCapacity,
                                     @RequestParam(required = false) Integer maxCapacity,
                                     @RequestParam(required = false) Integer preferenceWeight,
                                     @RequestParam(required = false) String notes,
                                     @RequestParam Long scheduleId,
                                     RedirectAttributes redirectAttributes) {
        try {
            TeacherPreferenceDTO preferenceDTO = new TeacherPreferenceDTO();
            preferenceDTO.setType(type);
            preferenceDTO.setPreferredDay(preferredDay);
            preferenceDTO.setPreferredSlot(preferredSlot);
            
            // Parse time strings if provided
            if (preferredSlot != null ) {
                preferenceDTO.setPreferredSlot(preferredSlot);
            }
            preferenceDTO.setPreferredRoomId(preferredRoomId);
            preferenceDTO.setPreferredRoomType(preferredRoomType);
            preferenceDTO.setMinCapacity(minCapacity);
            preferenceDTO.setMaxCapacity(maxCapacity);
            preferenceDTO.setPreferenceWeight(preferenceWeight != null ? preferenceWeight : 5);
            preferenceDTO.setNotes(notes);
            
            preferenceService.updatePreference(preferenceId, preferenceDTO);
            redirectAttributes.addFlashAttribute("message", "Time preference updated successfully");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating preference: " + e.getMessage());
        }
        
        return "redirect:/time-preferences/my-assignments/" + scheduleId;
    }
    
    // Delete preference
    @GetMapping("/delete/{preferenceId}")
    public String deleteTimePreference(@PathVariable Long preferenceId,
                                     @RequestParam Long scheduleId,
                                     RedirectAttributes redirectAttributes) {
        try {
            preferenceService.deletePreference(preferenceId);
            redirectAttributes.addFlashAttribute("message", "Time preference deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting preference: " + e.getMessage());
        }
        
        return "redirect:/time-preferences/my-assignments/" + scheduleId;
    }
    
    // Quick preference setup using slot system
    @PostMapping("/quick-setup")
    public String quickSetupPreferences(@RequestParam Long assignmentId,
                                      @RequestParam DayOfWeek preferredDay,
                                      @RequestParam Integer preferredSlot,
                                      @RequestParam Integer preferenceWeight,
                                      @RequestParam Long scheduleId,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        try {
            TeacherPreferenceDTO preferenceDTO = new TeacherPreferenceDTO();
            preferenceDTO.setAssignmentId(assignmentId);
            preferenceDTO.setType(TeacherPreference.PreferenceType.PREFERENCE);
            preferenceDTO.setPreferredDay(preferredDay);
            preferenceDTO.setPreferredSlot(preferredSlot);
            preferenceDTO.setPreferenceWeight(preferenceWeight);
            
            // Set automatic time slots based on slot number
            switch (preferredSlot) {
                case 0:
                    preferenceDTO.setPreferredStartTime(LocalTime.of(9, 0));
                    preferenceDTO.setPreferredEndTime(LocalTime.of(12, 0));
                    break;
                case 1:
                    preferenceDTO.setPreferredStartTime(LocalTime.of(12, 0));
                    preferenceDTO.setPreferredEndTime(LocalTime.of(15, 0));
                    break;
                case 2:
                    preferenceDTO.setPreferredStartTime(LocalTime.of(15, 0));
                    preferenceDTO.setPreferredEndTime(LocalTime.of(18, 0));
                    break;
                case 3:
                    preferenceDTO.setPreferredStartTime(LocalTime.of(18, 0));
                    preferenceDTO.setPreferredEndTime(LocalTime.of(21, 0));
                    break;
            }
            
            preferenceService.createPreference(preferenceDTO);
            redirectAttributes.addFlashAttribute("message", "Quick preference setup completed");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error setting up preference: " + e.getMessage());
        }
        
        return "redirect:/time-preferences/my-assignments/" + scheduleId;
    }
    
    // Admin view - see all preferences for schedule execution
    @GetMapping("/admin/overview/{scheduleId}")
    public String adminOverview(@PathVariable Long scheduleId, Model model) {
        try {
            var schedule = scheduleService.getScheduleById(scheduleId);
            var allAssignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            var allPreferences = preferenceService.getPreferencesBySchedule(scheduleId);
            
            model.addAttribute("schedule", schedule);
            model.addAttribute("assignments", allAssignments);
            model.addAttribute("preferences", allPreferences);
            
            // Check if all assignments have preferences
            boolean allAssignmentsHavePreferences = preferenceService.areAllPreferencesProvided(scheduleId);
            model.addAttribute("readyForExecution", allAssignmentsHavePreferences);
            
            return "admin-time-preferences-overview";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading preferences overview: " + e.getMessage());
            return "error";
        }
    }
    
    // Helper method to provide time slot options
    private List<Map<String, Object>> getTimeSlots() {
        return List.of(
            Map.of("value", 0, "label", "09:00 - 12:00", "startTime", "09:00", "endTime", "12:00"),
            Map.of("value", 1, "label", "12:00 - 15:00", "startTime", "12:00", "endTime", "15:00"),
            Map.of("value", 2, "label", "15:00 - 18:00", "startTime", "15:00", "endTime", "18:00"),
            Map.of("value", 3, "label", "18:00 - 21:00", "startTime", "18:00", "endTime", "21:00")
        );
    }
    
    // REST API methods
    @GetMapping("/api/assignments/{assignmentId}/preferences")
    @ResponseBody
    public ResponseEntity<List<TeacherPreferenceDTO>> getPreferencesForAssignment(@PathVariable Long assignmentId) {
        try {
            List<TeacherPreferenceDTO> preferences = preferenceService.getPreferencesByAssignment(assignmentId);
            return ResponseEntity.ok(preferences);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}