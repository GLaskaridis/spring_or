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
    
    @Query("SELECT tp FROM TeacherPreference tp " +
           "JOIN tp.assignment a " +
           "WHERE a.teacher.id = :teacherId " +
           "AND a.schedule.id = :scheduleId " +
           "AND tp.active = true")
    List<TeacherPreference> findByTeacherAndSchedule(@Param("teacherId") Long teacherId, 
                                                    @Param("scheduleId") Long scheduleId);
    
    @Query("SELECT tp FROM TeacherPreference tp " +
           "JOIN tp.assignment a " +
           "WHERE a.schedule.id = :scheduleId " +
           "AND tp.active = true " +
           "ORDER BY tp.preferenceWeight DESC, tp.priorityWeight DESC")
    List<TeacherPreference> findByScheduleOrderedByWeight(@Param("scheduleId") Long scheduleId);
    
    @Query("SELECT tp FROM TeacherPreference tp " +
           "WHERE tp.assignment.id = :assignmentId " +
           "AND tp.type = :type " +
           "AND tp.active = true")
    List<TeacherPreference> findByAssignmentAndType(@Param("assignmentId") Long assignmentId, 
                                                   @Param("type") TeacherPreference.PreferenceType type);
    
     @Query("SELECT tp FROM TeacherPreference tp " +
           "WHERE tp.preferredRoom.id = :roomId " +
           "AND tp.active = true")
    List<TeacherPreference> findByPreferredRoom(@Param("roomId") Long roomId);
    
    @Query("SELECT tp FROM TeacherPreference tp " +
           "WHERE tp.preferredDay = :day " +
           "AND tp.preferredSlot = :slot " +
           "AND tp.active = true")
    List<TeacherPreference> findByDayAndSlot(@Param("day") java.time.DayOfWeek day, 
                                           @Param("slot") Integer slot);
    
    @Query("SELECT COUNT(tp) FROM TeacherPreference tp " +
           "WHERE tp.assignment.id = :assignmentId " +
           "AND tp.active = true")
    Long countByAssignment(@Param("assignmentId") Long assignmentId);
    
    List<TeacherPreference> findByAssignmentAndActiveTrue(Assignment assignment);
    
    List<TeacherPreference> findByAssignmentIdAndActiveTrue(Long assignmentId);

    
}
