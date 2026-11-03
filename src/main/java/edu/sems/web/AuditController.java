package edu.sems.web;

import edu.sems.service.AuditService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/audit")
    public String list(Model model) {
        model.addAttribute("events", auditService.findAll());
        return "audit/list";
    }
}
