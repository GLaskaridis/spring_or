/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.repository;

import com.icsd.springor.model.TimeSlotPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TimeSlotPreferenceRepository extends JpaRepository<TimeSlotPreference, Long> {
    
    List<TimeSlotPreference> findByAssignmentIdAndActiveTrue(Long assignmentId);
    
    @Query("SELECT tp FROM TimeSlotPreference tp WHERE tp.assignment.teacher.id = :teacherId AND tp.assignment.schedule.id = :scheduleId AND tp.active = true")
    List<TimeSlotPreference> findByTeacherAndSchedule(@Param("teacherId") Long teacherId, @Param("scheduleId") Long scheduleId);
    
    @Query("SELECT tp FROM TimeSlotPreference tp WHERE tp.assignment.schedule.id = :scheduleId AND tp.active = true ORDER BY tp.preferenceWeight DESC")
    List<TimeSlotPreference> findByScheduleOrderedByWeight(@Param("scheduleId") Long scheduleId);
}