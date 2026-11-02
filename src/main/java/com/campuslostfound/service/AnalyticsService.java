package com.campuslostfound.service;

import com.campuslostfound.dao.AnalyticsDAO;
import com.campuslostfound.model.AdminAnalytics;

public class AnalyticsService {

    private final AnalyticsDAO analyticsDAO;

    public AnalyticsService() {
        this(new AnalyticsDAO());
    }

    public AnalyticsService(AnalyticsDAO analyticsDAO) {
        this.analyticsDAO = analyticsDAO;
    }

    public AdminAnalytics getSystemAnalytics() {
        return analyticsDAO.fetchSystemAnalytics();
    }
}
