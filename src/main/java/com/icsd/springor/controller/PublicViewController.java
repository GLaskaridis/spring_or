package com.icsd.springor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicViewController {

    //εμφάνιση δημόσιου προγράμματος
    @GetMapping("/public-schedule")
    public String publicSchedule() {
        return "public-schedule";
    }
}