/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.service;

import com.icsd.springor.DTO.AssignmentDTO;
import com.icsd.springor.model.Assignment;
import com.icsd.springor.model.Course;
import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.model.User;
import com.icsd.springor.repository.AssignmentRepository;
import com.icsd.springor.repository.CourseRepository;
import com.icsd.springor.repository.CourseScheduleRepository;
import com.icsd.springor.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Transactional
public class AssignmentService {
    
    @Autowired
    private AssignmentRepository assignmentRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CourseScheduleRepository scheduleRepository;
    
    public AssignmentDTO createAssignment(Long courseId, Long teacherId, 
                                        Course.TeachingHours.CourseComponent component, 
                                        Long scheduleId) {
        
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found with id: " + courseId));
        
        User teacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + teacherId));
        
        CourseSchedule schedule = null;
        if (scheduleId != null) {
            schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Δεν βρέθηκε χρονοπρογραμματισμός με id: " + scheduleId));

            // Έλεγχος αν υπάρχει ήδη ανάθεση για το συγκεκριμένο schedule
            if (assignmentRepository.existsByCourseAndCourseComponentAndSchedule(course, component, schedule)) {
                throw new RuntimeException("Υπάρχει ήδη ανάθεση για αυτό το στοιχείο μαθήματος στον χρονοπρογραμματισμό");
            }

            // Έλεγχος φάσης χρονοπρογραμματισμού
            if (schedule.getStatus() != CourseSchedule.ScheduleStatus.ASSIGNMENT_PHASE) {
                throw new RuntimeException("Δεν είναι δυνατή η δημιουργία αναθέσεων. Ο χρονοπρογραμματισμός δεν είναι στη φάση ανάθεσης.");
            }
        } else {
            // Έλεγχος για γενική ανάθεση (χωρίς schedule)
            if (assignmentRepository.existsByCourseAndCourseComponentAndScheduleIsNull(course, component)) {
                throw new RuntimeException("Υπάρχει ήδη γενική ανάθεση για αυτό το στοιχείο μαθήματος");
            }
        }
        
        Assignment assignment = new Assignment();
        assignment.setCourse(course);
        assignment.setTeacher(teacher);
        assignment.setCourseComponent(component);
        assignment.setSchedule(schedule);
        assignment.setActive(true);
        assignment.setGeneralAssignment(schedule == null);
        
        Assignment savedAssignment = assignmentRepository.save(assignment);
        return convertToDTO(savedAssignment);
    }
    
    public long countAssignmentsBySchedule(Long scheduleId) {
        return assignmentRepository.findByScheduleId(scheduleId).size();
    }
    
    
    
   public List<AssignmentDTO> getFilteredAssignments(String search, Long scheduleId, 
                                                     Long teacherId, Long courseId, 
                                                     String component) {
        List<Assignment> assignments = assignmentRepository.findAll();
        
        // Εφαρμογή φίλτρων
        if (scheduleId != null) {
            assignments = assignments.stream()
                .filter(a -> a.getSchedule().getId().equals(scheduleId))
                .collect(Collectors.toList());
        }
        
        if (teacherId != null) {
            assignments = assignments.stream()
                .filter(a -> a.getTeacher().getId().equals(teacherId))
                .collect(Collectors.toList());
        }
        
        if (courseId != null) {
            assignments = assignments.stream()
                .filter(a -> a.getCourse().getId().equals(courseId))
                .collect(Collectors.toList());
        }
        
        if (component != null && !component.isEmpty()) {
            Course.TeachingHours.CourseComponent comp = 
                Course.TeachingHours.CourseComponent.valueOf(component);
            assignments = assignments.stream()
                .filter(a -> a.getCourseComponent() == comp)
                .collect(Collectors.toList());
        }
        
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase().trim();
            assignments = assignments.stream()
                .filter(a -> 
                    a.getCourse().getName().toLowerCase().contains(searchLower) ||
                    a.getCourse().getCode().toLowerCase().contains(searchLower) ||
                    a.getTeacher().getFullName().toLowerCase().contains(searchLower)
                )
                .collect(Collectors.toList());
        }
        
        return assignments.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public List<AssignmentDTO> getAssignmentsBySchedule(Long scheduleId) {
        List<Assignment> assignments = assignmentRepository.findActiveAssignmentsBySchedule(scheduleId);
        return assignments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<AssignmentDTO> getAssignmentsByTeacher(Long teacherId) {
        List<Assignment> assignments = assignmentRepository.findByTeacherId(teacherId);
        return assignments.stream()
                .filter(Assignment::isActive)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<AssignmentDTO> getAssignmentsByTeacherAndSchedule(Long teacherId, Long scheduleId) {
        List<Assignment> assignments = assignmentRepository.findByTeacherAndSchedule(teacherId, scheduleId);
        return assignments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public AssignmentDTO updateAssignment(Long assignmentId, Long newTeacherId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new RuntimeException("Assignment not found with id: " + assignmentId));
        
        // Check if schedule allows modifications
        if (assignment.getSchedule().getStatus() != CourseSchedule.ScheduleStatus.ASSIGNMENT_PHASE) {
            throw new RuntimeException("Cannot modify assignments. Schedule is not in assignment phase.");
        }
        
        User newTeacher = userRepository.findById(newTeacherId)
            .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + newTeacherId));
        
        assignment.setTeacher(newTeacher);
        Assignment savedAssignment = assignmentRepository.save(assignment);
        return convertToDTO(savedAssignment);
    }
    
    
    public List<AssignmentDTO> getAllAssignments() {
        List<Assignment> assignments = assignmentRepository.findAll();
        return assignments.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public void deleteAssignment(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new RuntimeException("Assignment not found with id: " + assignmentId));
        
        // Check if schedule allows modifications
        if (assignment.getSchedule().getStatus() != CourseSchedule.ScheduleStatus.ASSIGNMENT_PHASE) {
            throw new RuntimeException("Cannot delete assignments. Schedule is not in assignment phase.");
        }
        
        assignmentRepository.delete(assignment);
    }
    
    
    
    public boolean areAllCoursesAssigned(Long scheduleId) {
        // Get all courses that should be assigned
        List<Course> allCourses = courseRepository.findAll();
        List<Assignment> assignments = assignmentRepository.findActiveAssignmentsBySchedule(scheduleId);
        
        // Check if every course component has an assignment
        for (Course course : allCourses) {
            if (!course.isActive()) continue;
            
            // Check if course has theory component and if it's assigned
            boolean hasTheory = course.getTeachingHours().stream()
                .anyMatch(th -> th.getComponent() == Course.TeachingHours.CourseComponent.THEORY);
            if (hasTheory) {
                boolean theoryAssigned = assignments.stream()
                    .anyMatch(a -> a.getCourse().getId().equals(course.getId()) && 
                              a.getCourseComponent() == Course.TeachingHours.CourseComponent.THEORY);
                if (!theoryAssigned) return false;
            }
            
            // Check if course has lab component and if it's assigned
            boolean hasLab = course.getTeachingHours().stream()
                .anyMatch(th -> th.getComponent() == Course.TeachingHours.CourseComponent.LABORATORY);
            if (hasLab) {
                boolean labAssigned = assignments.stream()
                    .anyMatch(a -> a.getCourse().getId().equals(course.getId()) && 
                              a.getCourseComponent() == Course.TeachingHours.CourseComponent.LABORATORY);
                if (!labAssigned) return false;
            }
        }
        
        return true;
    }
    
    
    
    public List<AssignmentDTO> getRecentAssignments(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"));
        List<Assignment> assignments = assignmentRepository.findAll(pageable).getContent();
        return assignments.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
     public boolean existsAssignment(Long courseId, String courseComponent, Long scheduleId) {
        try {
            Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Δεν βρέθηκε μάθημα"));
            
            CourseSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Δεν βρέθηκε χρονοπρογραμματισμός"));
            
            Course.TeachingHours.CourseComponent component = 
                Course.TeachingHours.CourseComponent.valueOf(courseComponent);
            
            return assignmentRepository.existsByCourseAndCourseComponentAndSchedule(course, component, schedule);
        } catch (Exception e) {
            return false;
        }
    }
    
    
    public AssignmentDTO getAssignmentByCourseAndComponent(Long courseId, String courseComponent, Long scheduleId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Δεν βρέθηκε μάθημα"));
        
        CourseSchedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new RuntimeException("Δεν βρέθηκε χρονοπρογραμματισμός"));
        
        Course.TeachingHours.CourseComponent component = 
            Course.TeachingHours.CourseComponent.valueOf(courseComponent);
        
        List<Assignment> assignments = assignmentRepository.findByScheduleId(scheduleId);
        Assignment assignment = assignments.stream()
            .filter(a -> a.getCourse().getId().equals(courseId) && 
                        a.getCourseComponent() == component)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Δεν βρέθηκε ανάθεση"));
        
        return convertToDTO(assignment);
    }
    
    public Map<String, Object> getAssignmentStatistics(Long scheduleId) {
        List<Assignment> assignments;
        
        if (scheduleId != null) {
            assignments = assignmentRepository.findByScheduleId(scheduleId);
        } else {
            assignments = assignmentRepository.findAll();
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", assignments.size());
        stats.put("active", assignments.stream().filter(Assignment::isActive).count());
        stats.put("theory", assignments.stream()
            .filter(a -> a.getCourseComponent() == Course.TeachingHours.CourseComponent.THEORY)
            .count());
        stats.put("lab", assignments.stream()
            .filter(a -> a.getCourseComponent() == Course.TeachingHours.CourseComponent.LABORATORY)
            .count());
        
        // Στατιστικά ανά διδάσκοντα
        Map<String, Long> teacherStats = assignments.stream()
            .collect(Collectors.groupingBy(
                a -> a.getTeacher().getFullName(),
                Collectors.counting()
            ));
        stats.put("byTeacher", teacherStats);
        
        // Στατιστικά ανά μάθημα
        Map<String, Long> courseStats = assignments.stream()
            .collect(Collectors.groupingBy(
                a -> a.getCourse().getName(),
                Collectors.counting()
            ));
        stats.put("byCourse", courseStats);
        
        return stats;
    }

  
    
    private AssignmentDTO convertToDTO(Assignment assignment) {
        AssignmentDTO dto = new AssignmentDTO();
        dto.setId(assignment.getId());
        dto.setCourseId(assignment.getCourse().getId());
        dto.setCourseName(assignment.getCourse().getName());
        dto.setCourseCode(assignment.getCourse().getCode());
        dto.setTeacherId(assignment.getTeacher().getId());
        dto.setTeacherName(assignment.getTeacher().getFullName());
        dto.setCourseComponent(assignment.getCourseComponent());
        dto.setScheduleId(assignment.getSchedule().getId());
        
        if (assignment.getSchedule() != null) {
            dto.setScheduleName(assignment.getSchedule().getName());
            dto.setScheduleStatus(assignment.getSchedule().getStatus().toString());
        }
        

        dto.setActive(assignment.isActive());
        
        return dto;
    }
}