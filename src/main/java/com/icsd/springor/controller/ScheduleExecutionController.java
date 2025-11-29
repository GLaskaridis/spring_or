/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.icsd.springor.controller;

import com.icsd.springor.service.*;
import org.springframework.web.bind.annotation.*;

import com.icsd.springor.CourseScheduler;
import com.icsd.springor.CourseScheduler.CourseAssignment;
import com.icsd.springor.DTO.AssignmentDTO;
import com.icsd.springor.DTO.TeacherPreferenceDTO;
import com.icsd.springor.model.Assignment;
import com.icsd.springor.model.Course;
import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.model.Room;
import com.icsd.springor.model.ScheduleResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

@Controller
@RequestMapping("/schedule-execution")
@PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
public class ScheduleExecutionController {

    @Autowired
    private CourseScheduleService scheduleService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private TeacherPreferenceService preferenceService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private ScheduleResultService scheduleResultService;

    @GetMapping("/execute/{scheduleId}")
    public String executeScheduling(@PathVariable Long scheduleId,
            Model model,
            RedirectAttributes redirectAttributes) {
        System.out.println("\n=== EXECUTE SCHEDULING START ===");
        System.out.println("Schedule ID: " + scheduleId);
        System.out.println("Timestamp: " + new java.util.Date());

        try {
            // Get schedule
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);
            System.out.println("Schedule: " + schedule.getName() + " (Status: " + schedule.getStatus() + ")");

            // Status check
            if (schedule.getStatus() != CourseSchedule.ScheduleStatus.EXECUTION_PHASE) {
                String errorMsg = "Wrong status: " + schedule.getStatus();
                System.out.println("ERROR: " + errorMsg);
                redirectAttributes.addFlashAttribute("error", errorMsg);
                return "redirect:/schedules";
            }
            System.out.println("✓ Status check passed");

            // Get data
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            System.out.println("✓ Assignments loaded: " + assignments.size());

            List<TeacherPreferenceDTO> preferences = preferenceService.getPreferencesBySchedule(scheduleId);
            System.out.println("✓ Preferences loaded: " + preferences.size());

            List<Room> rooms = roomService.getAllRooms();
            System.out.println("✓ Rooms loaded: " + rooms.size());

            // Prepare courses
            System.out.println("?Preparing courses with preferences...");
            List<Course> coursesWithPreferences = prepareCoursesWithPreferences(assignments, preferences);
            System.out.println("✓ Courses prepared: " + coursesWithPreferences.size());

            // Execute algorithm
            System.out.println("Starting algorithm execution...");
            CourseScheduler scheduler = new CourseScheduler();

            long startTime = System.currentTimeMillis();
            List<CourseScheduler.CourseAssignment> result = scheduler.createSchedule(coursesWithPreferences, rooms);
            long endTime = System.currentTimeMillis();

            System.out.println("Algorithm completed in: " + (endTime - startTime) + "ms");
            System.out.println(" Result analysis:");
            System.out.println(" Result is null: " + (result == null));

            if (result != null) {
                System.out.println("  - Result size: " + result.size());
                System.out.println("  - Result isEmpty: " + result.isEmpty());
            }

            if (result != null && !result.isEmpty()) {
                System.out.println("SUCCESS: Solution found with " + result.size() + " assignments");

                try {
                    // Update status
                    System.out.println("Updating schedule status to SOLUTION_FOUND...");
                    scheduleService.changeScheduleStatus(scheduleId, CourseSchedule.ScheduleStatus.SOLUTION_FOUND);
                    System.out.println("Status updated");
                } catch (Exception statusException) {
                    System.out.println("Warning: Could not update status but solution found: " + statusException.getMessage());
                    // Continue anyway since we have a solution
                }

                // Prepare model for results page
                model.addAttribute("schedule", schedule);
                model.addAttribute("assignments", result);
                model.addAttribute("coursesData", coursesWithPreferences);
                model.addAttribute("roomsData", rooms);
                model.addAttribute("success", true);

                System.out.println("Returning to schedule-execution-result view");
                return "schedule-execution-result";

            } else {
                System.out.println("FAILURE: No solution found (INFEASIBLE)");

                try {
                    // Try to update status to NO_SOLUTION_FOUND
                    System.out.println("Attempting to update schedule status to NO_SOLUTION_FOUND...");
                    scheduleService.changeScheduleStatus(scheduleId, CourseSchedule.ScheduleStatus.NO_SOLUTION_FOUND);
                    System.out.println("Status updated");
                } catch (Exception statusException) {
                    System.out.println("Warning: Could not update status: " + statusException.getMessage());
                    // Continue to show error message to user even if status update fails
                }

                // Create detailed error message
                String errorMsg = "⚠️ Δεν βρέθηκε λύση για τον χρονοπρογραμματισμό!\n\n"
                        + "?Πιθανές αιτίες:\n"
                        + "• Πολύ περιοριστικές προτιμήσεις καθηγητών\n"
                        + "• Ανεπαρκείς αίθουσες για τα μαθήματα\n"
                        + "• Συγκρούσεις στα χρονικά διαστήματα\n"
                        + "• Υπερβολικός φόρτος εργασίας σε συγκεκριμένες μέρες\n\n"
                        + " Προτεινόμενες ενέργειες:\n"
                        + "• Ελέγξτε και τροποποιήστε τις προτιμήσεις των καθηγητών\n"
                        + "• Βεβαιωθείτε ότι υπάρχουν αρκετές αίθουσες\n"
                        + "• Μειώστε τον αριθμό των περιοριστικών προτιμήσεων";

                redirectAttributes.addFlashAttribute("error", errorMsg);
                redirectAttributes.addFlashAttribute("errorType", "NO_SOLUTION");
                redirectAttributes.addFlashAttribute("scheduleId", scheduleId);
                redirectAttributes.addFlashAttribute("scheduleName", schedule.getName());

                System.out.println("Redirecting to schedules list with detailed error message");
                return "redirect:/schedules";
            }

        } catch (Exception e) {
            System.out.println("EXCEPTION in executeScheduling:");
            System.out.println("Exception type: " + e.getClass().getSimpleName());
            System.out.println("Exception message: " + e.getMessage());
            e.printStackTrace();

            String errorMsg = "Σφάλμα κατά την εκτέλεση του αλγορίθμου: " + e.getMessage();
            redirectAttributes.addFlashAttribute("error", errorMsg);
            redirectAttributes.addFlashAttribute("errorType", "SYSTEM_ERROR");

            System.out.println("🔙 Redirecting to schedules list due to exception");
            return "redirect:/schedules";
        }
    }

    private List<Course> prepareCoursesWithPreferences(List<AssignmentDTO> assignments,
            List<TeacherPreferenceDTO> preferences) {

        System.out.println("🔄 Starting prepareCoursesWithPreferences...");

        // Group preferences by assignment
        Map<Long, List<TeacherPreferenceDTO>> preferencesByAssignment = preferences.stream()
                .collect(Collectors.groupingBy(TeacherPreferenceDTO::getAssignmentId));

        System.out.println("✓ Grouped preferences by assignment: " + preferencesByAssignment.size() + " groups");

        return assignments.stream().map(assignment -> {
            System.out.println("📋 Processing assignment: " + assignment.getId()
                    + " for course: " + assignment.getCourseName());

            Course course = courseService.getCourseById(assignment.getCourseId());

            // Debug course loading
            System.out.println("  📚 Course loaded: " + course.getName());
            System.out.println("  🔢 Course ID: " + course.getId());
            System.out.println("  📅 Year: " + course.getYear());
            System.out.println("  📆 Semester: " + course.getSemester());
            System.out.println("  🏷️ Type: " + course.getType());
            System.out.println("  👥 Capacity: " + course.getCapacity());
            System.out.println("  ✅ Active: " + course.isActive());

            // Check teaching hours
            Set<Course.TeachingHours> teachingHours = course.getTeachingHours();
            if (teachingHours == null) {
                System.out.println("  ❌ WARNING: TeachingHours is NULL for course " + course.getName());
                // Initialize empty set to prevent NullPointerException
                course.setTeachingHours(new HashSet<>());
            } else if (teachingHours.isEmpty()) {
                System.out.println("  ⚠️ WARNING: TeachingHours is EMPTY for course " + course.getName());
            } else {
                System.out.println("  ✅ TeachingHours found: " + teachingHours.size() + " entries");
                for (Course.TeachingHours th : teachingHours) {
                    System.out.println("    - " + th.getComponent() + ": " + th.getHours() + " hours");
                }
            }

            // Get preferences for this assignment
            List<TeacherPreferenceDTO> assignmentPrefs = preferencesByAssignment.get(assignment.getId());

            if (assignmentPrefs != null && !assignmentPrefs.isEmpty()) {
                System.out.println("  🎯 Found " + assignmentPrefs.size() + " preferences for this assignment");

                // Apply the strongest preference to the course
                TeacherPreferenceDTO strongestPref = assignmentPrefs.stream()
                        .max((p1, p2) -> Integer.compare(
                        p1.getPreferenceWeight() != null ? p1.getPreferenceWeight() : 5,
                        p2.getPreferenceWeight() != null ? p2.getPreferenceWeight() : 5
                ))
                        .orElse(assignmentPrefs.get(0));

                System.out.println("  🏆 Strongest preference: Day=" + strongestPref.getPreferredDay()
                        + ", Slot=" + strongestPref.getPreferredSlot()
                        + ", Weight=" + strongestPref.getPreferenceWeight());

                // Create TimePreference for the course using only day and slot
                if (strongestPref.getPreferredDay() != null && strongestPref.getPreferredSlot() != null) {
                    Course.TimePreference timePreference = new Course.TimePreference(
                            strongestPref.getPreferredDay(),
                            strongestPref.getPreferredSlot(),
                            strongestPref.getPreferenceWeight() != null
                            ? strongestPref.getPreferenceWeight() : 5
                    );
                    course.setTimePreference(timePreference);
                    System.out.println("  ✅ TimePreference set successfully");
                } else {
                    System.out.println("  ⚠️ Preference has null day or slot, skipping");
                }
            } else {
                System.out.println("  ℹ️ No preferences found for this assignment");
            }

            // Final validation
            if (course.getTeachingHours() == null) {
                System.out.println("  🔧 Creating empty teachingHours set to prevent NPE");
                course.setTeachingHours(new HashSet<>());
            }

            System.out.println("  ✅ Course preparation completed for: " + course.getName());
            return course;
        }).collect(Collectors.toList());
    }

// Add this helper method to diagnose course loading issues
    @GetMapping("/debug-course/{courseId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> debugCourse(@PathVariable Long courseId) {
        Map<String, Object> debug = new HashMap<>();

        try {
            Course course = courseService.getCourseById(courseId);

            debug.put("courseFound", true);
            debug.put("courseId", course.getId());
            debug.put("name", course.getName());
            debug.put("code", course.getCode());
            debug.put("year", course.getYear());
            debug.put("semester", course.getSemester());
            debug.put("type", course.getType());
            debug.put("capacity", course.getCapacity());
            debug.put("active", course.isActive());

            // Check teaching hours
            Set<Course.TeachingHours> teachingHours = course.getTeachingHours();
            debug.put("teachingHoursNull", teachingHours == null);
            debug.put("teachingHoursEmpty", teachingHours != null && teachingHours.isEmpty());

            if (teachingHours != null) {
                debug.put("teachingHoursCount", teachingHours.size());

                List<Map<String, Object>> hoursDetails = new ArrayList<>();
                for (Course.TeachingHours th : teachingHours) {
                    Map<String, Object> hourInfo = new HashMap<>();
                    hourInfo.put("component", th.getComponent());
                    hourInfo.put("hours", th.getHours());
                    hoursDetails.add(hourInfo);
                }
                debug.put("teachingHoursDetails", hoursDetails);
            }

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            debug.put("courseFound", false);
            debug.put("error", e.getMessage());
            return ResponseEntity.ok(debug);
        }
    }

    @GetMapping("/test-simple-algorithm")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testSimpleAlgorithm() {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("\n=== TESTING ALGORITHM WITH COMPLETE SIMPLE DATA ===");

            // Create simple test courses with teaching hours
            List<Course> testCourses = new ArrayList<>();

            // Course 1 - Theory only
            Course course1 = new Course();
            course1.setId(1L);
            course1.setName("Test Course 1");
            course1.setCode("TEST1");
            course1.setYear(1);
            course1.setSemester(1);
            course1.setType(Course.CourseType.BASIC);
            course1.setCapacity(50);
            course1.setActive(true);

            // Add teaching hours
            Set<Course.TeachingHours> hours1 = new HashSet<>();
            Course.TeachingHours theoryHours1 = new Course.TeachingHours();
            theoryHours1.setComponent(Course.TeachingHours.CourseComponent.THEORY);
            theoryHours1.setHours(3);
            hours1.add(theoryHours1);
            course1.setTeachingHours(hours1);

            testCourses.add(course1);

            // Course 2 - Theory + Lab with preference
            Course course2 = new Course();
            course2.setId(2L);
            course2.setName("Test Course 2");
            course2.setCode("TEST2");
            course2.setYear(1);
            course2.setSemester(1);
            course2.setType(Course.CourseType.BASIC);
            course2.setCapacity(30);
            course2.setActive(true);

            // Add teaching hours
            Set<Course.TeachingHours> hours2 = new HashSet<>();
            Course.TeachingHours theoryHours2 = new Course.TeachingHours();
            theoryHours2.setComponent(Course.TeachingHours.CourseComponent.THEORY);
            theoryHours2.setHours(2);
            hours2.add(theoryHours2);

            Course.TeachingHours labHours2 = new Course.TeachingHours();
            labHours2.setComponent(Course.TeachingHours.CourseComponent.LABORATORY);
            labHours2.setHours(2);
            hours2.add(labHours2);
            course2.setTeachingHours(hours2);

            // Add a simple preference
            Course.TimePreference pref = new Course.TimePreference(DayOfWeek.MONDAY, 0, 5);
            course2.setTimePreference(pref);
            testCourses.add(course2);

            // Course 3 - Lab only with different preference
            Course course3 = new Course();
            course3.setId(3L);
            course3.setName("Test Course 3");
            course3.setCode("TEST3");
            course3.setYear(1);
            course3.setSemester(1);
            course3.setType(Course.CourseType.ELECTIVE);
            course3.setCapacity(25);
            course3.setActive(true);

            // Add teaching hours
            Set<Course.TeachingHours> hours3 = new HashSet<>();
            Course.TeachingHours labHours3 = new Course.TeachingHours();
            labHours3.setComponent(Course.TeachingHours.CourseComponent.LABORATORY);
            labHours3.setHours(3);
            hours3.add(labHours3);
            course3.setTeachingHours(hours3);

            // Add a different preference
            Course.TimePreference pref2 = new Course.TimePreference(DayOfWeek.TUESDAY, 1, 3);
            course3.setTimePreference(pref2);
            testCourses.add(course3);

            System.out.println("Created " + testCourses.size() + " test courses with teaching hours");
            for (Course c : testCourses) {
                System.out.println("  " + c.getName() + ": " + c.getTeachingHours().size() + " teaching hour entries");
                for (Course.TeachingHours th : c.getTeachingHours()) {
                    System.out.println("    " + th.getComponent() + ": " + th.getHours() + " hours");
                }
            }

            // Get real rooms from database
            List<Room> rooms = roomService.getAllRooms();
            if (rooms.isEmpty()) {
                // Create test rooms if none exist
                Room testRoom1 = new Room();
                testRoom1.setId(1L);
                testRoom1.setName("Test Room 1");
                testRoom1.setCapacity(50);
                testRoom1.setActive(true);
                rooms.add(testRoom1);

                Room testRoom2 = new Room();
                testRoom2.setId(2L);
                testRoom2.setName("Test Room 2");
                testRoom2.setCapacity(30);
                testRoom2.setActive(true);
                rooms.add(testRoom2);
            }

            System.out.println("Using " + rooms.size() + " rooms");

            // Test the algorithm
            CourseScheduler scheduler = new CourseScheduler();
            long startTime = System.currentTimeMillis();
            List<CourseScheduler.CourseAssignment> result = scheduler.createSchedule(testCourses, rooms);
            long endTime = System.currentTimeMillis();

            response.put("success", true);
            response.put("executionTimeMs", endTime - startTime);
            response.put("coursesInput", testCourses.size());
            response.put("roomsInput", rooms.size());

            if (result != null) {
                response.put("resultSize", result.size());
                response.put("resultEmpty", result.isEmpty());
                response.put("hasResult", !result.isEmpty());

                System.out.println("Simple test result: " + result.size() + " assignments");

                // Add detailed results
                List<Map<String, Object>> assignments = new ArrayList<>();
                for (CourseScheduler.CourseAssignment ca : result) {
                    Map<String, Object> assignment = new HashMap<>();
                    assignment.put("course", ca.course.getName());
                    assignment.put("room", ca.room.getName());
                    assignment.put("day", ca.day.toString());
                    assignment.put("slot", ca.slot);
                    assignment.put("startTime", ca.startTime.toString());
                    assignment.put("endTime", ca.endTime.toString());
                    assignments.add(assignment);

                    System.out.println("  " + ca.course.getName() + " -> " + ca.room.getName()
                            + " on " + ca.day + " at slot " + ca.slot);
                }
                response.put("assignments", assignments);

            } else {
                response.put("resultSize", 0);
                response.put("resultEmpty", true);
                response.put("hasResult", false);
                System.out.println("Simple test failed - no result");
            }

            // Add course details to response
            List<Map<String, Object>> courseDetails = new ArrayList<>();
            for (Course c : testCourses) {
                Map<String, Object> courseInfo = new HashMap<>();
                courseInfo.put("name", c.getName());
                courseInfo.put("id", c.getId());
                courseInfo.put("year", c.getYear());
                courseInfo.put("semester", c.getSemester());
                courseInfo.put("capacity", c.getCapacity());
                courseInfo.put("type", c.getType().toString());

                // Teaching hours details
                List<Map<String, Object>> teachingHoursDetails = new ArrayList<>();
                for (Course.TeachingHours th : c.getTeachingHours()) {
                    Map<String, Object> thInfo = new HashMap<>();
                    thInfo.put("component", th.getComponent().toString());
                    thInfo.put("hours", th.getHours());
                    teachingHoursDetails.add(thInfo);
                }
                courseInfo.put("teachingHours", teachingHoursDetails);

                if (c.getTimePreference() != null) {
                    Course.TimePreference tp = c.getTimePreference();
                    courseInfo.put("preference", Map.of(
                            "day", tp.getPreferredDay().toString(),
                            "slot", tp.getPreferredSlot(),
                            "weight", tp.getWeight()
                    ));
                } else {
                    courseInfo.put("preference", null);
                }
                courseDetails.add(courseInfo);
            }
            response.put("testCourses", courseDetails);

            System.out.println("=== SIMPLE TEST WITH COMPLETE DATA COMPLETE ===\n");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("Simple test failed with exception: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("exceptionType", e.getClass().getSimpleName());

            return ResponseEntity.ok(response);
        }
    }

    // Add a new endpoint to retry with relaxed constraints
    @GetMapping("/retry-with-relaxed/{scheduleId}")
    public String retryWithRelaxedConstraints(@PathVariable Long scheduleId,
            RedirectAttributes redirectAttributes) {
        try {
            // You could implement logic here to temporarily reduce preference weights
            // or ignore some constraints and try again

            redirectAttributes.addFlashAttribute("info",
                    "Επανάληψη με χαλαρότερους περιορισμούς δεν είναι ακόμη υλοποιημένη. "
                    + "Παρακαλώ τροποποιήστε τις προτιμήσεις μη αυτόματα.");

            return "redirect:/schedules";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Σφάλμα κατά την επανάληψη: " + e.getMessage());
            return "redirect:/schedules";
        }
    }

    // Corrected test algorithm endpoint
    @GetMapping("/test-algorithm/{scheduleId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testAlgorithm(@PathVariable Long scheduleId) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("\n=== TEST ALGORITHM START ===");

            // Get data
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            List<TeacherPreferenceDTO> preferences = preferenceService.getPreferencesBySchedule(scheduleId);
            List<Room> rooms = roomService.getAllRooms();

            System.out.println("Data loaded - Assignments: " + assignments.size()
                    + ", Preferences: " + preferences.size()
                    + ", Rooms: " + rooms.size());

            // Debug assignments
            List<Map<String, Object>> assignmentDetails = new ArrayList<>();
            for (AssignmentDTO a : assignments) {
                Map<String, Object> details = new HashMap<>();
                details.put("id", a.getId());
                details.put("courseName", a.getCourseName());
                details.put("courseId", a.getCourseId());
                details.put("teacherName", a.getTeacherName());
                details.put("teacherId", a.getTeacherId());
                details.put("component", a.getCourseComponent());
                assignmentDetails.add(details);
            }

            // Prepare courses
            List<Course> coursesWithPreferences = prepareCoursesWithPreferences(assignments, preferences);
            System.out.println("Courses prepared: " + coursesWithPreferences.size());

            // Test algorithm
            CourseScheduler scheduler = new CourseScheduler();
            long startTime = System.currentTimeMillis();
            List<CourseScheduler.CourseAssignment> result = scheduler.createSchedule(coursesWithPreferences, rooms);
            long endTime = System.currentTimeMillis();

            // Build response
            response.put("success", true);
            response.put("scheduleName", schedule.getName());
            response.put("executionTimeMs", endTime - startTime);
            response.put("assignmentsInput", assignments.size());
            response.put("preferencesInput", preferences.size());
            response.put("roomsInput", rooms.size());
            response.put("coursesProcessed", coursesWithPreferences.size());
            response.put("assignmentDetails", assignmentDetails);

            if (result != null) {
                response.put("resultSize", result.size());
                response.put("resultEmpty", result.isEmpty());
                response.put("hasResult", !result.isEmpty());

                // Add sample results
                if (!result.isEmpty()) {
                    List<Map<String, Object>> sampleResults = new ArrayList<>();
                    for (int i = 0; i < Math.min(5, result.size()); i++) {
                        CourseScheduler.CourseAssignment ca = result.get(i);
                        Map<String, Object> assignment = new HashMap<>();
                        assignment.put("course", ca.course.getName());
                        assignment.put("room", ca.room.getName());
                        assignment.put("day", ca.day.toString());
                        assignment.put("slot", ca.slot);
                        assignment.put("startTime", ca.startTime.toString());
                        assignment.put("endTime", ca.endTime.toString());
                        sampleResults.add(assignment);
                    }
                    response.put("sampleResults", sampleResults);
                }
            } else {
                response.put("resultSize", 0);
                response.put("resultEmpty", true);
                response.put("hasResult", false);
                response.put("result", "null");
            }

            System.out.println("Test completed. Result: "
                    + (result != null ? result.size() + " assignments" : "null"));
            System.out.println("=== TEST ALGORITHM END ===\n");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("Exception in test algorithm: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("exceptionType", e.getClass().getSimpleName());
            response.put("stackTrace", java.util.Arrays.toString(e.getStackTrace()));

            return ResponseEntity.ok(response); // Return 200 even for errors to see the debug info
        }
    }

    // Add this test endpoint to try with relaxed constraints
    @GetMapping("/test-relaxed-algorithm")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testRelaxedAlgorithm() {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("\n=== TESTING WITH RELAXED CONSTRAINTS ===");

            // Create test courses with DIFFERENT semesters to avoid same-semester constraint
            List<Course> testCourses = new ArrayList<>();

            // Course 1 - Semester 1
            Course course1 = new Course();
            course1.setId(1L);
            course1.setName("Test Course 1");
            course1.setCode("TEST1");
            course1.setYear(1);
            course1.setSemester(1);  // Semester 1
            course1.setType(Course.CourseType.BASIC);
            course1.setCapacity(30);  // Smaller capacity
            course1.setActive(true);

            Set<Course.TeachingHours> hours1 = new HashSet<>();
            Course.TeachingHours theoryHours1 = new Course.TeachingHours();
            theoryHours1.setComponent(Course.TeachingHours.CourseComponent.THEORY);
            theoryHours1.setHours(3);
            hours1.add(theoryHours1);
            course1.setTeachingHours(hours1);

            testCourses.add(course1);

            // Course 2 - Semester 2 (different semester!)
            Course course2 = new Course();
            course2.setId(2L);
            course2.setName("Test Course 2");
            course2.setCode("TEST2");
            course2.setYear(1);
            course2.setSemester(2);  // Semester 2
            course2.setType(Course.CourseType.BASIC);
            course2.setCapacity(25);
            course2.setActive(true);

            Set<Course.TeachingHours> hours2 = new HashSet<>();
            Course.TeachingHours theoryHours2 = new Course.TeachingHours();
            theoryHours2.setComponent(Course.TeachingHours.CourseComponent.THEORY);
            theoryHours2.setHours(2);
            hours2.add(theoryHours2);
            course2.setTeachingHours(hours2);

            // Weak preference (weight = 1)
            Course.TimePreference pref = new Course.TimePreference(DayOfWeek.MONDAY, 0, 1);
            course2.setTimePreference(pref);
            testCourses.add(course2);

            // Only 2 courses to make it easier
            System.out.println("Created " + testCourses.size() + " test courses with different semesters");
            for (Course c : testCourses) {
                System.out.println("  " + c.getName() + ": Semester " + c.getSemester()
                        + ", " + c.getTeachingHours().size() + " teaching hour entries");
            }

            // Get rooms
            List<Room> rooms = roomService.getAllRooms();
            if (rooms.isEmpty()) {
                Room testRoom1 = new Room();
                testRoom1.setId(1L);
                testRoom1.setName("Test Room 1");
                testRoom1.setCapacity(50);
                testRoom1.setActive(true);
                rooms.add(testRoom1);
            }

            // Take only first 3 rooms to make it simpler
            if (rooms.size() > 3) {
                rooms = rooms.subList(0, 3);
            }

            System.out.println("Using " + rooms.size() + " rooms");

            // Test the algorithm
            CourseScheduler scheduler = new CourseScheduler();
            long startTime = System.currentTimeMillis();
            List<CourseScheduler.CourseAssignment> result = scheduler.createSchedule(testCourses, rooms);
            long endTime = System.currentTimeMillis();

            response.put("success", true);
            response.put("executionTimeMs", endTime - startTime);
            response.put("coursesInput", testCourses.size());
            response.put("roomsInput", rooms.size());
            response.put("testScenario", "Different semesters, weak preferences, fewer courses");

            if (result != null) {
                response.put("resultSize", result.size());
                response.put("resultEmpty", result.isEmpty());
                response.put("hasResult", !result.isEmpty());

                System.out.println("Relaxed test result: " + result.size() + " assignments");

                List<Map<String, Object>> assignments = new ArrayList<>();
                for (CourseScheduler.CourseAssignment ca : result) {
                    Map<String, Object> assignment = new HashMap<>();
                    assignment.put("course", ca.course.getName());
                    assignment.put("room", ca.room.getName());
                    assignment.put("day", ca.day.toString());
                    assignment.put("slot", ca.slot);
                    assignment.put("startTime", ca.startTime.toString());
                    assignment.put("endTime", ca.endTime.toString());
                    assignments.add(assignment);

                    System.out.println("  " + ca.course.getName() + " -> " + ca.room.getName()
                            + " on " + ca.day + " at slot " + ca.slot);
                }
                response.put("assignments", assignments);

            } else {
                response.put("resultSize", 0);
                response.put("resultEmpty", true);
                response.put("hasResult", false);
                System.out.println("Relaxed test still failed");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("Relaxed test failed: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("error", e.getMessage());

            return ResponseEntity.ok(response);
        }
    }

// Add a minimal test with just 1 course
    @GetMapping("/test-minimal-algorithm")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testMinimalAlgorithm() {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("\n=== TESTING MINIMAL CASE: 1 COURSE ===");

            // Just 1 course
            List<Course> testCourses = new ArrayList<>();

            Course course1 = new Course();
            course1.setId(1L);
            course1.setName("Single Test Course");
            course1.setCode("SINGLE");
            course1.setYear(1);
            course1.setSemester(1);
            course1.setType(Course.CourseType.BASIC);
            course1.setCapacity(20);
            course1.setActive(true);

            Set<Course.TeachingHours> hours1 = new HashSet<>();
            Course.TeachingHours theoryHours1 = new Course.TeachingHours();
            theoryHours1.setComponent(Course.TeachingHours.CourseComponent.THEORY);
            theoryHours1.setHours(2);
            hours1.add(theoryHours1);
            course1.setTeachingHours(hours1);

            // No preference
            testCourses.add(course1);

            System.out.println("Created 1 course with no preferences");

            // Just 1 room
            List<Room> rooms = new ArrayList<>();
            Room testRoom = new Room();
            testRoom.setId(1L);
            testRoom.setName("Single Room");
            testRoom.setCapacity(50);
            testRoom.setActive(true);
            rooms.add(testRoom);

            System.out.println("Using 1 room");

            // Test
            CourseScheduler scheduler = new CourseScheduler();
            long startTime = System.currentTimeMillis();
            List<CourseScheduler.CourseAssignment> result = scheduler.createSchedule(testCourses, rooms);
            long endTime = System.currentTimeMillis();

            response.put("success", true);
            response.put("executionTimeMs", endTime - startTime);
            response.put("coursesInput", 1);
            response.put("roomsInput", 1);
            response.put("testScenario", "Minimal: 1 course, 1 room, no preferences");

            if (result != null && !result.isEmpty()) {
                CourseScheduler.CourseAssignment ca = result.get(0);
                response.put("hasResult", true);
                response.put("assignment", Map.of(
                        "course", ca.course.getName(),
                        "room", ca.room.getName(),
                        "day", ca.day.toString(),
                        "slot", ca.slot,
                        "startTime", ca.startTime.toString(),
                        "endTime", ca.endTime.toString()
                ));

                System.out.println("✅ MINIMAL TEST SUCCESS!");
                System.out.println("  " + ca.course.getName() + " -> " + ca.room.getName()
                        + " on " + ca.day + " at slot " + ca.slot);
            } else {
                response.put("hasResult", false);
                System.out.println("❌ Even minimal test failed!");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("Minimal test failed: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("error", e.getMessage());

            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/analyze-constraints/{scheduleId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> analyzeConstraints(@PathVariable Long scheduleId) {
        Map<String, Object> analysis = new HashMap<>();

        try {
            System.out.println("\n=== CONSTRAINT ANALYSIS ===");

            // Get data
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            List<TeacherPreferenceDTO> preferences = preferenceService.getPreferencesBySchedule(scheduleId);
            List<Course> coursesWithPreferences = prepareCoursesWithPreferences(assignments, preferences);
            List<Room> rooms = roomService.getAllRooms();

            // IMPORTANT: Use the CURRENT values from CourseScheduler
            final int CURRENT_MAX_HOURS_PER_YEAR = 3; // Updated to match your CourseScheduler
            final int CURRENT_SLOTS_PER_DAY = 4;
            final int CURRENT_DAYS_PER_WEEK = 5;
            final int CURRENT_TOTAL_SLOTS = CURRENT_SLOTS_PER_DAY * CURRENT_DAYS_PER_WEEK;

            analysis.put("algorithmConstants", Map.of(
                    "maxHoursPerYear", CURRENT_MAX_HOURS_PER_YEAR,
                    "slotsPerDay", CURRENT_SLOTS_PER_DAY,
                    "daysPerWeek", CURRENT_DAYS_PER_WEEK,
                    "totalSlots", CURRENT_TOTAL_SLOTS
            ));

            analysis.put("dataOverview", Map.of(
                    "assignments", assignments.size(),
                    "preferences", preferences.size(),
                    "courses", coursesWithPreferences.size(),
                    "rooms", rooms.size()
            ));

            // Analyze by semester
            Map<Integer, List<Course>> coursesBySemester = coursesWithPreferences.stream()
                    .collect(Collectors.groupingBy(Course::getSemester));

            Map<String, Object> semesterAnalysis = new HashMap<>();
            for (Map.Entry<Integer, List<Course>> entry : coursesBySemester.entrySet()) {
                int semester = entry.getKey();
                List<Course> semesterCourses = entry.getValue();

                semesterAnalysis.put("semester_" + semester, Map.of(
                        "courseCount", semesterCourses.size(),
                        "courses", semesterCourses.stream().map(Course::getName).collect(Collectors.toList()),
                        "maxAllowedSimultaneous", 1 // Same-semester constraint allows only 1
                ));

                System.out.println("Semester " + semester + ": " + semesterCourses.size() + " courses");
                semesterCourses.forEach(c -> System.out.println("  - " + c.getName()));
            }
            analysis.put("semesterBreakdown", semesterAnalysis);

            // Analyze by year  
            Map<Integer, List<Course>> coursesByYear = new HashMap<>();
            for (Course course : coursesWithPreferences) {
                int semester = course.getSemester();
                int year = (semester + 1) / 2;  // Same calculation as in algorithm
                coursesByYear.computeIfAbsent(year, k -> new ArrayList<>()).add(course);
            }

            Map<String, Object> yearAnalysis = new HashMap<>();
            for (Map.Entry<Integer, List<Course>> entry : coursesByYear.entrySet()) {
                int year = entry.getKey();
                List<Course> yearCourses = entry.getValue();

                yearAnalysis.put("year_" + year, Map.of(
                        "courseCount", yearCourses.size(),
                        "courses", yearCourses.stream().map(Course::getName).collect(Collectors.toList()),
                        "maxSlotsPerDay", CURRENT_MAX_HOURS_PER_YEAR,
                        "maxSlotsPerWeek", CURRENT_MAX_HOURS_PER_YEAR * CURRENT_DAYS_PER_WEEK
                ));

                System.out.println("Year " + year + ": " + yearCourses.size() + " courses (max " + CURRENT_MAX_HOURS_PER_YEAR + " slots per day)");
            }
            analysis.put("yearBreakdown", yearAnalysis);

            // Check room capacity constraints
            List<Map<String, Object>> capacityIssues = new ArrayList<>();
            for (Course course : coursesWithPreferences) {
                List<Room> compatibleRooms = rooms.stream()
                        .filter(room -> room.getCapacity() >= course.getCapacity())
                        .collect(Collectors.toList());

                if (compatibleRooms.isEmpty()) {
                    capacityIssues.add(Map.of(
                            "course", course.getName(),
                            "requiredCapacity", course.getCapacity(),
                            "maxRoomCapacity", rooms.stream().mapToInt(Room::getCapacity).max().orElse(0)
                    ));
                }
            }
            analysis.put("capacityIssues", capacityIssues);

            // Analyze preferences
            Map<String, Object> preferenceAnalysis = new HashMap<>();
            long strongPreferences = preferences.stream()
                    .filter(p -> p.getPreferenceWeight() != null && p.getPreferenceWeight() >= 5)
                    .count();

            Map<DayOfWeek, Long> preferencesByDay = preferences.stream()
                    .filter(p -> p.getPreferredDay() != null)
                    .collect(Collectors.groupingBy(TeacherPreferenceDTO::getPreferredDay, Collectors.counting()));

            Map<Integer, Long> preferencesBySlot = preferences.stream()
                    .filter(p -> p.getPreferredSlot() != null)
                    .collect(Collectors.groupingBy(TeacherPreferenceDTO::getPreferredSlot, Collectors.counting()));

            preferenceAnalysis.put("strongPreferences", strongPreferences);
            preferenceAnalysis.put("byDay", preferencesByDay);
            preferenceAnalysis.put("bySlot", preferencesBySlot);
            analysis.put("preferenceAnalysis", preferenceAnalysis);

            // Problem assessment with CORRECT values
            List<String> problems = new ArrayList<>();

            // Check year constraints
            for (Map.Entry<Integer, List<Course>> entry : coursesByYear.entrySet()) {
                int coursesInYear = entry.getValue().size();
                int maxSlotsPerWeek = CURRENT_MAX_HOURS_PER_YEAR * CURRENT_DAYS_PER_WEEK;

                if (coursesInYear > maxSlotsPerWeek) {
                    problems.add("Year " + entry.getKey() + " has " + coursesInYear
                            + " courses but only " + maxSlotsPerWeek + " slots per week allowed ("
                            + CURRENT_MAX_HOURS_PER_YEAR + " per day × " + CURRENT_DAYS_PER_WEEK + " days)");
                } else if (coursesInYear > CURRENT_MAX_HOURS_PER_YEAR) {
                    // This is actually OK since courses can be spread across different days
                    System.out.println("Year " + entry.getKey() + " has " + coursesInYear
                            + " courses, max " + CURRENT_MAX_HOURS_PER_YEAR + " per day (should be feasible)");
                }
            }

            // Check semester constraints
            for (Map.Entry<Integer, List<Course>> entry : coursesBySemester.entrySet()) {
                int coursesInSemester = entry.getValue().size();
                if (coursesInSemester > CURRENT_TOTAL_SLOTS) {
                    problems.add("Semester " + entry.getKey() + " has " + coursesInSemester
                            + " courses but only " + CURRENT_TOTAL_SLOTS + " total slots available");
                } else if (coursesInSemester > 1) {
                    // Note: Same-semester constraint means courses from same semester cannot run simultaneously
                    problems.add("Semester " + entry.getKey() + " has " + coursesInSemester
                            + " courses but same-semester constraint prevents simultaneous scheduling");
                }
            }

            // Check total constraints
            if (coursesWithPreferences.size() > CURRENT_TOTAL_SLOTS) {
                problems.add("Total " + coursesWithPreferences.size() + " courses exceeds " + CURRENT_TOTAL_SLOTS + " available slots");
            }

            if (!capacityIssues.isEmpty()) {
                problems.add(capacityIssues.size() + " courses have capacity issues");
            }

            analysis.put("identifiedProblems", problems);

            // Suggested solutions
            List<String> solutions = new ArrayList<>();

            // Dynamic suggestions based on actual constraints
            int maxCoursesInYear = coursesByYear.values().stream().mapToInt(List::size).max().orElse(0);
            if (maxCoursesInYear > CURRENT_MAX_HOURS_PER_YEAR * CURRENT_DAYS_PER_WEEK) {
                int neededSlotsPerDay = (int) Math.ceil((double) maxCoursesInYear / CURRENT_DAYS_PER_WEEK);
                solutions.add("Increase MAX_HOURS_PER_YEAR from " + CURRENT_MAX_HOURS_PER_YEAR + " to " + neededSlotsPerDay);
            }

            solutions.add("Consider removing or relaxing the same-semester constraint");
            solutions.add("Spread courses across more semesters to reduce conflicts");
            solutions.add("Reduce preference weights to make scheduling more flexible");

            if (!capacityIssues.isEmpty()) {
                solutions.add("Add more rooms with higher capacity");
            }

            analysis.put("suggestedSolutions", solutions);

            System.out.println("\n--- PROBLEMS IDENTIFIED ---");
            problems.forEach(System.out::println);
            System.out.println("\n--- SUGGESTED SOLUTIONS ---");
            solutions.forEach(System.out::println);
            System.out.println("=== ANALYSIS COMPLETE ===\n");

            return ResponseEntity.ok(analysis);

        } catch (Exception e) {
            analysis.put("error", e.getMessage());
            return ResponseEntity.ok(analysis);
        }
    }

    // Add this debug endpoint to your ScheduleExecutionController
    @GetMapping("/debug-data/{scheduleId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> debugScheduleData(@PathVariable Long scheduleId) {
        Map<String, Object> debug = new HashMap<>();

        try {
            System.out.println("\n=== DETAILED DEBUG DATA ANALYSIS ===");

            // Get raw data
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            List<TeacherPreferenceDTO> preferences = preferenceService.getPreferencesBySchedule(scheduleId);
            List<Room> rooms = roomService.getAllRooms();

            debug.put("scheduleInfo", Map.of(
                    "id", schedule.getId(),
                    "name", schedule.getName(),
                    "status", schedule.getStatus().toString()
            ));

            // Debug assignments
            List<Map<String, Object>> assignmentDebug = new ArrayList<>();
            System.out.println("\n--- ASSIGNMENTS DEBUG ---");
            for (AssignmentDTO a : assignments) {
                System.out.println("Assignment ID: " + a.getId());
                System.out.println("  Course ID: " + a.getCourseId());
                System.out.println("  Course Name: " + a.getCourseName());
                System.out.println("  Teacher ID: " + a.getTeacherId());
                System.out.println("  Teacher Name: " + a.getTeacherName());
                System.out.println("  Component: " + a.getCourseComponent());
                System.out.println("  Active: " + a.isActive());

                // Try to get the actual course
                Course course = null;
                Exception courseException = null;
                try {
                    course = courseService.getCourseById(a.getCourseId());
                    System.out.println("  Course loaded: " + course.getName());
                    System.out.println("  Course code: " + course.getCode());
                    System.out.println("  Course year: " + course.getYear());
                    System.out.println("  Course semester: " + course.getSemester());
                } catch (Exception e) {
                    courseException = e;
                    System.out.println("  ERROR loading course: " + e.getMessage());
                }

                Map<String, Object> assignmentInfo = new HashMap<>();
                assignmentInfo.put("assignmentId", a.getId());
                assignmentInfo.put("courseId", a.getCourseId());
                assignmentInfo.put("courseName", a.getCourseName());
                assignmentInfo.put("teacherId", a.getTeacherId());
                assignmentInfo.put("teacherName", a.getTeacherName());
                assignmentInfo.put("component", a.getCourseComponent());
                assignmentInfo.put("active", a.isActive());

                if (course != null) {
                    assignmentInfo.put("courseDetails", Map.of(
                            "name", course.getName(),
                            "code", course.getCode(),
                            "year", course.getYear(),
                            "semester", course.getSemester(),
                            "hasTimePreference", course.getTimePreference() != null
                    ));
                } else {
                    assignmentInfo.put("courseLoadError", courseException != null ? courseException.getMessage() : "Unknown error");
                }

                assignmentDebug.add(assignmentInfo);
            }
            debug.put("assignments", assignmentDebug);

            // Debug preferences
            List<Map<String, Object>> preferenceDebug = new ArrayList<>();
            System.out.println("\n--- PREFERENCES DEBUG ---");
            for (TeacherPreferenceDTO p : preferences) {
                System.out.println("Preference ID: " + p.getId());
                System.out.println("  Assignment ID: " + p.getAssignmentId());
                System.out.println("  Preferred Day: " + p.getPreferredDay());
                System.out.println("  Preferred Slot: " + p.getPreferredSlot());
                System.out.println("  Weight: " + p.getPreferenceWeight());
                System.out.println("  Active: " + p.isActive());

                Map<String, Object> prefInfo = new HashMap<>();
                prefInfo.put("preferenceId", p.getId());
                prefInfo.put("assignmentId", p.getAssignmentId());
                prefInfo.put("preferredDay", p.getPreferredDay());
                prefInfo.put("preferredSlot", p.getPreferredSlot());
                prefInfo.put("weight", p.getPreferenceWeight());
                prefInfo.put("active", p.isActive());

                preferenceDebug.add(prefInfo);
            }
            debug.put("preferences", preferenceDebug);

            // Debug rooms
            List<Map<String, Object>> roomDebug = new ArrayList<>();
            System.out.println("\n--- ROOMS DEBUG ---");
            for (Room r : rooms) {
                System.out.println("Room ID: " + r.getId());
                System.out.println("  Name: " + r.getName());
                System.out.println("  Capacity: " + r.getCapacity());
                System.out.println("  Active: " + r.isActive());

                Map<String, Object> roomInfo = new HashMap<>();
                roomInfo.put("roomId", r.getId());
                roomInfo.put("name", r.getName());
                roomInfo.put("capacity", r.getCapacity());
                roomInfo.put("active", r.isActive());

                roomDebug.add(roomInfo);
            }
            debug.put("rooms", roomDebug);

            // Debug course preparation
            System.out.println("\n--- COURSE PREPARATION DEBUG ---");
            List<Course> coursesWithPreferences = null;
            Exception prepException = null;
            List<Map<String, Object>> coursePreparationDebug = new ArrayList<>();

            try {
                coursesWithPreferences = prepareCoursesWithPreferences(assignments, preferences);

                for (Course c : coursesWithPreferences) {
                    System.out.println("Prepared Course: " + c.getName());
                    System.out.println("  ID: " + c.getId());
                    System.out.println("  Code: " + c.getCode());
                    System.out.println("  Year: " + c.getYear());
                    System.out.println("  Semester: " + c.getSemester());
                    System.out.println("  Has Time Preference: " + (c.getTimePreference() != null));

                    if (c.getTimePreference() != null) {
                        Course.TimePreference tp = c.getTimePreference();
                        System.out.println("    Preferred Day: " + tp.getPreferredDay());
                        System.out.println("    Preferred Slot: " + tp.getPreferredSlot());
                        System.out.println("    Weight: " + tp.getWeight());
                    }

                    Map<String, Object> courseInfo = new HashMap<>();
                    courseInfo.put("courseId", c.getId());
                    courseInfo.put("name", c.getName());
                    courseInfo.put("code", c.getCode());
                    courseInfo.put("year", c.getYear());
                    courseInfo.put("semester", c.getSemester());

                    if (c.getTimePreference() != null) {
                        Course.TimePreference tp = c.getTimePreference();
                        courseInfo.put("timePreference", Map.of(
                                "preferredDay", tp.getPreferredDay(),
                                "preferredSlot", tp.getPreferredSlot(),
                                "weight", tp.getWeight()
                        ));
                    } else {
                        courseInfo.put("timePreference", null);
                    }

                    coursePreparationDebug.add(courseInfo);
                }

            } catch (Exception e) {
                prepException = e;
                System.out.println("ERROR in course preparation: " + e.getMessage());
                e.printStackTrace();
            }

            debug.put("coursesWithPreferences", coursePreparationDebug);
            if (prepException != null) {
                debug.put("coursePreparationError", prepException.getMessage());
            }

            // Test algorithm with minimal setup
            System.out.println("\n--- ALGORITHM TEST ---");
            if (coursesWithPreferences != null && !coursesWithPreferences.isEmpty() && !rooms.isEmpty()) {
                try {
                    CourseScheduler scheduler = new CourseScheduler();
                    long startTime = System.currentTimeMillis();
                    List<CourseScheduler.CourseAssignment> result = scheduler.createSchedule(coursesWithPreferences, rooms);
                    long endTime = System.currentTimeMillis();

                    debug.put("algorithmTest", Map.of(
                            "executionTimeMs", endTime - startTime,
                            "resultNull", result == null,
                            "resultSize", result != null ? result.size() : 0,
                            "successful", result != null && !result.isEmpty()
                    ));

                    System.out.println("Algorithm test completed in: " + (endTime - startTime) + "ms");
                    System.out.println("Result: " + (result != null ? result.size() + " assignments" : "null"));

                } catch (Exception e) {
                    debug.put("algorithmError", e.getMessage());
                    System.out.println("Algorithm test failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            debug.put("summary", Map.of(
                    "totalAssignments", assignments.size(),
                    "totalPreferences", preferences.size(),
                    "totalRooms", rooms.size(),
                    "coursesAfterPreparation", coursesWithPreferences != null ? coursesWithPreferences.size() : 0
            ));

            System.out.println("=== DEBUG DATA ANALYSIS COMPLETE ===\n");

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            debug.put("error", e.getMessage());
            debug.put("exceptionType", e.getClass().getSimpleName());
            e.printStackTrace();
            return ResponseEntity.ok(debug);
        }
    }

    // Test scheduling with sample data (for debugging)
    @GetMapping("/test")
    public String testScheduling(Model model) {
        try {
            CourseScheduler scheduler = new CourseScheduler();
            List<Course> courses = courseService.getAllCourses();

            // Add sample preferences to first course
            if (!courses.isEmpty()) {
                Course course = courses.get(0);
                System.out.println("Testing with course: " + course.getName());
                course.setTimePreference(new Course.TimePreference(
                        DayOfWeek.FRIDAY, // προτιμώμενη μέρα
                        0, // πρώτο slot (9πμ-12μμ)
                        8 // βάρος προτίμησης (1-10)
                ));
            }

            List<Room> rooms = roomService.getAllRooms();
            System.out.println("Available rooms: " + rooms.size());

            List<CourseAssignment> assignments = scheduler.createSchedule(courses, rooms);
            System.out.println("Generated assignments: " + assignments.size());
            assignments.forEach(System.out::println);

            model.addAttribute("assignments", assignments);
            model.addAttribute("courses", courses);
            model.addAttribute("rooms", rooms);

            return "schedule-test-result";

        } catch (Exception e) {
            model.addAttribute("error", "Error testing schedule: " + e.getMessage());
            return "error";
        }
    }

    // Approve a generated schedule
    @PostMapping("/approve/{scheduleId}")
    public String approveSchedule(@PathVariable Long scheduleId,
            RedirectAttributes redirectAttributes) {
        try {
            scheduleService.changeScheduleStatus(scheduleId, CourseSchedule.ScheduleStatus.SOLUTION_APPROVED);
            redirectAttributes.addFlashAttribute("message", "Schedule approved successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error approving schedule: " + e.getMessage());
        }

        return "redirect:/schedules";
    }

    @PostMapping("/reject/{scheduleId}")
    public String rejectSchedule(@PathVariable Long scheduleId,
            RedirectAttributes redirectAttributes) {
        try {
            scheduleService.changeScheduleStatus(scheduleId, CourseSchedule.ScheduleStatus.EXECUTION_PHASE);
            redirectAttributes.addFlashAttribute("message", "Schedule rejected. You can modify preferences and re-execute.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error rejecting schedule: " + e.getMessage());
        }

        return "redirect:/schedules";
    }

    @GetMapping("/re-execute-results/{scheduleId}")
    public String viewScheduleResults(@PathVariable Long scheduleId, Model model) {
        try {
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);

            // Check if schedule has results to show
            if (schedule.getStatus() != CourseSchedule.ScheduleStatus.SOLUTION_FOUND
                    && schedule.getStatus() != CourseSchedule.ScheduleStatus.SOLUTION_APPROVED) {
                model.addAttribute("error", "Δεν υπάρχουν αποτελέσματα για αυτό το χρονοδιάγραμμα");
                return "error";
            }

            // Get assignments and preferences to re-execute algorithm and get results
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            List<TeacherPreferenceDTO> preferences = preferenceService.getPreferencesBySchedule(scheduleId);
            List<Course> coursesWithPreferences = prepareCoursesWithPreferences(assignments, preferences);
            List<Room> rooms = roomService.getAllRooms();

            // Re-execute algorithm to get results
            CourseScheduler scheduler = new CourseScheduler();
            List<CourseScheduler.CourseAssignment> result = scheduler.createSchedule(coursesWithPreferences, rooms);

            if (result != null && !result.isEmpty()) {
                // Create enhanced assignment objects for the template
                List<Map<String, Object>> enhancedAssignments = new ArrayList<>();

                for (CourseScheduler.CourseAssignment ca : result) {
                    // Find the original assignment to get teacher info
                    AssignmentDTO originalAssignment = assignments.stream()
                            .filter(a -> a.getCourseId().equals(ca.course.getId()))
                            .findFirst()
                            .orElse(null);

                    Map<String, Object> enhanced = new HashMap<>();
                    enhanced.put("course", Map.of(
                            "id", ca.course.getId(),
                            "name", ca.course.getName(),
                            "code", ca.course.getCode(),
                            "teacherName", originalAssignment != null ? originalAssignment.getTeacherName() : "Άγνωστος",
                            "component", originalAssignment != null ? originalAssignment.getCourseComponent().toString() : "THEORY"
                    ));
                    enhanced.put("room", Map.of(
                            "name", ca.room.getName()
                    ));
                    enhanced.put("day", ca.day);
                    enhanced.put("slot", ca.slot);
                    enhanced.put("timeSlot", ca.slot % 4);
                    enhanced.put("startTime", ca.startTime);
                    enhanced.put("endTime", ca.endTime);

                    enhancedAssignments.add(enhanced);
                }

                model.addAttribute("schedule", schedule);
                model.addAttribute("assignments", enhancedAssignments);
                model.addAttribute("success", true);

                return "schedule-execution-result";

            } else {
                model.addAttribute("error", "Δεν ήταν δυνατή η ανάκτηση των αποτελεσμάτων.");
                return "error";
            }

        } catch (Exception e) {
            System.out.println("Error in viewScheduleResults: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Σφάλμα κατά τη φόρτωση των αποτελεσμάτων: " + e.getMessage());
            return "error";
        }
    }
    
    

    @GetMapping("/debug/{scheduleId}")
    public String debugScheduleExecution(@PathVariable Long scheduleId, Model model) {
        try {
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);

            // Get all data for debugging
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            List<TeacherPreferenceDTO> preferences = preferenceService.getPreferencesBySchedule(scheduleId);
            List<Course> courses = courseService.getAllCourses();
            List<Room> rooms = roomService.getAllRooms();

            // Prepare courses with preferences
            List<Course> coursesWithPreferences = prepareCoursesWithPreferences(assignments, preferences);

            // Debug information
            model.addAttribute("schedule", schedule);
            model.addAttribute("assignments", assignments);
            model.addAttribute("preferences", preferences);
            model.addAttribute("courses", courses);
            model.addAttribute("coursesWithPreferences", coursesWithPreferences);
            model.addAttribute("rooms", rooms);

            // Try to execute algorithm
            try {
                CourseScheduler scheduler = new CourseScheduler();
                List<CourseScheduler.CourseAssignment> result = scheduler.createSchedule(coursesWithPreferences, rooms);
                model.addAttribute("algorithmResult", result);
                model.addAttribute("algorithmSuccess", result != null && !result.isEmpty());
            } catch (Exception e) {
                model.addAttribute("algorithmError", e.getMessage());
                model.addAttribute("algorithmSuccess", false);
            }

            return "schedule-debug";

        } catch (Exception e) {
            model.addAttribute("error", "Σφάλμα κατά το debug: " + e.getMessage());
            return "error";
        }
    }

    // REST API endpoint to get results as JSON
    @GetMapping("/api/results/{scheduleId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getScheduleResultsAPI(@PathVariable Long scheduleId) {
        try {
            CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            List<TeacherPreferenceDTO> preferences = preferenceService.getPreferencesBySchedule(scheduleId);

            List<Course> coursesWithPreferences = prepareCoursesWithPreferences(assignments, preferences);
            List<Room> rooms = roomService.getAllRooms();

            CourseScheduler scheduler = new CourseScheduler();
            List<CourseScheduler.CourseAssignment> result = scheduler.createSchedule(coursesWithPreferences, rooms);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("schedule", schedule);
            response.put("assignments", result);
            response.put("totalAssignments", result != null ? result.size() : 0);
            response.put("coursesProcessed", coursesWithPreferences.size());
            response.put("roomsAvailable", rooms.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Σφάλμα: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/results/{scheduleId}")
    public String viewResults(@PathVariable Long scheduleId, Model model) {
        CourseSchedule schedule = scheduleService.getScheduleById(scheduleId);

        if (!scheduleResultService.hasScheduleResults(scheduleId)) {
            model.addAttribute("error", "");
            model.addAttribute("schedule", schedule);
            model.addAttribute("assignments", null);  // Πρόσθεσα αυτό
            model.addAttribute("success", false);
            return "schedule-execution-result";
        }

        List<ScheduleResult> results = scheduleResultService.getScheduleResults(scheduleId);
        List<Map<String, Object>> assignmentMaps = new ArrayList<>();

        for (ScheduleResult result : results) {
            Map<String, Object> assignmentMap = new HashMap<>();

            //πληροφορίες μαθήματος από το assignment
            Assignment assignment = result.getAssignment();
            Course course = assignment.getCourse();

            assignmentMap.put("course", Map.of(
                    "id", course.getId(),
                    "name", course.getName(),
                    "code", course.getCode()
            ));

            assignmentMap.put("room", Map.of(
                    "id", result.getRoom().getId(),
                    "name", result.getRoom().getName(),
                    "building", result.getRoom().getBuilding() != null ? result.getRoom().getBuilding() : "",
                    "capacity", result.getRoom().getCapacity()
            ));

            assignmentMap.put("day", result.getDayOfWeek().name());
            assignmentMap.put("slot", result.getSlotNumber());
            assignmentMap.put("timeSlot", result.getSlotNumber());
            assignmentMap.put("startTime", result.getStartTime().toString());
            assignmentMap.put("endTime", result.getEndTime().toString());

            assignmentMaps.add(assignmentMap);
        }

        model.addAttribute("schedule", schedule);
        model.addAttribute("assignments", assignmentMaps);
        model.addAttribute("success", true);
        model.addAttribute("message", "βρέθηκε λύση με " + results.size() + " αναθέσεις");

        return "schedule-execution-result";
    }

}
