/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.controller;


import com.icsd.springor.DTO.CoursePreferenceDTO;
import com.icsd.springor.model.Course;
import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.service.CoursePreferenceService;
import com.icsd.springor.service.CourseService;
import com.icsd.springor.service.CourseScheduleService;
import com.icsd.springor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;

@Controller
@RequestMapping("/course-preferences")
@PreAuthorize("hasRole('TEACHER')")
public class CoursePreferenceController {
    
    @Autowired
    private CoursePreferenceService preferenceService;
    
    @Autowired
    private CourseService courseService;
    
    @Autowired
    private CourseScheduleService scheduleService;
    
    @Autowired
    private UserService userService;
    
    // Teacher interface - Show available courses to select preferences
    @GetMapping("/select/{scheduleId}")
    public String showCourseSelection(@PathVariable Long scheduleId, 
                                    Model model, 
                                    Authentication authentication) {
        try {
            Long teacherId = getCurrentTeacherId(authentication);
            
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);
            List<Course> allCourses = courseService.getAllCourses();
            List<CoursePreferenceDTO> teacherPreferences = preferenceService.getTeacherPreferences(teacherId, scheduleId);
            
            model.addAttribute("schedule", schedule);
            model.addAttribute("courses", allCourses);
            model.addAttribute("teacherPreferences", teacherPreferences);
            model.addAttribute("teacherId", teacherId);
            model.addAttribute("courseComponents", Course.TeachingHours.CourseComponent.values());
            
            return "course-selection";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading courses: " + e.getMessage());
            return "error";
        }
    }
    
    // Teacher adds/removes course preference
    @PostMapping("/toggle")
    public String toggleCoursePreference(@RequestParam Long courseId,
                                       @RequestParam Course.TeachingHours.CourseComponent courseComponent,
                                       @RequestParam Long scheduleId,
                                       @RequestParam(required = false) Integer preferenceLevel,
                                       @RequestParam(required = false) String notes,
                                       @RequestParam boolean add, // true to add, false to remove
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        try {
            Long teacherId = getCurrentTeacherId(authentication);
            
            if (add) {
                preferenceService.addCoursePreference(teacherId, courseId, courseComponent, scheduleId, preferenceLevel, notes);
                redirectAttributes.addFlashAttribute("message", "Course preference added successfully");
            } else {
                preferenceService.removeCoursePreference(teacherId, courseId, courseComponent, scheduleId);
                redirectAttributes.addFlashAttribute("message", "Course preference removed successfully");
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating preference: " + e.getMessage());
        }
        
        return "redirect:/course-preferences/select/" + scheduleId;
    }
    
    // Teacher updates preference level
    @PostMapping("/update-level")
    public String updatePreferenceLevel(@RequestParam Long courseId,
                                      @RequestParam Course.TeachingHours.CourseComponent courseComponent,
                                      @RequestParam Long scheduleId,
                                      @RequestParam Integer preferenceLevel,
                                      @RequestParam(required = false) String notes,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        try {
            Long teacherId = getCurrentTeacherId(authentication);
            
            preferenceService.updatePreferenceLevel(teacherId, courseId, courseComponent, scheduleId, preferenceLevel, notes);
            redirectAttributes.addFlashAttribute("message", "Preference level updated successfully");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating preference: " + e.getMessage());
        }
        
        return "redirect:/course-preferences/select/" + scheduleId;
    }
    
    // Admin interface - View all preferences for assignment decisions
    @GetMapping("/admin/overview/{scheduleId}")
    public String showPreferencesOverview(@PathVariable Long scheduleId, Model model) {
        try {
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);
            List<Course> allCourses = courseService.getAllCourses();
            List<CoursePreferenceDTO> allPreferences = preferenceService.getAllPreferencesForSchedule(scheduleId);
            
            model.addAttribute("schedule", schedule);
            model.addAttribute("courses", allCourses);
            model.addAttribute("preferences", allPreferences);
            model.addAttribute("courseComponents", Course.TeachingHours.CourseComponent.values());
            
            return "admin-course-preferences-overview";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading preferences: " + e.getMessage());
            return "error";
        }
    }
    
    // Admin interface - View preferences for specific course
    @GetMapping("/admin/course/{courseId}/schedule/{scheduleId}")
    public String showCoursePreferences(@PathVariable Long courseId, 
                                      @PathVariable Long scheduleId, 
                                      Model model) {
        try {
            Course course = courseService.getCourseById(courseId);
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);
            
            // Get preferences for theory component
            List<CoursePreferenceDTO> theoryPreferences = 
                preferenceService.getPreferencesForCourseComponent(courseId, Course.TeachingHours.CourseComponent.THEORY, scheduleId);
            
            // Get preferences for lab component  
            List<CoursePreferenceDTO> labPreferences = 
                preferenceService.getPreferencesForCourseComponent(courseId, Course.TeachingHours.CourseComponent.LABORATORY, scheduleId);
            
            model.addAttribute("course", course);
            model.addAttribute("schedule", schedule);
            model.addAttribute("theoryPreferences", theoryPreferences);
            model.addAttribute("labPreferences", labPreferences);
            
            return "admin-course-specific-preferences";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading course preferences: " + e.getMessage());
            return "error";
        }
    }
    
    // REST API methods
    @GetMapping("/api/teacher/{teacherId}/schedule/{scheduleId}")
    @ResponseBody
    public ResponseEntity<List<CoursePreferenceDTO>> getTeacherPreferences(@PathVariable Long teacherId, 
                                                                          @PathVariable Long scheduleId) {
        try {
            List<CoursePreferenceDTO> preferences = preferenceService.getTeacherPreferences(teacherId, scheduleId);
            return ResponseEntity.ok(preferences);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/api/course/{courseId}/schedule/{scheduleId}")
    @ResponseBody
    public ResponseEntity<List<CoursePreferenceDTO>> getCoursePreferences(@PathVariable Long courseId, 
                                                                         @PathVariable Long scheduleId) {
        try {
            List<CoursePreferenceDTO> preferences = preferenceService.getPreferencesForCourse(courseId, scheduleId);
            return ResponseEntity.ok(preferences);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/api/add")
    @ResponseBody
    public ResponseEntity<?> addCoursePreferenceAPI(@RequestBody Map<String, Object> request) {
        try {
            Long teacherId = Long.valueOf(request.get("teacherId").toString());
            Long courseId = Long.valueOf(request.get("courseId").toString());
            Course.TeachingHours.CourseComponent component = 
                Course.TeachingHours.CourseComponent.valueOf(request.get("courseComponent").toString());
            Long scheduleId = Long.valueOf(request.get("scheduleId").toString());
            Integer preferenceLevel = request.get("preferenceLevel") != null ? 
                Integer.valueOf(request.get("preferenceLevel").toString()) : 3;
            String notes = request.get("notes") != null ? request.get("notes").toString() : null;
            
            CoursePreferenceDTO preference = preferenceService.addCoursePreference(
                teacherId, courseId, component, scheduleId, preferenceLevel, notes);
            
            return ResponseEntity.ok(preference);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/api/remove")
    @ResponseBody
    public ResponseEntity<?> removeCoursePreferenceAPI(@RequestBody Map<String, Object> request) {
        try {
            Long teacherId = Long.valueOf(request.get("teacherId").toString());
            Long courseId = Long.valueOf(request.get("courseId").toString());
            Course.TeachingHours.CourseComponent component = 
                Course.TeachingHours.CourseComponent.valueOf(request.get("courseComponent").toString());
            Long scheduleId = Long.valueOf(request.get("scheduleId").toString());
            
            preferenceService.removeCoursePreference(teacherId, courseId, component, scheduleId);
            
            return ResponseEntity.ok(Map.of("message", "Preference removed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // Helper method - now properly implemented
    private Long getCurrentTeacherId(Authentication authentication) {
        return userService.getCurrentUserId(authentication);
    }
}