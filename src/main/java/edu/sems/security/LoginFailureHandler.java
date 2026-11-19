package edu.sems.security;

import edu.sems.service.LoginAttemptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Remembers which username was just rejected so the login page can ask the server
 * for that account's remaining attempts or lockout deadline.
 */
@Component
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    public static final String ATTEMPTED_USERNAME = "sems.attemptedUsername";

    private final LoginAttemptService loginAttempts;

    public LoginFailureHandler(LoginAttemptService loginAttempts) {
        this.loginAttempts = loginAttempts;
        setDefaultFailureUrl("/login?error");
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String username = loginAttempts.normalizeUsername(request.getParameter("username"));
        if (username != null) {
            request.getSession().setAttribute(ATTEMPTED_USERNAME, username);
        }
        super.onAuthenticationFailure(request, response, exception);
    }
}
