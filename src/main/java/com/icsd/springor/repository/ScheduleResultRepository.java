/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.repository;

import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.model.ScheduleResult;
import java.time.DayOfWeek;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import com.icsd.springor.model.ScheduleResult;
import com.icsd.springor.model.CourseSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface ScheduleResultRepository extends JpaRepository<ScheduleResult, Long> {
    
    /**
     * Βρίσκει όλα τα αποτελέσματα για έναν συγκεκριμένο χρονοπρογραμματισμό
     */
    List<ScheduleResult> findByScheduleOrderByDayOfWeekAscSlotNumberAsc(CourseSchedule schedule);
    
    /**
     * Βρίσκει όλα τα αποτελέσματα για έναν συγκεκριμένο χρονοπρογραμματισμό (με ID)
     */
    List<ScheduleResult> findByScheduleIdOrderByDayOfWeekAscSlotNumberAsc(Long scheduleId);
    
    /**
     * Ελέγχει αν υπάρχουν αποτελέσματα για έναν χρονοπρογραμματισμό
     */
    boolean existsByScheduleId(Long scheduleId);
    
    /**
     * Βρίσκει αποτελέσματα για συγκεκριμένη ημέρα
     */
    List<ScheduleResult> findByScheduleIdAndDayOfWeekOrderBySlotNumberAsc(Long scheduleId, DayOfWeek dayOfWeek);
    
    /**
     * Διαγραφή όλων των αποτελεσμάτων για έναν χρονοπρογραμματισμό
     */
    void deleteByScheduleId(Long scheduleId);
    
    /**
     * Μετρά τα αποτελέσματα για έναν χρονοπρογραμματισμό
     */
    @Query("SELECT COUNT(sr) FROM ScheduleResult sr WHERE sr.schedule.id = :scheduleId")
    long countByScheduleId(@Param("scheduleId") Long scheduleId);
    
    /**
     * Βρίσκει αποτελέσματα με λεπτομέρειες για εμφάνιση προγράμματος
     */
    @Query("""
        SELECT sr FROM ScheduleResult sr 
        JOIN FETCH sr.assignment a 
        JOIN FETCH a.course c 
        JOIN FETCH a.teacher t 
        JOIN FETCH sr.room r 
        WHERE sr.schedule.id = :scheduleId 
        ORDER BY sr.dayOfWeek ASC, sr.slotNumber ASC
    """)
    List<ScheduleResult> findScheduleResultsWithDetails(@Param("scheduleId") Long scheduleId);
}