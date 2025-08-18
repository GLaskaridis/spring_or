/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.controller;

import com.icsd.springor.DTO.TeacherPreferenceDTO;
import com.icsd.springor.DTO.AssignmentDTO;
import com.icsd.springor.model.TeacherPreference;
import com.icsd.springor.model.Room;
import com.icsd.springor.model.User;
import com.icsd.springor.service.TeacherPreferenceService;
import com.icsd.springor.service.AssignmentService;
import com.icsd.springor.service.UserService;
import com.icsd.springor.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.icsd.springor.DTO.AssignmentDTO;
import com.icsd.springor.DTO.TeacherPreferenceDTO;
import com.icsd.springor.model.Room;
import com.icsd.springor.model.TeacherPreference;
import com.icsd.springor.model.User;
import com.icsd.springor.service.AssignmentService;
import com.icsd.springor.service.RoomService;
import com.icsd.springor.service.TeacherPreferenceService;
import com.icsd.springor.service.UserService;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static com.google.protobuf.JavaFeaturesProto.java;
import com.icsd.springor.DTO.AssignmentDTO;
import com.icsd.springor.DTO.TeacherPreferenceDTO;
import com.icsd.springor.model.Room;
import com.icsd.springor.model.TeacherPreference;
import com.icsd.springor.model.User;
import com.icsd.springor.service.AssignmentService;
import com.icsd.springor.service.RoomService;
import com.icsd.springor.service.TeacherPreferenceService;
import com.icsd.springor.service.UserService;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static com.google.protobuf.JavaFeaturesProto.java;
import com.icsd.springor.DTO.AssignmentDTO;
import com.icsd.springor.DTO.TeacherPreferenceDTO;
import com.icsd.springor.model.Room;
import com.icsd.springor.model.TeacherPreference;
import com.icsd.springor.model.User;
import com.icsd.springor.service.AssignmentService;
import com.icsd.springor.service.RoomService;
import com.icsd.springor.service.TeacherPreferenceService;
import com.icsd.springor.service.UserService;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/preferences")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherPreferencesController {
    
    @Autowired
    private TeacherPreferenceService preferenceService;
    
    @Autowired
    private AssignmentService assignmentService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private RoomService roomService;
    
   
    @GetMapping("")
    public String showPreferencesPage(Model model, Authentication auth) {
        try {
            String username = auth.getName();
            User teacher = userService.findByUsername(username);
            
            // Φόρτωση αναθέσεων διδάσκοντα
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsByTeacher(teacher.getId());
            
            // Φόρτωση διαθέσιμων αιθουσών
            List<Room> rooms = roomService.getAllRooms();
            
            model.addAttribute("assignments", assignments);
            model.addAttribute("rooms", rooms);
            model.addAttribute("preferenceTypes", TeacherPreference.PreferenceType.values());
            model.addAttribute("daysOfWeek", DayOfWeek.values());
            
            return "teacher-preferences";
        } catch (Exception e) {
            model.addAttribute("error", "Σφάλμα κατά τη φόρτωση των προτιμήσεων: " + e.getMessage());
            return "error";
        }
    }
    
  
    @GetMapping("/api/assignments/{assignmentId}/preferences")
    @ResponseBody
    public ResponseEntity<List<TeacherPreferenceDTO>> getPreferencesForAssignment(
            @PathVariable Long assignmentId, 
            Authentication auth) {
        try {
            // Έλεγχος ότι η ανάθεση ανήκει στον διδάσκοντα
            if (!isAssignmentOwnedByTeacher(assignmentId, auth)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            List<TeacherPreferenceDTO> preferences = preferenceService.getPreferencesByAssignment(assignmentId);
            return ResponseEntity.ok(preferences);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    
    @PostMapping("/api/assignments/{assignmentId}/preferences")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createPreference(
            @PathVariable Long assignmentId,
            @RequestBody TeacherPreferenceDTO preferenceDTO,
            Authentication auth) {
        try {
            // Έλεγχος ότι η ανάθεση ανήκει στον διδάσκοντα
            if (!isAssignmentOwnedByTeacher(assignmentId, auth)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Δεν έχετε δικαίωμα πρόσβασης"));
            }
            
            // Ορισμός του assignmentId
            preferenceDTO.setAssignmentId(assignmentId);
            
            // Δημιουργία προτίμησης
            TeacherPreferenceDTO savedPreference = preferenceService.createPreference(preferenceDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Η προτίμηση δημιουργήθηκε επιτυχώς");
            response.put("preference", savedPreference);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Σφάλμα κατά τη δημιουργία: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    @GetMapping("/api/preferences/{preferenceId}")
    @ResponseBody
    public ResponseEntity<TeacherPreferenceDTO> getPreference(
            @PathVariable Long preferenceId,
            Authentication auth) {
        try {
            TeacherPreferenceDTO preference = preferenceService.getPreferenceById(preferenceId);
            
            // Έλεγχος ότι η προτίμηση ανήκει στον διδάσκοντα
            if (!isAssignmentOwnedByTeacher(preference.getAssignmentId(), auth)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            return ResponseEntity.ok(preference);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    @PutMapping("/api/preferences/{preferenceId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updatePreference(
            @PathVariable Long preferenceId,
            @RequestBody TeacherPreferenceDTO preferenceDTO,
            Authentication auth) {
        try {
            // Έλεγχος ότι η προτίμηση ανήκει στον διδάσκοντα
            TeacherPreferenceDTO existingPreference = preferenceService.getPreferenceById(preferenceId);
            if (!isAssignmentOwnedByTeacher(existingPreference.getAssignmentId(), auth)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Δεν έχετε δικαίωμα πρόσβασης"));
            }
            
            // Ενημέρωση προτίμησης
            TeacherPreferenceDTO updatedPreference = preferenceService.updatePreference(preferenceId, preferenceDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Η προτίμηση ενημερώθηκε επιτυχώς");
            response.put("preference", updatedPreference);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Σφάλμα κατά την ενημέρωση: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
  
    @DeleteMapping("/api/preferences/{preferenceId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deletePreference(
            @PathVariable Long preferenceId,
            Authentication auth) {
        try {
            // Έλεγχος ότι η προτίμηση ανήκει στον διδάσκοντα
            TeacherPreferenceDTO preference = preferenceService.getPreferenceById(preferenceId);
            if (!isAssignmentOwnedByTeacher(preference.getAssignmentId(), auth)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Δεν έχετε δικαίωμα πρόσβασης"));
            }
            
            // Διαγραφή προτίμησης
            preferenceService.deletePreference(preferenceId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Η προτίμηση διαγράφηκε επιτυχώς");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Σφάλμα κατά τη διαγραφή: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
   
    @GetMapping("/api/rooms")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAvailableRooms() {
        try {
            List<Room> rooms = roomService.getAllRooms();
            
            List<Map<String, Object>> roomData = rooms.stream()
                .map(room -> {
                    Map<String, Object> roomInfo = new HashMap<>();
                    roomInfo.put("id", room.getId());
                    roomInfo.put("name", room.getName());
                    roomInfo.put("building", room.getBuilding());
                    roomInfo.put("capacity", room.getCapacity());
                    roomInfo.put("type", room.getType().toString());
                    return roomInfo;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(roomData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
 
    private boolean isAssignmentOwnedByTeacher(Long assignmentId, Authentication auth) {
        try {
            String username = auth.getName();
            User teacher = userService.findByUsername(username);
            
            List<AssignmentDTO> teacherAssignments = assignmentService.getAssignmentsByTeacher(teacher.getId());
            
            return teacherAssignments.stream()
                .anyMatch(assignment -> assignment.getId().equals(assignmentId));
        } catch (Exception e) {
            return false;
        }
    }
}