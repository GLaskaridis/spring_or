package com.icsd.springor.controller;

import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.service.CourseScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Απλοποιημένος Controller για διαχείριση Προγραμμάτων (Schedules)
 * Χρησιμοποιεί ΜΟΝΟ τα ΠΡΑΓΜΑΤΙΚΑ ΥΠΑΡΧΟΝΤΑ methods από το CourseScheduleService
 */
@RestController
@RequestMapping("/api/schedules")
@PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
public class ScheduleController {

    @Autowired
    private CourseScheduleService scheduleService;

    /**
     * GET /api/schedules - Λήψη όλων των προγραμμάτων
     * Χρησιμοποιεί: scheduleService.getAllSchedules()
     */
    @GetMapping
    public ResponseEntity<List<CourseSchedule>> getAllSchedules() {
        return ResponseEntity.ok(scheduleService.getAllSchedules());
    }

    /**
     * GET /api/schedules/{id} - Λήψη συγκεκριμένου προγράμματος
     * Χρησιμοποιεί: scheduleService.getScheduleById(id)
     */
    @GetMapping("/{id}")
    public ResponseEntity<CourseSchedule> getSchedule(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.getScheduleById(id));
    }

    /**
     * POST /api/schedules - Δημιουργία νέου προγράμματος (overload 1)
     * Χρησιμοποιεί: scheduleService.createSchedule(name, semester, startTime, endTime, maxHoursPerDay, maxDistanceKm)
     */
    @PostMapping
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
     * POST /api/schedules/simple - Δημιουργία νέου προγράμματος (overload 2)
     * Χρησιμοποιεί: scheduleService.createSchedule(name, semester, year, description)
     */
    @PostMapping("/simple")
    public ResponseEntity<CourseSchedule> createSimpleSchedule(
            @RequestParam String name,
            @RequestParam String semester,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String description) {
        
        CourseSchedule schedule = scheduleService.createSchedule(
            name, semester, year, description);
        
        return ResponseEntity.ok(schedule);
    }

    /**
     * PUT /api/schedules/{id} - Ενημέρωση προγράμματος
     * Χρησιμοποιεί: scheduleService.updateSchedule(scheduleId, name, semester, year, description, active)
     */
    @PutMapping("/{id}")
    public ResponseEntity<CourseSchedule> updateSchedule(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String semester,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "true") boolean active) {
        
        CourseSchedule updated = scheduleService.updateSchedule(
            id, name, semester, year, description, active);
        
        return ResponseEntity.ok(updated);
    }

    /**
     * PUT /api/schedules/{id}/status - Αλλαγή κατάστασης προγράμματος
     * Χρησιμοποιεί: scheduleService.changeScheduleStatus(id, status)
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<CourseSchedule> updateStatus(
            @PathVariable Long id,
            @RequestParam CourseSchedule.ScheduleStatus status) {
        return ResponseEntity.ok(
            scheduleService.changeScheduleStatus(id, status));
    }

    /**
     * DELETE /api/schedules/{id} - Διαγραφή προγράμματος
     * Χρησιμοποιεί: scheduleService.deleteSchedule(id)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/schedules/count - Μέτρηση active schedules
     * Χρησιμοποιεί: scheduleService.countActiveSchedules()
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countActiveSchedules() {
        return ResponseEntity.ok(scheduleService.countActiveSchedules());
    }

    /**
     * POST /api/schedules/{id}/save - Save/persist schedule
     * Χρησιμοποιεί: scheduleService.saveSchedule(schedule)
     */
    @PostMapping("/{id}/save")
    public ResponseEntity<CourseSchedule> saveSchedule(
            @PathVariable Long id,
            @RequestBody CourseSchedule schedule) {
        schedule.setId(id);
        return ResponseEntity.ok(scheduleService.saveSchedule(schedule));
    }
}