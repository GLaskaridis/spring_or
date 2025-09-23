package com.icsd.springor;

import com.google.ortools.Loader;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.icsd.springor.model.Course;
import com.icsd.springor.model.Course.TeachingHours;
import com.icsd.springor.model.Course.TeachingHours.CourseComponent;
import com.icsd.springor.model.Room;
import com.icsd.springor.model.RoomType;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public class CourseScheduler {

    // Static initialization block for OR-Tools
    static {
        try {
            System.out.println("Initializing OR-Tools...");
            Loader.loadNativeLibraries();
            System.out.println("OR-Tools initialized successfully!");
        } catch (Exception e) {
            System.err.println("Failed to initialize OR-Tools: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Cannot initialize OR-Tools", e);
        }
    }

    private static final int SLOTS_PER_DAY = 4; // 4 slots of 3 hours each
    private static final int DAYS_PER_WEEK = 5;  // Monday to Friday
    private static final int MAX_HOURS_PER_YEAR = 3;  //wres gia ti xronia gia mia imera
    private static final int HOURS_PER_SLOT = 3;  // 
    private static final int TOTAL_SLOTS = SLOTS_PER_DAY * DAYS_PER_WEEK;
    private static final int PREFERENCE_MULTIPLIER = 100; // Weight for preferences

    public static class CourseAssignment {

        public final Course course;
        public final Room room;
        public final DayOfWeek day;
        public final int slot;
        public final LocalTime startTime;
        public final LocalTime endTime;

        public CourseAssignment(Course course, Room room, DayOfWeek day, int slot) {
            this.course = course;
            this.room = room;
            this.day = day;
            this.slot = slot;
            this.startTime = getSlotStartTime(slot % SLOTS_PER_DAY);
            this.endTime = getSlotEndTime(slot % SLOTS_PER_DAY);
        }

        @Override
        public String toString() {
            return String.format("""
                Course: %s (%s)
                Room: %s
                Day: %s
                Time: %s-%s
                """,
                    course.getName(),
                    course.getCode(),
                    room.getName(),
                    day,
                    startTime,
                    endTime);
        }
    }

    private static LocalTime getSlotStartTime(int slotIndex) {
        return LocalTime.of(9 + (slotIndex * 3), 0);
    }

    private static LocalTime getSlotEndTime(int slotIndex) {
        return LocalTime.of(12 + (slotIndex * 3), 0);
    }

    public List<CourseAssignment> createSchedule(List<Course> courses, List<Room> rooms) {
        System.out.println("Starting schedule creation...");
        System.out.println("Courses: " + (courses != null ? courses.size() : 0));
        System.out.println("Rooms: " + (rooms != null ? rooms.size() : 0));

        try {
            CpModel model = new CpModel();
            System.out.println("CpModel created successfully");

            IntVar[][][] schedule = new IntVar[courses.size()][rooms.size()][TOTAL_SLOTS];

            for (int c = 0; c < courses.size(); c++) {
                Course course = courses.get(c);

                for (int r = 0; r < rooms.size(); r++) {
                    Room room = rooms.get(r);
                    boolean isCompatible = isRoomCompatibleWithCourse(room, course);

                    for (int t = 0; t < TOTAL_SLOTS; t++) {
                        if (isCompatible && isRoomAvailable(room, t)) {
                            schedule[c][r][t] = model.newIntVar(0, 1,
                                    String.format("course_%d_room_%d_slot_%d", c, r, t));
                        }
                    }
                }
            }

            System.out.println("Variables created, adding constraints...");

            //1.each course must be assigned exactly one slot
            for (int c = 0; c < courses.size(); c++) {
                List<IntVar> courseVars = new ArrayList<>();
                for (int r = 0; r < rooms.size(); r++) {
                    for (int t = 0; t < TOTAL_SLOTS; t++) {
                        if (schedule[c][r][t] != null) {
                            courseVars.add(schedule[c][r][t]);
                        }
                    }
                }
                if (!courseVars.isEmpty()) {
                    model.addEquality(LinearExpr.sum(courseVars.toArray(new IntVar[0])), 1);
                }
            }

            //2.no more than 1 courses at the same time in the same room
            for (int r = 0; r < rooms.size(); r++) {
                for (int t = 0; t < TOTAL_SLOTS; t++) {
                    List<IntVar> timeSlotVars = new ArrayList<>();
                    for (int c = 0; c < courses.size(); c++) {
                        if (schedule[c][r][t] != null) {
                            timeSlotVars.add(schedule[c][r][t]);
                        }
                    }
                    if (!timeSlotVars.isEmpty()) {
                        model.addLessOrEqual(LinearExpr.sum(timeSlotVars.toArray(new IntVar[0])), 1);
                    }
                }
            }

            // 3.room capacity constraint
            for (int c = 0; c < courses.size(); c++) {
                Course course = courses.get(c);
                for (int r = 0; r < rooms.size(); r++) {
                    Room room = rooms.get(r);
                    if (course.getCapacity() > room.getCapacity()) {
                        for (int t = 0; t < TOTAL_SLOTS; t++) {
                            if (schedule[c][r][t] != null) {
                                model.addEquality(schedule[c][r][t], 0);
                            }
                        }
                    }
                }
            }

            //4.new constraint: No courses from the same semester at the same time
            for (int t = 0; t < TOTAL_SLOTS; t++) {
                // Group courses by semester
                Map<Integer, List<Integer>> courseBySemester = new HashMap<>();
                for (int c = 0; c < courses.size(); c++) {
                    Course course = courses.get(c);
                    courseBySemester
                            .computeIfAbsent(course.getSemester(), k -> new ArrayList<>())
                            .add(c);
                }

                //for each semester, ensure no more than one course is scheduled at this time
                for (List<Integer> semesterCourses : courseBySemester.values()) {
                    List<IntVar> timeSlotVars = new ArrayList<>();
                    for (int c : semesterCourses) {
                        for (int r = 0; r < rooms.size(); r++) {
                            if (schedule[c][r][t] != null) {
                                timeSlotVars.add(schedule[c][r][t]);
                            }
                        }
                    }
                    if (!timeSlotVars.isEmpty()) {
                        //Μέγιστο 1 μάθημα ίδιου εξαμήνου ταυτόχρονα
                        model.addLessOrEqual(
                                LinearExpr.sum(timeSlotVars.toArray(new IntVar[0])),
                                1
                        );
                    }
                }
            }

            // Group courses by year
            Map<Integer, List<Course>> coursesByYear = new HashMap<>();
            for (int c = 0; c < courses.size(); c++) {
                Course course = courses.get(c);
                int semester = course.getSemester();
                // Calculate the year: (semester + 1) / 2
                //int year = (semester + 1) / 2;
                
                Integer courseYear = course.getYear();
                int year;
                if (courseYear != null) {
                    year = courseYear;
                } else {
                    year = (course.getSemester() + 1) / 2;
                }
                
                coursesByYear
                        .computeIfAbsent(year, k -> new ArrayList<>())
                        .add(course);
            }

            //5.maximum hours per year constraint
            for (Map.Entry<Integer, List<Course>> entry : coursesByYear.entrySet()) {
                //For each day of week
                for (int day = 0; day < DAYS_PER_WEEK; day++) {
                    List<IntVar> daySlotVars = new ArrayList<>();

                    for (Course course : entry.getValue()) {
                        int courseIndex = courses.indexOf(course);

                        for (int r = 0; r < rooms.size(); r++) {
                            //Get only slots for this specific day
                            for (int t = day * SLOTS_PER_DAY; t < (day + 1) * SLOTS_PER_DAY; t++) {
                                if (schedule[courseIndex][r][t] != null) {
                                    daySlotVars.add(schedule[courseIndex][r][t]);
                                }
                            }
                        }
                    }

                    if (!daySlotVars.isEmpty()) {
                        int maxSlotsPerDay = MAX_HOURS_PER_YEAR;

                        //Μέχρι 3 μαθήματα ίδιου έτους σε μία ημέρα
                        model.addLessOrEqual(LinearExpr.sum(daySlotVars.toArray(new IntVar[0])), maxSlotsPerDay);

                        //προβληματικό minimum constraint
                        // if (maxSlotsPerDay > 1) {
                        //     model.addGreaterOrEqual(LinearExpr.sum(daySlotVars.toArray(new IntVar[0])), maxSlotsPerDay - 1);
                        // }
                        System.out.println("Max slots per day: " + maxSlotsPerDay);
                        System.out.println("Year " + entry.getKey() + " courses: " + entry.getValue().size());
                    }
                }
            }

            List<IntVar> objectiveTerms = new ArrayList<>();
            for (int c = 0; c < courses.size(); c++) {
                Course course = courses.get(c);
                Course.TimePreference pref = course.getTimePreference();

                if (pref != null) {
                    // Debug prints
                    System.out.println("Course: " + course.getName());
                    System.out.println("Preferred Day: " + pref.getPreferredDay());
                    System.out.println("Preferred Slot: " + pref.getPreferredSlot());
                    System.out.println("Preference Weight: " + pref.getWeight());

                    for (int r = 0; r < rooms.size(); r++) {
                        for (int t = 0; t < TOTAL_SLOTS; t++) {
                            if (schedule[c][r][t] != null) {
                                DayOfWeek slotDay = getDayFromSlot(t);
                                int slotTime = t % SLOTS_PER_DAY;

                                // Day preference penalty
                                if (pref.getPreferredDay() != null && slotDay != pref.getPreferredDay()) {
                                    IntVar penaltyVar = model.newIntVar(0, pref.getWeight(),
                                            String.format("penalty_day_course_%d_day_%d_%d", c, r, t));
                                    model.addEquality(penaltyVar,
                                            LinearExpr.term(schedule[c][r][t], pref.getWeight()));
                                    objectiveTerms.add(penaltyVar);
                                }

                                // Time slot preference penalty
                                if (pref.getPreferredSlot() != -1 && slotTime != pref.getPreferredSlot()) {
                                    IntVar penaltyVar = model.newIntVar(0, pref.getWeight(),
                                            String.format("penalty_slot_course_%d_day_%d_%d", c, r, t));
                                    model.addEquality(penaltyVar,
                                            LinearExpr.term(schedule[c][r][t], pref.getWeight()));
                                    objectiveTerms.add(penaltyVar);
                                }
                            }
                        }
                    }
                }
            }

            if (!objectiveTerms.isEmpty()) {
                System.out.println("Number of preference terms: " + objectiveTerms.size());
                LinearExpr sumPenalties = LinearExpr.sum(objectiveTerms.toArray(new IntVar[0]));
                model.minimize(sumPenalties);
            }

            System.out.println("Starting solver...");
            CpSolver solver = new CpSolver();
            CpSolverStatus status = solver.solve(model);
            System.out.println("Solver status: " + status);

            List<CourseAssignment> assignments = new ArrayList<>();

            if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
                //collecting all assignments
                for (int c = 0; c < courses.size(); c++) {
                    for (int r = 0; r < rooms.size(); r++) {
                        for (int t = 0; t < TOTAL_SLOTS; t++) {
                            //System.out.println(" " + solver.value(schedule[c][r][t]));
                            if (schedule[c][r][t] != null && solver.value(schedule[c][r][t]) == 1) {
                                assignments.add(new CourseAssignment(
                                        courses.get(c),
                                        rooms.get(r),
                                        getDayFromSlot(t),
                                        t
                                ));
                            }
                        }
                    }
                }

                // Print schedule in different formats
                printScheduleByDay(assignments);
                printScheduleByRoom(assignments);
                printScheduleByCourse(assignments);

            } else {
                System.out.println("No solution found! Status: " + status);
            }

            return assignments;

        } catch (Exception e) {
            System.err.println("Error in createSchedule: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create schedule", e);
        }
    }

    // Rest of your methods remain the same...
    private void printScheduleByDay(List<CourseAssignment> assignments) {
        System.out.println("\n=== Schedule By Day ===");
        Map<DayOfWeek, List<CourseAssignment>> byDay = new TreeMap<>();

        for (CourseAssignment assignment : assignments) {
            byDay.computeIfAbsent(assignment.day, k -> new ArrayList<>()).add(assignment);
        }

        for (Map.Entry<DayOfWeek, List<CourseAssignment>> entry : byDay.entrySet()) {
            System.out.println("\n" + entry.getKey());
            entry.getValue().stream()
                    .sorted(Comparator.comparing(a -> a.startTime))
                    .forEach(a -> System.out.printf("  %s-%s: %s (%s) in %s%n",
                    a.startTime, a.endTime, a.course.getName(),
                    a.course.getCode(), a.room.getName()));
        }
    }

    private void printScheduleByRoom(List<CourseAssignment> assignments) {
        System.out.println("\n=== Schedule By Room ===");
        Map<String, List<CourseAssignment>> byRoom = new TreeMap<>();

        for (CourseAssignment assignment : assignments) {
            byRoom.computeIfAbsent(assignment.room.getName(), k -> new ArrayList<>()).add(assignment);
        }

        for (Map.Entry<String, List<CourseAssignment>> entry : byRoom.entrySet()) {
            System.out.println("\nRoom: " + entry.getKey());
            entry.getValue().stream()
                    .sorted(Comparator.comparing(a -> ((CourseAssignment) a).day).thenComparing(a -> ((CourseAssignment) a).startTime))
                    .forEach(a -> System.out.printf("  %s %s-%s: %s (%s)%n",
                    a.day, a.startTime, a.endTime,
                    a.course.getName(), a.course.getCode()));
        }
    }

    private void printScheduleByCourse(List<CourseAssignment> assignments) {
        System.out.println("\n=== Schedule By Course ===");
        assignments.stream()
                .sorted(Comparator.comparing(a -> a.course.getCode()))
                .forEach(a -> System.out.println(a.toString()));
    }

    private boolean isRoomCompatibleWithCourse(Room room, Course course) {
        for (TeachingHours hours : course.getTeachingHours()) {
            //check the compatibility
        }
        return true;
    }

    private boolean isRoomAvailable(Room room, int timeSlot) {
        return true;
//        DayOfWeek day = getDayFromSlot(timeSlot);
//        LocalTime startTime = getSlotStartTime(timeSlot % SLOTS_PER_DAY);
//        LocalTime endTime = getSlotEndTime(timeSlot % SLOTS_PER_DAY);
//
//        return room.getAvailability().stream()
//                .anyMatch(availability
//                        -> availability.getDay() == day
//                && !startTime.isBefore(availability.getStartTime())
//                && !endTime.isAfter(availability.getEndTime()));
    }

    private DayOfWeek getDayFromSlot(int timeSlot) {
        int dayIndex = timeSlot / SLOTS_PER_DAY;
        return DayOfWeek.of(dayIndex + 1);
    }
}
