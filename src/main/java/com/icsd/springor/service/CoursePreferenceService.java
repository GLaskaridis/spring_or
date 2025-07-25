package com.icsd.springor.service;

import com.icsd.springor.DTO.CoursePreferenceDTO;
import com.icsd.springor.model.Course;
import com.icsd.springor.model.CoursePreference;
import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.model.User;
import com.icsd.springor.repository.CoursePreferenceRepository;
import com.icsd.springor.repository.CourseRepository;
import com.icsd.springor.repository.CourseScheduleRepository;
import com.icsd.springor.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CoursePreferenceService {
    
    @Autowired
    private CoursePreferenceRepository preferenceRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CourseScheduleRepository scheduleRepository;
    
    public CoursePreferenceDTO addCoursePreference(Long teacherId, Long courseId, 
                                                  Course.TeachingHours.CourseComponent component, 
                                                  Long scheduleId, Integer preferenceLevel, String notes) {
        
        User teacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new RuntimeException("Teacher not found"));
        
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));
        
        CourseSchedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new RuntimeException("Schedule not found"));
        
       
        if (schedule.getStatus() != CourseSchedule.ScheduleStatus.ASSIGNMENT_PHASE) {
            throw new RuntimeException("Cannot add preferences. Schedule is not in assignment phase.");
        }
        
        if (preferenceRepository.existsByTeacherAndCourseAndCourseComponentAndSchedule(teacher, course, component, schedule)) {
            throw new RuntimeException("Preference for this course component already exists");
        }
        
        CoursePreference preference = new CoursePreference();
        preference.setTeacher(teacher);
        preference.setCourse(course);
        preference.setCourseComponent(component);
        preference.setSchedule(schedule);
        preference.setPreferenceLevel(preferenceLevel != null ? preferenceLevel : 3);
        preference.setNotes(notes);
        
        CoursePreference saved = preferenceRepository.save(preference);
        return convertToDTO(saved);
    }
    
    public void removeCoursePreference(Long teacherId, Long courseId, 
                                     Course.TeachingHours.CourseComponent component, 
                                     Long scheduleId) {
        
        User teacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new RuntimeException("Teacher not found"));
        
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));
        
        CourseSchedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new RuntimeException("Schedule not found"));
        
        if (schedule.getStatus() != CourseSchedule.ScheduleStatus.ASSIGNMENT_PHASE) {
            throw new RuntimeException("Cannot remove preferences. Schedule is not in assignment phase.");
        }
        
        CoursePreference preference = preferenceRepository.findByTeacherAndCourseAndCourseComponentAndSchedule(teacher, course, component, schedule)
            .orElseThrow(() -> new RuntimeException("Preference not found"));
        
        preferenceRepository.delete(preference);
    }
    
    public List<CoursePreferenceDTO> getTeacherPreferences(Long teacherId, Long scheduleId) {
        List<CoursePreference> preferences = preferenceRepository.findByTeacherAndScheduleOrderByCourse(teacherId, scheduleId);
        return preferences.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<CoursePreferenceDTO> getAllPreferencesForSchedule(Long scheduleId) {
        List<CoursePreference> preferences = preferenceRepository.findByScheduleIdAndActiveTrue(scheduleId);
        return preferences.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<CoursePreferenceDTO> getPreferencesForCourse(Long courseId, Long scheduleId) {
        List<CoursePreference> preferences = preferenceRepository.findByCourseIdAndScheduleIdAndActiveTrue(courseId, scheduleId);
        return preferences.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<CoursePreferenceDTO> getPreferencesForCourseComponent(Long courseId, 
                                                                    Course.TeachingHours.CourseComponent component,
                                                                    Long scheduleId) {
        List<CoursePreference> preferences = preferenceRepository.findPreferencesForCourseComponent(courseId, component, scheduleId);
        return preferences.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public CoursePreferenceDTO updatePreferenceLevel(Long teacherId, Long courseId, 
                                                   Course.TeachingHours.CourseComponent component,
                                                   Long scheduleId, Integer newLevel, String notes) {
        
        User teacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new RuntimeException("Teacher not found"));
        
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));
        
        CourseSchedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new RuntimeException("Schedule not found"));
        
        CoursePreference preference = preferenceRepository.findByTeacherAndCourseAndCourseComponentAndSchedule(teacher, course, component, schedule)
            .orElseThrow(() -> new RuntimeException("Preference not found"));
        
        preference.setPreferenceLevel(newLevel);
        preference.setNotes(notes);
        
        CoursePreference saved = preferenceRepository.save(preference);
        return convertToDTO(saved);
    }
    
    private CoursePreferenceDTO convertToDTO(CoursePreference preference) {
        CoursePreferenceDTO dto = new CoursePreferenceDTO();
        dto.setId(preference.getId());
        dto.setTeacherId(preference.getTeacher().getId());
        dto.setTeacherName(preference.getTeacher().getFullName());
        dto.setTeacherUsername(preference.getTeacher().getUsername());
        dto.setCourseId(preference.getCourse().getId());
        dto.setCourseName(preference.getCourse().getName());
        dto.setCourseCode(preference.getCourse().getCode());
        dto.setCourseComponent(preference.getCourseComponent());
        dto.setScheduleId(preference.getSchedule().getId());
        dto.setPreferenceLevel(preference.getPreferenceLevel());
        dto.setNotes(preference.getNotes());
        dto.setCreatedAt(preference.getCreatedAt());
        dto.setActive(preference.isActive());
        return dto;
    }
}