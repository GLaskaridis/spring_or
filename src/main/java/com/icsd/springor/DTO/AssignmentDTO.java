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


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentDTO {
    private Long id;
    private Long courseId;
    private String courseName;
    private String courseCode;
    private Long teacherId;
    private String teacherName;
    private String teacherUsername;
    private Course.TeachingHours.CourseComponent courseComponent;
    private boolean active;
    private Long scheduleId;
}