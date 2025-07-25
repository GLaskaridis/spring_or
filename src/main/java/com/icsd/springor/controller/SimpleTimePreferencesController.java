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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;


@Controller
@RequestMapping("/time-preferences")
public class SimpleTimePreferencesController {
    
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
    
    
    @GetMapping("/my-assignments/{scheduleId}")
    @PreAuthorize("hasRole('TEACHER')")
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
            
            return "teacher-simple-time-preferences";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading assignments: " + e.getMessage());
            return "error";
        }
    }
    
    
    @PostMapping("/create-simple")
    public String createSimpleTimePreference(@RequestParam Long assignmentId,
                                           @RequestParam DayOfWeek preferredDay,
                                           @RequestParam Integer preferredSlot,
                                           @RequestParam(required = false) Integer preferenceWeight,
                                           @RequestParam(required = false) String notes,
                                           @RequestParam Long scheduleId,
                                           RedirectAttributes redirectAttributes) {
        try {
            TeacherPreferenceDTO preferenceDTO = new TeacherPreferenceDTO();
            preferenceDTO.setAssignmentId(assignmentId);
            preferenceDTO.setType(TeacherPreference.PreferenceType.PREFERENCE);
            preferenceDTO.setPreferredDay(preferredDay);
            preferenceDTO.setPreferredSlot(preferredSlot);
            preferenceDTO.setPreferenceWeight(preferenceWeight != null ? preferenceWeight : 5);
            preferenceDTO.setNotes(notes);
            
            preferenceService.createPreference(preferenceDTO);
            redirectAttributes.addFlashAttribute("message", "Time preference created successfully");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating preference: " + e.getMessage());
        }
        
        return "redirect:/time-preferences/my-assignments/" + scheduleId;
    }
    
    
    @PostMapping("/update-simple/{preferenceId}")
    public String updateSimpleTimePreference(@PathVariable Long preferenceId,
                                           @RequestParam DayOfWeek preferredDay,
                                           @RequestParam Integer preferredSlot,
                                           @RequestParam(required = false) Integer preferenceWeight,
                                           @RequestParam(required = false) String notes,
                                           @RequestParam Long scheduleId,
                                           RedirectAttributes redirectAttributes) {
        try {
            TeacherPreferenceDTO preferenceDTO = new TeacherPreferenceDTO();
            preferenceDTO.setType(TeacherPreference.PreferenceType.PREFERENCE);
            preferenceDTO.setPreferredDay(preferredDay);
            preferenceDTO.setPreferredSlot(preferredSlot);
            preferenceDTO.setPreferenceWeight(preferenceWeight != null ? preferenceWeight : 5);
            preferenceDTO.setNotes(notes);
            
            preferenceService.updatePreference(preferenceId, preferenceDTO);
            redirectAttributes.addFlashAttribute("message", "Time preference updated successfully");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating preference: " + e.getMessage());
        }
        
        return "redirect:/time-preferences/my-assignments/" + scheduleId;
    }
    
    
    @PostMapping("/bulk-setup")
    public String bulkSetupPreferences(@RequestParam DayOfWeek preferredDay,
                                     @RequestParam Integer preferredSlot,
                                     @RequestParam Integer preferenceWeight,
                                     @RequestParam Long scheduleId,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            Long teacherId = userService.getCurrentUserId(authentication);
            var myAssignments = assignmentService.getAssignmentsByTeacherAndSchedule(teacherId, scheduleId);
            
            int created = 0;
            for (AssignmentDTO assignment : myAssignments) {
                try {
                    TeacherPreferenceDTO preferenceDTO = new TeacherPreferenceDTO();
                    preferenceDTO.setAssignmentId(assignment.getId());
                    preferenceDTO.setType(TeacherPreference.PreferenceType.PREFERENCE);
                    preferenceDTO.setPreferredDay(preferredDay);
                    preferenceDTO.setPreferredSlot(preferredSlot);
                    preferenceDTO.setPreferenceWeight(preferenceWeight);
                    
                    preferenceService.createPreference(preferenceDTO);
                    created++;
                } catch (Exception e) {
                    // Skip if preference already exists
                    System.out.println("Skipping assignment " + assignment.getId() + ": " + e.getMessage());
                }
            }
            
            redirectAttributes.addFlashAttribute("message", 
                "Bulk setup completed. Created " + created + " preferences.");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error in bulk setup: " + e.getMessage());
        }
        
        return "redirect:/time-preferences/my-assignments/" + scheduleId;
    }
    
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
    
    @GetMapping("/admin/overview/{scheduleId}")
    @PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public String adminOverview(@PathVariable Long scheduleId, Model model) {
        try {
            var schedule = scheduleService.getScheduleById(scheduleId);
            var allAssignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            var allPreferences = preferenceService.getPreferencesBySchedule(scheduleId);
            
            model.addAttribute("schedule", schedule);
            model.addAttribute("assignments", allAssignments);
            model.addAttribute("preferences", allPreferences);
            model.addAttribute("timeSlots", getTimeSlots());
            
            // Check if all assignments have preferences
            boolean allAssignmentsHavePreferences = preferenceService.areAllPreferencesProvided(scheduleId);
            model.addAttribute("readyForExecution", allAssignmentsHavePreferences);
            
            return "admin-simple-time-preferences-overview";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading preferences overview: " + e.getMessage());
            return "error";
        }
    }
    
    
    private List<Map<String, Object>> getTimeSlots() {
        return List.of(
            Map.of(
                "value", 0, 
                "label", "Πρωινό (09:00 - 12:00)",
                "shortLabel", "09:00-12:00"
            ),
            Map.of(
                "value", 1, 
                "label", "Μεσημεριανό (12:00 - 15:00)",
                "shortLabel", "12:00-15:00"
            ),
            Map.of(
                "value", 2, 
                "label", "Απογευματινό (15:00 - 18:00)",
                "shortLabel", "15:00-18:00"
            ),
            Map.of(
                "value", 3, 
                "label", "Βραδινό (18:00 - 21:00)",
                "shortLabel", "18:00-21:00"
            )
        );
    }
    
    
    public static String getSlotLabel(Integer slot) {
        if (slot == null) return "Δεν έχει οριστεί";
        
        switch (slot) {
            case 0: return "Πρωινό (09:00-12:00)";
            case 1: return "Μεσημεριανό (12:00-15:00)";
            case 2: return "Απογευματινό (15:00-18:00)";
            case 3: return "Βραδινό (18:00-21:00)";
            default: return "Άγνωστο slot";
        }
    }
    
    
    public static String getDayLabel(DayOfWeek day) {
        if (day == null) return "Δεν έχει οριστεί";
        
        switch (day) {
            case MONDAY: return "Δευτέρα";
            case TUESDAY: return "Τρίτη";
            case WEDNESDAY: return "Τετάρτη";
            case THURSDAY: return "Πέμπτη";
            case FRIDAY: return "Παρασκευή";
            case SATURDAY: return "Σάββατο";
            case SUNDAY: return "Κυριακή";
            default: return day.toString();
        }
    }
}