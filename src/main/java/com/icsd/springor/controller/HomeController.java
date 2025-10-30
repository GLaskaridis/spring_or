package com.icsd.springor.controller;

import com.icsd.springor.CourseScheduler;
import com.icsd.springor.CourseScheduler.CourseAssignment;
import com.icsd.springor.model.Course;
import com.icsd.springor.model.Room;
import com.icsd.springor.service.CourseService;
import com.icsd.springor.service.RoomService;
import java.time.DayOfWeek;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    
    @Autowired
    private CourseService courseService;
    
    @Autowired
    private RoomService roomService;

      @GetMapping("/")
    public String home(Model model) {
        // Check if the user is authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // If user is authenticated and not the anonymous user
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getName().equals("anonymousUser")) {
            
            
            System.out.println("AUTHORITY " + authentication.getAuthorities().stream().findFirst().get().getAuthority());
            //analoga me ton rolo
            if (authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
                return "redirect:/users/admin_dashboard";
            } else if (authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("USER"))) {
                System.out.println("yeeeeeeahhhhhhhhhh");
                return "redirect:users/user_dashboard";
            } else {
                return "redirect:users/user_dashboard";
            }
        } else {
            //ean den uparxei sindesi, se petaei sto login
            return "redirect:/users/login";
        }
    }
    
//    @GetMapping("/dashboard")
//    public String dashboard(Model model, Authentication authentication) {
//        return "redirect:/schedules/dashboard";
//    }
//    
    @GetMapping("/assign")
    public String assign(Model model) {
        // Redirect to test execution
        return "redirect:/schedule-execution/test";
    }
    
    @GetMapping("/preferences")
    public String preferences() {
        return "teacher-preferences";
    }

//    @GetMapping("/users/admin_dashboard")
//    public String adminDashboardRedirect() {
//        return "redirect:/admin/dashboard";
//    }
    
}