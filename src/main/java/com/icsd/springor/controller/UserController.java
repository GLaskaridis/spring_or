/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.icsd.springor.controller;

import com.icsd.springor.DTO.LoginRequest;
import org.springframework.web.bind.annotation.*;

import com.icsd.springor.model.User;
import com.icsd.springor.service.UserService;
import com.icsd.springor.utilities.JwtUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.lang.System.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/users")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @GetMapping("/login")
    public String showLoginForm(Model model,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout
    ) {
        model.addAttribute("user", new User());
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out");
        }
        return "login";
    }

    @PostMapping("/register")
    @ResponseBody  // Add this to indicate we're returning data, not a view name
    public ResponseEntity<?> registerUser(@Valid @RequestBody User user,
            BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Validation errors"));
        }

        if (userService.existsByUsername(user.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username is already taken!"));
        }

        if (userService.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is already registered!"));
        }

        user.setActive(false);
        user.setRole("ROLE_USER");
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userService.save(user);
        return ResponseEntity.ok(Map.of("message", "Registration successful"));
    }
    
    @GetMapping("/logout")
    public String logout(Model model) {
        model.addAttribute("user", new User());
        
        return "login";
    }
    

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        try {
            // Create authentication token
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(), 
                loginRequest.getPassword()
            );
            
            // Authenticate
            Authentication authentication = authenticationManager.authenticate(authToken);
            
            // Store in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Store in session
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, 
                                SecurityContextHolder.getContext());
            
            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("username", loginRequest.getUsername());
            response.put("success", true);
            
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @GetMapping("/profile")
    public String showProfile(Model model) {
        // Get current user and add to model
        return "profile";
    }

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UserController.class);
    
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        logger.info("Dashboard controller called, user: {}", 
                    authentication != null ? authentication.getName() : "none");
        
        // Try with explicit template location
        return "dashboard"; // or try "templates/dashboard" or "/dashboard"
    }

    @GetMapping("/manage_users")
    public String showUserManagement(Model model) {
        // Get current user and add to model
        return "users_manage";
    }

    @PostMapping("/update")
    public String updateProfile(@ModelAttribute User user) {
        // Update current user's profile
        return "redirect:/users/profile";
    }

    @GetMapping("/change-password")
    public String showChangePasswordForm() {
        return "change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword, @RequestParam String newPassword) {
        // Change password for current user
        return "redirect:/users/profile";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm() {
        return "reset-password-request";
    }

    @PostMapping("/reset-password")
    public String requestPasswordReset(@RequestParam String email) {
        userService.requestPasswordReset(email);
        return "reset-password-sent";
    }

    @GetMapping("/reset-password/{token}")
    public String showNewPasswordForm(@PathVariable String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password-new";
    }

    @PostMapping("/reset-password/{token}")
    public String resetPassword(@PathVariable String token, @RequestParam String newPassword) {
        userService.resetPassword(token, newPassword);
        return "redirect:/login";
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        response.put("status", "error");
        return response;
    }

   

}
