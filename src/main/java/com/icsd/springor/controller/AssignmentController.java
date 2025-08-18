/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.controller;

import com.icsd.springor.DTO.AssignmentDTO;
import com.icsd.springor.model.Course;
import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.model.User;
import com.icsd.springor.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;

@Controller
@RequestMapping("/assignments")
public class AssignmentController {
    
    @Autowired
    private AssignmentService assignmentService;
    
    @Autowired
    private CourseService courseService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CourseScheduleService scheduleService;
    
    
    @GetMapping("/schedules/{scheduleId}")
    @PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public String listAssignments(@PathVariable Long scheduleId, Model model) {
        try {
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            
            model.addAttribute("schedule", schedule);
            model.addAttribute("assignments", assignments);
            
            return "assignment-list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading assignments: " + e.getMessage());
            return "error";
        }
    }
    
    @GetMapping("/schedules/{scheduleId}/create")
    @PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public String showCreateForm(@PathVariable Long scheduleId, Model model) {
        try {
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);
            
            // Check if schedule is in correct phase
            if (schedule.getStatus() != CourseSchedule.ScheduleStatus.ASSIGNMENT_PHASE) {
                model.addAttribute("error", "Cannot create assignments. Schedule is not in assignment phase.");
                return "error";
            }
            
            List<Course> allCourses = courseService.getAllCourses();
            List<User> allTeachers = userService.findAllTeachers();
            List<AssignmentDTO> existingAssignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            
            // Filter active courses and teachers
            List<Course> activeCourses = allCourses.stream()
                .filter(Course::isActive)
                .collect(Collectors.toList());
            
            List<User> activeTeachers = allTeachers.stream()
                .filter(User::isActive)
                .collect(Collectors.toList());
            
            model.addAttribute("schedule", schedule);
            model.addAttribute("courses", activeCourses);
            model.addAttribute("teachers", activeTeachers);
            model.addAttribute("existingAssignments", existingAssignments);
            model.addAttribute("courseComponents", Course.TeachingHours.CourseComponent.values());
            
            return "assignment-create";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading create form: " + e.getMessage());
            return "error";
        }
    }
    
    @PostMapping("/schedules/{scheduleId}/create")
    @PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public String createAssignment(@PathVariable Long scheduleId,
                                 @RequestParam Long courseId,
                                 @RequestParam Long teacherId,
                                 @RequestParam Course.TeachingHours.CourseComponent courseComponent,
                                 RedirectAttributes redirectAttributes) {
        try {
            AssignmentDTO assignment = assignmentService.createAssignment(courseId, teacherId, courseComponent, scheduleId);
            redirectAttributes.addFlashAttribute("message", 
                "Assignment created successfully: " + assignment.getCourseName() + 
                " (" + assignment.getCourseComponent() + ") → " + assignment.getTeacherName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating assignment: " + e.getMessage());
        }
        
        return "redirect:/assignments/schedules/" + scheduleId;
    }
    
    @GetMapping("/schedules/{scheduleId}/bulk-create")
    @PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public String showBulkCreateForm(@PathVariable Long scheduleId, Model model) {
        try {
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);
            
            if (schedule.getStatus() != CourseSchedule.ScheduleStatus.ASSIGNMENT_PHASE) {
                model.addAttribute("error", "Cannot create assignments. Schedule is not in assignment phase.");
                return "error";
            }
            
            List<Course> allCourses = courseService.getAllCourses();
            List<User> allTeachers = userService.findAllTeachers();
            List<AssignmentDTO> existingAssignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            
            // Filter active items
            List<Course> activeCourses = allCourses.stream()
                .filter(Course::isActive)
                .collect(Collectors.toList());
            
            List<User> activeTeachers = allTeachers.stream()
                .filter(User::isActive)
                .collect(Collectors.toList());
            
            // Group existing assignments for easy viewing
            Map<Long, List<AssignmentDTO>> assignmentsByCourse = existingAssignments.stream()
                .collect(Collectors.groupingBy(AssignmentDTO::getCourseId));
            
            model.addAttribute("schedule", schedule);
            model.addAttribute("courses", activeCourses);
            model.addAttribute("teachers", activeTeachers);
            model.addAttribute("existingAssignments", existingAssignments);
            model.addAttribute("assignmentsByCourse", assignmentsByCourse);
            model.addAttribute("courseComponents", Course.TeachingHours.CourseComponent.values());
            
            return "assignment-bulk-create";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading bulk create form: " + e.getMessage());
            return "error";
        }
    }
    
    @PostMapping("/schedules/{scheduleId}/bulk-create")
    @PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public String processBulkCreate(@PathVariable Long scheduleId,
                                  @RequestParam("courseIds[]") Long[] courseIds,
                                  @RequestParam("teacherIds[]") Long[] teacherIds,
                                  @RequestParam("components[]") String[] components,
                                  RedirectAttributes redirectAttributes) {
        try {
            int created = 0;
            int failed = 0;
            StringBuilder errors = new StringBuilder();
            
            for (int i = 0; i < courseIds.length; i++) {
                try {
                    Course.TeachingHours.CourseComponent component = 
                        Course.TeachingHours.CourseComponent.valueOf(components[i]);
                    
                    assignmentService.createAssignment(courseIds[i], teacherIds[i], component, scheduleId);
                    created++;
                } catch (Exception e) {
                    failed++;
                    errors.append("Assignment ").append(i + 1).append(": ").append(e.getMessage()).append("; ");
                }
            }
            
            if (created > 0) {
                redirectAttributes.addFlashAttribute("message", 
                    "Bulk creation completed: " + created + " assignments created" +
                    (failed > 0 ? ", " + failed + " failed" : ""));
            }
            
            if (failed > 0 && errors.length() > 0) {
                redirectAttributes.addFlashAttribute("error", "Some assignments failed: " + errors.toString());
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error in bulk creation: " + e.getMessage());
        }
        
        return "redirect:/assignments/schedules/" + scheduleId;
    }
    
    @GetMapping("/{assignmentId}/edit")
    @PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public String showEditForm(@PathVariable Long assignmentId, Model model) {
        try {
            // Get all teacher assignments to find this one
            List<User> allTeachers = userService.findAllTeachers();
            AssignmentDTO assignment = null;
            
            for (User teacher : allTeachers) {
                List<AssignmentDTO> teacherAssignments = assignmentService.getAssignmentsByTeacher(teacher.getId());
                assignment = teacherAssignments.stream()
                    .filter(a -> a.getId().equals(assignmentId))
                    .findFirst()
                    .orElse(null);
                if (assignment != null) break;
            }
            
            if (assignment == null) {
                model.addAttribute("error", "Assignment not found");
                return "error";
            }
            
            CourseSchedule schedule = scheduleService.getScheduleById(assignment.getScheduleId());
            
            if (schedule.getStatus() != CourseSchedule.ScheduleStatus.ASSIGNMENT_PHASE) {
                model.addAttribute("error", "Cannot edit assignments. Schedule is not in assignment phase.");
                return "error";
            }
            
            List<User> activeTeachers = userService.findAllTeachers().stream()
                .filter(User::isActive)
                .collect(Collectors.toList());
            
            model.addAttribute("assignment", assignment);
            model.addAttribute("schedule", schedule);
            model.addAttribute("teachers", activeTeachers);
            
            return "assignment-edit";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading edit form: " + e.getMessage());
            return "error";
        }
    }
    
    @PostMapping("/{assignmentId}/edit")
    @PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public String updateAssignment(@PathVariable Long assignmentId,
                                 @RequestParam Long teacherId,
                                 RedirectAttributes redirectAttributes) {
        try {
            AssignmentDTO updated = assignmentService.updateAssignment(assignmentId, teacherId);
            redirectAttributes.addFlashAttribute("message", 
                "Assignment updated successfully: " + updated.getCourseName() + 
                " → " + updated.getTeacherName());
            
            return "redirect:/assignments/schedules/" + updated.getScheduleId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating assignment: " + e.getMessage());
            return "redirect:/assignments/" + assignmentId + "/edit";
        }
    }
    
    @PostMapping("/{assignmentId}/delete")
    @PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public String deleteAssignment(@PathVariable Long assignmentId,
                                 @RequestParam Long scheduleId,
                                 RedirectAttributes redirectAttributes) {
        try {
            assignmentService.deleteAssignment(assignmentId);
            redirectAttributes.addFlashAttribute("message", "Assignment deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting assignment: " + e.getMessage());
        }
        
        return "redirect:/assignments/schedules/" + scheduleId;
    }
    
    @GetMapping("/api/my-assignments")
    @ResponseBody
    public ResponseEntity<List<AssignmentDTO>> getMyAssignments(Authentication auth) {
        try {
            if (auth == null) {
                System.out.println("❌ Authentication is null");
                return ResponseEntity.status(401).build();
            }

            String username = auth.getName();
            System.out.println("Username: " + username);
            System.out.println("Authorities: " + auth.getAuthorities());

            User teacher = userService.findByUsername(username);
            if (teacher == null) {
                System.out.println("❌ Teacher not found for username: " + username);
                return ResponseEntity.status(404).build();
            }

            System.out.println("Teacher found: " + teacher);
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsByTeacher(teacher.getId());
            System.out.println("Assignments: " + assignments.size());

            return ResponseEntity.ok(assignments);
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/teachers/{teacherId}")
    @PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public String viewTeacherAssignments(@PathVariable Long teacherId, Model model) {
        try {
            User teacher = userService.getUserById(teacherId);
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsByTeacher(teacherId);
            
            // Group by schedule
            Map<Long, List<AssignmentDTO>> assignmentsBySchedule = assignments.stream()
                .collect(Collectors.groupingBy(AssignmentDTO::getScheduleId));
            
            model.addAttribute("teacher", teacher);
            model.addAttribute("assignments", assignments);
            model.addAttribute("assignmentsBySchedule", assignmentsBySchedule);
            
            return "teacher-assignments-view";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading teacher assignments: " + e.getMessage());
            return "error";
        }
    }
    
    @GetMapping("/api/schedules/{scheduleId}")
    @ResponseBody
    public ResponseEntity<List<AssignmentDTO>> getAssignmentsByScheduleAPI(@PathVariable Long scheduleId) {
        try {
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            return ResponseEntity.ok(assignments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/api/schedules/{scheduleId}")
    @ResponseBody
    @PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> createAssignmentAPI(@PathVariable Long scheduleId,
                                               @RequestBody Map<String, Object> request) {
        try {
            Long courseId = Long.valueOf(request.get("courseId").toString());
            Long teacherId = Long.valueOf(request.get("teacherId").toString());
            Course.TeachingHours.CourseComponent component = 
                Course.TeachingHours.CourseComponent.valueOf(request.get("courseComponent").toString());
            
            AssignmentDTO assignment = assignmentService.createAssignment(courseId, teacherId, component, scheduleId);
            return ResponseEntity.ok(assignment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/api/{assignmentId}")
    @ResponseBody
    @PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteAssignmentAPI(@PathVariable Long assignmentId) {
        try {
            assignmentService.deleteAssignment(assignmentId);
            return ResponseEntity.ok(Map.of("message", "Assignment deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}