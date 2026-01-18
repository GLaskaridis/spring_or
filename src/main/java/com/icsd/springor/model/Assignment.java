package com.icsd.springor.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "assignments", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "course_component", "schedule_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Assignment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "course_component", nullable = false)
    private Course.TeachingHours.CourseComponent courseComponent; // THEORY or LABORATORY
    
    @Column(nullable = false)
    private boolean active = true;
    
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = true)
    private CourseSchedule schedule;
    
    @Column(name = "is_general_assignment", nullable = false)
    private boolean isGeneralAssignment = false;
}