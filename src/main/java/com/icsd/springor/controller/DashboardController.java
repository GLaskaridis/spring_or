/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.controller;

import com.icsd.springor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    
    @Autowired
    private UserService userService;
    
      @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        try {
            if (userService.isCurrentUserAdmin(authentication)) {
                return "redirect:/teachers/list";
            } else if (userService.isCurrentUserProgramManager(authentication)) {
                return "redirect:/schedules";
            } else if (userService.isCurrentUserTeacher(authentication)) {
                return "redirect:/schedules/teacher/available";
            } else {
                model.addAttribute("error", "Unknown user role");
                return "error";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error loading dashboard: " + e.getMessage());
            return "error";
        }
    }
}