/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.DTO;

import com.icsd.springor.model.Course;
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
    
    // Constructors
    public AssignmentDTO() {}
    
    public AssignmentDTO(Long id, Long courseId, String courseName, String courseCode,
                        Integer courseYear, Integer courseSemester,
                        Long teacherId, String teacherName, String teacherRank,
                        Course.TeachingHours.CourseComponent courseComponent,
                        Long scheduleId, String scheduleName, boolean active) {
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
    }
}