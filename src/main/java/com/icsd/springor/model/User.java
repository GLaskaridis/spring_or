/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.icsd.springor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.icsd.springor.DTO.TeacherType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;


@Entity
@Table(name = "users")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeacherType teacherType;

    @Column(nullable = false)
    private String teacherRank;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private boolean active = false;

    // Change this from String to UserRole enum
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.TEACHER;

    @Column
    private LocalDateTime passwordResetTokenExpiryTime;

    @Column
    private String passwordResetToken;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public TeacherType getTeacherType() {
        return teacherType;
    }

    public void setTeacherType(TeacherType teacherType) {
        this.teacherType = teacherType;
    }

    public String getTeacherRank() {
        return teacherRank;
    }

    public void setTeacherRank(String teacherRank) {
        this.teacherRank = teacherRank;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // Updated role methods
    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    // Helper method to get role name for Spring Security
    public String getRoleName() {
        return role != null ? role.getRoleName() : "ROLE_TEACHER";
    }

    // Convenience methods
    public boolean isTeacher() {
        return role == UserRole.TEACHER;
    }

    public boolean isProgramManager() {
        return role == UserRole.PROGRAM_MANAGER;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public LocalDateTime getPasswordResetTokenExpiryTime() {
        return passwordResetTokenExpiryTime;
    }

    public void setPasswordResetTokenExpiryTime(LocalDateTime passwordResetTokenExpiryTime) {
        this.passwordResetTokenExpiryTime = passwordResetTokenExpiryTime;
    }

    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }
    
    
}