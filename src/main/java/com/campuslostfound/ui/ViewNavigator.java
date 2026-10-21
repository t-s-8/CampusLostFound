package com.campuslostfound.ui;

import com.campuslostfound.dao.LocationDAO;
import com.campuslostfound.model.User;
import com.campuslostfound.model.UserRole;
import com.campuslostfound.service.AuthService;
import com.campuslostfound.service.ClaimService;
import com.campuslostfound.service.ItemService;
import com.campuslostfound.service.MatchService;
import com.campuslostfound.service.SearchService;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Central UI navigation controller coordinating scenes and service injection.
 */
public class ViewNavigator {

    private static Stage primaryStage;

    // Backend Services
    private static final AuthService authService = new AuthService();
    private static final MatchService matchService = new MatchService();
    private static final LocationDAO locationDAO = new LocationDAO();
    private static final ItemService itemService = new ItemService(new com.campuslostfound.dao.ItemDAO(), locationDAO, matchService);
    private static final SearchService searchService = new SearchService();
    private static final ClaimService claimService = new ClaimService();
    private static final com.campuslostfound.service.AnalyticsService analyticsService = new com.campuslostfound.service.AnalyticsService();

    private ViewNavigator() {
    }

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static AuthService getAuthService() {
        return authService;
    }

    public static ItemService getItemService() {
        return itemService;
    }

    public static SearchService getSearchService() {
        return searchService;
    }

    public static MatchService getMatchService() {
        return matchService;
    }

    public static ClaimService getClaimService() {
        return claimService;
    }

    public static com.campuslostfound.service.AnalyticsService getAnalyticsService() {
        return analyticsService;
    }

    public static LocationDAO getLocationDAO() {
        return locationDAO;
    }

    public static void showLogin() {
        LoginView view = new LoginView();
        setScene(view.getRoot(), 900, 600, "Campus Lost & Found - Login");
    }

    public static void showRegister() {
        RegisterView view = new RegisterView();
        setScene(view.getRoot(), 900, 650, "Campus Lost & Found - Register");
    }

    public static void showStudentDashboard() {
        StudentDashboardView view = new StudentDashboardView();
        setScene(wrapWithNavigation(view.getRoot(), "dashboard"), 1100, 750, "Campus Lost & Found - Student Portal");
    }

    public static void showAdminDashboard() {
        AdminDashboardView view = new AdminDashboardView();
        setScene(wrapWithNavigation(view.getRoot(), "admin"), 1150, 800, "Campus Lost & Found - Administrative Portal");
    }

    public static void showReportItem() {
        ReportItemView view = new ReportItemView();
        setScene(wrapWithNavigation(view.getRoot(), "report"), 1100, 750, "Campus Lost & Found - Report Item");
    }

    public static void showSearch() {
        SearchView view = new SearchView();
        setScene(wrapWithNavigation(view.getRoot(), "search"), 1150, 750, "Campus Lost & Found - Search Items");
    }

    public static void showMatches() {
        MatchesView view = new MatchesView();
        setScene(wrapWithNavigation(view.getRoot(), "matches"), 1150, 750, "Campus Lost & Found - Candidate Matches");
    }

    public static void showClaims() {
        ClaimsView view = new ClaimsView();
        setScene(wrapWithNavigation(view.getRoot(), "claims"), 1150, 750, "Campus Lost & Found - Claims Management");
    }

    private static void setScene(Parent root, double width, double height, String title) {
        if (primaryStage == null) {
            return;
        }
        Scene scene = new Scene(root, width, height);
        String css = ViewNavigator.class.getResource("/css/style.css") != null
                ? ViewNavigator.class.getResource("/css/style.css").toExternalForm()
                : null;
        if (css != null) {
            scene.getStylesheets().add(css);
        }
        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    /**
     * Decorates view content with top branding bar and navigation tabs.
     */
    public static BorderPane wrapWithNavigation(Parent content, String activeTab) {
        BorderPane layout = new BorderPane();

        // 1. Top Navbar
        HBox topBar = new HBox(15);
        topBar.getStyleClass().add("top-navbar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label titleLbl = new Label("CAMPUS LOST & FOUND");
        titleLbl.getStyleClass().add("nav-title");
        Label subLbl = new Label("Secure Item Recovery System");
        subLbl.getStyleClass().add("nav-subtitle");
        titleBox.getChildren().addAll(titleLbl, subLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        User user = SessionManager.getCurrentUser();
        HBox userBox = new HBox(10);
        userBox.setAlignment(Pos.CENTER_RIGHT);

        if (user != null) {
            Label nameLbl = new Label(user.getName());
            nameLbl.getStyleClass().add("user-badge");

            Label roleBadge = new Label(user.getRole().name());
            roleBadge.getStyleClass().add(user.getRole() == UserRole.ADMIN ? "role-tag-admin" : "role-tag-student");

            Button logoutBtn = new Button("Sign Out");
            logoutBtn.getStyleClass().add("btn-logout");
            logoutBtn.setOnAction(e -> {
                SessionManager.logout();
                showLogin();
            });

            userBox.getChildren().addAll(nameLbl, roleBadge, logoutBtn);
        }

        topBar.getChildren().addAll(titleBox, spacer, userBox);

        // 2. Sub-navigation tabs
        HBox subNav = new HBox(8);
        subNav.getStyleClass().add("sub-navbar");

        if (SessionManager.isAdmin()) {
            Button adminBtn = createNavButton("Admin Overview", "admin".equals(activeTab), e -> showAdminDashboard());
            Button searchBtn = createNavButton("Search & Browse", "search".equals(activeTab), e -> showSearch());
            Button matchesBtn = createNavButton("All Matches", "matches".equals(activeTab), e -> showMatches());
            Button claimsBtn = createNavButton("Review Claims", "claims".equals(activeTab), e -> showClaims());
            Button reportBtn = createNavButton("Report Item", "report".equals(activeTab), e -> showReportItem());
            subNav.getChildren().addAll(adminBtn, searchBtn, matchesBtn, claimsBtn, reportBtn);
        } else {
            Button dashBtn = createNavButton("My Dashboard", "dashboard".equals(activeTab), e -> showStudentDashboard());
            Button reportBtn = createNavButton("Report Item", "report".equals(activeTab), e -> showReportItem());
            Button searchBtn = createNavButton("Search Items", "search".equals(activeTab), e -> showSearch());
            Button matchesBtn = createNavButton("Possible Matches", "matches".equals(activeTab), e -> showMatches());
            Button claimsBtn = createNavButton("My Claims", "claims".equals(activeTab), e -> showClaims());
            subNav.getChildren().addAll(dashBtn, reportBtn, searchBtn, matchesBtn, claimsBtn);
        }

        VBox header = new VBox(topBar, subNav);
        layout.setTop(header);
        layout.setCenter(content);

        return layout;
    }

    private static Button createNavButton(String text, boolean isActive, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.getStyleClass().add(isActive ? "nav-tab-button-active" : "nav-tab-button");
        btn.setOnAction(handler);
        return btn;
    }
}
