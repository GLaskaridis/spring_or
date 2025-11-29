/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.DTO;

import com.icsd.springor.model.Assignment;
import com.icsd.springor.model.Course;
import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.model.User;
import lombok.Data;

@Data
public class AssignmentDTO {
    private Long id;
    private Long courseId;
    private String courseName;
    private String courseCode;
    private Integer courseYear;
    private Integer courseSemester;
    private Long teacherId;
    private String teacherName;
    private String teacherRank;
    private Course.TeachingHours.CourseComponent courseComponent;
    private Long scheduleId;
    private String scheduleName;
    private boolean active;
    private String scheduleStatus;
    
    public AssignmentDTO() {}
    
    public AssignmentDTO(Long id, Long courseId, String courseName, String courseCode,
                    Integer courseYear, Integer courseSemester,
                    Long teacherId, String teacherName, String teacherRank,
                    Course.TeachingHours.CourseComponent courseComponent,
                    Long scheduleId, String scheduleName, String scheduleStatus, boolean active){
        this.id = id;
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.courseYear = courseYear;
        this.courseSemester = courseSemester;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.teacherRank = teacherRank;
        this.courseComponent = courseComponent;
        this.scheduleId = scheduleId;
        this.scheduleName = scheduleName;
        this.active = active;
        this.scheduleStatus = scheduleStatus;
    }
    
    /**
     *μετατρέπει το DTO σε Assignment entity
     */
    public Assignment toEntity() {
        Assignment assignment = new Assignment();
        assignment.setId(this.id);
        assignment.setActive(this.active);
        assignment.setCourseComponent(this.courseComponent);
        
        // Δημιουργία Course entity με τα βασικά στοιχεία
        Course course = new Course();
        course.setId(this.courseId);
        course.setName(this.courseName);
        course.setCode(this.courseCode);
        course.setYear(this.courseYear);
        course.setSemester(this.courseSemester);
        assignment.setCourse(course);
        
        // Δημιουργία User entity για τον teacher
        User teacher = new User();
        teacher.setId(this.teacherId);
        teacher.setFullName(this.teacherName);
        assignment.setTeacher(teacher);
        
        // Δημιουργία CourseSchedule entity
        if (this.scheduleId != null) {
            CourseSchedule schedule = new CourseSchedule();
            schedule.setId(this.scheduleId);
            schedule.setName(this.scheduleName);
            assignment.setSchedule(schedule);
        }
        
        return assignment;
    }
    
    /**
     *μέθοδος για δημιουργία DTO από Assignment entity
     */
    public static AssignmentDTO fromEntity(Assignment assignment) {
        AssignmentDTO dto = new AssignmentDTO();
        dto.setId(assignment.getId());
        dto.setActive(assignment.isActive());
        dto.setCourseComponent(assignment.getCourseComponent());
        
        if (assignment.getCourse() != null) {
            dto.setCourseId(assignment.getCourse().getId());
            dto.setCourseName(assignment.getCourse().getName());
            dto.setCourseCode(assignment.getCourse().getCode());
            dto.setCourseYear(assignment.getCourse().getYear());
            dto.setCourseSemester(assignment.getCourse().getSemester());
        }
        
        if (assignment.getTeacher() != null) {
            dto.setTeacherId(assignment.getTeacher().getId());
            dto.setTeacherName(assignment.getTeacher().getFullName());
            // Assuming teacher rank is stored in User entity or can be derived
        }
        
        if (assignment.getSchedule() != null) {
            dto.setScheduleId(assignment.getSchedule().getId());
            dto.setScheduleName(assignment.getSchedule().getName());
            dto.setScheduleStatus(assignment.getSchedule().getStatus().name());
        }
        
        return dto;
    }
    
    //getter για courseComponent ως String (για compatibility)
    public String getCourseComponentAsString() {
        return courseComponent != null ? courseComponent.name() : null;
    }

}