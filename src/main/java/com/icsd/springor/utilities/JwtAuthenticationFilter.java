package com.icsd.springor.utilities;

import com.icsd.springor.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.LoggerFactory;


@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

   private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    
    @Autowired
    private JwtUtils jwtUtil;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        logger.debug("Processing request: {} {}" +  request.getMethod() + requestURI);
        
        // Skip JWT processing for login and public endpoints
        if (shouldSkipAuthentication(requestURI)) {
            logger.debug("Skipping JWT authentication for public endpoint: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }
        
        String username = null;
        String jwt = null;
        
        // First try to get JWT from Authorization header
        final String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            logger.debug("Found JWT in Authorization header");
        } else {
            // If not in header, try to get from cookie
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                jwt = Arrays.stream(cookies)
                    .filter(cookie -> "jwt".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
                
                if (jwt != null) {
                    logger.debug("Found JWT in cookie");
                } else {
                    logger.debug("No JWT found in cookies");
                }
            } else {
                logger.debug("No cookies found in request");
            }
        }
        
        // Extract username from JWT if available
        if (jwt != null && !jwt.isEmpty()) {
            try {
                username = jwtUtil.getUsernameFromToken(jwt);
                logger.debug("Extracted username from JWT: {}", username);
            } catch (Exception e) {
                logger.error("Error extracting username from JWT: {}", e.getMessage());
                jwt = null; // Invalidate the token
            }
        }
        
        // If we have a username and no authentication exists yet
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                logger.debug("Loaded user details for: {}", username);
                
                // If the token is valid, set up the authentication
                if (jwtUtil.validateToken(jwt)) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    logger.info("User authenticated successfully: {} with roles: {}", 
                              username, userDetails.getAuthorities());
                } else {
                    logger.warn("Invalid JWT token for user: {}", username);
                }
            } catch (Exception e) {
                logger.error("Error during authentication for user {}: {}", username, e.getMessage());
            }
        } else if (username == null && jwt != null) {
            logger.warn("JWT token present but no username extracted");
        } else if (SecurityContextHolder.getContext().getAuthentication() != null) {
            logger.debug("User already authenticated: {}", 
                        SecurityContextHolder.getContext().getAuthentication().getName());
        }
        
        filterChain.doFilter(request, response);
    }
    
      private boolean shouldSkipAuthentication(String requestURI) {
        return requestURI.equals("/users/login") ||
               requestURI.equals("/users/register") ||
               requestURI.equals("/logout") ||
               requestURI.equals("/login") ||
               requestURI.startsWith("/css/") ||
               requestURI.startsWith("/js/") ||
               requestURI.startsWith("/images/") ||
               requestURI.equals("/error") ||
               requestURI.equals("/favicon.ico");
    }
}