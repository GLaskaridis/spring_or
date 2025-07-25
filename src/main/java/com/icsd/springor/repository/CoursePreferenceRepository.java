/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.repository;

import com.icsd.springor.model.CoursePreference;
import com.icsd.springor.model.Course;
import com.icsd.springor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CoursePreferenceRepository extends JpaRepository<CoursePreference, Long> {
    
    List<CoursePreference> findByTeacherIdAndScheduleIdAndActiveTrue(Long teacherId, Long scheduleId);
    
    List<CoursePreference> findByScheduleIdAndActiveTrue(Long scheduleId);
    
    List<CoursePreference> findByCourseIdAndScheduleIdAndActiveTrue(Long courseId, Long scheduleId);
    
    Optional<CoursePreference> findByTeacherAndCourseAndCourseComponentAndSchedule(
        User teacher, Course course, Course.TeachingHours.CourseComponent component, 
        com.icsd.springor.model.CourseSchedule schedule);
    
    @Query("SELECT cp FROM CoursePreference cp WHERE cp.course.id = :courseId AND cp.courseComponent = :component AND cp.schedule.id = :scheduleId AND cp.active = true ORDER BY cp.preferenceLevel DESC")
    List<CoursePreference> findPreferencesForCourseComponent(@Param("courseId") Long courseId, 
                                                            @Param("component") Course.TeachingHours.CourseComponent component,
                                                            @Param("scheduleId") Long scheduleId);
    
    @Query("SELECT cp FROM CoursePreference cp WHERE cp.teacher.id = :teacherId AND cp.schedule.id = :scheduleId AND cp.active = true ORDER BY cp.course.name")
    List<CoursePreference> findByTeacherAndScheduleOrderByCourse(@Param("teacherId") Long teacherId, @Param("scheduleId") Long scheduleId);
    
    boolean existsByTeacherAndCourseAndCourseComponentAndSchedule(
        User teacher, Course course, Course.TeachingHours.CourseComponent component, 
        com.icsd.springor.model.CourseSchedule schedule);
}