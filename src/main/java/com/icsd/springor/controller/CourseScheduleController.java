
package com.icsd.springor.controller;

import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.service.CourseScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/schedules")
public class CourseScheduleController {
    
    @Autowired
    private CourseScheduleService scheduleService;
    
    // Web interface methods
    @GetMapping("/list")
    public String listSchedules(Model model) {
        List<CourseSchedule> schedules = scheduleService.getAllSchedules();
        model.addAttribute("schedules", schedules);
        model.addAttribute("statusValues", CourseSchedule.ScheduleStatus.values());
        return "schedule-list";
    }
    
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("schedule", new CourseSchedule());
        return "schedule-create";
    }
    
    @PostMapping("/create")
    public String createSchedule(@RequestParam String name,
                               @RequestParam String semester,
                               @RequestParam(required = false) String startTime,
                               @RequestParam(required = false) String endTime,
                               @RequestParam(required = false) Integer maxHoursPerDay,
                               @RequestParam(required = false) Double maxDistanceKm,
                               RedirectAttributes redirectAttributes) {
        try {
            CourseSchedule schedule = scheduleService.createSchedule(name, semester, startTime, endTime, maxHoursPerDay, maxDistanceKm);
            redirectAttributes.addFlashAttribute("message", "Schedule created successfully");
            return "redirect:/schedules/list";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating schedule: " + e.getMessage());
            return "redirect:/schedules/create";
        }
    }
    
    @PostMapping("/change-status/{scheduleId}")
    public String changeStatus(@PathVariable Long scheduleId,
                             @RequestParam CourseSchedule.ScheduleStatus newStatus,
                             RedirectAttributes redirectAttributes) {
        try {
            scheduleService.changeScheduleStatus(scheduleId, newStatus);
            redirectAttributes.addFlashAttribute("message", "Schedule status updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating status: " + e.getMessage());
        }
        return "redirect:/schedules/list";
    }
    
    @GetMapping("/delete/{scheduleId}")
    public String deleteSchedule(@PathVariable Long scheduleId,
                               RedirectAttributes redirectAttributes) {
        try {
            scheduleService.deleteSchedule(scheduleId);
            redirectAttributes.addFlashAttribute("message", "Schedule deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting schedule: " + e.getMessage());
        }
        return "redirect:/schedules/list";
    }
    
    // REST API methods
    @GetMapping("/api/all")
    @ResponseBody
    public ResponseEntity<List<CourseSchedule>> getAllSchedulesAPI() {
        try {
            List<CourseSchedule> schedules = scheduleService.getAllSchedules();
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/api/{scheduleId}")
    @ResponseBody
    public ResponseEntity<CourseSchedule> getScheduleAPI(@PathVariable Long scheduleId) {
        try {
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);
            return ResponseEntity.ok(schedule);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/api/create")
    @ResponseBody
    public ResponseEntity<?> createScheduleAPI(@RequestBody Map<String, Object> request) {
        try {
            String name = request.get("name").toString();
            String semester = request.get("semester").toString();
            String startTime = request.get("startTime") != null ? request.get("startTime").toString() : null;
            String endTime = request.get("endTime") != null ? request.get("endTime").toString() : null;
            Integer maxHoursPerDay = request.get("maxHoursPerDay") != null ? 
                Integer.valueOf(request.get("maxHoursPerDay").toString()) : null;
            Double maxDistanceKm = request.get("maxDistanceKm") != null ? 
                Double.valueOf(request.get("maxDistanceKm").toString()) : null;
            
            CourseSchedule schedule = scheduleService.createSchedule(name, semester, startTime, endTime, maxHoursPerDay, maxDistanceKm);
            return ResponseEntity.ok(schedule);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/api/{scheduleId}/status")
    @ResponseBody
    public ResponseEntity<?> changeStatusAPI(@PathVariable Long scheduleId,
                                           @RequestBody Map<String, Object> request) {
        try {
            CourseSchedule.ScheduleStatus newStatus = 
                CourseSchedule.ScheduleStatus.valueOf(request.get("status").toString());
            CourseSchedule schedule = scheduleService.changeScheduleStatus(scheduleId, newStatus);
            return ResponseEntity.ok(schedule);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/api/{scheduleId}")
    @ResponseBody
    public ResponseEntity<?> deleteScheduleAPI(@PathVariable Long scheduleId) {
        try {
            scheduleService.deleteSchedule(scheduleId);
            return ResponseEntity.ok(Map.of("message", "Schedule deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}