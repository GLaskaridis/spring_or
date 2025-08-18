package com.icsd.springor.service;

import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.repository.CourseScheduleRepository;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CourseScheduleService {

    @Autowired
    private CourseScheduleRepository scheduleRepository;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private TeacherPreferenceService preferenceService;

    public CourseSchedule createSchedule(String name, String semester, String startTime, String endTime,
            Integer maxHoursPerDay, Double maxDistanceKm) {

        // Check if schedule with same name and semester already exists
        if (scheduleRepository.findByNameAndSemester(name, semester).isPresent()) {
            throw new RuntimeException("Schedule with this name and semester already exists");
        }

        CourseSchedule schedule = new CourseSchedule();
        schedule.setName(name);
        schedule.setSemester(semester);
        schedule.setStartTime(startTime != null ? startTime : "09:00");
        schedule.setEndTime(endTime != null ? endTime : "21:00");
        schedule.setMaxHoursPerDay(maxHoursPerDay != null ? maxHoursPerDay : 9);
        schedule.setMaxDistanceKm(maxDistanceKm != null ? maxDistanceKm : 1.0);
        schedule.setStatus(CourseSchedule.ScheduleStatus.ASSIGNMENT_PHASE);

        return scheduleRepository.save(schedule);
    }

    public CourseSchedule getScheduleById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + id));
    }

    public List<CourseSchedule> getAllSchedules() {
        return scheduleRepository.findByOrderByCreatedAtDesc();
    }

//    public CourseSchedule changeScheduleStatus(Long scheduleId, CourseSchedule.ScheduleStatus newStatus) {
//        CourseSchedule schedule = getScheduleById(scheduleId);
//        
//        if (!isValidStateTransition(schedule.getStatus(), newStatus)) {
//            throw new RuntimeException("Invalid state transition from " + schedule.getStatus() + " to " + newStatus);
//        }
//        
//        if (newStatus == CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE) {
//            if (!assignmentService.areAllCoursesAssigned(scheduleId)) {
//                throw new RuntimeException("Cannot move to requirements phase. Not all courses are assigned.");
//            }
//        }
//        
//        
//        if (newStatus == CourseSchedule.ScheduleStatus.EXECUTION_PHASE) {
//            if (!preferenceService.areAllPreferencesProvided(scheduleId)) {
//                throw new RuntimeException("Cannot move to execution phase. Not all teachers have provided their preferences.");
//            }
//        }
//        
//        schedule.setStatus(newStatus);
//        return scheduleRepository.save(schedule);
//    }
    public long countActiveSchedules() {
        return scheduleRepository.findAll().stream()
                .filter(schedule -> schedule.getStatus() != CourseSchedule.ScheduleStatus.SOLUTION_APPROVED
                && schedule.getStatus() != CourseSchedule.ScheduleStatus.TERMINATED)
                .count();
    }

    public CourseSchedule changeScheduleStatus(Long scheduleId, CourseSchedule.ScheduleStatus newStatus) {
        CourseSchedule schedule = getScheduleById(scheduleId);

        // Validate state transitions
        if (!isValidStateTransition(schedule.getStatus(), newStatus)) {
            throw new RuntimeException("Invalid state transition from " + schedule.getStatus() + " to " + newStatus);
        }

        if (newStatus == CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE) {
            if (!assignmentService.areAllCoursesAssigned(scheduleId)) {
                throw new RuntimeException("Cannot move to requirements phase. Not all courses are assigned.");
            }
        }

        if (newStatus == CourseSchedule.ScheduleStatus.EXECUTION_PHASE) {
            // elegxos an ola ta assignment exoun protimisi
            // You'll need to inject the new preference services and add this check:
            // if (!hasAllRequiredPreferences(scheduleId)) {
            //     throw new RuntimeException("Cannot move to execution phase. Not all teachers have provided required preferences.");
            // }
        }

        //elegxos protimisewn
        /*
        if (newStatus == CourseSchedule.ScheduleStatus.EXECUTION_PHASE) {
            if (!preferenceService.areAllPreferencesProvided(scheduleId)) {
                throw new RuntimeException("Cannot move to execution phase. Not all teachers have provided their preferences.");
            }
        }
         */
        schedule.setStatus(newStatus);
        return scheduleRepository.save(schedule);
    }

    private boolean isValidStateTransition(CourseSchedule.ScheduleStatus current, CourseSchedule.ScheduleStatus target) {
        switch (current) {
            case ASSIGNMENT_PHASE:
                return target == CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE;
            case REQUIREMENTS_PHASE:
                return target == CourseSchedule.ScheduleStatus.EXECUTION_PHASE;
            case EXECUTION_PHASE:
                return target == CourseSchedule.ScheduleStatus.SOLUTION_FOUND
                        || target == CourseSchedule.ScheduleStatus.NO_SOLUTION_FOUND;
            case SOLUTION_FOUND:
                return target == CourseSchedule.ScheduleStatus.SOLUTION_APPROVED
                        || target == CourseSchedule.ScheduleStatus.EXECUTION_PHASE;
            case NO_SOLUTION_FOUND:
                return target == CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE
                        || target == CourseSchedule.ScheduleStatus.TERMINATED;
            case SOLUTION_APPROVED:
                return false;
            case TERMINATED:
                return false;
            default:
                return false;
        }
    }

    public void deleteSchedule(Long id) {
        CourseSchedule schedule = getScheduleById(id);

        if (schedule.getStatus() != CourseSchedule.ScheduleStatus.ASSIGNMENT_PHASE
                && schedule.getStatus() != CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE) {
            throw new RuntimeException("Cannot delete schedule in current state: " + schedule.getStatus());
        }

        scheduleRepository.delete(schedule);
    }

    @Transactional
    public CourseSchedule createSchedule(String name, String semester, Integer year, String description) {
        // Έλεγχος αν υπάρχει ήδη χρονοπρογραμματισμός με το ίδιο όνομα
        if (scheduleRepository.existsByName(name)) {
            throw new RuntimeException("Υπάρχει ήδη χρονοπρογραμματισμός με το όνομα: " + name);
        }

        CourseSchedule schedule = new CourseSchedule();
        schedule.setName(name);
        schedule.setSemester(semester);
        schedule.setYear(year);
        schedule.setStatus(CourseSchedule.ScheduleStatus.ASSIGNMENT_PHASE); // Αρχική κατάσταση
        schedule.setCreatedAt(LocalDateTime.now());
        schedule.setActive(true);


        return scheduleRepository.save(schedule);
    }

    
    @Transactional
    public CourseSchedule updateSchedule(Long scheduleId, String name, String semester, Integer year, String description, boolean active) {
        CourseSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Δεν βρέθηκε χρονοπρογραμματισμός με id: " + scheduleId));

        // Έλεγχος αν το νέο όνομα υπάρχει ήδη (εκτός από τον τρέχοντα)
        if (!schedule.getName().equals(name) && scheduleRepository.existsByName(name)) {
            throw new RuntimeException("Υπάρχει ήδη χρονοπρογραμματισμός με το όνομα: " + name);
        }

        schedule.setName(name);
        schedule.setSemester(semester);
        schedule.setYear(year);
        schedule.setActive(active);
        schedule.setCreatedAt(LocalDateTime.now());

        return scheduleRepository.save(schedule);
    }


    private boolean isStatusChangeAllowed(CourseSchedule.ScheduleStatus currentStatus, CourseSchedule.ScheduleStatus newStatus) {
        // Έλεγχος με βάση τις πραγματικές καταστάσεις του συστήματός σας
        switch (currentStatus) {
            case ASSIGNMENT_PHASE:
                // Από ΑΝΑΘΕΣΗ_ΜΑΘΗΜΑΤΩΝ μπορούμε να πάμε στην ΠΑΡΟΧΗ_ΠΡΟΤΙΜΗΣΕΩΝ ή ΛΗΞΗ
                return newStatus == CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE
                        || newStatus == CourseSchedule.ScheduleStatus.TERMINATED;

            case REQUIREMENTS_PHASE:
                // Από ΠΑΡΟΧΗ_ΠΡΟΤΙΜΗΣΕΩΝ μπορούμε να πάμε στην ΕΚΤΕΛΕΣΗ ή πίσω στην ΑΝΑΘΕΣΗ ή ΛΗΞΗ
                return newStatus == CourseSchedule.ScheduleStatus.EXECUTION_PHASE
                        || newStatus == CourseSchedule.ScheduleStatus.ASSIGNMENT_PHASE
                        || newStatus == CourseSchedule.ScheduleStatus.TERMINATED;

            case EXECUTION_PHASE:
                // Από ΕΚΤΕΛΕΣΗ μπορούμε να πάμε στην ΕΥΡΕΣΗ_ΛΥΣΗΣ ή ΜΗ_ΕΥΡΕΣΗ_ΛΥΣΗΣ
                return newStatus == CourseSchedule.ScheduleStatus.SOLUTION_FOUND
                        || newStatus == CourseSchedule.ScheduleStatus.NO_SOLUTION_FOUND;

            case SOLUTION_FOUND:
                // Από ΕΥΡΕΣΗ_ΛΥΣΗΣ μπορούμε να πάμε στην ΕΓΚΡΙΣΗ_ΛΥΣΗΣ ή πίσω στην ΕΚΤΕΛΕΣΗ
                return newStatus == CourseSchedule.ScheduleStatus.SOLUTION_APPROVED
                        || newStatus == CourseSchedule.ScheduleStatus.EXECUTION_PHASE;

            case NO_SOLUTION_FOUND:
                // Από ΜΗ_ΕΥΡΕΣΗ_ΛΥΣΗΣ μπορούμε να πάμε στην ΠΑΡΟΧΗ_ΠΡΟΤΙΜΗΣΕΩΝ ή ΛΗΞΗ
                return newStatus == CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE
                        || newStatus == CourseSchedule.ScheduleStatus.TERMINATED;

            case SOLUTION_APPROVED:
                // Από ΕΓΚΡΙΣΗ_ΛΥΣΗΣ τέλος - δεν επιτρέπεται αλλαγή
                return false;

            case TERMINATED:
                // Από ΛΗΞΗ δεν επιτρέπεται αλλαγή
                return false;

            default:
                return true;
        }
    }

}
