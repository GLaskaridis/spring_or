/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.controller;

import com.icsd.springor.DTO.AssignmentDTO;
import com.icsd.springor.DTO.RoomPreferenceDTO;
import com.icsd.springor.DTO.TeacherPreferenceDTO;
import com.icsd.springor.DTO.TimeSlotPreferenceDTO;
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
    private AssignmentService assignmentService;
    
    @Autowired
    private TimeSlotPreferenceService timeSlotPreferenceService;
    
    @Autowired
    private RoomPreferenceService roomPreferenceService;
    
    @Autowired
    private UserService userService;
    
    // Main dashboard for Program Manager
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
    
    // View teacher preferences for assignment decisions
    @GetMapping("/assignments/{scheduleId}")
    public String viewAssignments(@PathVariable Long scheduleId, Model model) {
        try {
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            
            model.addAttribute("schedule", schedule);
            model.addAttribute("assignments", assignments);
            
            return "program-manager-assignments";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading assignments: " + e.getMessage());
            return "error";
        }
    }
    
    // Assignment interface - where program manager manages assignments
    @GetMapping("/assignments/{scheduleId}/manage")
    public String manageAssignments(@PathVariable Long scheduleId, Model model) {
        try {
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);
            
            if (schedule.getStatus() != CourseSchedule.ScheduleStatus.ASSIGNMENT_PHASE) {
                model.addAttribute("error", "Schedule is not in assignment phase. Current status: " + schedule.getStatus());
                return "error";
            }
            
            List<AssignmentDTO> existingAssignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            
            model.addAttribute("schedule", schedule);
            model.addAttribute("existingAssignments", existingAssignments);
            
            return "program-manager-assignment-management";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading assignment interface: " + e.getMessage());
            return "error";
        }
    }
    
    // View time and room preferences after assignments are made
    @GetMapping("/preferences/{scheduleId}")
    public String viewPreferences(@PathVariable Long scheduleId, Model model) {
        try {
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            List<TimeSlotPreferenceDTO> timeSlotPreferences = timeSlotPreferenceService.getAllPreferencesForSchedule(scheduleId);
            List<RoomPreferenceDTO> roomPreferences = roomPreferenceService.getAllPreferencesForSchedule(scheduleId);
            
            // Group preferences by assignment
            Map<Long, List<TimeSlotPreferenceDTO>> timeSlotPrefsByAssignment = timeSlotPreferences.stream()
                .collect(Collectors.groupingBy(TimeSlotPreferenceDTO::getAssignmentId));
            Map<Long, List<RoomPreferenceDTO>> roomPrefsByAssignment = roomPreferences.stream()
                .collect(Collectors.groupingBy(RoomPreferenceDTO::getAssignmentId));
            
            // Check readiness for execution - simple check for now
            boolean readyForExecution = assignments.size() > 0 && 
                (!timeSlotPreferences.isEmpty() || !roomPreferences.isEmpty());
            
            model.addAttribute("schedule", schedule);
            model.addAttribute("assignments", assignments);
            model.addAttribute("timeSlotPrefsByAssignment", timeSlotPrefsByAssignment);
            model.addAttribute("roomPrefsByAssignment", roomPrefsByAssignment);
            model.addAttribute("readyForExecution", readyForExecution);
            
            return "program-manager-preferences";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading preferences: " + e.getMessage());
            return "error";
        }
    }
    
    // Execute scheduling algorithm
    @GetMapping("/execute-scheduling/{scheduleId}")
    public String executeScheduling(@PathVariable Long scheduleId, 
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            // Verify user can execute scheduling
            if (!userService.canManageSchedules(authentication)) {
                redirectAttributes.addFlashAttribute("error", "You don't have permission to execute scheduling");
                return "redirect:/program-manager/dashboard";
            }
            
            // Redirect to schedule execution controller
            return "redirect:/schedule-execution/execute/" + scheduleId;
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error executing scheduling: " + e.getMessage());
            return "redirect:/program-manager/dashboard";
        }
    }
    
    // Change schedule status
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
    
    // Statistics and overview
    @GetMapping("/statistics/{scheduleId}")
    public String viewStatistics(@PathVariable Long scheduleId, Model model) {
        try {
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            List<TimeSlotPreferenceDTO> timeSlotPrefs = timeSlotPreferenceService.getAllPreferencesForSchedule(scheduleId);
            List<RoomPreferenceDTO> roomPrefs = roomPreferenceService.getAllPreferencesForSchedule(scheduleId);
            
            // Calculate statistics
            long totalTeachers = assignments.stream()
                .map(AssignmentDTO::getTeacherId)
                .distinct()
                .count();
            
            long totalCourses = assignments.stream()
                .map(AssignmentDTO::getCourseId)
                .distinct()
                .count();
            
            long assignedCourses = assignments.size();
            
            long assignmentsWithTimePrefs = timeSlotPrefs.stream()
                .map(TimeSlotPreferenceDTO::getAssignmentId)
                .distinct()
                .count();
            
            long assignmentsWithRoomPrefs = roomPrefs.stream()
                .map(RoomPreferenceDTO::getAssignmentId)
                .distinct()
                .count();
            
            model.addAttribute("schedule", schedule);
            model.addAttribute("totalTeachers", totalTeachers);
            model.addAttribute("totalCourses", totalCourses);
            model.addAttribute("assignedCourses", assignedCourses);
            model.addAttribute("assignmentsWithTimePrefs", assignmentsWithTimePrefs);
            model.addAttribute("assignmentsWithRoomPrefs", assignmentsWithRoomPrefs);
            model.addAttribute("totalTimeSlotPreferences", timeSlotPrefs.size());
            model.addAttribute("totalRoomPreferences", roomPrefs.size());
            
            return "program-manager-statistics";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading statistics: " + e.getMessage());
            return "error";
        }
    }
}