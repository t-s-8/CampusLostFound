package com.campuslostfound.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class CsrfTokenFilter extends OncePerRequestFilter {
    public static final String TOKEN = "campusCsrfToken";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        boolean apiMutation = path.startsWith("/api/") && !("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method));
        if (apiMutation) {
            HttpSession session = request.getSession(false);
            String expected = session == null ? null : (String) session.getAttribute(TOKEN);
            String supplied = request.getHeader("X-CSRF-TOKEN");
            if (expected == null || supplied == null || !MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF token missing or invalid.");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
