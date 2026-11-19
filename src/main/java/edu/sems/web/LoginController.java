package edu.sems.web;

import edu.sems.security.LoginFailureHandler;
import edu.sems.service.LoginAttemptService;
import edu.sems.service.LoginAttemptStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    private final LoginAttemptService loginAttempts;

    public LoginController(LoginAttemptService loginAttempts) {
        this.loginAttempts = loginAttempts;
    }

    @GetMapping("/login")
    public String login(HttpServletRequest request, Model model) {
        model.addAttribute("attempt", loginAttempts.status(attemptedUsername(request)));
        return "login";
    }

    /**
     * Fresh lockout state for the username this browser just tried.
     * The username comes from the session, not from the query string, so this cannot be used to look up arbitrary accounts.
     */
    @GetMapping("/login/status")
    public ResponseEntity<LoginAttemptStatus> status(HttpServletRequest request) {
        LoginAttemptStatus status = loginAttempts.status(attemptedUsername(request));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(status);
    }

    private static String attemptedUsername(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(LoginFailureHandler.ATTEMPTED_USERNAME);
        return value instanceof String username ? username : null;
    }
}
