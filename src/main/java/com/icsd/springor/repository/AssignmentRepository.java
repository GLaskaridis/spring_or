/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.repository;

import com.icsd.springor.model.Assignment;
import com.icsd.springor.model.Course;
import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    
    List<Assignment> findByTeacherId(Long teacherId);
    
    List<Assignment> findByCourseId(Long courseId);
    
    List<Assignment> findByScheduleId(Long scheduleId);
    
    Optional<Assignment> findByCourseAndCourseComponent(Course course, Course.TeachingHours.CourseComponent component);
    
    @Query("SELECT a FROM Assignment a WHERE a.schedule.id = :scheduleId AND a.active = true")
    List<Assignment> findActiveAssignmentsBySchedule(@Param("scheduleId") Long scheduleId);
    
    @Query("SELECT a FROM Assignment a WHERE a.teacher.id = :teacherId AND a.schedule.id = :scheduleId AND a.active = true")
    List<Assignment> findByTeacherAndSchedule(@Param("teacherId") Long teacherId, @Param("scheduleId") Long scheduleId);
    
    boolean existsByCourseAndCourseComponentAndSchedule(Course course, Course.TeachingHours.CourseComponent component, CourseSchedule schedule);
    
    boolean existsByCourseAndCourseComponentAndScheduleIsNull(
        Course course, 
        Course.TeachingHours.CourseComponent component
    );
    
    @Query("SELECT a FROM Assignment a WHERE a.schedule IS NULL AND a.active = true")
    List<Assignment> findGeneralAssignments();
    
    
    @Query("SELECT a FROM Assignment a WHERE a.active = true AND (a.schedule.id = :scheduleId OR (:includeGeneral = true AND a.schedule IS NULL))")
    List<Assignment> findAssignmentsByScheduleOrGeneral(@Param("scheduleId") Long scheduleId, @Param("includeGeneral") boolean includeGeneral);

}