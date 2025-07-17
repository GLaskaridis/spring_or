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
import java.time.LocalTime;

@Entity
@Table(name = "teacher_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherPreference {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "preference_type", nullable = false)
    private PreferenceType type; //eidos (uparxei enum apaitisi i protimisi)
    
    //preference sxetika me tin imera
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_day")
    private DayOfWeek preferredDay;
    
    
    //preference sxetika me tin wra
    @Column(name = "preferred_start_time")
    private LocalTime preferredStartTime;
    
    @Column(name = "preferred_end_time")
    private LocalTime preferredEndTime;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_room_id")
    private Room preferredRoom;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_room_type")
    private RoomType preferredRoomType;
    
    @Column(name = "min_capacity")
    private Integer minCapacity;
    
    @Column(name = "max_capacity")
    private Integer maxCapacity;
    
    //priority/Weight (1-10, where 10 is highest priority)
    @Column(name = "priority_weight", nullable = false)
    private Integer priorityWeight = 5;
    
    @Column(name = "notes")
    private String notes;
    
    @Column(nullable = false)
    private boolean active = true;
    
    public enum PreferenceType {
        REQUIREMENT,    
        PREFERENCE      
    }
}