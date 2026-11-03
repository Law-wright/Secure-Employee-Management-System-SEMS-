package edu.sems.web;

import edu.sems.service.ProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        profileService.findEmployeeByUsername(authentication.getName())
                .ifPresent(employee -> model.addAttribute("employee", employee));
        return "profile";
    }
}
