package com.icsd.springor.controller;

import com.icsd.springor.DTO.TeacherPreferenceDTO;
import com.icsd.springor.service.TeacherPreferenceService;
import com.icsd.springor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Απλοποιημένος Controller για διαχείριση Προτιμήσεων Καθηγητών
 * Χρησιμοποιεί ΜΟΝΟ τα ΗΔΗ ΥΠΑΡΧΟΝΤΑ methods από το TeacherPreferenceService
 */
@RestController
@RequestMapping("/api/preferences")
public class PreferenceController {

    @Autowired
    private TeacherPreferenceService preferenceService;

    @Autowired
    private UserService userService;

    /**
     * GET /api/preferences?scheduleId={id} - Όλες οι προτιμήσεις για πρόγραμμα
     * Χρησιμοποιεί: preferenceService.getPreferencesBySchedule(scheduleId)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
    public ResponseEntity<List<TeacherPreferenceDTO>> getAllPreferences(
            @RequestParam Long scheduleId) {
        return ResponseEntity.ok(
            preferenceService.getPreferencesBySchedule(scheduleId));
    }

    /**
     * GET /api/preferences/my - Οι δικές μου προτιμήσεις
     * Χρησιμοποιεί: preferenceService.getPreferencesByTeacherAndSchedule(teacherId, scheduleId)
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<TeacherPreferenceDTO>> getMyPreferences(
            @RequestParam Long scheduleId,
            Authentication authentication) {
        
        Long teacherId = userService.getCurrentUserId(authentication);
        return ResponseEntity.ok(
            preferenceService.getPreferencesByTeacherAndSchedule(teacherId, scheduleId));
    }

    /**
     * GET /api/preferences/teacher/{teacherId} - Προτιμήσεις συγκεκριμένου καθηγητή
     * Χρησιμοποιεί: preferenceService.getPreferencesByTeacher(teacherId)
     */
    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
    public ResponseEntity<List<TeacherPreferenceDTO>> getTeacherPreferences(
            @PathVariable Long teacherId) {
        return ResponseEntity.ok(
            preferenceService.getPreferencesByTeacher(teacherId));
    }

    /**
     * GET /api/preferences/assignment/{assignmentId} - Προτιμήσεις για ανάθεση
     * Χρησιμοποιεί: preferenceService.getPreferencesByAssignment(assignmentId)
     */
    @GetMapping("/assignment/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER', 'TEACHER')")
    public ResponseEntity<List<TeacherPreferenceDTO>> getAssignmentPreferences(
            @PathVariable Long assignmentId) {
        return ResponseEntity.ok(
            preferenceService.getPreferencesByAssignment(assignmentId));
    }

    /**
     * POST /api/preferences - Δημιουργία προτίμησης
     * Χρησιμοποιεί: preferenceService.createPreference(preferenceDTO)
     */
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TeacherPreferenceDTO> createPreference(
            @RequestBody TeacherPreferenceDTO preferenceDTO) {

        // Validate that assignmentId is provided
        if (preferenceDTO.getAssignmentId() == null) {
            throw new RuntimeException("Assignment ID is required for creating preferences");
        }

        return ResponseEntity.ok(preferenceService.createPreference(preferenceDTO));
    }

    /**
     * PUT /api/preferences/{id} - Ενημέρωση προτίμησης
     * Χρησιμοποιεί: preferenceService.updatePreference(id, preferenceDTO)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TeacherPreferenceDTO> updatePreference(
            @PathVariable Long id,
            @RequestBody TeacherPreferenceDTO preferenceDTO) {
        return ResponseEntity.ok(
            preferenceService.updatePreference(id, preferenceDTO));
    }

    /**
     * DELETE /api/preferences/{id} - Διαγραφή προτίμησης
     * Χρησιμοποιεί: preferenceService.deletePreference(id)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> deletePreference(@PathVariable Long id) {
        preferenceService.deletePreference(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/preferences/check-complete?scheduleId={id} - Έλεγχος ολοκλήρωσης
     * Χρησιμοποιεί: preferenceService.areAllPreferencesProvided(scheduleId)
     */
    @GetMapping("/check-complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER')")
    public ResponseEntity<Boolean> checkPreferencesComplete(
            @RequestParam Long scheduleId) {
        return ResponseEntity.ok(
            preferenceService.areAllPreferencesProvided(scheduleId));
    }
}