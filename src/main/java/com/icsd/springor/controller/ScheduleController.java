package com.icsd.springor.controller;

import static com.google.protobuf.JavaFeaturesProto.java;
import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.service.CourseScheduleService;
import com.icsd.springor.service.ScheduleResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/schedules")
@PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
public class ScheduleController {

    @Autowired
    private CourseScheduleService scheduleService;
    
    @Autowired
    private ScheduleResultService scheduleResultService;

    /**
     * GET /api/schedules - Λήψη όλων των προγραμμάτων (TEACHERS μπορούν να δουν)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER', 'TEACHER')")
    public ResponseEntity<List<CourseSchedule>> getAllSchedules() {
        try {
            System.out.println("📅 Getting all schedules");
            List<CourseSchedule> schedules = scheduleService.getAllSchedules();
            System.out.println("✅ Found " + schedules.size() + " schedules");
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            System.out.println("❌ Error getting schedules: " + e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * GET /api/schedules/{id} - Λήψη συγκεκριμένου προγράμματος (TEACHERS μπορούν να δουν)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER', 'TEACHER')")
    public ResponseEntity<CourseSchedule> getSchedule(@PathVariable Long id) {
        try {
            System.out.println("📅 Getting schedule: " + id);
            CourseSchedule schedule = scheduleService.getScheduleById(id);
            return ResponseEntity.ok(schedule);
        } catch (Exception e) {
            System.out.println("❌ Error getting schedule " + id + ": " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/schedules - Δημιουργία νέου προγράμματος (ADMIN only)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
    public ResponseEntity<CourseSchedule> createSchedule(
            @RequestParam String name,
            @RequestParam String semester,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Integer maxHoursPerDay,
            @RequestParam(required = false) Double maxDistanceKm) {
        
        CourseSchedule schedule = scheduleService.createSchedule(
            name, semester, startTime, endTime, maxHoursPerDay, maxDistanceKm);
        return ResponseEntity.ok(schedule);
    }

    /**
     * PUT /api/schedules/{id}/status - Αλλαγή κατάστασης προγράμματος (ADMIN only)
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
    public ResponseEntity<CourseSchedule> changeScheduleStatus(
            @PathVariable Long id,
            @RequestParam CourseSchedule.ScheduleStatus status) {
        
        CourseSchedule schedule = scheduleService.changeScheduleStatus(id, status);
        return ResponseEntity.ok(schedule);
    }

    /**
     * DELETE /api/schedules/{id} - Διαγραφή προγράμματος (ADMIN only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.ok().build();
    }

    // ================================
    // SCHEDULE RESULTS ENDPOINTS - TEACHER ACCESSIBLE
    // ================================

    /**
     * GET /api/schedules/{id}/results - Επιστρέφει τα αποτελέσματα (TEACHERS μπορούν να δουν)
     */
    @GetMapping("/{id}/results")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER', 'TEACHER')")
    public ResponseEntity<List<ScheduleResultService.ScheduleDisplayDTO>> getScheduleResults(@PathVariable Long id) {
        try {
            System.out.println("📊 Getting schedule results for: " + id);
            
            // Έλεγχος ότι υπάρχει ο χρονοπρογραμματισμός
            CourseSchedule schedule = scheduleService.getScheduleById(id);
            
            // Έλεγχος ότι υπάρχουν αποτελέσματα
            if (!scheduleResultService.hasScheduleResults(id)) {
                System.out.println("⚠️ No results found for schedule " + id);
                return ResponseEntity.notFound().build();
            }
            
            // Φόρτωση αποτελεσμάτων
            List<ScheduleResultService.ScheduleDisplayDTO> results = 
                scheduleResultService.getScheduleForDisplay(id);
            
            System.out.println("✅ Found " + results.size() + " results for schedule " + id);
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            System.out.println("❌ Error getting schedule results for " + id + ": " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/schedules/{id}/results/exists - Ελέγχει αν υπάρχουν αποτελέσματα (TEACHERS μπορούν να δουν)
     */
    @GetMapping("/{id}/results/exists")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER', 'TEACHER')")
    public ResponseEntity<Map<String, Object>> checkScheduleResults(@PathVariable Long id) {
        try {
            System.out.println("🔍 Checking schedule results for: " + id);
            
            CourseSchedule schedule = scheduleService.getScheduleById(id);
            boolean hasResults = scheduleResultService.hasScheduleResults(id);
            long resultCount = scheduleResultService.countScheduleResults(id);
            
            Map<String, Object> response = Map.of(
                "hasResults", hasResults,
                "resultCount", resultCount,
                "scheduleStatus", schedule.getStatus().name(),
                "canExecute", schedule.getStatus() == CourseSchedule.ScheduleStatus.EXECUTION_PHASE && !hasResults,
                "canApprove", schedule.getStatus() == CourseSchedule.ScheduleStatus.SOLUTION_FOUND && hasResults,
                "isApproved", schedule.getStatus() == CourseSchedule.ScheduleStatus.SOLUTION_APPROVED
            );
            
            System.out.println("✅ Schedule " + id + " check: hasResults=" + hasResults + ", count=" + resultCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error checking schedule results for " + id + ": " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * DELETE /api/schedules/{id}/results - Διαγραφή αποτελεσμάτων (ADMIN only)
     */
    @DeleteMapping("/{id}/results")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
    public ResponseEntity<Map<String, String>> deleteScheduleResults(@PathVariable Long id) {
        try {
            CourseSchedule schedule = scheduleService.getScheduleById(id);
            
            // Έλεγχος ότι μπορεί να διαγραφεί
            if (schedule.getStatus() == CourseSchedule.ScheduleStatus.SOLUTION_APPROVED) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Δεν μπορούν να διαγραφούν τα αποτελέσματα εγκεκριμένης λύσης"));
            }
            
            scheduleResultService.deleteScheduleResults(id);
            
            return ResponseEntity.ok(Map.of("message", "Τα αποτελέσματα διαγράφηκαν επιτυχώς"));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Σφάλμα κατά τη διαγραφή: " + e.getMessage()));
        }
    }

    /**
     * GET /api/schedules/active/count - Μετρά τα ενεργά προγράμματα (όλοι)
     */
    @GetMapping("/active/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER', 'TEACHER')")
    public ResponseEntity<Long> countActiveSchedules() {
        try {
            long count = scheduleService.countActiveSchedules();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            System.out.println("❌ Error counting active schedules: " + e.getMessage());
            return ResponseEntity.ok(0L);
        }
    }
}