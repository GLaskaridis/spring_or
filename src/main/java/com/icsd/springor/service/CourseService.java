package com.icsd.springor.service;


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

    
}