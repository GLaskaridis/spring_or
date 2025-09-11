/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.icsd.springor.controller;

import com.icsd.springor.DTO.AssignmentDTO;
import com.icsd.springor.model.Assignment;
import com.icsd.springor.model.Course;
import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.model.User;
import com.icsd.springor.service.*;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN') or hasRole('PROGRAM_MANAGER')")
public class AdminController {

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserService userService;

    @Autowired
    private CourseScheduleService scheduleService;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        try {
            // Στατιστικά στοιχεία για το dashboard
            long totalUsers = userService.countAllUsers();
            long totalCourses = courseService.countAllCourses();
            long totalRooms = 0; // Αν έχετε RoomService
            long activeSchedules = scheduleService.countActiveSchedules();

            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("totalCourses", totalCourses);
            model.addAttribute("totalRooms", totalRooms);
            model.addAttribute("activeSchedules", activeSchedules);

            // Πρόσφατη δραστηριότητα
            List<AssignmentDTO> recentAssignments = assignmentService.getRecentAssignments(10);
            model.addAttribute("recentAssignments", recentAssignments);

            return "admin_dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Σφάλμα κατά τη φόρτωση του dashboard: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Εμφάνιση της σελίδας διαχείρισης αναθέσεων
     */
    @GetMapping("/assignments")
    public String assignmentsManagement(Model model) {
        try {
            // Φόρτωση δεδομένων για τη σελίδα
            List<CourseSchedule> schedules = scheduleService.getAllSchedules();
            List<Course> courses = courseService.getAllActiveCourses();
            List<User> teachers = userService.findAllActiveTeachers();
            List<AssignmentDTO> allAssignments = assignmentService.getAllAssignments();

            // Στατιστικά
            long totalAssignments = allAssignments.size();
            long activeAssignments = allAssignments.stream()
                    .filter(a -> a.isActive())
                    .count();
            long theoryAssignments = allAssignments.stream()
                    .filter(a -> a.getCourseComponent() == Course.TeachingHours.CourseComponent.THEORY)
                    .count();
            long labAssignments = allAssignments.stream()
                    .filter(a -> a.getCourseComponent() == Course.TeachingHours.CourseComponent.LABORATORY)
                    .count();

            model.addAttribute("schedules", schedules);
            model.addAttribute("courses", courses);
            model.addAttribute("teachers", teachers);
            model.addAttribute("assignments", allAssignments);
            model.addAttribute("totalAssignments", totalAssignments);
            model.addAttribute("activeAssignments", activeAssignments);
            model.addAttribute("theoryAssignments", theoryAssignments);
            model.addAttribute("labAssignments", labAssignments);
            model.addAttribute("courseComponents", Course.TeachingHours.CourseComponent.values());

            return "manage_assignments";
        } catch (Exception e) {
            model.addAttribute("error", "Σφάλμα κατά τη φόρτωση των αναθέσεων: " + e.getMessage());
            return "error";
        }
    }

    /**
     * API endpoint για λήψη όλων των αναθέσεων (για DataTables)
     */
    @GetMapping("/api/assignments")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAssignmentsData(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String component) {

        try {
            List<AssignmentDTO> assignments = assignmentService.getFilteredAssignments(
                    search, scheduleId, teacherId, courseId, component);

            Map<String, Object> response = new HashMap<>();
            response.put("data", assignments);
            response.put("recordsTotal", assignments.size());
            response.put("recordsFiltered", assignments.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Δημιουργία νέας ανάθεσης
     */
    @PostMapping("/api/assignments")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createAssignment(
            @RequestParam Long scheduleId,
            @RequestParam Long courseId,
            @RequestParam String courseComponent,
            @RequestParam Long teacherId,
            @RequestParam(required = false) String notes,
            @RequestParam(defaultValue = "true") boolean active) {

        try {
            Course.TeachingHours.CourseComponent component
                    = Course.TeachingHours.CourseComponent.valueOf(courseComponent);

            AssignmentDTO assignment = assignmentService.createAssignment(
                    courseId, teacherId, component, scheduleId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Η ανάθεση δημιουργήθηκε επιτυχώς");
            response.put("assignment", assignment);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Σφάλμα κατά τη δημιουργία: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Ενημέρωση ανάθεσης
     */
    @PutMapping("/api/assignments/{assignmentId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateAssignment(
            @PathVariable Long assignmentId,
            @RequestParam Long teacherId,
            @RequestParam(required = false) String notes,
            @RequestParam(defaultValue = "true") boolean active) {

        try {
            AssignmentDTO assignment = assignmentService.updateAssignment(assignmentId, teacherId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Η ανάθεση ενημερώθηκε επιτυχώς");
            response.put("assignment", assignment);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Σφάλμα κατά την ενημέρωση: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Διαγραφή ανάθεσης
     */
    @DeleteMapping("/api/assignments/{assignmentId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteAssignment(@PathVariable Long assignmentId) {
        try {
            assignmentService.deleteAssignment(assignmentId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Η ανάθεση διαγράφηκε επιτυχώς");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Σφάλμα κατά τη διαγραφή: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Λήψη πληροφοριών μαθήματος
     */
    @GetMapping("/api/courses/{courseId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCourseInfo(@PathVariable Long courseId) {
        try {
            Course course = courseService.getCourseById(courseId);

            Map<String, Object> courseInfo = new HashMap<>();
            courseInfo.put("id", course.getId());
            courseInfo.put("name", course.getName());
            courseInfo.put("code", course.getCode());
            courseInfo.put("semester", course.getSemester() + "ο Εξάμηνο");
            courseInfo.put("year", course.getYear() + "ο Έτος");
            courseInfo.put("type", course.getType().toString());
            courseInfo.put("isRequired", course.getType() == Course.CourseType.BASIC);
            courseInfo.put("capacity", course.getCapacity());

            // Ώρες διδασκαλίας
            if (course.getTeachingHours() != null && !course.getTeachingHours().isEmpty()) {
                Map<String, Object> hours = new HashMap<>();
                int theoryHours = 0;
                int labHours = 0;

                for (Course.TeachingHours th : course.getTeachingHours()) {
                    if (th.getComponent() == Course.TeachingHours.CourseComponent.THEORY) {
                        theoryHours = th.getHours() != null ? th.getHours() : 0;
                    } else if (th.getComponent() == Course.TeachingHours.CourseComponent.LABORATORY) {
                        labHours = th.getHours() != null ? th.getHours() : 0;
                    }
                }

                hours.put("theory", theoryHours);
                hours.put("lab", labHours);
                hours.put("total", theoryHours + labHours);
                courseInfo.put("teachingHours", hours);
            }

            return ResponseEntity.ok(courseInfo);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    /**
     * Έλεγχος για υπάρχουσα ανάθεση
     */
    @GetMapping("/api/assignments/check")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkExistingAssignment(
            @RequestParam Long courseId,
            @RequestParam String courseComponent,
            @RequestParam Long scheduleId) {

        try {
            boolean exists = assignmentService.existsAssignment(courseId, courseComponent, scheduleId);

            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);

            if (exists) {
                AssignmentDTO existingAssignment = assignmentService.getAssignmentByCourseAndComponent(
                        courseId, courseComponent, scheduleId);
                response.put("existingAssignment", existingAssignment);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Λήψη διαθέσιμων διδασκόντων για ένα μάθημα
     */
    @GetMapping("/api/courses/{courseId}/available-teachers")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAvailableTeachers(
            @PathVariable Long courseId,
            @RequestParam(required = false) Long excludeTeacherId) {

        try {
            List<User> teachers = userService.findAllActiveTeachers();

            // Φιλτράρισμα αν χρειάζεται
            if (excludeTeacherId != null) {
                teachers = teachers.stream()
                        .filter(t -> !t.getId().equals(excludeTeacherId))
                        .collect(Collectors.toList());
            }

            List<Map<String, Object>> teacherList = teachers.stream()
                    .map(teacher -> {
                        Map<String, Object> teacherInfo = new HashMap<>();
                        teacherInfo.put("id", teacher.getId());
                        teacherInfo.put("name", teacher.getFullName());
                        teacherInfo.put("type", teacher.getTeacherType());
                        teacherInfo.put("rank", teacher.getTeacherRank());
                        return teacherInfo;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(teacherList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * API endpoint για λήψη όλων των διδασκόντων
     */
    @GetMapping("/api/teachers")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllTeachers() {
        try {
            List<User> teachers = userService.findAllActiveTeachers();

            List<Map<String, Object>> teacherList = teachers.stream()
                    .map(teacher -> {
                        Map<String, Object> teacherInfo = new HashMap<>();
                        teacherInfo.put("id", teacher.getId());
                        teacherInfo.put("name", teacher.getFullName());
                        teacherInfo.put("type", teacher.getTeacherType() != null ? teacher.getTeacherType().toString() : "");
                        teacherInfo.put("rank", teacher.getTeacherRank() != null ? teacher.getTeacherRank().toString() : "");
                        teacherInfo.put("email", teacher.getEmail());
                        return teacherInfo;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(teacherList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * API endpoint για λήψη όλων των μαθημάτων
     */
    @GetMapping("/api/courses")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllCourses() {
        try {
            List<Course> courses = courseService.getAllActiveCourses();

            List<Map<String, Object>> courseList = courses.stream()
                    .map(course -> {
                        Map<String, Object> courseInfo = new HashMap<>();
                        courseInfo.put("id", course.getId());
                        courseInfo.put("name", course.getName());
                        courseInfo.put("code", course.getCode());
                        courseInfo.put("semester", course.getSemester());
                        courseInfo.put("year", course.getYear());
                        courseInfo.put("type", course.getType().toString());
                        courseInfo.put("capacity", course.getCapacity());
                        return courseInfo;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(courseList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

//    /**
//     * API endpoint για λήψη όλων των χρονοπρογραμματισμών
//     */
//    @GetMapping("/api/schedules")
//    @ResponseBody
//    public ResponseEntity<List<Map<String, Object>>> getAllSchedules() {
//        try {
//            List<CourseSchedule> schedules = scheduleService.getAllSchedules();
//            
//            List<Map<String, Object>> scheduleList = schedules.stream()
//                .map(schedule -> {
//                    Map<String, Object> scheduleInfo = new HashMap<>();
//                    scheduleInfo.put("id", schedule.getId());
//                    scheduleInfo.put("name", schedule.getName());
//                    scheduleInfo.put("status", schedule.getStatus().toString());
//                    scheduleInfo.put("semester", schedule.getSemester());
//                    scheduleInfo.put("year", schedule.getYear());
//                    return scheduleInfo;
//                })
//                .collect(Collectors.toList());
//            
//            return ResponseEntity.ok(scheduleList);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }
    /**
     * Δημιουργία νέου χρονοπρογραμματισμού
     */
    @PostMapping("/api/schedules")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createSchedule(
            @RequestParam String name,
            @RequestParam String semester,
            @RequestParam Integer year,
            @RequestParam(required = false) String description) {

        try {
            CourseSchedule schedule = scheduleService.createSchedule(name, semester, year, description);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ο χρονοπρογραμματισμός δημιουργήθηκε επιτυχώς");
            response.put("schedule", Map.of(
                    "id", schedule.getId(),
                    "name", schedule.getName(),
                    "semester", schedule.getSemester(),
                    "year", schedule.getYear(),
                    "status", schedule.getStatus().toString()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Σφάλμα κατά τη δημιουργία: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Σελίδα διαχείρισης χρονοπρογραμματισμών
     */
    @GetMapping("/schedules")
    public String schedulesManagement(Model model) {
        try {
            // Φόρτωση στατιστικών
            List<CourseSchedule> schedules = scheduleService.getAllSchedules();
            long totalSchedules = schedules.size();
            long activeSchedules = schedules.stream().filter(CourseSchedule::isActive).count();

            model.addAttribute("totalSchedules", totalSchedules);
            model.addAttribute("activeSchedules", activeSchedules);
            model.addAttribute("schedules", schedules);

            return "schedules";
        } catch (Exception e) {
            model.addAttribute("error", "Σφάλμα κατά τη φόρτωση των χρονοπρογραμματισμών: " + e.getMessage());
            return "error";
        }
    }

    /**
     * API endpoint για λήψη χρονοπρογραμματισμών με επιπλέον πληροφορίες
     */
    @GetMapping("/api/schedules")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSchedulesWithDetails() {
        try {
            List<CourseSchedule> schedules = scheduleService.getAllSchedules();

            List<Map<String, Object>> scheduleList = schedules.stream()
                    .map(schedule -> {
                        Map<String, Object> scheduleInfo = new HashMap<>();
                        scheduleInfo.put("id", schedule.getId());
                        scheduleInfo.put("name", schedule.getName());
                        scheduleInfo.put("semester", schedule.getSemester());
                        scheduleInfo.put("year", schedule.getYear());
                        scheduleInfo.put("status", schedule.getStatus().toString());
                        scheduleInfo.put("active", schedule.isActive());
                        scheduleInfo.put("createdDate", schedule.getCreatedAt());

                        // Πλήθος αναθέσεων για αυτόν τον χρονοπρογραμματισμό
                        long assignmentCount = assignmentService.countAssignmentsBySchedule(schedule.getId());
                        scheduleInfo.put("assignmentCount", assignmentCount);

                        return scheduleInfo;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(scheduleList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/api/schedules/{scheduleId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateSchedule(
            @PathVariable Long scheduleId,
            @RequestParam String name,
            @RequestParam String semester,
            @RequestParam Integer year,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "true") boolean active) {

        try {
            CourseSchedule schedule = scheduleService.updateSchedule(scheduleId, name, semester, year, description, active);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ο χρονοπρογραμματισμός ενημερώθηκε επιτυχώς");
            response.put("schedule", Map.of(
                    "id", schedule.getId(),
                    "name", schedule.getName(),
                    "semester", schedule.getSemester(),
                    "year", schedule.getYear(),
                    "status", schedule.getStatus().toString()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Σφάλμα κατά την ενημέρωση: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/api/schedules/{scheduleId}/change-status")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROGRAM_MANAGER')")
    public ResponseEntity<Map<String, Object>> changeScheduleStatus(
            @PathVariable Long scheduleId,
            @RequestParam String newStatus) {
        try {
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);

            // Change status to REQUIREMENTS_PHASE
            CourseSchedule.ScheduleStatus status = CourseSchedule.ScheduleStatus.valueOf(newStatus);
            schedule.setStatus(status);
            scheduleService.saveSchedule(schedule);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Status αλλάχθηκε σε " + newStatus);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Σφάλμα: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @DeleteMapping("/api/schedules/{scheduleId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteSchedule(@PathVariable Long scheduleId) {
        try {
            scheduleService.deleteSchedule(scheduleId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ο χρονοπρογραμματισμός διαγράφηκε επιτυχώς");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Σφάλμα κατά τη διαγραφή: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    // Προσθέστε νέο endpoint για JSON requests
    @PutMapping("/api/schedules/{scheduleId}/change-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changeScheduleStatusJSON(
            @PathVariable Long scheduleId,
            @RequestBody Map<String, String> request) {

        try {
            String status = request.get("status");
            if (status == null || status.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Το πεδίο status είναι υποχρεωτικό");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            CourseSchedule.ScheduleStatus newStatus = CourseSchedule.ScheduleStatus.valueOf(status);
            CourseSchedule schedule = scheduleService.changeScheduleStatus(scheduleId, newStatus);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Η κατάσταση του χρονοπρογραμματισμού άλλαξε επιτυχώς");
            response.put("newStatus", schedule.getStatus().toString());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Μη έγκυρη κατάσταση: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Σφάλμα κατά την αλλαγή κατάστασης: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/api/schedules/status-values")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getValidStatusValues() {
        try {
            CourseSchedule.ScheduleStatus[] statusValues = CourseSchedule.ScheduleStatus.values();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statusValues", Arrays.stream(statusValues)
                    .map(Enum::name)
                    .collect(Collectors.toList()));
            response.put("friendlyNames", Map.of(
                    "ASSIGNMENT_PHASE", "Φάση Αναθέσεων",
                    "REQUIREMENTS_PHASE", "Φάση Απαιτήσεων",
                    "EXECUTION_PHASE", "Φάση Εκτέλεσης",
                    "SOLUTION_FOUND", "Εύρεση Λύσης",
                    "NO_SOLUTION_FOUND", "Μη Εύρεση Λύσης",
                    "SOLUTION_APPROVED", "Έγκριση Λύσης",
                    "TERMINATED", "Λήξη"
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Σφάλμα: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PutMapping("/api/schedules/{scheduleId}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changeScheduleStatuss(
            @PathVariable Long scheduleId,
            @RequestParam String status) {

        try {
            // Map friendly names to actual enum values
            String mappedStatus = mapStatusValue(status);

            CourseSchedule.ScheduleStatus newStatus = CourseSchedule.ScheduleStatus.valueOf(mappedStatus);
            CourseSchedule schedule = scheduleService.changeScheduleStatus(scheduleId, newStatus);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Η κατάσταση του χρονοπρογραμματισμού άλλαξε επιτυχώς");
            response.put("newStatus", schedule.getStatus().toString());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Μη έγκυρη κατάσταση: " + status + ". Έγκυρες τιμές: "
                    + String.join(", ", (Iterable<? extends CharSequence>) getValidStatusValues()));
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Σφάλμα κατά την αλλαγή κατάστασης: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    private String mapStatusValue(String status) {
        // Map user-friendly values to enum values
        switch (status.toUpperCase()) {
            case "ASSIGNMENT":
            case "ASSIGNMENT_PHASE":
                return "ASSIGNMENT_PHASE";
            case "REQUIREMENTS":
            case "REQUIREMENTS_PHASE":
                return "REQUIREMENTS_PHASE";
            case "EXECUTION":
            case "EXECUTION_PHASE":
                return "EXECUTION_PHASE";
            case "SOLUTION_FOUND":
                return "SOLUTION_FOUND";
            case "NO_SOLUTION_FOUND":
                return "NO_SOLUTION_FOUND";
            case "SOLUTION_APPROVED":
                return "SOLUTION_APPROVED";
            case "TERMINATED":
                return "TERMINATED";
            default:
                return status; // Return as-is if no mapping found
        }
    }

//    private String[] getValidStatusValues() {
//        return Arrays.stream(CourseSchedule.ScheduleStatus.values())
//                .map(Enum::name)
//                .toArray(String[]::new);
//    }

    @GetMapping("/schedules/create")
    public String createSchedule() {
        return "create-schedule";  // create-schedule.html
    }
}
