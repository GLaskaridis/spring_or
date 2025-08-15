/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.service;

import com.icsd.springor.DTO.TimeSlotPreferenceDTO;
import com.icsd.springor.model.Assignment;
import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.model.TimeSlotPreference;
import com.icsd.springor.repository.AssignmentRepository;
import com.icsd.springor.repository.TimeSlotPreferenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TimeSlotPreferenceService {
    
    @Autowired
    private TimeSlotPreferenceRepository preferenceRepository;
    
    @Autowired
    private AssignmentRepository assignmentRepository;
    
    public TimeSlotPreferenceDTO addTimeSlotPreference(Long assignmentId, DayOfWeek day, 
                                                      Integer slot, Integer weight, String notes) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new RuntimeException("Assignment not found"));
        
        if (assignment.getSchedule().getStatus() != CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE) {
            throw new RuntimeException("Cannot modify preferences. Schedule is not in requirements phase.");
        }
        
        TimeSlotPreference preference = new TimeSlotPreference();
        preference.setAssignment(assignment);
        preference.setPreferredDay(day);
        preference.setPreferredSlot(slot);
        preference.setPreferenceWeight(weight);
        preference.setNotes(notes);
        
        TimeSlotPreference saved = preferenceRepository.save(preference);
        return convertToDTO(saved);
    }
    
    public List<TimeSlotPreferenceDTO> getPreferencesByAssignment(Long assignmentId) {
        List<TimeSlotPreference> preferences = preferenceRepository.findByAssignmentIdAndActiveTrue(assignmentId);
        return preferences.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<TimeSlotPreferenceDTO> getPreferencesByTeacherAndSchedule(Long teacherId, Long scheduleId) {
        List<TimeSlotPreference> preferences = preferenceRepository.findByTeacherAndSchedule(teacherId, scheduleId);
        return preferences.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<TimeSlotPreferenceDTO> getAllPreferencesForSchedule(Long scheduleId) {
        List<TimeSlotPreference> preferences = preferenceRepository.findByScheduleOrderedByWeight(scheduleId);
        return preferences.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public TimeSlotPreferenceDTO updatePreference(Long preferenceId, DayOfWeek day, 
                                                 Integer slot, Integer weight, String notes) {
        TimeSlotPreference preference = preferenceRepository.findById(preferenceId)
            .orElseThrow(() -> new RuntimeException("Preference not found"));
        
        if (preference.getAssignment().getSchedule().getStatus() != CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE) {
            throw new RuntimeException("Cannot modify preferences. Schedule is not in requirements phase.");
        }
        
        preference.setPreferredDay(day);
        preference.setPreferredSlot(slot);
        preference.setPreferenceWeight(weight);
        preference.setNotes(notes);
        
        TimeSlotPreference saved = preferenceRepository.save(preference);
        return convertToDTO(saved);
    }
    
    public void deletePreference(Long preferenceId) {
        TimeSlotPreference preference = preferenceRepository.findById(preferenceId)
            .orElseThrow(() -> new RuntimeException("Preference not found"));
        
        if (preference.getAssignment().getSchedule().getStatus() != CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE) {
            throw new RuntimeException("Cannot delete preferences. Schedule is not in requirements phase.");
        }
        
        preferenceRepository.delete(preference);
    }
    
    private TimeSlotPreferenceDTO convertToDTO(TimeSlotPreference preference) {
        TimeSlotPreferenceDTO dto = new TimeSlotPreferenceDTO();
        dto.setId(preference.getId());
        dto.setAssignmentId(preference.getAssignment().getId());
        dto.setCourseName(preference.getAssignment().getCourse().getName());
        dto.setCourseCode(preference.getAssignment().getCourse().getCode());
        dto.setPreferredDay(preference.getPreferredDay());
        dto.setPreferredSlot(preference.getPreferredSlot());
        dto.setPreferenceWeight(preference.getPreferenceWeight());
        dto.setNotes(preference.getNotes());
        dto.setActive(preference.isActive());
        return dto;
    }
}