/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.service;

import com.icsd.springor.DTO.RoomPreferenceDTO;
import com.icsd.springor.model.Assignment;
import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.model.Room;
import com.icsd.springor.model.RoomPreference;
import com.icsd.springor.repository.AssignmentRepository;
import com.icsd.springor.repository.RoomPreferenceRepository;
import com.icsd.springor.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomPreferenceService {
    
    @Autowired
    private RoomPreferenceRepository preferenceRepository;
    
    @Autowired
    private AssignmentRepository assignmentRepository;
    
    @Autowired
    private RoomRepository roomRepository;
    
    public RoomPreferenceDTO addRoomPreference(Long assignmentId, Long roomId, 
                                             Integer weight, String notes) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new RuntimeException("Assignment not found"));
        
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));
        
        if (assignment.getSchedule().getStatus() != CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE) {
            throw new RuntimeException("Cannot modify preferences. Schedule is not in requirements phase.");
        }
        
        RoomPreference preference = new RoomPreference();
        preference.setAssignment(assignment);
        preference.setRoom(room);
        preference.setPreferenceWeight(weight);
        preference.setNotes(notes);
        
        RoomPreference saved = preferenceRepository.save(preference);
        return convertToDTO(saved);
    }
    
    public List<RoomPreferenceDTO> getPreferencesByAssignment(Long assignmentId) {
        List<RoomPreference> preferences = preferenceRepository.findByAssignmentIdAndActiveTrue(assignmentId);
        return preferences.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<RoomPreferenceDTO> getPreferencesByTeacherAndSchedule(Long teacherId, Long scheduleId) {
        List<RoomPreference> preferences = preferenceRepository.findByTeacherAndSchedule(teacherId, scheduleId);
        return preferences.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<RoomPreferenceDTO> getAllPreferencesForSchedule(Long scheduleId) {
        List<RoomPreference> preferences = preferenceRepository.findByScheduleOrderedByWeight(scheduleId);
        return preferences.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public RoomPreferenceDTO updatePreference(Long preferenceId, Integer weight, String notes) {
        RoomPreference preference = preferenceRepository.findById(preferenceId)
            .orElseThrow(() -> new RuntimeException("Preference not found"));
        
        if (preference.getAssignment().getSchedule().getStatus() != CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE) {
            throw new RuntimeException("Cannot modify preferences. Schedule is not in requirements phase.");
        }
        
        preference.setPreferenceWeight(weight);
        preference.setNotes(notes);
        
        RoomPreference saved = preferenceRepository.save(preference);
        return convertToDTO(saved);
    }
    
    public void deletePreference(Long preferenceId) {
        RoomPreference preference = preferenceRepository.findById(preferenceId)
            .orElseThrow(() -> new RuntimeException("Preference not found"));
        
        if (preference.getAssignment().getSchedule().getStatus() != CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE) {
            throw new RuntimeException("Cannot delete preferences. Schedule is not in requirements phase.");
        }
        
        preferenceRepository.delete(preference);
    }
    
    private RoomPreferenceDTO convertToDTO(RoomPreference preference) {
        RoomPreferenceDTO dto = new RoomPreferenceDTO();
        dto.setId(preference.getId());
        dto.setAssignmentId(preference.getAssignment().getId());
        dto.setCourseName(preference.getAssignment().getCourse().getName());
        dto.setCourseCode(preference.getAssignment().getCourse().getCode());
        dto.setRoomId(preference.getRoom().getId());
        dto.setRoomName(preference.getRoom().getName());
        dto.setRoomBuilding(preference.getRoom().getBuilding());
        dto.setRoomCapacity(preference.getRoom().getCapacity());
        dto.setRoomType(preference.getRoom().getType().toString());
        dto.setPreferenceWeight(preference.getPreferenceWeight());
        dto.setNotes(preference.getNotes());
        dto.setActive(preference.isActive());
        return dto;
    }
}