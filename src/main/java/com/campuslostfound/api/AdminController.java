package com.campuslostfound.api;

import com.campuslostfound.model.AdminAnalytics;
import com.campuslostfound.service.AnalyticsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AnalyticsService analytics;
    private final SessionAuth sessions;
    public AdminController(AnalyticsService analytics, SessionAuth sessions) { this.analytics=analytics; this.sessions=sessions; }
    @GetMapping("/analytics")
    public AdminAnalytics analytics(HttpSession session) { sessions.requireAdmin(session); return analytics.getSystemAnalytics(); }
}
