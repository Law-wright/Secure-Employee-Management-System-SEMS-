package edu.sems.config;

import edu.sems.security.AuditAccessDeniedHandler;
import edu.sems.security.LoginFailureHandler;
import edu.sems.security.LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuditAccessDeniedHandler auditAccessDeniedHandler,
            LoginFailureHandler loginFailureHandler,
            LoginSuccessHandler loginSuccessHandler)
            throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/login/status", "/css/**", "/js/**", "/access-denied").permitAll()
                        .requestMatchers("/employees", "/employees/**").hasRole("ADMIN")
                        .requestMatchers("/audit", "/audit/**").hasRole("ADMIN")
                        .requestMatchers("/leave/review", "/leave/review/**").hasRole("MANAGER")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(handler -> handler.accessDeniedHandler(auditAccessDeniedHandler));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
