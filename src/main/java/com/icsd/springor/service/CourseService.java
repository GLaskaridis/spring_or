package com.icsd.springor.service;


import com.icsd.springor.model.Course;
import com.icsd.springor.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import com.icsd.springor.DTO.AssignmentDTO;
import com.icsd.springor.model.Course;
import com.icsd.springor.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private AssignmentService assignmentService;

    public Course addCourse(Course course) {
        if (isValidCourse(course)) {
            return courseRepository.save(course);
        }
        throw new IllegalArgumentException("Invalid course data");
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));
    }

    public Course updateCourse(Long id, Course updatedCourse) {
        Course course = getCourseById(id);
        // Update the fields
        course.setName(updatedCourse.getName());
        course.setCode(updatedCourse.getCode());
        course.setType(updatedCourse.getType());
        course.setYear(updatedCourse.getYear());
        course.setSemester(updatedCourse.getSemester());
        course.setTeachingHours(updatedCourse.getTeachingHours());
        course.setCapacity(updatedCourse.getCapacity());
        // Save the updated course
        return courseRepository.save(course);
    }

    public void deleteCourse(Long id) {
        Course course = getCourseById(id);
        courseRepository.delete(course);
    }

    public void deactivateCourse(Long id) {
        Course course = getCourseById(id);
        course.setActive(false);
        courseRepository.save(course);
    }

    private boolean isValidCourse(Course course) {
        // Implement validation logic here
        return true; // Placeholder
    }
    
    public long countAllCourses() {
        return courseRepository.count();
    }

    public List<Course> getAllActiveCourses() {
        return courseRepository.findAll().stream()
            .filter(Course::isActive)
            .collect(Collectors.toList());
    }
    
    /**
     * ΝΕΑ ΜΕΘΟΔΟΣ: Επιστρέφει τα μαθήματα που έχουν ανατεθεί σε συγκεκριμένο χρονοπρογραμματισμό
     */
    public List<Course> getCoursesBySchedule(Long scheduleId) {
        System.out.println("📚 Getting courses for schedule: " + scheduleId);
        
        try {
            // παιρνουμε τις αναθεσεις για αυτον τον schedule
            List<AssignmentDTO> assignments = assignmentService.getAssignmentsBySchedule(scheduleId);
            System.out.println("  - Found " + assignments.size() + " assignments");
            
            // εξαγουμε τα course IDs και φερνουμε τα courses
            List<Course> courses = assignments.stream()
                .map(AssignmentDTO::getCourseId)
                .distinct() // αποφυγη διπλων course IDs
                .map(this::getCourseById)
                .collect(Collectors.toList());
            
            System.out.println("  - Returning " + courses.size() + " unique courses");
            
            // προετοιμασια των courses με active components
            for (Course course : courses) {
                prepareActiveCourseComponent(course, assignments);
            }
            
            return courses;
            
        } catch (Exception e) {
            System.out.println("❌ Error getting courses by schedule: " + e.getMessage());
            e.printStackTrace();
            
            // fallback: επιστρεφουμε ολα τα ενεργα μαθηματα
            System.out.println("🔄 Falling back to all active courses");
            return getAllActiveCourses();
        }
    }
    
    /**
     * Προετοιμάζει το active component για ένα course βάσει των assignments
     */
    private void prepareActiveCourseComponent(Course course, List<AssignmentDTO> assignments) {
        try {
            // βρισκουμε τα assignments για αυτο το course
            List<AssignmentDTO> courseAssignments = assignments.stream()
                .filter(a -> a.getCourseId().equals(course.getId()))
                .collect(Collectors.toList());
            
            if (!courseAssignments.isEmpty()) {
                // παιρνουμε το πρωτο component ως default
                Course.TeachingHours.CourseComponent firstComponent = courseAssignments.get(0).getCourseComponent();
                course.setActiveComponent(firstComponent);
                
                System.out.println("  - Set active component for " + course.getCode() + ": " + firstComponent);
            } else {
                // fallback: ελεγχουμε τι εχει το course
                setDefaultActiveComponent(course);
            }
            
        } catch (Exception e) {
            System.out.println("⚠️ Warning: Could not set active component for " + course.getCode() + ": " + e.getMessage());
            setDefaultActiveComponent(course);
        }
    }
    
    /**
     * Θέτει το default active component για ένα course
     */
    private void setDefaultActiveComponent(Course course) {
        if (course.hasTheory()) {
            course.setActiveComponent(Course.TeachingHours.CourseComponent.THEORY);
        } else if (course.hasLab()) {
            course.setActiveComponent(Course.TeachingHours.CourseComponent.LABORATORY);
        } else {
            // fallback
            course.setActiveComponent(Course.TeachingHours.CourseComponent.THEORY);
        }
        
        System.out.println("  - Set default active component for " + course.getCode() + ": " + course.getActiveComponent());
    }
    
    /**
     * Επιστρέφει όλα τα μαθήματα που έχουν ανατεθεί (σε οποιοδήποτε schedule)
     */
    public List<Course> getAssignedCourses() {
        return courseRepository.findAll().stream()
            .filter(Course::isActive)
            .collect(Collectors.toList());
    }
    
    /**
     * Επιστρέφει μαθήματα ανά εξάμηνο
     */
    public List<Course> getCoursesBySemester(Integer semester) {
        return courseRepository.findAll().stream()
            .filter(Course::isActive)
            .filter(course -> course.getSemester().equals(semester))
            .collect(Collectors.toList());
    }
    
    /**
     * Επιστρέφει μαθήματα ανά έτος
     */
    public List<Course> getCoursesByYear(Integer year) {
        return courseRepository.findAll().stream()
            .filter(Course::isActive)
            .filter(course -> course.getYear().equals(year))
            .collect(Collectors.toList());
    }
}






