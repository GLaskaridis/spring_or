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
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class NavigationController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CourseScheduleService scheduleService;
    
    @ModelAttribute
    public void addNavigationData(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getName().equals("anonymousUser")) {
            
            try {
                // Add user info
                var currentUser = userService.getCurrentUser(authentication);
                model.addAttribute("currentUser", currentUser);
                model.addAttribute("userRole", currentUser.getRole());
                
                // Add role-based navigation
                boolean isAdmin = userService.canManageUsers(authentication);
                boolean isManager = userService.canManageSchedules(authentication);
                boolean isTeacher = userService.isCurrentUserTeacher(authentication);
                
                model.addAttribute("isAdmin", isAdmin);
                model.addAttribute("isManager", isManager);
                model.addAttribute("isTeacher", isTeacher);
                
                // Add schedules for navigation
                List<CourseSchedule> allSchedules = scheduleService.getAllSchedules();
                
                if (isManager || isAdmin) {
                    // Managers see all schedules
                    model.addAttribute("navigationSchedules", allSchedules);
                    
                    // Separate by status for quick access
                    var activeSchedules = allSchedules.stream()
                        .filter(s -> s.getStatus() == CourseSchedule.ScheduleStatus.ASSIGNMENT_PHASE ||
                                   s.getStatus() == CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE)
                        .collect(Collectors.toList());
                    
                    model.addAttribute("activeSchedules", activeSchedules);
                } else if (isTeacher) {
                    // Teachers see only schedules where they can provide preferences
                    var teacherSchedules = allSchedules.stream()
                        .filter(s -> s.getStatus() == CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE)
                        .collect(Collectors.toList());
                    
                    model.addAttribute("navigationSchedules", teacherSchedules);
                }
                
            } catch (Exception e) {
                // Silent fail for navigation data
                System.out.println("Error loading navigation data: " + e.getMessage());
            }
        }
    }
}