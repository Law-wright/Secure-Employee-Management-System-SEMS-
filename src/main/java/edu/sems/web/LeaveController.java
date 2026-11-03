package edu.sems.web;

import edu.sems.service.LeaveRequestService;
import edu.sems.service.LeaveValidationException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LeaveController {

    private final LeaveRequestService leaveService;

    public LeaveController(LeaveRequestService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping("/leave/mine")
    public String mine(Model model) {
        model.addAttribute("leaveRequests", leaveService.findMine());
        return "leave/mine";
    }

    @GetMapping("/leave/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("leaveForm")) {
            model.addAttribute("leaveForm", new CreateLeaveForm());
        }
        return "leave/new";
    }

    @PostMapping("/leave")
    public String create(
            @Valid @ModelAttribute("leaveForm") CreateLeaveForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                leaveService.submit(form.getStartDate(), form.getEndDate(), form.getReason());
                redirectAttributes.addFlashAttribute("message", "Leave request submitted.");
                return "redirect:/leave/mine";
            } catch (LeaveValidationException exception) {
                reject(bindingResult, exception);
            }
        }
        return "leave/new";
    }

    @GetMapping("/leave/review")
    public String review(Model model) {
        model.addAttribute("leaveRequests", leaveService.findPending());
        return "leave/review";
    }

    @PostMapping("/leave/review/{id}/approve")
    public String approve(@PathVariable long id, RedirectAttributes redirectAttributes) {
        return review(id, true, redirectAttributes);
    }

    @PostMapping("/leave/review/{id}/reject")
    public String reject(@PathVariable long id, RedirectAttributes redirectAttributes) {
        return review(id, false, redirectAttributes);
    }

    private String review(long id, boolean approve, RedirectAttributes redirectAttributes) {
        try {
            if (approve) {
                leaveService.approve(id);
                redirectAttributes.addFlashAttribute("message", "Leave request approved.");
            } else {
                leaveService.reject(id);
                redirectAttributes.addFlashAttribute("message", "Leave request rejected.");
            }
        } catch (LeaveValidationException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/leave/review";
    }

    private static void reject(BindingResult bindingResult, LeaveValidationException exception) {
        if (bindingResult.getFieldType(exception.getField()) != null) {
            bindingResult.rejectValue(exception.getField(), "invalid", exception.getMessage());
        } else {
            bindingResult.reject("invalid", exception.getMessage());
        }
    }
}
