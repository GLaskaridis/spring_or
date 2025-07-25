package com.icsd.springor.service;

import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.repository.CourseScheduleRepository;
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
        schedule.setStatus(CourseSchedule.ScheduleStatus.COURSE_PREFERENCES);
        
        return scheduleRepository.save(schedule);
    }
    
    public CourseSchedule getScheduleById(Long id) {
        return scheduleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + id));
    }
    
    public List<CourseSchedule> getAllSchedules() {
        return scheduleRepository.findByOrderByCreatedAtDesc();
    }
    
    public CourseSchedule changeScheduleStatus(Long scheduleId, CourseSchedule.ScheduleStatus newStatus) {
        CourseSchedule schedule = getScheduleById(scheduleId);
        
        if (!isValidStateTransition(schedule.getStatus(), newStatus)) {
            throw new RuntimeException("Invalid state transition from " + schedule.getStatus() + " to " + newStatus);
        }
        
        if (newStatus == CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE) {
            if (!assignmentService.areAllCoursesAssigned(scheduleId)) {
                throw new RuntimeException("Cannot move to requirements phase. Not all courses are assigned.");
            }
        }
        
        
        if (newStatus == CourseSchedule.ScheduleStatus.EXECUTION_PHASE) {
            if (!preferenceService.areAllPreferencesProvided(scheduleId)) {
                throw new RuntimeException("Cannot move to execution phase. Not all teachers have provided their preferences.");
            }
        }
        
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
                return target == CourseSchedule.ScheduleStatus.SOLUTION_FOUND || 
                       target == CourseSchedule.ScheduleStatus.NO_SOLUTION_FOUND;
            case SOLUTION_FOUND:
                return target == CourseSchedule.ScheduleStatus.SOLUTION_APPROVED || 
                       target == CourseSchedule.ScheduleStatus.EXECUTION_PHASE;
            case NO_SOLUTION_FOUND:
                return target == CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE || 
                       target == CourseSchedule.ScheduleStatus.TERMINATED;
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
        
         if (schedule.getStatus() != CourseSchedule.ScheduleStatus.ASSIGNMENT_PHASE && 
            schedule.getStatus() != CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE) {
            throw new RuntimeException("Cannot delete schedule in current state: " + schedule.getStatus());
        }
        
        scheduleRepository.delete(schedule);
    }
}