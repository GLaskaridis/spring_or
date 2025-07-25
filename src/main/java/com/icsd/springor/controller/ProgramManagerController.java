/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.controller;

import com.icsd.springor.DTO.AssignmentDTO;
import com.icsd.springor.DTO.CoursePreferenceDTO;
import com.icsd.springor.DTO.TeacherPreferenceDTO;
import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/program-manager")
@PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
public class ProgramManagerController {
    
    @Autowired
    private CourseScheduleService scheduleService;
    
    @Autowired
    private CoursePreferenceService coursePreferenceService;
    
    @Autowired
    private AssignmentService assignmentService;
    
    @Autowired
    private TeacherPreferenceService timePreferenceService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            List<CourseSchedule> allSchedules = scheduleService.getAllSchedules();
            
            // Separate schedules by status
            var activeSchedules = allSchedules.stream()
                .filter(s -> s.getStatus() != CourseSchedule.ScheduleStatus.SOLUTION_APPROVED && 
                           s.getStatus() != CourseSchedule.ScheduleStatus.TERMINATED)
                .collect(Collectors.toList());
            
            var completedSchedules = allSchedules.stream()
                .filter(s -> s.getStatus() == CourseSchedule.ScheduleStatus.SOLUTION_APPROVED)
                .collect(Collectors.toList());
            
            model.addAttribute("activeSchedules", activeSchedules);
            model.addAttribute("completedSchedules", completedSchedules);
            model.addAttribute("statusValues", CourseSchedule.ScheduleStatus.values());
            
            return "program-manager-dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading dashboard: " + e.getMessage());
            return "error";
        }
    }
    
     @GetMapping("/course-preferences/{scheduleId}")
    public String viewCoursePreferences(@PathVariable Long scheduleId, Model model) {
        try {
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);
            List<CoursePreferenceDTO> allPreferences = coursePreferenceService.getAllPreferencesForSchedule(scheduleId);
            
            Map<String, List<CoursePreferenceDTO>> preferencesByCourse = allPreferences.stream()
                .collect(Collectors.groupingBy(p -> p.getCourseCode() + " - " + p.getCourseName()));
            
           Map<String, List<CoursePreferenceDTO>> preferencesByTeacher = allPreferences.stream()
                .collect(Collectors.groupingBy(CoursePreferenceDTO::getTeacherName));
            
            model.addAttribute("schedule", schedule);
            model.addAttribute("preferencesByCourse", preferencesByCourse);
            model.addAttribute("preferencesByTeacher", preferencesByTeacher);
            model.addAttribute("allPreferences", allPreferences);
            
            return "program-manager-course-preferences";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading preferences: " + e.getMessage());
            return "error";
        }
    }
    
    @GetMapping("/assignments/{scheduleId}")
    public String manageAssignments(@PathVariable Long scheduleId, Model model) {
        try {
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);
            
            if (schedule.getStatus() != CourseSchedule.ScheduleStatus.ASSIGNMENT_PHASE) {
                model.addAttribute("error", "Schedule is not in assignment phase. Current status: " + schedule.getStatus());
                return "error";
            }
            
            List<CoursePreferenceDTO> preferences = coursePreferenceService.getAllPreferencesForSchedule(scheduleId);
            List<AssignmentDTO> existingAssignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            
            // Group preferences by course component for easier assignment
            Map<String, List<CoursePreferenceDTO>> preferencesByComponent = preferences.stream()
                .collect(Collectors.groupingBy(p -> p.getCourseCode() + "_" + p.getCourseComponent()));
            
            model.addAttribute("schedule", schedule);
            model.addAttribute("preferencesByComponent", preferencesByComponent);
            model.addAttribute("existingAssignments", existingAssignments);
            
            return "program-manager-assignments";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading assignment interface: " + e.getMessage());
            return "error";
        }
    }
    
     @GetMapping("/time-preferences/{scheduleId}")
    public String viewTimePreferences(@PathVariable Long scheduleId, Model model) {
        try {
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            List<TeacherPreferenceDTO> timePreferences = timePreferenceService.getPreferencesBySchedule(scheduleId);
            
            Map<Long, List<TeacherPreferenceDTO>> preferencesByAssignment = timePreferences.stream()
                .collect(Collectors.groupingBy(TeacherPreferenceDTO::getAssignmentId));
            
             boolean readyForExecution = timePreferenceService.areAllPreferencesProvided(scheduleId);
            
            model.addAttribute("schedule", schedule);
            model.addAttribute("assignments", assignments);
            model.addAttribute("preferencesByAssignment", preferencesByAssignment);
            model.addAttribute("readyForExecution", readyForExecution);
            
            return "program-manager-time-preferences";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading time preferences: " + e.getMessage());
            return "error";
        }
    }
    
    @GetMapping("/execute-scheduling/{scheduleId}")
    public String executeScheduling(@PathVariable Long scheduleId, 
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            if (!userService.canManageSchedules(authentication)) {
                redirectAttributes.addFlashAttribute("error", "You don't have permission to execute scheduling");
                return "redirect:/program-manager/dashboard";
            }
            
            return "redirect:/schedule-execution/execute/" + scheduleId;
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error executing scheduling: " + e.getMessage());
            return "redirect:/program-manager/dashboard";
        }
    }
    
    @PostMapping("/change-status/{scheduleId}")
    public String changeScheduleStatus(@PathVariable Long scheduleId,
                                     @RequestParam CourseSchedule.ScheduleStatus newStatus,
                                     RedirectAttributes redirectAttributes) {
        try {
            scheduleService.changeScheduleStatus(scheduleId, newStatus);
            redirectAttributes.addFlashAttribute("message", "Schedule status updated to " + newStatus);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating status: " + e.getMessage());
        }
        
        return "redirect:/program-manager/dashboard";
    }
    
    @GetMapping("/statistics/{scheduleId}")
    public String viewStatistics(@PathVariable Long scheduleId, Model model) {
        try {
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);
            List<CoursePreferenceDTO> coursePrefs = coursePreferenceService.getAllPreferencesForSchedule(scheduleId);
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            List<TeacherPreferenceDTO> timePrefs = timePreferenceService.getPreferencesBySchedule(scheduleId);
            
           long totalTeachers = coursePrefs.stream()
                .map(CoursePreferenceDTO::getTeacherId)
                .distinct()
                .count();
            
            long totalCourses = coursePrefs.stream()
                .map(CoursePreferenceDTO::getCourseId)
                .distinct()
                .count();
            
            long assignedCourses = assignments.size();
            long teachersWithTimePrefs = timePrefs.stream()
                .map(TeacherPreferenceDTO::getAssignmentId)
                .distinct()
                .count();
            
            model.addAttribute("schedule", schedule);
            model.addAttribute("totalTeachers", totalTeachers);
            model.addAttribute("totalCourses", totalCourses);
            model.addAttribute("assignedCourses", assignedCourses);
            model.addAttribute("teachersWithTimePrefs", teachersWithTimePrefs);
            model.addAttribute("coursePreferences", coursePrefs.size());
            model.addAttribute("timePreferences", timePrefs.size());
            
            return "program-manager-statistics";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading statistics: " + e.getMessage());
            return "error";
        }
    }
}