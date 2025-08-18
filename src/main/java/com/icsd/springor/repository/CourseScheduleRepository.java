/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.repository;

import com.icsd.springor.model.CourseSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CourseScheduleRepository extends JpaRepository<CourseSchedule, Long> {
    
    List<CourseSchedule> findByOrderByCreatedAtDesc();
    
    Optional<CourseSchedule> findByNameAndSemester(String name, String semester);
    
    @Query("SELECT cs FROM CourseSchedule cs WHERE cs.status IN ('SOLUTION_APPROVED', 'SOLUTION_FOUND') ORDER BY cs.updatedAt DESC")
    List<CourseSchedule> findApprovedSchedules();
    
    @Query("SELECT cs FROM CourseSchedule cs WHERE cs.status = 'ASSIGNMENT_PHASE' ORDER BY cs.createdAt DESC")
    List<CourseSchedule> findSchedulesInAssignmentPhase();
    
    boolean existsByName(String name);
    
    List<CourseSchedule> findByStatus(CourseSchedule.ScheduleStatus status);
    
    List<CourseSchedule> findBySemesterAndYear(String semester, Integer year);
    
    List<CourseSchedule> findByActiveTrue();
}