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

    public SecurityConfig() {
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
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize
                // Public endpoints
                .requestMatchers("/users/login", "/users/register", "/logout", "/login", 
                                "/css/**", "/js/**", "/images/**", "/error").permitAll()
                    
                
                .requestMatchers("/assignments/api/my-assignments").hasRole("TEACHER")
            
                .requestMatchers("/preferences/**").hasRole("TEACHER")
                    
                .requestMatchers("/users/**").hasAnyRole("TEACHER", "ADMIN")
                
                .requestMatchers("/course-preferences/**", "/time-preferences/my-assignments/**").hasRole("TEACHER")
                
                .requestMatchers("/admin/assignments/**", "/schedules/admin/**", 
                                "/schedule-execution/**", "/assignments/**").hasAnyRole("PROGRAM_MANAGER", "ADMIN")
                
                .requestMatchers("/teachers/**", "/users/manage_users").hasRole("ADMIN")
                
                .requestMatchers("/schedules/dashboard", "/rooms/list", "/courses/list").hasAnyRole("TEACHER", "PROGRAM_MANAGER", "ADMIN")
                
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
            )
            .formLogin(form -> form.disable())
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint((request, response, exception) -> {
                    String accept = request.getHeader("Accept");
                    if (accept != null && accept.contains("application/json")) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"message\":\"Unauthorized\"}");
                    } else {
                        response.sendRedirect("/users/login");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String accept = request.getHeader("Accept");
                    if (accept != null && accept.contains("application/json")) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"message\":\"Access Denied - Insufficient permissions\"}");
                    } else {
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