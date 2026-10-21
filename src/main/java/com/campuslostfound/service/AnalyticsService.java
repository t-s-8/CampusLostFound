package com.campuslostfound.service;

import com.campuslostfound.dao.AnalyticsDAO;
import com.campuslostfound.model.AdminAnalytics;

/**
 * Service providing aggregated statistical reporting and analytics metrics
 * for administrative dashboards and executive reporting.
 */
public class AnalyticsService {

    private final AnalyticsDAO analyticsDAO;

    public AnalyticsService() {
        this(new AnalyticsDAO());
    }

    public AnalyticsService(AnalyticsDAO analyticsDAO) {
        this.analyticsDAO = analyticsDAO;
    }

    /**
     * Retrieves aggregated system analytics computed directly via SQL from the database.
     */
    public AdminAnalytics getSystemAnalytics() {
        return analyticsDAO.fetchSystemAnalytics();
    }
}
