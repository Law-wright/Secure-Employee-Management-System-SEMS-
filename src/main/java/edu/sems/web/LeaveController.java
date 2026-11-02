package edu.sems.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LeaveController {

    @GetMapping("/leave/mine")
    public String mine() {
        return "leave/mine";
    }

    @GetMapping("/leave/new")
    public String createForm() {
        return "leave/new";
    }

    @GetMapping("/leave/review")
    public String review() {
        return "leave/review";
    }
}
