/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.DTO;

import com.icsd.springor.model.Course;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CoursePreferenceDTO {
    private Long id;
    private Long teacherId;
    private String teacherName;
    private String teacherUsername;
    private Long courseId;
    private String courseName;
    private String courseCode;
    private Course.TeachingHours.CourseComponent courseComponent;
    private Long scheduleId;
    private Integer preferenceLevel;
    private String notes;
    private LocalDateTime createdAt;
    private boolean active;
}