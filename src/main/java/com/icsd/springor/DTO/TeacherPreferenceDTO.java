/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.DTO;

import com.icsd.springor.model.RoomType;
import com.icsd.springor.model.TeacherPreference;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherPreferenceDTO {
    private Long id;
    private Long assignmentId;
    private String courseName;
    private String courseCode;
    private TeacherPreference.PreferenceType type;
    
    
    private DayOfWeek preferredDay;
    private LocalTime preferredStartTime;
    private LocalTime preferredEndTime;
    private Integer preferredSlot; // 0-3 for the 4 daily slots
    private Integer preferenceWeight; // 1-10
    
    private Long preferredRoomId;
    private String preferredRoomName;
    private RoomType preferredRoomType;
    private Integer minCapacity;
    private Integer maxCapacity;
    
    private Integer priorityWeight;
    private String notes;
    private boolean active;
}