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

/**
 * Απλοποιημένος Controller για διαχείριση Αναθέσεων
 * Χρησιμοποιεί ΜΟΝΟ τα ΠΡΑΓΜΑΤΙΚΑ ΥΠΑΡΧΟΝΤΑ methods από το AssignmentService
 */
@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    @Autowired
    private AssignmentService assignmentService;

    /**
     * GET /api/assignments - Όλες οι αναθέσεις
     * Χρησιμοποιεί: assignmentService.getAllAssignments()
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
    public ResponseEntity<List<AssignmentDTO>> getAllAssignments() {
        return ResponseEntity.ok(assignmentService.getAllAssignments());
    }

    /**
     * GET /api/assignments/schedule/{scheduleId} - Αναθέσεις για πρόγραμμα
     * Χρησιμοποιεί: assignmentService.getAssignmentsBySchedule(scheduleId)
     */
      @GetMapping("/schedule/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
    public ResponseEntity<List<AssignmentDTO>> getAssignmentsBySchedule(
            @PathVariable Long scheduleId) {
        return ResponseEntity.ok(
            assignmentService.getAssignmentsBySchedule(scheduleId));
    }

    /**
     * GET /api/assignments/{id} - Συγκεκριμένη ανάθεση
     * ΔΕΝ υπάρχει getAssignmentById() - χρησιμοποιούμε search
     */
      @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
    public ResponseEntity<AssignmentDTO> getAssignment(@PathVariable Long id) {
        // Fallback: search through all assignments
        List<AssignmentDTO> all = assignmentService.getAllAssignments();
        return all.stream()
            .filter(a -> a.getId().equals(id))
            .findFirst()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/assignments - Δημιουργία νέας ανάθεσης
     * Χρησιμοποιεί: assignmentService.createAssignment(courseId, teacherId, component, scheduleId)
     */
        @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
    public ResponseEntity<AssignmentDTO> createAssignment(
            @RequestParam Long courseId,
            @RequestParam Long teacherId,
            @RequestParam String courseComponent,
            @RequestParam Long scheduleId) {
        
        Course.TeachingHours.CourseComponent component = 
            Course.TeachingHours.CourseComponent.valueOf(courseComponent);
        
        AssignmentDTO assignment = assignmentService.createAssignment(
            courseId, teacherId, component, scheduleId);
        
        return ResponseEntity.ok(assignment);
    }
    /**
     * PUT /api/assignments/{id} - Ενημέρωση ανάθεσης (αλλαγή καθηγητή)
     * Χρησιμοποιεί: assignmentService.updateAssignment(id, newTeacherId)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
    public ResponseEntity<AssignmentDTO> updateAssignment(
            @PathVariable Long id,
            @RequestParam Long teacherId) {
        
        AssignmentDTO updated = assignmentService.updateAssignment(id, teacherId);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/assignments/{id} - Διαγραφή ανάθεσης
     * Χρησιμοποιεί: assignmentService.deleteAssignment(id)
     */
      @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        assignmentService.deleteAssignment(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/assignments/teacher/{teacherId} - Αναθέσεις καθηγητή
     * Χρησιμοποιεί: assignmentService.getAssignmentsByTeacher(teacherId)
     */
     @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER', 'TEACHER')")
    public ResponseEntity<List<AssignmentDTO>> getTeacherAssignments(
            @PathVariable Long teacherId) {
        return ResponseEntity.ok(
            assignmentService.getAssignmentsByTeacher(teacherId));
    }

    /**
     * GET /api/assignments/teacher/{teacherId}/schedule/{scheduleId}
     * Χρησιμοποιεί: assignmentService.getAssignmentsByTeacherAndSchedule(teacherId, scheduleId)
     */
    @GetMapping("/teacher/{teacherId}/schedule/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER', 'TEACHER')")
    public ResponseEntity<List<AssignmentDTO>> getTeacherScheduleAssignments(
            @PathVariable Long teacherId,
            @PathVariable Long scheduleId) {
        return ResponseEntity.ok(
            assignmentService.getAssignmentsByTeacherAndSchedule(teacherId, scheduleId));
    }

    /**
     * GET /api/assignments/recent - Πρόσφατες αναθέσεις
     * Χρησιμοποιεί: assignmentService.getRecentAssignments(limit)
     */
    @GetMapping("/recent")
    public ResponseEntity<List<AssignmentDTO>> getRecentAssignments(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(assignmentService.getRecentAssignments(limit));
    }

    /**
     * GET /api/assignments/stats/{scheduleId} - Στατιστικά αναθέσεων
     * Χρησιμοποιεί: assignmentService.getAssignmentStatistics(scheduleId)
     */
    @GetMapping("/stats/{scheduleId}")
    public ResponseEntity<Map<String, Object>> getAssignmentStats(
            @PathVariable Long scheduleId) {
        return ResponseEntity.ok(
            assignmentService.getAssignmentStatistics(scheduleId));
    }

    /**
     * GET /api/assignments/search - Αναζήτηση με φίλτρα
     * Χρησιμοποιεί: assignmentService.getFilteredAssignments(...)
     */
    @GetMapping("/search")
    public ResponseEntity<List<AssignmentDTO>> searchAssignments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String component) {
        
        return ResponseEntity.ok(
            assignmentService.getFilteredAssignments(
                search, scheduleId, teacherId, courseId, component));
    }

    /**
     * GET /api/assignments/exists - Έλεγχος ύπαρξης ανάθεσης
     * Χρησιμοποιεί: assignmentService.existsAssignment(...)
     */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkAssignmentExists(
            @RequestParam Long courseId,
            @RequestParam String courseComponent,
            @RequestParam Long scheduleId) {
        
        return ResponseEntity.ok(
            assignmentService.existsAssignment(courseId, courseComponent, scheduleId));
    }

    /**
     * GET /api/assignments/by-course - Ανάθεση για συγκεκριμένο μάθημα & component
     * Χρησιμοποιεί: assignmentService.getAssignmentByCourseAndComponent(...)
     */
    @GetMapping("/by-course")
    public ResponseEntity<AssignmentDTO> getAssignmentByCourse(
            @RequestParam Long courseId,
            @RequestParam String courseComponent,
            @RequestParam Long scheduleId) {
        
        return ResponseEntity.ok(
            assignmentService.getAssignmentByCourseAndComponent(
                courseId, courseComponent, scheduleId));
    }

    /**
     * GET /api/assignments/check-complete/{scheduleId} - Έλεγχος πληρότητας αναθέσεων
     * Χρησιμοποιεί: assignmentService.areAllCoursesAssigned(scheduleId)
     */
    @GetMapping("/check-complete/{scheduleId}")
    public ResponseEntity<Boolean> checkAssignmentsComplete(
            @PathVariable Long scheduleId) {
        return ResponseEntity.ok(
            assignmentService.areAllCoursesAssigned(scheduleId));
    }
    
    @RestController
    @RequestMapping("/assignments/api")
    class TeacherAssignmentController {

        @Autowired
        private AssignmentService assignmentService;

        @Autowired
        private com.icsd.springor.service.UserService userService;

        /**
         * GET /assignments/api/my-assignments - Οι αναθέσεις του τρέχοντος καθηγητή
         */
        @GetMapping("/my-assignments")
        @PreAuthorize("hasRole('TEACHER')")
        public ResponseEntity<List<AssignmentDTO>> getMyAssignments(
                org.springframework.security.core.Authentication authentication) {
            Long teacherId = userService.getCurrentUserId(authentication);
            return ResponseEntity.ok(assignmentService.getAssignmentsByTeacher(teacherId));
        }
    }
    
    
}