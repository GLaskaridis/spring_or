package com.icsd.springor.config;

import com.icsd.springor.utilities.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HeaderWriterLogoutHandler;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter;
import static org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter.Directive.COOKIES;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // Constructor to verify this config is loaded
    public SecurityConfig() {
        System.out.println("====================================");
        System.out.println("SECURITY CONFIG LOADED");
        System.out.println("====================================");
    }

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .logout((logout) -> logout
                .addLogoutHandler(new HeaderWriterLogoutHandler(new ClearSiteDataHeaderWriter(COOKIES)))
                .logoutSuccessUrl("/users/login")
            )
            .csrf(csrf -> csrf.disable())  // For simplicity during debugging
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/users/login", "/users/register", "/logout", "/login", "/css/**", "/js/**", "/images/**", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
            )
            .formLogin(form -> form.disable()) // Disable form login since you're using REST
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint((request, response, exception) -> {
                    // Handle unauthenticated requests based on Accept header
                    String accept = request.getHeader("Accept");
                    if (accept != null && accept.contains("application/json")) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"message\":\"Unauthorized\"}");
                    } else {
                        // Redirect to login for HTML requests
                        response.sendRedirect("/users/login");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    // Handle access denied - redirect to login or show error page
                    String accept = request.getHeader("Accept");
                    if (accept != null && accept.contains("application/json")) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"message\":\"Access Denied\"}");
                    } else {
                        // Redirect to login instead of trying to render access_denied template
                        response.sendRedirect("/users/login?error=access_denied");
                    }
                })
            );
        
        return http.build();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}