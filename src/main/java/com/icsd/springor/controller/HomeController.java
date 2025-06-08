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
            // User is logged in, redirect to dashboard
            return "redirect:/users/dashboard";
        } else {
            // User is not logged in, redirect to login page
            return "redirect:/users/login";
        }
    }
    
    @GetMapping("/assign")
    public String assign(Model model) {
        CourseScheduler scheduler = new CourseScheduler();
        List<Course> courses = courseService.getAllCourses();
        
        Course course = courses.get(0);
        System.out.println(course.getName());
        course.setTimePreference(new Course.TimePreference(
            DayOfWeek.FRIDAY,    // προτιμώμενη μέρα
            0,                   // πρώτο slot (9πμ-12μμ)
            8                    // βάρος προτίμησης (1-10)
        ));
        //courses.add(course);
        
        List<Room> rooms = roomService.getAllRooms();
        System.out.println(rooms);
        List<CourseAssignment> assignments = scheduler.createSchedule(courses, rooms);
        System.out.println(assignments.size());
        assignments.forEach(System.out::println);
        
        return "home";
    }
    
}