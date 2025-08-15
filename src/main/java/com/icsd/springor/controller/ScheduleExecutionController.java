/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.controller;

import com.icsd.springor.service.*;
import org.springframework.web.bind.annotation.*;

import com.icsd.springor.CourseScheduler;
import com.icsd.springor.CourseScheduler.CourseAssignment;
import com.icsd.springor.DTO.AssignmentDTO;
import com.icsd.springor.DTO.TeacherPreferenceDTO;
import com.icsd.springor.model.Course;
import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.model.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;


@Controller
@RequestMapping("/schedule-execution")
@PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
public class ScheduleExecutionController {
    
    @Autowired
    private CourseScheduleService scheduleService;
    
    @Autowired
    private AssignmentService assignmentService;
    
    @Autowired
    private TeacherPreferenceService preferenceService;
    
    @Autowired
    private CourseService courseService;
    
    @Autowired
    private RoomService roomService;
    
    // Execute scheduling algorithm
    @GetMapping("/execute/{scheduleId}")
    public String executeScheduling(@PathVariable Long scheduleId, 
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        try {
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);
            
            // Check if schedule is ready for execution
            if (schedule.getStatus() != CourseSchedule.ScheduleStatus.EXECUTION_PHASE) {
                redirectAttributes.addFlashAttribute("error", 
                    "Schedule is not ready for execution. Current status: " + schedule.getStatus());
                return "redirect:/schedules/admin/all";
            }
            
            // Get assignments and preferences
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            List<TeacherPreferenceDTO> preferences = preferenceService.getPreferencesBySchedule(scheduleId);
            
            // Convert assignments to courses with preferences
            List<Course> coursesWithPreferences = prepareCoursesWithPreferences(assignments, preferences);
            
            // Get rooms
            List<Room> rooms = roomService.getAllRooms();
            
            // Execute scheduling algorithm
            CourseScheduler scheduler = new CourseScheduler();
            List<CourseAssignment> result = scheduler.createSchedule(coursesWithPreferences, rooms);
            
            if (result != null && !result.isEmpty()) {
                // Success - update schedule status
                scheduleService.changeScheduleStatus(scheduleId, CourseSchedule.ScheduleStatus.SOLUTION_FOUND);
                
                // Store results (you might want to create a ScheduleResult entity)
                // For now, we'll display them
                model.addAttribute("schedule", schedule);
                model.addAttribute("assignments", result);
                model.addAttribute("success", true);
                
                return "schedule-execution-result";
            } else {
                // No solution found
                scheduleService.changeScheduleStatus(scheduleId, CourseSchedule.ScheduleStatus.NO_SOLUTION_FOUND);
                redirectAttributes.addFlashAttribute("error", "No solution found for the current constraints");
                return "redirect:/schedules/admin/all";
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error executing schedule: " + e.getMessage());
            return "redirect:/schedules/admin/all";
        }
    }
    
    // Prepare courses with teacher preferences for the scheduling algorithm
    private List<Course> prepareCoursesWithPreferences(List<AssignmentDTO> assignments, 
                                                     List<TeacherPreferenceDTO> preferences) {
        
        // Group preferences by assignment
        Map<Long, List<TeacherPreferenceDTO>> preferencesByAssignment = preferences.stream()
            .collect(Collectors.groupingBy(TeacherPreferenceDTO::getAssignmentId));
        
        return assignments.stream().map(assignment -> {
            Course course = courseService.getCourseById(assignment.getCourseId());
            
            // Get preferences for this assignment
            List<TeacherPreferenceDTO> assignmentPrefs = preferencesByAssignment.get(assignment.getId());
            
            if (assignmentPrefs != null && !assignmentPrefs.isEmpty()) {
                // Apply the strongest preference to the course
                TeacherPreferenceDTO strongestPref = assignmentPrefs.stream()
                    .max((p1, p2) -> Integer.compare(
                        p1.getPreferenceWeight() != null ? p1.getPreferenceWeight() : 5,
                        p2.getPreferenceWeight() != null ? p2.getPreferenceWeight() : 5
                    ))
                    .orElse(assignmentPrefs.get(0));
                
                // Create TimePreference for the course using only day and slot
                if (strongestPref.getPreferredDay() != null && strongestPref.getPreferredSlot() != null) {
                    Course.TimePreference timePreference = new Course.TimePreference(
                        strongestPref.getPreferredDay(),
                        strongestPref.getPreferredSlot(),
                        strongestPref.getPreferenceWeight() != null ? strongestPref.getPreferenceWeight() : 5
                    );
                    course.setTimePreference(timePreference);
                }
            }
            
            return course;
        }).collect(Collectors.toList());
    }
    
    // Test scheduling with sample data (for debugging)
    @GetMapping("/test")
    public String testScheduling(Model model) {
        try {
            CourseScheduler scheduler = new CourseScheduler();
            List<Course> courses = courseService.getAllCourses();
            
            // Add sample preferences to first course
            if (!courses.isEmpty()) {
                Course course = courses.get(0);
                System.out.println("Testing with course: " + course.getName());
                course.setTimePreference(new Course.TimePreference(
                    DayOfWeek.FRIDAY,    // προτιμώμενη μέρα
                    0,                   // πρώτο slot (9πμ-12μμ)
                    8                    // βάρος προτίμησης (1-10)
                ));
            }
            
            List<Room> rooms = roomService.getAllRooms();
            System.out.println("Available rooms: " + rooms.size());
            
            List<CourseAssignment> assignments = scheduler.createSchedule(courses, rooms);
            System.out.println("Generated assignments: " + assignments.size());
            assignments.forEach(System.out::println);
            
            model.addAttribute("assignments", assignments);
            model.addAttribute("courses", courses);
            model.addAttribute("rooms", rooms);
            
            return "schedule-test-result";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error testing schedule: " + e.getMessage());
            return "error";
        }
    }
    
    // Approve a generated schedule
    @PostMapping("/approve/{scheduleId}")
    public String approveSchedule(@PathVariable Long scheduleId,
                                RedirectAttributes redirectAttributes) {
        try {
            scheduleService.changeScheduleStatus(scheduleId, CourseSchedule.ScheduleStatus.SOLUTION_APPROVED);
            redirectAttributes.addFlashAttribute("message", "Schedule approved successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error approving schedule: " + e.getMessage());
        }
        
        return "redirect:/schedules/admin/all";
    }
    
    // Reject a generated schedule and go back to execution
    @PostMapping("/reject/{scheduleId}")
    public String rejectSchedule(@PathVariable Long scheduleId,
                               RedirectAttributes redirectAttributes) {
        try {
            scheduleService.changeScheduleStatus(scheduleId, CourseSchedule.ScheduleStatus.EXECUTION_PHASE);
            redirectAttributes.addFlashAttribute("message", "Schedule rejected. You can modify preferences and re-execute.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error rejecting schedule: " + e.getMessage());
        }
        
        return "redirect:/schedules/admin/all";
    }
}