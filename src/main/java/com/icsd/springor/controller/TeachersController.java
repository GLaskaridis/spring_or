/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.controller;

import com.icsd.springor.DTO.UserCreateDTO;
import com.icsd.springor.DTO.UserDTO;
import com.icsd.springor.DTO.UserUpdateDTO;
import com.icsd.springor.model.User;
import com.icsd.springor.service.UserService;
import com.icsd.springor.utilities.JwtUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/teachers")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class TeachersController {
    
     @Autowired
    private UserService userService;
    
     
    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllTeachers() {
        List<User> teachers = userService.findAllTeachers();
        List<UserDTO> teacherDTOs = teachers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(teacherDTOs);
    }
    
     @GetMapping("/add")
    public String showAddTeacherForm(Model model) {
        model.addAttribute("teacher", new UserDTO());
        return "add-teacher";
    }
    
     @PostMapping("/add")
    public String addTeacher(@ModelAttribute UserDTO userDTO,
                            RedirectAttributes redirectAttributes) {
        try {
//            if (!userDTO.getPassword().equals(confirmPassword)) {
//                redirectAttributes.addFlashAttribute("error", "Οι κωδικοί δεν ταιριάζουν");
//                return "redirect:/teachers/add";
//            }
            
            //metatropi tou DTO se xristi kai apothikeusi
            User user = convertToEntity(userDTO);
            user.setRole("TEACHER"); //rolos teacher
            userService.save(user);
            
            redirectAttributes.addFlashAttribute("message", "Ο διδάσκων προστέθηκε επιτυχώς");
            return "redirect:/teachers/list";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Σφάλμα κατά την προσθήκη διδάσκοντα: " + e.getMessage());
            return "redirect:/teachers/add";
        }
    }
    
    @GetMapping("/list")
    public String listTeachers(Model model) {
        List<User> teachers = userService.findAllTeachers();
        List<UserDTO> teacherDTOs = teachers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        model.addAttribute("teachers", teacherDTOs);
        return "teacher-list";
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getTeacherById(@PathVariable Long id) {
        User teacher = userService.getUserById(id);
        return ResponseEntity.ok(convertToDTO(teacher));
    }

    @PostMapping
    public ResponseEntity<UserDTO> createTeacher(@RequestBody UserCreateDTO userCreateDTO) {
        User newUser = new User();
        newUser.setFullName(userCreateDTO.getFullName());
        newUser.setUsername(userCreateDTO.getUsername());
        newUser.setEmail(userCreateDTO.getEmail());
        newUser.setTeacherType(userCreateDTO.getTeacherType());
        newUser.setTeacherRank(userCreateDTO.getTeacherRank());
        newUser.setPassword(userCreateDTO.getPassword()); // Will be encoded in service
        newUser.setActive(userCreateDTO.isActive());
        newUser.setRole("TEACHER");
        
        User savedUser = userService.registerUser(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(savedUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateTeacher(@PathVariable Long id, @RequestBody UserUpdateDTO userUpdateDTO) {
        User existingUser = userService.getUserById(id);
        
        existingUser.setFullName(userUpdateDTO.getFullName());
        existingUser.setUsername(userUpdateDTO.getUsername());
        existingUser.setEmail(userUpdateDTO.getEmail());
        existingUser.setTeacherType(userUpdateDTO.getTeacherType());
        existingUser.setTeacherRank(userUpdateDTO.getTeacherRank());
        existingUser.setActive(userUpdateDTO.isActive());
        
        User updatedUser = userService.updateUser(id, existingUser);
        return ResponseEntity.ok(convertToDTO(updatedUser));
    }
    
    @PostMapping("/edit/{id}")
    public String updateTeacher(@PathVariable Long id, 
                               @ModelAttribute UserDTO userDTO,
                               @RequestParam(value = "newPassword", required = false) String newPassword,
                               @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                               RedirectAttributes redirectAttributes) {
        try {
            User existingUser = userService.getUserById(id);
            if (existingUser == null) {
                redirectAttributes.addFlashAttribute("error", "Ο διδάσκων δεν βρέθηκε");
                return "redirect:/teachers/list";
            }
            existingUser.setFullName(userDTO.getFullName());
            existingUser.setUsername(userDTO.getUsername());
            existingUser.setEmail(userDTO.getEmail());
            existingUser.setTeacherType(userDTO.getTeacherType());
            existingUser.setTeacherRank(userDTO.getTeacherRank());
            existingUser.setActive(userDTO.isActive());
            
            if (newPassword != null && !newPassword.isEmpty()) {
                if (!newPassword.equals(confirmPassword)) {
                    redirectAttributes.addFlashAttribute("error", "Οι κωδικοί δεν ταιριάζουν");
                    return "redirect:/teachers/edit/" + id;
                }
                existingUser.setPassword(newPassword); // You should hash this password before saving
            }
            
            userService.save(existingUser);
            redirectAttributes.addFlashAttribute("message", "Ο διδάσκων ενημερώθηκε επιτυχώς");
            return "redirect:/teachers/list";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Σφάλμα κατά την ενημέρωση διδάσκοντα: " + e.getMessage());
            return "redirect:/teachers/edit/" + id;
        }
    }
    
    @GetMapping("/edit/{id}")
    public String showEditTeacherForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Ο διδάσκων δεν βρέθηκε");
                return "redirect:/teachers/list";
            }
            
            UserDTO userDTO = convertToDTO(user);
            model.addAttribute("teacher", userDTO);
            return "edit-teacher";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Σφάλμα κατά την ανάκτηση διδάσκοντα: " + e.getMessage());
            return "redirect:/teachers/list";
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteTeacher(@PathVariable Long id) {
        try {
            // Check if teacher exists
            if (!userService.existsById(id)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found with id: " + id);
            }
            
            userService.deleteUser(id);
            return ResponseEntity.ok().body(
                Map.of(
                    "message", "Teacher deleted successfully",
                    "id", id
                )
            );
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete teacher", e);
        }
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setTeacherType(user.getTeacherType());
        dto.setTeacherRank(user.getTeacherRank());
        dto.setActive(user.isActive());
        dto.setRole(user.getRole());
        return dto;
    }
    
    @GetMapping("/delete/{id}")
    public String deleteTeacher(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("message", "Ο διδάσκων διαγράφηκε επιτυχώς");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Σφάλμα κατά τη διαγραφή διδάσκοντα: " + e.getMessage());
        }
        return "redirect:/teachers/list";
    }


    // Helper method to convert UserDTO to User entity
    private User convertToEntity(UserDTO dto) {
        User user = new User();
        if (dto.getId() != null) {
            user.setId(dto.getId());
        }
        user.setFullName(dto.getFullName());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setTeacherType(dto.getTeacherType());
        user.setTeacherRank(dto.getTeacherRank());
        user.setActive(dto.isActive());
        return user;
    }
}
