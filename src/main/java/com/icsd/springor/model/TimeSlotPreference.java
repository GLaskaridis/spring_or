/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.DayOfWeek;

@Entity
@Table(name = "time_slot_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotPreference {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_day", nullable = false)
    private DayOfWeek preferredDay;
    
    @Column(name = "preferred_slot", nullable = false)
    private Integer preferredSlot; // 0-3
    
    @Column(name = "preference_weight", nullable = false)
    private Integer preferenceWeight; // 1-10 (10 = highest preference)
    
    @Column(name = "notes")
    private String notes;
    
    @Column(nullable = false)
    private boolean active = true;
}
