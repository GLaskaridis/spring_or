/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.icsd.springor.controller;

import com.icsd.springor.DTO.LoginRequest;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.icsd.springor.model.User;
import com.icsd.springor.model.UserRole;
import com.icsd.springor.service.CourseScheduleService;
import com.icsd.springor.service.CourseService;
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
    private CourseService courseService;
    
     @Autowired
    private CourseScheduleService courseScheduleService;

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
    @ResponseBody  
    public ResponseEntity<?> registerUser(@Valid @RequestBody User user,
            BindingResult result) {

        // Έλεγχος της φόρμας που έγινε υποβολή
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Validation errors"));
        }

        // Έλεγχος από τη βάση αν υπάρχει το username
        if (userService.existsByUsername(user.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username is already taken!"));
        }

        if (userService.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is already registered!"));
        }

        // δημιουργία αντικειμένου User με προσθήκη χαρακτηριστικών
        // είναι ανενεργός στην αρχή
        user.setActive(false);

        //εξ ορισμού όλοι οι νέοι χρήστες είναι διδάσκοντες
        //ο διαχειριστής μπορεί να τους αλλάξει ρόλο αργότερα
        UserRole assignedRole = UserRole.TEACHER;
        user.setRole(assignedRole);

        // Κρυπτογράφηση του κωδικού του
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userService.save(user);
        return ResponseEntity.ok(Map.of("message", "Registration successful. Account pending activation."));
    }
    
    @GetMapping("/logout")
    public String logout(Model model) {
        model.addAttribute("user", new User());
        
        return "login";
    }
    

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request,HttpServletResponse response) {
        try {
            //dimiourgia token kata tin eisodo
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(), 
                loginRequest.getPassword()
            );
            
            //elegxos authentikopoiisi me to token
            Authentication authentication = authenticationManager.authenticate(authToken);
            
            System.out.println("AUTHORITY " + authentication.getAuthorities().stream().findFirst().get().getAuthority());
             //anaktisi antikeimenou user
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            
            // Παραγωγή JWT token
            String jwt = jwtUtils.generateToken(userDetails);
        
            // Καθορισμός redirect URL με βάση το ρόλο
            String redirectUrl = "/users/user_dashboard"; // default για USER
            if (userDetails.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
                redirectUrl = "/users/admin_dashboard";
            }
        
            // Αποθήκευση JWT ως HttpOnly cookie
            Cookie jwtCookie = new Cookie("jwt", jwt);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(24 * 60 * 60); // 24 ώρες
            response.addCookie(jwtCookie);
            
            //apothikeusi sto security context
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            //apothikeysi sto session
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, 
                                SecurityContextHolder.getContext());
            
            //epistrofi minimatos epitixias
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("username", loginRequest.getUsername());
            responseMap.put("success", true);
            responseMap.put("redirectUrl", redirectUrl); // Αυτό έλειπε!
            responseMap.put("token", jwt);
            responseMap.put("role", userDetails.getAuthorities().iterator().next().getAuthority());

            return ResponseEntity.ok(responseMap);
        } catch (AuthenticationException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

     @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        logger.info("Profile page accessed by: {}", 
                   authentication != null ? authentication.getName() : "anonymous");
        
        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
            
            // Determine user type for profile customization
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("userRole", isAdmin ? "ADMIN" : "USER");
        }
        
        return "profile";
    }

    /**
     * Settings page - Common for both user types but with different options
     */
    @GetMapping("/settings")
    public String settings(Model model, Authentication authentication) {
        logger.info("Settings page accessed by: {}", 
                   authentication != null ? authentication.getName() : "anonymous");
        
        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
            
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("userRole", isAdmin ? "ADMIN" : "USER");
        }
        
        return "settings";
    }

    /**
     * Help page - Common for both user types
     */
    @GetMapping("/help")
    public String help(Model model, Authentication authentication) {
        logger.info("Help page accessed by: {}", 
                   authentication != null ? authentication.getName() : "anonymous");
        
        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
            
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            model.addAttribute("isAdmin", isAdmin);
        }
        
        return "help";
    }

    /**
     * Access denied page
     */
    @GetMapping("/access-denied")
    public String accessDenied(Model model, Authentication authentication) {
        logger.warn("Access denied page accessed by: {}", 
                   authentication != null ? authentication.getName() : "anonymous");
        
        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
            model.addAttribute("userRole", 
                authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")) ? "ADMIN" : "USER");
        }
        
        return "access_denied";
    }

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UserController.class);
    
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        logger.info("Dashboard controller called, user: {}", 
                authentication != null ? authentication.getName() : "none");
    
        
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ADMIN"))) {
            return "redirect:/users/admin_dashboard";
        } else if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("USER"))) {
            return "redirect:/users/user_dashboard";
        } else {
            return "redirect:/users/secretary_dashboard";
        }
    }
    
    @GetMapping("/user_dashboard")
    public String userDashboard(Model model, Authentication authentication) {
        logger.info("User dashboard accessed by: {}", 
                   authentication != null ? authentication.getName() : "anonymous");
        
        // Add user information to the model for the template
        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
            model.addAttribute("userRole", "USER");
            
            // Add user authorities for template logic
            boolean isUser = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER"));
            model.addAttribute("isUser", isUser);
            
            logger.debug("User {} accessing user dashboard with authorities: {}", 
                        authentication.getName(), authentication.getAuthorities());
        }
        
        // Return the user dashboard template
        return "teacher_dashboard";
    }

    /**
     * Admin Dashboard - Serves the admin_dashboard.html template
     * Only accessible to users with ROLE_ADMIN
     */
    @GetMapping("/admin_dashboard")
    public String adminDashboard(Model model, Authentication authentication) {
        logger.info("Admin dashboard accessed by: {}", 
                   authentication != null ? authentication.getName() : "anonymous");

        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
            model.addAttribute("userRole", "ADMIN");
            model.addAttribute("isAdmin", true);

            // Add dashboard data
            try {
                model.addAttribute("totalUsers", userService.countAllUsers());
                model.addAttribute("totalCourses", courseService.countAllCourses());
                model.addAttribute("activeSchedules", courseScheduleService.countActiveSchedules());
            } catch (Exception e) {
                model.addAttribute("error", "Σφάλμα κατά τη φόρτωση δεδομένων");
            }
        }

        // Return template directly - NO REDIRECT!
        return "admin_dashboard";
    }


    @GetMapping("/manage_users")
    public String showUserManagement(Model model) {
        return "users_manage";
    }

    @PostMapping("/update")
    public String updateProfile(@ModelAttribute User user) {
        return "redirect:/users/profile";
    }

    @GetMapping("/change-password")
    public String showChangePasswordForm() {
        return "change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword, @RequestParam String newPassword) {
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
    
    @GetMapping("/preferences")
    public String redirectToPreferences() {
        return "teacher-preferences";
    }

   @GetMapping("/my-courses")
    public String myCourses() {
        return "my-courses";
    }
    
    @GetMapping("/schedule")
    public String schedule() {
        return "schedule";
    }
    
    @GetMapping("/rooms")
    public String rooms() {
        return "rooms";
    }
    
    @GetMapping("/api/current")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            User currentUser = userService.getCurrentUser(authentication);
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", currentUser.getId());
            userInfo.put("username", currentUser.getUsername());
            userInfo.put("firstName", currentUser.getFullName());
            userInfo.put("email", currentUser.getEmail());
            userInfo.put("role", currentUser.getRole().toString());

            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            logger.error("Error getting current user info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/teacher-preferences")
    public String teacherPreferences() {
        return "teacher-preferences";
    }
    
    //api endpoints για διαχείριση καθηγητών
    
    /**
     * επιστρέφει λίστα με όλους τους καθηγητές σε μορφή json
     * καλείται από το users_manage.html στη συνάρτηση loadTeachersData()
     * url: GET /users/api/teachers
     * χρησιμοποιείται για να φορτώσει τον πίνακα με τους καθηγητές
     */
    @GetMapping("/api/teachers")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllTeachersApi() {
        try {
            //ανάκτηση όλων των καθηγητών από τη βάση
            List<User> teachers = userService.findAllTeachers();
            
            //μετατροπή των user objects σε maps για json response
            List<Map<String, Object>> teacherData = teachers.stream()
                .map(teacher -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", teacher.getId());
                    data.put("fullName", teacher.getFullName());
                    data.put("username", teacher.getUsername());
                    data.put("email", teacher.getEmail());
                    data.put("teacherType", teacher.getTeacherType() != null ? teacher.getTeacherType().name() : "");
                    data.put("teacherRank", teacher.getTeacherRank());
                    data.put("active", teacher.isActive());
                    return data;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(teacherData);
        } catch (Exception e) {
            logger.error("Error fetching teachers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * επιστρέφει τα στοιχεία ενός συγκεκριμένου καθηγητή
     * καλείται από το users_manage.html στη συνάρτηση editTeacher(id)
     * url: GET /users/api/teachers/{id}
     * χρησιμοποιείται για να φορτώσει τα στοιχεία στη φόρμα επεξεργασίας
     */
    @GetMapping("/api/teachers/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTeacherByIdApi(@PathVariable Long id) {
        try {
            //αναζήτηση καθηγητή με βάση το id
            User teacher = userService.getUserById(id);
            if (teacher == null) {
                return ResponseEntity.notFound().build();
            }
            
            //δημιουργία map με τα στοιχεία του καθηγητή
            Map<String, Object> data = new HashMap<>();
            data.put("id", teacher.getId());
            data.put("fullName", teacher.getFullName());
            data.put("username", teacher.getUsername());
            data.put("email", teacher.getEmail());
            data.put("teacherType", teacher.getTeacherType() != null ? teacher.getTeacherType().name() : "");
            data.put("teacherRank", teacher.getTeacherRank());
            data.put("active", teacher.isActive());
            
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            logger.error("Error fetching teacher with id: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * δημιουργεί νέο καθηγητή στη βάση δεδομένων
     * καλείται από το users_manage.html στη συνάρτηση saveTeacher() όταν δεν υπάρχει teacherId
     * url: POST /users/api/teachers
     * χρησιμοποιείται όταν πατάμε "αποθήκευση" στο modal προσθήκης καθηγητή
     * παραλαμβάνει json με τα στοιχεία του νέου καθηγητή
     */
    @PostMapping("/api/teachers")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createTeacherApi(@RequestBody Map<String, Object> teacherData) {
        try {
            //δημιουργία νέου user object
            User newTeacher = new User();
            newTeacher.setFullName((String) teacherData.get("fullName"));
            newTeacher.setUsername((String) teacherData.get("username"));
            newTeacher.setEmail((String) teacherData.get("email"));
            
            //ρύθμιση τύπου καθηγητή (δεπ, εδιπ, ετεπ)
            String teacherType = (String) teacherData.get("teacherType");
            if (teacherType != null && !teacherType.isEmpty()) {
                newTeacher.setTeacherType(com.icsd.springor.DTO.TeacherType.valueOf(teacherType));
            }
            
            newTeacher.setTeacherRank((String) teacherData.get("teacherRank"));
            newTeacher.setActive((Boolean) teacherData.getOrDefault("active", true));
            newTeacher.setRole(UserRole.TEACHER);
            
            //κρυπτογράφηση κωδικού πρόσβασης
            String password = (String) teacherData.get("password");
            if (password != null && !password.isEmpty()) {
                newTeacher.setPassword(passwordEncoder.encode(password));
            }
            
            //αποθήκευση στη βάση
            User savedTeacher = userService.save(newTeacher);
            
            //επιστροφή μηνύματος επιτυχίας
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ο καθηγητής προστέθηκε επιτυχώς");
            response.put("id", savedTeacher.getId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating teacher", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Σφάλμα κατά την προσθήκη καθηγητή: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * ενημερώνει τα στοιχεία υπάρχοντος καθηγητή
     * καλείται από το users_manage.html στη συνάρτηση saveTeacher() όταν υπάρχει teacherId
     * url: PUT /users/api/teachers/{id}
     * χρησιμοποιείται όταν πατάμε "αποθήκευση" στο modal επεξεργασίας καθηγητή
     * παραλαμβάνει json με τα νέα στοιχεία του καθηγητή
     */
    @PutMapping("/api/teachers/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateTeacherApi(
            @PathVariable Long id, 
            @RequestBody Map<String, Object> teacherData) {
        try {
            //αναζήτηση του υπάρχοντος καθηγητή
            User teacher = userService.getUserById(id);
            if (teacher == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Ο καθηγητής δεν βρέθηκε");
                return ResponseEntity.notFound().build();
            }
            
            //ενημέρωση των στοιχείων
            teacher.setFullName((String) teacherData.get("fullName"));
            teacher.setUsername((String) teacherData.get("username"));
            teacher.setEmail((String) teacherData.get("email"));
            
            //ενημέρωση τύπου καθηγητή
            String teacherType = (String) teacherData.get("teacherType");
            if (teacherType != null && !teacherType.isEmpty()) {
                teacher.setTeacherType(com.icsd.springor.DTO.TeacherType.valueOf(teacherType));
            }
            
            teacher.setTeacherRank((String) teacherData.get("teacherRank"));
            teacher.setActive((Boolean) teacherData.getOrDefault("active", true));
            
            //ενημέρωση κωδικού μόνο αν δόθηκε νέος
            String password = (String) teacherData.get("password");
            if (password != null && !password.isEmpty()) {
                teacher.setPassword(passwordEncoder.encode(password));
            }
            
            //αποθήκευση των αλλαγών
            userService.save(teacher);
            
            //επιστροφή μηνύματος επιτυχίας
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ο καθηγητής ενημερώθηκε επιτυχώς");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating teacher with id: " + id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Σφάλμα κατά την ενημέρωση καθηγητή: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * διαγράφει έναν καθηγητή από τη βάση δεδομένων
     * καλείται από το users_manage.html στη συνάρτηση deleteTeacher()
     * url: DELETE /users/api/teachers/{id}
     * χρησιμοποιείται όταν πατάμε "διαγραφή" στο confirmation modal
     * διαγράφει οριστικά τον καθηγητή με το συγκεκριμένο id
     */
    @DeleteMapping("/api/teachers/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteTeacherApi(@PathVariable Long id) {
        try {
            //έλεγχος αν υπάρχει ο καθηγητής
            User teacher = userService.getUserById(id);
            if (teacher == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Ο καθηγητής δεν βρέθηκε");
                return ResponseEntity.notFound().build();
            }
            
            //διαγραφή από τη βάση
            userService.deleteUser(id);
            
            //επιστροφή μηνύματος επιτυχίας
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ο καθηγητής διαγράφηκε επιτυχώς");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting teacher with id: " + id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Σφάλμα κατά τη διαγραφή καθηγητή: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

   

}
