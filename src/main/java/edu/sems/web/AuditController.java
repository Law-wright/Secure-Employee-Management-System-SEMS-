package edu.sems.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuditController {

    @GetMapping("/audit")
    public String list() {
        return "audit/list";
    }
}
