/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.controller;



import com.icsd.springor.DTO.AssignmentDTO;
import com.icsd.springor.DTO.CoursePreferenceDTO;
import com.icsd.springor.model.Course;
import com.icsd.springor.service.AssignmentService;
import com.icsd.springor.service.CoursePreferenceService;
import com.icsd.springor.service.CourseScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/assignments")
public class AssignmentFromPreferencesController {
    
    @Autowired
    private AssignmentService assignmentService;
    
    @Autowired
    private CoursePreferenceService preferenceService;
    
    @Autowired
    private CourseScheduleService scheduleService;
    
    @GetMapping("/from-preferences/{scheduleId}")
    public String showAssignmentInterface(@PathVariable Long scheduleId, Model model) {
        try {
            var schedule = scheduleService.getScheduleById(scheduleId);
            var allPreferences = preferenceService.getAllPreferencesForSchedule(scheduleId);
            var existingAssignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            
            // Group preferences by course and component
            Map<String, List<CoursePreferenceDTO>> groupedPreferences = allPreferences.stream()
                .collect(Collectors.groupingBy(p -> p.getCourseCode() + "_" + p.getCourseComponent()));
            
            model.addAttribute("schedule", schedule);
            model.addAttribute("groupedPreferences", groupedPreferences);
            model.addAttribute("existingAssignments", existingAssignments);
            
            return "admin-assignment-from-preferences";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading assignment interface: " + e.getMessage());
            return "error";
        }
    }
    
    @PostMapping("/assign")
    public String createAssignmentFromPreference(@RequestParam Long courseId,
                                               @RequestParam Long teacherId,
                                               @RequestParam Course.TeachingHours.CourseComponent courseComponent,
                                               @RequestParam Long scheduleId,
                                               RedirectAttributes redirectAttributes) {
        try {
            assignmentService.createAssignment(courseId, teacherId, courseComponent, scheduleId);
            redirectAttributes.addFlashAttribute("message", "Assignment created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating assignment: " + e.getMessage());
        }
        
        return "redirect:/admin/assignments/from-preferences/" + scheduleId;
    }
}