package com.icsd.springor.controller;

import com.icsd.springor.model.Course;
import com.icsd.springor.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Απλοποιημένος Controller για διαχείριση Μαθημάτων
 * Χρησιμοποιεί ΜΟΝΟ τα ΗΔΗ ΥΠΑΡΧΟΝΤΑ methods από το CourseService
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    /**
     * GET /api/courses - Λήψη όλων των μαθημάτων
     * Χρησιμοποιεί: courseService.getAllCourses()
     */
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    /**
     * GET /api/courses/active - Λήψη μόνο ενεργών μαθημάτων
     * Χρησιμοποιεί: courseService.getAllActiveCourses()
     */
    @GetMapping("/active")
    public ResponseEntity<List<Course>> getActiveCourses() {
        return ResponseEntity.ok(courseService.getAllActiveCourses());
    }

    /**
     * GET /api/courses/{id} - Λήψη συγκεκριμένου μαθήματος
     * Χρησιμοποιεί: courseService.getCourseById(id)
     */
    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourse(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    /**
     * POST /api/courses - Δημιουργία νέου μαθήματος
     * Χρησιμοποιεί: courseService.addCourse(course)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        return ResponseEntity.ok(courseService.addCourse(course));
    }

    /**
     * PUT /api/courses/{id} - Ενημέρωση μαθήματος
     * Χρησιμοποιεί: courseService.updateCourse(id, course)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
    public ResponseEntity<Course> updateCourse(
            @PathVariable Long id,
            @RequestBody Course course) {
        return ResponseEntity.ok(courseService.updateCourse(id, course));
    }

    /**
     * DELETE /api/courses/{id} - Διαγραφή μαθήματος
     * Χρησιμοποιεί: courseService.deleteCourse(id)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/courses/{id}/deactivate - Απενεργοποίηση μαθήματος
     * Χρησιμοποιεί: courseService.deactivateCourse(id)
     */
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
    public ResponseEntity<Void> deactivateCourse(@PathVariable Long id) {
        courseService.deactivateCourse(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/courses/count - Πλήθος μαθημάτων
     * Χρησιμοποιεί: courseService.countAllCourses()
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countCourses() {
        return ResponseEntity.ok(courseService.countAllCourses());
    }
}