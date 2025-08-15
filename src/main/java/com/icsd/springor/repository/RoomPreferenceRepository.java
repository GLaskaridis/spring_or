/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.repository;

import com.icsd.springor.model.RoomPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RoomPreferenceRepository extends JpaRepository<RoomPreference, Long> {
    
    List<RoomPreference> findByAssignmentIdAndActiveTrue(Long assignmentId);
    
    @Query("SELECT rp FROM RoomPreference rp WHERE rp.assignment.teacher.id = :teacherId AND rp.assignment.schedule.id = :scheduleId AND rp.active = true")
    List<RoomPreference> findByTeacherAndSchedule(@Param("teacherId") Long teacherId, @Param("scheduleId") Long scheduleId);
    
    @Query("SELECT rp FROM RoomPreference rp WHERE rp.assignment.schedule.id = :scheduleId AND rp.active = true ORDER BY rp.preferenceWeight DESC")
    List<RoomPreference> findByScheduleOrderedByWeight(@Param("scheduleId") Long scheduleId);
    
    @Query("SELECT rp FROM RoomPreference rp WHERE rp.room.id = :roomId AND rp.assignment.schedule.id = :scheduleId AND rp.active = true")
    List<RoomPreference> findByRoomAndSchedule(@Param("roomId") Long roomId, @Param("scheduleId") Long scheduleId);
}