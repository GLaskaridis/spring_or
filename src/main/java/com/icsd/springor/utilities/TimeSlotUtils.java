/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.utilities;


import java.time.LocalTime;
import java.util.Map;

public class TimeSlotUtils {
    
    public static LocalTime getSlotStartTime(int slot) {
        switch (slot) {
            case 0: return LocalTime.of(9, 0);   // 09:00
            case 1: return LocalTime.of(12, 0);  // 12:00
            case 2: return LocalTime.of(15, 0);  // 15:00
            case 3: return LocalTime.of(18, 0);  // 18:00
            default: throw new IllegalArgumentException("Invalid slot: " + slot);
        }
    }
    
    public static LocalTime getSlotEndTime(int slot) {
        switch (slot) {
            case 0: return LocalTime.of(12, 0);  // 12:00
            case 1: return LocalTime.of(15, 0);  // 15:00
            case 2: return LocalTime.of(18, 0);  // 18:00
            case 3: return LocalTime.of(21, 0);  // 21:00
            default: throw new IllegalArgumentException("Invalid slot: " + slot);
        }
    }
    
    
    public static String getSlotLabel(int slot) {
        switch (slot) {
            case 0: return "Πρωινό (09:00-12:00)";
            case 1: return "Μεσημεριανό (12:00-15:00)";
            case 2: return "Απογευματινό (15:00-18:00)";
            case 3: return "Βραδινό (18:00-21:00)";
            default: throw new IllegalArgumentException("Invalid slot: " + slot);
        }
    }
    
   
    public static String getSlotShortLabel(int slot) {
        switch (slot) {
            case 0: return "09:00-12:00";
            case 1: return "12:00-15:00";
            case 2: return "15:00-18:00";
            case 3: return "18:00-21:00";
            default: throw new IllegalArgumentException("Invalid slot: " + slot);
        }
    }
    
    
    public static boolean isValidSlot(int slot) {
        return slot >= 0 && slot <= 3;
    }
  
    public static Map<String, Object> getSlotInfo(int slot) {
        if (!isValidSlot(slot)) {
            throw new IllegalArgumentException("Invalid slot: " + slot);
        }
        
        return Map.of(
            "slot", slot,
            "startTime", getSlotStartTime(slot),
            "endTime", getSlotEndTime(slot),
            "label", getSlotLabel(slot),
            "shortLabel", getSlotShortLabel(slot)
        );
    }
    
    public static Map<Integer, Map<String, Object>> getAllSlotsInfo() {
        return Map.of(
            0, getSlotInfo(0),
            1, getSlotInfo(1),
            2, getSlotInfo(2),
            3, getSlotInfo(3)
        );
    }
}