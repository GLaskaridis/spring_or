package com.icsd.springor.controller;

import com.icsd.springor.DTO.AssignmentDTO;
import com.icsd.springor.model.Course;
import com.icsd.springor.service.AssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    @Autowired
    private AssignmentService assignmentService;

    /**
     * GET /api/assignments - όλες οι αναθέσεις (ADMIN only)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
    public ResponseEntity<List<AssignmentDTO>> getAllAssignments() {
        return ResponseEntity.ok(assignmentService.getAllAssignments());
    }

    /**
     * GET /api/assignments/my - αναθέσεις του τρέχοντος χρήστη (TEACHER accessible)
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER', 'TEACHER')")
    public ResponseEntity<List<AssignmentDTO>> getMyAssignments() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            System.out.println("🔍 Getting assignments for user: " + username);
            
            //για τώρα επιστρέφουμε όλες - μπορείς να το φιλτράρεις μετά
            List<AssignmentDTO> assignments = assignmentService.getAllAssignments();
            
            System.out.println("📋 Found " + assignments.size() + " assignments");
            
            return ResponseEntity.ok(assignments);
            
        } catch (Exception e) {
            System.out.println("❌ Error getting my assignments: " + e.getMessage());
            return ResponseEntity.ok(List.of()); //επιστρεφουμε κενη λιστα
        }
    }

    /**
     * GET /api/assignments/schedule/{scheduleId} - αναθέσεις για πρόγραμμα (TEACHER accessible για προβολή)
     */
    @GetMapping("/schedule/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER', 'TEACHER')")
    public ResponseEntity<List<AssignmentDTO>> getAssignmentsBySchedule(@PathVariable Long scheduleId) {
        try {
            System.out.println("📋 Getting assignments for schedule: " + scheduleId);
            
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            
            System.out.println("✅ Found " + assignments.size() + " assignments for schedule " + scheduleId);
            
            return ResponseEntity.ok(assignments);
            
        } catch (Exception e) {
            System.out.println("❌ Error getting assignments for schedule " + scheduleId + ": " + e.getMessage());
            return ResponseEntity.ok(List.of()); //επιστρεφουμε κενη λιστα
        }
    }

    /**
     * GET /api/assignments/teacher/{teacherId} - αναθέσεις καθηγητή
     */
    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER', 'TEACHER')")
    public ResponseEntity<List<AssignmentDTO>> getTeacherAssignments(@PathVariable Long teacherId) {
        try {
            System.out.println("👨‍🏫 Getting assignments for teacher: " + teacherId);
            
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsByTeacher(teacherId);
            
            System.out.println("✅ Found " + assignments.size() + " assignments for teacher " + teacherId);
            
            return ResponseEntity.ok(assignments);
            
        } catch (Exception e) {
            System.out.println("❌ Error getting assignments for teacher " + teacherId + ": " + e.getMessage());
            return ResponseEntity.ok(List.of()); //επιστρεφουμε κενη λιστα
        }
    }

    /**
     * GET /api/assignments/teacher/{teacherId}/schedule/{scheduleId}
     */
    @GetMapping("/teacher/{teacherId}/schedule/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER', 'TEACHER')")
    public ResponseEntity<List<AssignmentDTO>> getTeacherScheduleAssignments(
            @PathVariable Long teacherId,
            @PathVariable Long scheduleId) {
        try {
            System.out.println("👨‍🏫📋 Getting assignments for teacher " + teacherId + " and schedule " + scheduleId);
            
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsByTeacherAndSchedule(teacherId, scheduleId);
            
            System.out.println("✅ Found " + assignments.size() + " assignments");
            
            return ResponseEntity.ok(assignments);
            
        } catch (Exception e) {
            System.out.println("❌ Error getting teacher schedule assignments: " + e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * POST /api/assignments - δημιουργία ανάθεσης (ADMIN only)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
    public ResponseEntity<AssignmentDTO> createAssignment(
            @RequestParam Long courseId,
            @RequestParam Long teacherId,
            @RequestParam String courseComponent,
            @RequestParam(required = false) Long scheduleId) {
        
        Course.TeachingHours.CourseComponent component = 
            Course.TeachingHours.CourseComponent.valueOf(courseComponent);
        
        AssignmentDTO assignment = assignmentService.createAssignment(
            courseId, teacherId, component, scheduleId);
        
        return ResponseEntity.ok(assignment);
    }

    /**
     * DELETE /api/assignments/{id} - διαγραφή ανάθεσης (ADMIN only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        assignmentService.deleteAssignment(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/assignments/recent - πρόσφατες αναθέσεις (όλοι)
     */
    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER', 'TEACHER')")
    public ResponseEntity<List<AssignmentDTO>> getRecentAssignments(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<AssignmentDTO> assignments = assignmentService.getRecentAssignments(limit);
            return ResponseEntity.ok(assignments);
        } catch (Exception e) {
            System.out.println("❌ Error getting recent assignments: " + e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * GET /api/assignments/stats/{scheduleId} - στατιστικά αναθέσεων (όλοι)
     */
    @GetMapping("/stats/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER', 'TEACHER')")
    public ResponseEntity<Map<String, Object>> getAssignmentStats(@PathVariable Long scheduleId) {
        try {
            Map<String, Object> stats = assignmentService.getAssignmentStatistics(scheduleId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.out.println("❌ Error getting assignment stats: " + e.getMessage());
            return ResponseEntity.ok(Map.of());
        }
    }

    /**
     * GET /api/assignments/count/{scheduleId} - μετρητής αναθέσεων (όλοι)
     */
    @GetMapping("/count/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER', 'TEACHER')")
    public ResponseEntity<Long> getAssignmentCount(@PathVariable Long scheduleId) {
        try {
            long count = assignmentService.countAssignmentsBySchedule(scheduleId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            System.out.println("❌ Error getting assignment count: " + e.getMessage());
            return ResponseEntity.ok(0L);
        }
    }
    
    /**
     * GET /api/assignments/assigned-course-ids - λίστα ids μαθημάτων που έχουν ήδη ανατεθεί για συγκεκριμένο χρονοπρογραμματισμό
     */
    @GetMapping("/assigned-course-ids")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
    public ResponseEntity<List<Long>> getAssignedCourseIds(@RequestParam(required = false) Long scheduleId) {
        try {
            List<Long> assignedIds = assignmentService.getAssignedCourseIdsBySchedule(scheduleId);
            return ResponseEntity.ok(assignedIds);
        } catch (Exception e) {
            System.out.println("❌ Error getting assigned course ids: " + e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }
}