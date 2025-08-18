/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.service;

import com.icsd.springor.DTO.TeacherPreferenceDTO;
import com.icsd.springor.model.Assignment;
import com.icsd.springor.model.Room;
import com.icsd.springor.model.TeacherPreference;
import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.repository.AssignmentRepository;
import com.icsd.springor.repository.RoomRepository;
import com.icsd.springor.repository.TeacherPreferenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TeacherPreferenceService {
    
    @Autowired
    private TeacherPreferenceRepository preferenceRepository;
    
    @Autowired
    private AssignmentRepository assignmentRepository;
    
    @Autowired
    private RoomRepository roomRepository;
    
    public TeacherPreferenceDTO createPreference(TeacherPreferenceDTO preferenceDTO) {
        Assignment assignment = assignmentRepository.findById(preferenceDTO.getAssignmentId())
            .orElseThrow(() -> new RuntimeException("Assignment not found"));
        
        if (assignment.getSchedule().getStatus() != CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE) {
            throw new RuntimeException("Cannot modify preferences. Schedule is not in requirements phase.");
        }
        
        TeacherPreference preference = new TeacherPreference();
        preference.setAssignment(assignment);
        preference.setType(preferenceDTO.getType());
        preference.setPreferredDay(preferenceDTO.getPreferredDay());
        preference.setPreferredSlot(preferenceDTO.getPreferredSlot());
        preference.setPreferenceWeight(preferenceDTO.getPreferenceWeight());
        preference.setPreferredRoomType(preferenceDTO.getPreferredRoomType());
        preference.setMinCapacity(preferenceDTO.getMinCapacity());
        preference.setMaxCapacity(preferenceDTO.getMaxCapacity());
        preference.setPriorityWeight(preferenceDTO.getPriorityWeight() != null ? preferenceDTO.getPriorityWeight() : 5);
        preference.setNotes(preferenceDTO.getNotes());
        
        if (preferenceDTO.getPreferredRoomId() != null) {
            Room room = roomRepository.findById(preferenceDTO.getPreferredRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found"));
            preference.setPreferredRoom(room);
        }
        
        TeacherPreference savedPreference = preferenceRepository.save(preference);
        return convertToDTO(savedPreference);
    }
    
    public List<TeacherPreferenceDTO> getPreferencesByTeacher(Long teacherId) {
        List<TeacherPreference> preferences = preferenceRepository.findByTeacherId(teacherId);
        return preferences.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<TeacherPreferenceDTO> getPreferencesByTeacherAndSchedule(Long teacherId, Long scheduleId) {
        List<TeacherPreference> preferences = preferenceRepository.findByTeacherAndSchedule(teacherId, scheduleId);
        return preferences.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<TeacherPreferenceDTO> getPreferencesByAssignment(Long assignmentId) {
        List<TeacherPreference> preferences = preferenceRepository.findByAssignmentId(assignmentId);
        return preferences.stream()
                .filter(TeacherPreference::isActive)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<TeacherPreferenceDTO> getPreferencesBySchedule(Long scheduleId) {
        List<TeacherPreference> preferences = preferenceRepository.findByScheduleId(scheduleId);
        return preferences.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
//    public TeacherPreferenceDTO updatePreference(Long preferenceId, TeacherPreferenceDTO preferenceDTO) {
//        TeacherPreference preference = preferenceRepository.findById(preferenceId)
//            .orElseThrow(() -> new RuntimeException("Preference not found"));
//        
//        // Check if schedule allows modifications
//        if (preference.getAssignment().getSchedule().getStatus() != CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE) {
//            throw new RuntimeException("Cannot modify preferences. Schedule is not in requirements phase.");
//        }
//        
//        preference.setType(preferenceDTO.getType());
//        preference.setPreferredDay(preferenceDTO.getPreferredDay());
//        preference.setPreferredSlot(preferenceDTO.getPreferredSlot());
//        preference.setPreferenceWeight(preferenceDTO.getPreferenceWeight());
//        preference.setPreferredRoomType(preferenceDTO.getPreferredRoomType());
//        preference.setMinCapacity(preferenceDTO.getMinCapacity());
//        preference.setMaxCapacity(preferenceDTO.getMaxCapacity());
//        preference.setPriorityWeight(preferenceDTO.getPriorityWeight());
//        preference.setNotes(preferenceDTO.getNotes());
//        
//        if (preferenceDTO.getPreferredRoomId() != null) {
//            Room room = roomRepository.findById(preferenceDTO.getPreferredRoomId())
//                .orElseThrow(() -> new RuntimeException("Room not found"));
//            preference.setPreferredRoom(room);
//        } else {
//            preference.setPreferredRoom(null);
//        }
//        
//        TeacherPreference savedPreference = preferenceRepository.save(preference);
//        return convertToDTO(savedPreference);
//    }
    
    public void deletePreference(Long preferenceId) {
        TeacherPreference preference = preferenceRepository.findById(preferenceId)
            .orElseThrow(() -> new RuntimeException("Preference not found"));
        
        // Check if schedule allows modifications
        if (preference.getAssignment().getSchedule().getStatus() != CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE) {
            throw new RuntimeException("Cannot delete preferences. Schedule is not in requirements phase.");
        }
        
        preferenceRepository.delete(preference);
    }
    
    public boolean areAllPreferencesProvided(Long scheduleId) {
        // Get all assignments for this schedule
        List<Assignment> assignments = assignmentRepository.findActiveAssignmentsBySchedule(scheduleId);
        
        // Check if each assignment has at least one preference
        for (Assignment assignment : assignments) {
            List<TeacherPreference> preferences = preferenceRepository.findByAssignmentAndActiveTrue(assignment);
            if (preferences.isEmpty()) {
                return false;
            }
        }
        
        return true;
    }
    

    public TeacherPreferenceDTO updatePreference(Long preferenceId, TeacherPreferenceDTO preferenceDTO) {
        TeacherPreference preference = preferenceRepository.findById(preferenceId)
            .orElseThrow(() -> new RuntimeException("Preference not found with id: " + preferenceId));

        // Check if schedule allows modifications
        if (preference.getAssignment().getSchedule().getStatus() != CourseSchedule.ScheduleStatus.REQUIREMENTS_PHASE) {
            throw new RuntimeException("Cannot modify preferences. Schedule is not in requirements phase.");
        }

        // Update fields
        if (preferenceDTO.getType() != null) {
            preference.setType(preferenceDTO.getType());
        }
        if (preferenceDTO.getPreferredDay() != null) {
            preference.setPreferredDay(preferenceDTO.getPreferredDay());
        }
        if (preferenceDTO.getPreferredSlot() != null) {
            preference.setPreferredSlot(preferenceDTO.getPreferredSlot());
        }
        if (preferenceDTO.getPreferenceWeight() != null) {
            preference.setPreferenceWeight(preferenceDTO.getPreferenceWeight());
        }
        if (preferenceDTO.getPreferredRoomType() != null) {
            preference.setPreferredRoomType(preferenceDTO.getPreferredRoomType());
        }
        if (preferenceDTO.getMinCapacity() != null) {
            preference.setMinCapacity(preferenceDTO.getMinCapacity());
        }
        if (preferenceDTO.getMaxCapacity() != null) {
            preference.setMaxCapacity(preferenceDTO.getMaxCapacity());
        }
        if (preferenceDTO.getPriorityWeight() != null) {
            preference.setPriorityWeight(preferenceDTO.getPriorityWeight());
        }
        if (preferenceDTO.getNotes() != null) {
            preference.setNotes(preferenceDTO.getNotes());
        }

        // Update preferred room if specified
        if (preferenceDTO.getPreferredRoomId() != null) {
            Room room = roomRepository.findById(preferenceDTO.getPreferredRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found"));
            preference.setPreferredRoom(room);
        } else if (preferenceDTO.getPreferredRoomId() == null && preference.getPreferredRoom() != null) {
            // Clear room preference if explicitly set to null
            preference.setPreferredRoom(null);
        }

        TeacherPreference savedPreference = preferenceRepository.save(preference);
        return convertToDTO(savedPreference);
    }
    
    public TeacherPreferenceDTO getPreferenceById(Long preferenceId) {
        TeacherPreference preference = preferenceRepository.findById(preferenceId)
            .orElseThrow(() -> new RuntimeException("Preference not found with id: " + preferenceId));

        if (!preference.isActive()) {
            throw new RuntimeException("Preference is not active");
        }

        return convertToDTO(preference);
    }
    
    
    
   private TeacherPreferenceDTO convertToDTO(TeacherPreference preference) {
        TeacherPreferenceDTO dto = new TeacherPreferenceDTO();
        dto.setId(preference.getId());
        dto.setAssignmentId(preference.getAssignment().getId());
        dto.setType(preference.getType());
        dto.setPreferredDay(preference.getPreferredDay());
        dto.setPreferredSlot(preference.getPreferredSlot());
        dto.setPreferenceWeight(preference.getPreferenceWeight());
        dto.setPreferredRoomType(preference.getPreferredRoomType());
        dto.setMinCapacity(preference.getMinCapacity());
        dto.setMaxCapacity(preference.getMaxCapacity());
        dto.setPriorityWeight(preference.getPriorityWeight());
        dto.setNotes(preference.getNotes());

        // Room information
        if (preference.getPreferredRoom() != null) {
            dto.setPreferredRoomId(preference.getPreferredRoom().getId());
            dto.setPreferredRoomName(preference.getPreferredRoom().getName());
        }

        // Assignment information for display
        if (preference.getAssignment() != null && preference.getAssignment().getCourse() != null) {
            dto.setCourseName(preference.getAssignment().getCourse().getName());
            dto.setCourseCode(preference.getAssignment().getCourse().getCode());

            // 🔧 ΔΙΟΡΘΩΣΗ: Σωστό courseComponent mapping
            if (preference.getAssignment().getCourseComponent() != null) {
                dto.setCourseComponent(preference.getAssignment().getCourseComponent().name());
            }
        }

        return dto;
    }
}