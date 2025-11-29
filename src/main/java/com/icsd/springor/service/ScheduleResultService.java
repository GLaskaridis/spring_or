/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.service;import com.icsd.springor.CourseScheduler;
import com.icsd.springor.DTO.AssignmentDTO;
import com.icsd.springor.model.*;
import com.icsd.springor.repository.ScheduleResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service για διαχείριση αποτελεσμάτων χρονοπρογραμματισμού
 */
@Service
@Transactional
public class ScheduleResultService {
    
    @Autowired
    private ScheduleResultRepository scheduleResultRepository;
    
    @Autowired
    private AssignmentService assignmentService;
    
    @Autowired
    private CourseService courseService;
    
    /**
     * Αποθηκεύει τα αποτελέσματα του αλγορίθμου στη βάση δεδομένων
     */
    public void saveScheduleResults(CourseSchedule schedule, List<CourseScheduler.CourseAssignment> algorithmResults) {
        // Πρώτα διαγράφουμε τυχόν προηγούμενα αποτελέσματα
        scheduleResultRepository.deleteByScheduleId(schedule.getId());
        
        // Παίρνουμε τις αναθέσεις για αυτό το schedule
        List<AssignmentDTO> assignments = assignmentService.getAssignmentsBySchedule(schedule.getId());
        
        // Δημιουργούμε χάρτη για γρήγορη αναζήτηση assignments
        Map<String, AssignmentDTO> assignmentMap = assignments.stream()
            .collect(Collectors.toMap(
                assignment -> createAssignmentKey(assignment.getCourseCode(), assignment.getCourseComponent()),
                assignment -> assignment,
                (existing, replacement) -> existing
            ));
        
        for (CourseScheduler.CourseAssignment algorithmResult : algorithmResults) {
            // Βρίσκουμε την αντίστοιχη assignment
            String key = createAssignmentKey(algorithmResult.course.getCode(), algorithmResult.course.getActiveComponent());
            AssignmentDTO assignmentDTO = assignmentMap.get(key);
            
            if (assignmentDTO != null) {
                Assignment assignment = assignmentDTO.toEntity();
                
                ScheduleResult result = new ScheduleResult();
                result.setSchedule(schedule);
                result.setAssignment(assignment);
                result.setRoom(algorithmResult.room);
                result.setDayOfWeek(algorithmResult.day);
                result.setStartTime(algorithmResult.startTime);
                result.setEndTime(algorithmResult.endTime);
                result.setSlotNumber(algorithmResult.slot % 4); // 0-3 slots ανά ημέρα
                
                scheduleResultRepository.save(result);
            }
        }
    }
    
    /**
     * Επιστρέφει τα αποτελέσματα για έναν χρονοπρογραμματισμό
     */
    public List<ScheduleResult> getScheduleResults(Long scheduleId) {
        return scheduleResultRepository.findByScheduleIdOrderByDayOfWeekAscSlotNumberAsc(scheduleId);
    }
    
    /**
     * Επιστρέφει τα αποτελέσματα με όλες τις λεπτομέρειες για εμφάνιση
     */
    public List<ScheduleResult> getScheduleResultsWithDetails(Long scheduleId) {
        return scheduleResultRepository.findScheduleResultsWithDetails(scheduleId);
    }
    
    /**
     * Ελέγχει αν υπάρχουν αποθηκευμένα αποτελέσματα
     */
    public boolean hasScheduleResults(Long scheduleId) {
        return scheduleResultRepository.existsByScheduleId(scheduleId);
    }
    
    /**
     * Μετρά τα αποτελέσματα για έναν χρονοπρογραμματισμό
     */
    public long countScheduleResults(Long scheduleId) {
        return scheduleResultRepository.countByScheduleId(scheduleId);
    }
    
    /**
     * Διαγράφει τα αποτελέσματα για έναν χρονοπρογραμματισμό
     */
    public void deleteScheduleResults(Long scheduleId) {
        scheduleResultRepository.deleteByScheduleId(scheduleId);
    }
    
    /**
     * Επιστρέφει τα αποτελέσματα σε μορφή κατάλληλη για το frontend
     */
    public List<ScheduleDisplayDTO> getScheduleForDisplay(Long scheduleId) {
        List<ScheduleResult> results = getScheduleResultsWithDetails(scheduleId);
        
        return results.stream().map(result -> {
            ScheduleDisplayDTO dto = new ScheduleDisplayDTO();
            dto.setId(result.getId());
            dto.setCourseName(result.getAssignment().getCourse().getName());
            dto.setCourseCode(result.getAssignment().getCourse().getCode());
            dto.setCourseComponent(result.getAssignment().getCourseComponent().name());
            dto.setTeacherName(result.getAssignment().getTeacher().getFullName());
            dto.setRoomName(result.getRoom().getName());
            dto.setDay(result.getDayOfWeek().name());
            dto.setTimeSlot(result.getSlotNumber());
            dto.setStartTime(result.getStartTime().toString());
            dto.setEndTime(result.getEndTime().toString());
            dto.setStatus("CONFIRMED");
            return dto;
        }).collect(Collectors.toList());
    }
    
    /**
     * Δημιουργεί κλειδί για το mapping assignment
     */
    private String createAssignmentKey(String courseCode, Course.TeachingHours.CourseComponent component) {
        return courseCode + "_" + component.name();
    }
    
    /**
     * DTO για εμφάνιση αποτελεσμάτων στο frontend
     */
    public static class ScheduleDisplayDTO {
        private Long id;
        private String courseName;
        private String courseCode;
        private String courseComponent;
        private String teacherName;
        private String roomName;
        private String day;
        private Integer timeSlot;
        private String startTime;
        private String endTime;
        private String status;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }
        
        public String getCourseCode() { return courseCode; }
        public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
        
        public String getCourseComponent() { return courseComponent; }
        public void setCourseComponent(String courseComponent) { this.courseComponent = courseComponent; }
        
        public String getTeacherName() { return teacherName; }
        public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
        
        public String getRoomName() { return roomName; }
        public void setRoomName(String roomName) { this.roomName = roomName; }
        
        public String getDay() { return day; }
        public void setDay(String day) { this.day = day; }
        
        public Integer getTimeSlot() { return timeSlot; }
        public void setTimeSlot(Integer timeSlot) { this.timeSlot = timeSlot; }
        
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}






