package com.campuslostfound.api;

import com.campuslostfound.dao.*;
import com.campuslostfound.dsa.MatchAlgorithm;
import com.campuslostfound.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfiguration {
    @Bean public UserDAO userDAO() { return new UserDAO(); }
    @Bean public ItemDAO itemDAO() { return new ItemDAO(); }
    @Bean public LocationDAO locationDAO() { return new LocationDAO(); }
    @Bean public MatchDAO matchDAO() { return new MatchDAO(); }
    @Bean public ClaimDAO claimDAO() { return new ClaimDAO(); }
    @Bean public AnalyticsDAO analyticsDAO() { return new AnalyticsDAO(); }
    @Bean public MatchAlgorithm matchAlgorithm() { return new MatchAlgorithm(); }
    @Bean public MatchService matchService(MatchDAO matchDAO, ItemDAO itemDAO, MatchAlgorithm algorithm) {
        return new MatchService(matchDAO, itemDAO, algorithm);
    }
    @Bean public ItemService itemService(ItemDAO itemDAO, LocationDAO locationDAO, MatchService matchService) {
        return new ItemService(itemDAO, locationDAO, matchService);
    }
    @Bean public SearchService searchService(ItemDAO itemDAO) { return new SearchService(itemDAO); }
    @Bean public AuthService authService(UserDAO userDAO) { return new AuthService(userDAO); }
    @Bean public ClaimService claimService(ClaimDAO claimDAO, ItemDAO itemDAO, UserDAO userDAO) {
        return new ClaimService(claimDAO, itemDAO, userDAO);
    }
    @Bean public AnalyticsService analyticsService(AnalyticsDAO dao) { return new AnalyticsService(dao); }
}
