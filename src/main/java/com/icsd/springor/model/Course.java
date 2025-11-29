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
import jakarta.persistence.Transient;
import java.time.DayOfWeek;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

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

    @Transient
    private TeachingHours.CourseComponent activeComponent;

   @Transient
    private List<TimePreference> timePreferences = new ArrayList<>();

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
        
        public TimePreference() {} 
        
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

   
    public TeachingHours.CourseComponent getActiveComponent() {
        if (activeComponent != null) {
            return activeComponent;
        }
        
        if (hasTheory()) {
            return TeachingHours.CourseComponent.THEORY;
        } else if (hasLab()) {
            return TeachingHours.CourseComponent.LABORATORY;
        }
        
        return TeachingHours.CourseComponent.THEORY;
    }

    
    public void setActiveComponent(TeachingHours.CourseComponent activeComponent) {
        this.activeComponent = activeComponent;
    }

    public int getActiveComponentHours() {
        TeachingHours.CourseComponent active = getActiveComponent();
        if (active == TeachingHours.CourseComponent.THEORY) {
            return getTheoryHours();
        } else if (active == TeachingHours.CourseComponent.LABORATORY) {
            return getLabHours();
        }
        return 0;
    }

    public List<TimePreference> getTimePreferences() {
        return timePreferences;
    }
    

    public void setTimePreferences(List<TimePreference> timePreferences) {
        this.timePreferences = timePreferences;
    }
    
    public void addTimePreference(TimePreference preference) {
        if (this.timePreferences == null) {
            this.timePreferences = new ArrayList<>();
        }
        this.timePreferences.add(preference);
    }

    public boolean hasTimePreferences() {
        return timePreferences != null && !timePreferences.isEmpty();
    }


    @Override
    public String toString() {
        return String.format("Course{id=%d, code='%s', name='%s', type=%s, year=%d, semester=%d, active=%s}", 
            id, code, name, type, year, semester, active);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return code != null ? code.equals(course.code) : course.code == null;
    }

    @Override
    public int hashCode() {
        return code != null ? code.hashCode() : 0;
    }
}