/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.DTO;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.DayOfWeek;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotPreferenceDTO {
    private Long id;
    private Long assignmentId;
    private String courseName;
    private String courseCode;
    private DayOfWeek preferredDay;
    private Integer preferredSlot;
    private Integer preferenceWeight;
    private String notes;
    private boolean active;
    
    // Helper methods
    public String getSlotLabel() {
        return getSlotLabel(preferredSlot);
    }
    
    public static String getSlotLabel(Integer slot) {
        if (slot == null) return "Δεν έχει οριστεί";
        switch (slot) {
            case 0: return "09:00-12:00";
            case 1: return "12:00-15:00";
            case 2: return "15:00-18:00";
            case 3: return "18:00-21:00";
            default: return "Άγνωστο";
        }
    }
}