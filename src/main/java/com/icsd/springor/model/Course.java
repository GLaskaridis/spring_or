package com.icsd.springor.model;


import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseType type;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer semester;

    @ElementCollection
    @CollectionTable(name = "course_teaching_hours", joinColumns = @JoinColumn(name = "course_id"))
    private Set<TeachingHours> teachingHours;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private boolean active = true;


    public enum CourseType {
        BASIC, ELECTIVE
    }

    public Course(String name, String code) {
        this.name = name;
        this.code = code;
    }
    
    

    @Embeddable
    @Getter
    @Setter
    public static class TeachingHours {
        @Enumerated(EnumType.STRING)
        private CourseComponent component;
        private Integer hours;

        public enum CourseComponent {
            THEORY, LABORATORY
        }
    }
    
    @Embeddable
    public static class TimePreference {
        @Column
        @Enumerated(EnumType.STRING)
        private DayOfWeek preferredDay;
        
        @Column
        private int preferredSlot;
        
        @Column
        private int weight;
        
        public TimePreference() {} // JPA requires empty constructor
        
        public TimePreference(DayOfWeek preferredDay, int preferredSlot, int weight) {
            this.preferredDay = preferredDay;
            this.preferredSlot = preferredSlot;
            this.weight = weight;
        }

        public DayOfWeek getPreferredDay() {
            return preferredDay;
        }

        public int getPreferredSlot() {
            return preferredSlot;
        }

        public int getWeight() {
            return weight;
        }
        
        
    }
    
    public boolean isActive() {
        return this.active;
    }

    public boolean isRequired() {
        return this.type == CourseType.BASIC;
    }

    public int getTotalHours() {
        if (this.teachingHours == null || this.teachingHours.isEmpty()) {
            return 0;
        }
        return this.teachingHours.stream()
            .mapToInt(th -> th.getHours() != null ? th.getHours() : 0)
            .sum();
    }
    
    public int getTheoryHours() {
        if (this.teachingHours == null) return 0;
        return this.teachingHours.stream()
            .filter(th -> th.getComponent() == TeachingHours.CourseComponent.THEORY)
            .mapToInt(th -> th.getHours() != null ? th.getHours() : 0)
            .findFirst()
            .orElse(0);
    }
    
    public int getLabHours() {
        if (this.teachingHours == null) return 0;
        return this.teachingHours.stream()
            .filter(th -> th.getComponent() == TeachingHours.CourseComponent.LABORATORY)
            .mapToInt(th -> th.getHours() != null ? th.getHours() : 0)
            .findFirst()
            .orElse(0);
    }

    public boolean hasTheory() {
        return getTheoryHours() > 0;
    }

    public boolean hasLab() {
        return getLabHours() > 0;
    }
    
    private TimePreference timePreference;
    
   
}