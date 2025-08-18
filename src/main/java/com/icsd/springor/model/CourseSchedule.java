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
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "course_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseSchedule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String semester; // e.g., "2024-2025 Fall"
    
    @Column(nullable = false)
    private Integer year;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleStatus status = ScheduleStatus.ASSIGNMENT_PHASE;
    
     @Column(nullable = false)
    private boolean active = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Scheduling time constraints
    @Column(name = "start_time")
    private String startTime = "09:00"; // e.g., "09:00"
    
    @Column(name = "end_time")
    private String endTime = "21:00"; // e.g., "21:00"
    
    @Column(name = "max_hours_per_day")
    private Integer maxHoursPerDay = 9;
    
    @Column(name = "max_distance_km")
    private Double maxDistanceKm = 1.0;
    
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Assignment> assignments;
    
    public enum ScheduleStatus {
        ASSIGNMENT_PHASE,      // ΑΝΑΘΕΣΗ_ΜΑΘΗΜΑΤΩΝ (διαχειριστής εισάγει έτοιμες αναθέσεις)
        REQUIREMENTS_PHASE,    // ΠΑΡΟΧΗ_ΠΡΟΤΙΜΗΣΕΩΝ (διδάσκοντες δίνουν προτιμήσεις)
        EXECUTION_PHASE,       // ΕΚΤΕΛΕΣΗ
        SOLUTION_FOUND,        // ΕΥΡΕΣΗ_ΛΥΣΗΣ
        NO_SOLUTION_FOUND,     // ΜΗ_ΕΥΡΕΣΗ_ΛΥΣΗΣ
        SOLUTION_APPROVED,     // ΕΓΚΡΙΣΗ_ΛΥΣΗΣ
        TERMINATED            // ΛΗΞΗ
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}