/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.service;

import com.icsd.springor.model.User;
import com.icsd.springor.model.UserRole;
import com.icsd.springor.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;
    
    
    @Autowired
    public UserService(@Lazy PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        
    }

    public User registerUser(User user) {
        if (isValidUser(user)) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            return userRepository.save(user);
        }
        throw new IllegalArgumentException("Invalid user data");
    }
    
   
    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    public User updateUser(Long id, User updatedUser) {
        User user = getUserById(id);
        // Update fields except password
        user.setFullName(updatedUser.getFullName());
        user.setTeacherType(updatedUser.getTeacherType());
        user.setTeacherRank(updatedUser.getTeacherRank());
        user.setEmail(updatedUser.getEmail());
        return userRepository.save(user);
    }
    
  
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        if (passwordEncoder.matches(oldPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        } else {
            throw new IllegalArgumentException("Old password is incorrect");
        }
    }

    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiryTime(LocalDateTime.now().plusHours(2));
        userRepository.save(user);
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        if (user.getPasswordResetTokenExpiryTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token has expired");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiryTime(null);
        userRepository.save(user);
    }

    public List<User> searchUsers() {
        return userRepository.findAll(); // Placeholder
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public void deactivateUser(Long id) {
        User user = getUserById(id);
        user.setActive(false);
        userRepository.save(user);
    }

    private boolean isValidUser(User user) {
        return true; 
    }
    
   
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User save(User user) {
        return userRepository.save(user);
    }
    
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
    }

    private Long getCurrentTeacherId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String username = authentication.getName();

        try {
            //βρίσκουμε τον χρήστη από το username
            User user = findAllTeachers().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

            return user.getId();

        } catch (Exception e) {
            throw new RuntimeException("Error getting current teacher ID: " + e.getMessage());
        }
    }
    
    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String username = authentication.getName();
        return findByUsername(username);
    }

    public Long getCurrentUserId(Authentication authentication) {
        return getCurrentUser(authentication).getId();
    }
    
    public boolean isCurrentUserAdmin(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return "ROLE_ADMIN".equals(user.getRole());
    }
    
    public boolean isCurrentUserTeacher(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return "ROLE_USER".equals(user.getRole()) || "ROLE_TEACHER".equals(user.getRole());
    }
    

    public boolean isCurrentUserProgramManager(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return user.isProgramManager();
    }


    public boolean hasRole(Authentication authentication, UserRole role) {
        User user = getCurrentUser(authentication);
        return user.getRole() == role;
    }

    public boolean canManageSchedules(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return user.isProgramManager() || user.isAdmin();
    }

    public boolean canManageUsers(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return user.isAdmin();
    }
    
     
    public List<User> findAllTeachers() {
        return userRepository.findAll().stream()
            .filter(user -> user.getRole() == UserRole.TEACHER)
            .collect(Collectors.toList());
    }
    
    public long countAllUsers() {
        return userRepository.count();
    }
    
    public List<User> findAllActiveTeachers() {
        return userRepository.findAll().stream()
            .filter(user -> user.isActive() && 
                    (user.getTeacherType() != null || user.getTeacherRank() != null))
            .collect(Collectors.toList());
    }
    
   
    

}