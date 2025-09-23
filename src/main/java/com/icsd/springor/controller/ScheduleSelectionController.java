/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.controller;

import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.service.CourseScheduleService;
import com.icsd.springor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/schedules")
public class ScheduleSelectionController {
    
    @Autowired
    private CourseScheduleService scheduleService;
    
    @Autowired
    private UserService userService;
    
    // Teacher interface - Show available schedules for preferences
    @GetMapping("/teacher/available")
    public String showAvailableSchedules(Model model, Authentication authentication) {
        try {
            Long teacherId = getCurrentTeacherId(authentication);
            
            // Get schedules that are in REQUIREMENTS_PHASE (where teachers provide preferences)
            List<CourseSchedule> availableSchedules = scheduleService.getAllSchedules().stream()
                .filter(s -> s.getStatus() == CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE)
                .collect(Collectors.toList());
            
            model.addAttribute("availableSchedules", availableSchedules);
            model.addAttribute("teacherId", teacherId);
            
            return "teacher-schedule-selection";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading schedules: " + e.getMessage());
            return "error";
        }
    }
    
    @GetMapping("/admin/all")
    public String showAllSchedulesAdmin(Model model) {
        try {
            List<CourseSchedule> allSchedules = scheduleService.getAllSchedules();
            model.addAttribute("schedules", allSchedules);
            model.addAttribute("statusValues", CourseSchedule.ScheduleStatus.values());

            return "schedules"; // <- Change to match the existing template
        } catch (Exception e) {
            model.addAttribute("error", "Error loading schedules: " + e.getMessage());
            return "error";
        }
    }
    
    // Dashboard redirect based on user role and schedule status
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        try {
            // Check if user is admin or teacher
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            
            if (isAdmin) {
                return "redirect:/schedules/admin/all";
            } else {
                // Teacher - redirect to available schedules for preferences
                return "redirect:/schedules/teacher/available";
            }
            
        } catch (Exception e) {
            model.addAttribute("error", "Error loading dashboard: " + e.getMessage());
            return "error";
        }
    }
    
    private Long getCurrentTeacherId(Authentication authentication) {
        return userService.getCurrentUserId(authentication);
    }
}