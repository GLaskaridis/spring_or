/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.repository;

import com.icsd.springor.model.TeacherPreference;
import com.icsd.springor.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeacherPreferenceRepository extends JpaRepository<TeacherPreference, Long> {
    
    List<TeacherPreference> findByAssignmentId(Long assignmentId);
    
    @Query("SELECT tp FROM TeacherPreference tp WHERE tp.assignment.teacher.id = :teacherId AND tp.active = true")
    List<TeacherPreference> findByTeacherId(@Param("teacherId") Long teacherId);
    
    @Query("SELECT tp FROM TeacherPreference tp WHERE tp.assignment.schedule.id = :scheduleId AND tp.active = true")
    List<TeacherPreference> findByScheduleId(@Param("scheduleId") Long scheduleId);
    
    @Query("SELECT tp FROM TeacherPreference tp WHERE tp.assignment.teacher.id = :teacherId AND tp.assignment.schedule.id = :scheduleId AND tp.active = true")
    List<TeacherPreference> findByTeacherAndSchedule(@Param("teacherId") Long teacherId, @Param("scheduleId") Long scheduleId);
    
    List<TeacherPreference> findByAssignmentAndActiveTrue(Assignment assignment);
}