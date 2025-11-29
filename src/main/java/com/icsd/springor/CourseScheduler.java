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

    //Static initialization block for OR-Tools
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

    private static final int SLOTS_PER_DAY = 4; //4 slots of 3 hours each
    private static final int DAYS_PER_WEEK = 5;  //Monday to Friday
    private static final int MAX_HOURS_PER_YEAR = 3;  //ώρες για τη χρονιά για μια ημέρα
    private static final int HOURS_PER_SLOT = 3;  //
    private static final int TOTAL_SLOTS = SLOTS_PER_DAY * DAYS_PER_WEEK;
    private static final int PREFERENCE_MULTIPLIER = 100; //Weight for preferences

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

            if (courses == null || courses.isEmpty()) {
                System.out.println("âŒ No courses provided");
                return new ArrayList<>();
            }

            if (rooms == null || rooms.isEmpty()) {
                System.out.println("âŒ No rooms provided");
                return new ArrayList<>();
            }

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

            //3.room capacity constraint
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
                //Group courses by semester
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
                        model.addLessOrEqual(
                                LinearExpr.sum(timeSlotVars.toArray(new IntVar[0])),
                                1
                        );
                    }
                }
            }

            
            Map<Integer, List<Integer>> courseIndexesByYear = new HashMap<>();
            for (int c = 0; c < courses.size(); c++) {
                Course course = courses.get(c);
                
                Integer courseYear = course.getYear();
                int year;
                if (courseYear != null) {
                    year = courseYear;
                } else {
                    year = (course.getSemester() + 1) / 2;
                }
                
                courseIndexesByYear
                        .computeIfAbsent(year, k -> new ArrayList<>())
                        .add(c); 
            }

            //5.maximum hours per year constraint
            for (Map.Entry<Integer, List<Integer>> entry : courseIndexesByYear.entrySet()) {
                for (int day = 0; day < DAYS_PER_WEEK; day++) {
                    List<IntVar> daySlotVars = new ArrayList<>();

                    for (Integer courseIndex : entry.getValue()) { 
                        if (courseIndex >= 0 && courseIndex < courses.size()) {
                            for (int r = 0; r < rooms.size(); r++) {
                                for (int t = day * SLOTS_PER_DAY; t < (day + 1) * SLOTS_PER_DAY; t++) {
                                    if (schedule[courseIndex][r][t] != null) {
                                        daySlotVars.add(schedule[courseIndex][r][t]);
                                    }
                                }
                            }
                        } else {
                            System.out.println("Invalid course index: " + courseIndex + " (max: " + (courses.size() - 1) + ")");
                        }
                    }

                    if (!daySlotVars.isEmpty()) {
                        int maxSlotsPerDay = MAX_HOURS_PER_YEAR;

                        model.addLessOrEqual(LinearExpr.sum(daySlotVars.toArray(new IntVar[0])), maxSlotsPerDay);

                        System.out.println("Max slots per day: " + maxSlotsPerDay);
                        System.out.println("Year " + entry.getKey() + " course indexes: " + entry.getValue().size());
                    }
                }
            }

            
            applyTeacherPreferences(model, schedule, courses, rooms);

            System.out.println("All constraints added, solving...");

            CpSolver solver = new CpSolver();
            CpSolverStatus status = solver.solve(model);

            if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
                System.out.println("… Solution found!");
                return extractSolution(solver, schedule, courses, rooms);
            } else {
                System.out.println(" No solution found. Status: " + status);
                return null;
            }

        } catch (Exception e) {
            System.out.println(" Exception in CourseScheduler: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create schedule", e);
        }
    }

    private boolean isRoomCompatibleWithCourse(Room room, Course course) {
        if (room == null || course == null) {
            return false;
        }
        
        if (course.getCapacity() > room.getCapacity()) {
            return false;
        }
        
        Course.TeachingHours.CourseComponent activeComponent = course.getActiveComponent();
        if (activeComponent == Course.TeachingHours.CourseComponent.LABORATORY) {
            return room.getType() == RoomType.LABORATORY;
        }
        
        return true; 
    }

    private boolean isRoomAvailable(Room room, int timeSlot) {
        return true;
    }

    private void applyTeacherPreferences(CpModel model, IntVar[][][] schedule, List<Course> courses, List<Room> rooms) {
        System.out.println("Applying teacher preferences...");

        //λίστα με όλες τις μεταβλητές προτίμησης για το objective function
        List<IntVar> allPreferenceVars = new ArrayList<>();
        List<Integer> allPreferenceWeights = new ArrayList<>();

        for (int c = 0; c < courses.size(); c++) {
            Course course = courses.get(c);

            if (course.hasTimePreferences()) {
                List<Course.TimePreference> prefs = course.getTimePreferences();
                System.out.println("Course " + course.getCode() + " has " + prefs.size() + " preferences");

                //επεξεργασία όλων των προτιμήσεων για αυτό το μάθημα
                for (Course.TimePreference pref : prefs) {
                    DayOfWeek prefDay = pref.getPreferredDay();
                    int prefSlot = pref.getPreferredSlot();
                    int weight = pref.getWeight();

                    //μετατροπή DayOfWeek σε day index (0-4 για Monday-Friday)
                    int dayIndex = prefDay.getValue() - 1;

                    if (dayIndex >= 0 && dayIndex < DAYS_PER_WEEK && prefSlot >= 0 && prefSlot < SLOTS_PER_DAY) {
                        int timeSlot = dayIndex * SLOTS_PER_DAY + prefSlot;

                        //δημιουργία μεταβλητών προτίμησης για αυτό το course στο preferred time slot
                        List<IntVar> preferredVars = new ArrayList<>();
                        for (int r = 0; r < rooms.size(); r++) {
                            if (schedule[c][r][timeSlot] != null) {
                                preferredVars.add(schedule[c][r][timeSlot]);
                            }
                        }

                        if (!preferredVars.isEmpty()) {
                            //soft constraint: ενθάρρυνση προγραμματισμού στον preferred time
                            IntVar preferenceSum = model.newIntVar(0, preferredVars.size(), 
                                "pref_" + c + "_" + prefDay + "_" + prefSlot);
                            model.addEquality(preferenceSum, LinearExpr.sum(preferredVars.toArray(new IntVar[0])));

                            //προσθήκη στις λίστες για το objective function
                            allPreferenceVars.add(preferenceSum);
                            allPreferenceWeights.add(weight * PREFERENCE_MULTIPLIER);

                            System.out.println("  Applied preference for course " + course.getCode() + 
                                             " on " + prefDay + " slot " + prefSlot + " (weight: " + weight + ")");
                        }
                    }
                }
            }
        }

        //προσθήκη του objective function για μεγιστοποίηση της ικανοποίησης των προτιμήσεων
        if (!allPreferenceVars.isEmpty()) {
            //δημιουργία weighted sum: Σ(weight_i * preference_i)
            IntVar[] prefVarsArray = allPreferenceVars.toArray(new IntVar[0]);
            long[] weightsArray = allPreferenceWeights.stream().mapToLong(Integer::longValue).toArray();
            
            LinearExpr objective = LinearExpr.weightedSum(prefVarsArray, weightsArray);
            model.maximize(objective);
            
            System.out.println("✅ Objective function added with " + allPreferenceVars.size() + " preference variables");
        } else {
            System.out.println("⚠️ No preferences found, no objective function added");
        }
    }

    private List<CourseAssignment> extractSolution(CpSolver solver, IntVar[][][] schedule, List<Course> courses, List<Room> rooms) {
        List<CourseAssignment> assignments = new ArrayList<>();

        for (int c = 0; c < courses.size(); c++) {
            Course course = courses.get(c);
            for (int r = 0; r < rooms.size(); r++) {
                Room room = rooms.get(r);
                for (int t = 0; t < TOTAL_SLOTS; t++) {
                    if (schedule[c][r][t] != null && solver.value(schedule[c][r][t]) == 1) {
                        int day = t / SLOTS_PER_DAY;
                        DayOfWeek dayOfWeek = DayOfWeek.of(day + 1); //DayOfWeek starts from 1 (Monday)
                        assignments.add(new CourseAssignment(course, room, dayOfWeek, t));
                    }
                }
            }
        }

        System.out.println("Extracted " + assignments.size() + " assignments");
        return assignments;
    }
}