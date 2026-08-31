package com.example.payheurebackend.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Serves Angular's entry point when the browser refreshes an Angular route. */
@Controller
public class SpaController {

    @GetMapping({"/", "/home", "/pointage", "/anomalies", "/paie"})
    public String forwardToIndex() {
        return "forward:/index.html";
    }

    @GetMapping("/paie/detail/{employeeId}")
    public String forwardDetailToIndex(@PathVariable String employeeId) {
        return "forward:/index.html";
    }
}
