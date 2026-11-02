package com.campuslostfound.ui;

import com.campuslostfound.model.AdminAnalytics;
import com.campuslostfound.model.Claim;
import com.campuslostfound.model.Item;
import com.campuslostfound.model.Location;
import com.campuslostfound.model.User;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class AdminDashboardView {

    private final ScrollPane scrollPane;
    private final VBox root;

    private final Label totalLostStat;
    private final Label totalFoundStat;
    private final Label activeReportsStat;
    private final Label recoveredItemsStat;
    private final Label pendingClaimsStat;
    private final Label approvedClaimsStat;
    private final Label recoveryRateStat;

    private final TableView<AdminAnalytics.LocationStat> locationAnalyticsTable;
    private final TableView<AdminAnalytics.CategoryStat> categoryAnalyticsTable;

    private final TableView<Claim> pendingClaimsTable;
    private final TextArea claimProofInspector;
    private final Label claimInspectorMetaLbl;
    private final Button approveBtn;
    private final Button rejectBtn;
    private final Label adminActionFeedback;

    private final TableView<Item> recentItemsTable;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public AdminDashboardView() {
        root = new VBox(16);
        root.getStyleClass().add("content-container");

        scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #f1f5f9;");

        User admin = SessionManager.getCurrentUser();
        String adminName = admin != null ? admin.getName() : "Administrator";

        VBox topCard = new VBox(12);
        topCard.getStyleClass().add("card");

        HBox welcomeRow = new HBox(12);
        welcomeRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label heading = new Label("Administrative Analytics & Operations Command Center");
        heading.getStyleClass().add("card-title");
        heading.setStyle("-fx-font-size: 18px;");
        Label subtitle = new Label("Logged in as " + adminName + " | Campus Lost, Found & Property Recovery System");
        subtitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        titleBox.getChildren().addAll(heading, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        welcomeRow.getChildren().add(titleBox);

        HBox navBar = new HBox(10);
        navBar.setAlignment(Pos.CENTER_LEFT);

        Button navSearch = new Button("Search & Browse Reports");
        navSearch.getStyleClass().add("btn-secondary");
        navSearch.setOnAction(e -> ViewNavigator.showSearch());

        Button navMatches = new Button("Candidate Matches");
        navMatches.getStyleClass().add("btn-secondary");
        navMatches.setOnAction(e -> ViewNavigator.showMatches());

        Button navClaims = new Button("All Claims");
        navClaims.getStyleClass().add("btn-secondary");
        navClaims.setOnAction(e -> ViewNavigator.showClaims());

        Button navReport = new Button("+ Report Lost/Found");
        navReport.getStyleClass().add("btn-primary");
        navReport.setOnAction(e -> ViewNavigator.showReportItem());

        Button refreshAllBtn = new Button("Refresh Overview");
        refreshAllBtn.getStyleClass().add("btn-secondary");
        refreshAllBtn.setOnAction(e -> refreshData());

        navBar.getChildren().addAll(navSearch, navMatches, navClaims, navReport, refreshAllBtn);
        topCard.getChildren().addAll(welcomeRow, navBar);

        HBox statsRow1 = new HBox(12);
        statsRow1.setAlignment(Pos.CENTER);

        VBox cardLost = createStatCard("TOTAL LOST ITEMS", totalLostStat = new Label("0"), "#dc2626");
        VBox cardFound = createStatCard("TOTAL FOUND ITEMS", totalFoundStat = new Label("0"), "#059669");
        VBox cardActive = createStatCard("ACTIVE REPORTS", activeReportsStat = new Label("0"), "#2563eb");
        VBox cardRecovered = createStatCard("RECOVERED ITEMS", recoveredItemsStat = new Label("0"), "#047857");
        VBox cardPendingClaims = createStatCard("PENDING CLAIMS", pendingClaimsStat = new Label("0"), "#d97706");
        VBox cardApprovedClaims = createStatCard("APPROVED CLAIMS", approvedClaimsStat = new Label("0"), "#16a34a");
        VBox cardRate = createStatCard("RECOVERY RATE", recoveryRateStat = new Label("0.0%"), "#7c3aed");

        HBox.setHgrow(cardLost, Priority.ALWAYS);
        HBox.setHgrow(cardFound, Priority.ALWAYS);
        HBox.setHgrow(cardActive, Priority.ALWAYS);
        HBox.setHgrow(cardRecovered, Priority.ALWAYS);
        HBox.setHgrow(cardPendingClaims, Priority.ALWAYS);
        HBox.setHgrow(cardApprovedClaims, Priority.ALWAYS);
        HBox.setHgrow(cardRate, Priority.ALWAYS);

        statsRow1.getChildren().addAll(cardLost, cardFound, cardActive, cardRecovered, cardPendingClaims, cardApprovedClaims, cardRate);

        HBox analyticsSection = new HBox(16);

        VBox locationsCard = new VBox(10);
        locationsCard.getStyleClass().add("card");
        HBox.setHgrow(locationsCard, Priority.ALWAYS);

        Label locTitle = new Label("Campus Hotspots (Most Reported Locations)");
        locTitle.getStyleClass().add("card-title");
        Label locSub = new Label("Aggregated via SQL JOIN and GROUP BY on location incidents.");
        locSub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        locationAnalyticsTable = new TableView<>();
        locationAnalyticsTable.setPlaceholder(new Label("No location data available."));
        locationAnalyticsTable.setPrefHeight(180);

        TableColumn<AdminAnalytics.LocationStat, String> locNameCol = new TableColumn<>("Location");
        locNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().locationName()));
        locNameCol.setPrefWidth(140);

        TableColumn<AdminAnalytics.LocationStat, String> locBldgCol = new TableColumn<>("Building");
        locBldgCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().building()));
        locBldgCol.setPrefWidth(120);

        TableColumn<AdminAnalytics.LocationStat, Number> locTotalCol = new TableColumn<>("Total Incidents");
        locTotalCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().totalReports()));
        locTotalCol.setPrefWidth(100);

        TableColumn<AdminAnalytics.LocationStat, Number> locLostCol = new TableColumn<>("Lost");
        locLostCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().lostCount()));
        locLostCol.setPrefWidth(60);

        TableColumn<AdminAnalytics.LocationStat, Number> locFoundCol = new TableColumn<>("Found");
        locFoundCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().foundCount()));
        locFoundCol.setPrefWidth(60);

        locationAnalyticsTable.getColumns().addAll(List.of(locNameCol, locBldgCol, locTotalCol, locLostCol, locFoundCol));
        locationsCard.getChildren().addAll(locTitle, locSub, locationAnalyticsTable);

        VBox categoriesCard = new VBox(10);
        categoriesCard.getStyleClass().add("card");
        HBox.setHgrow(categoriesCard, Priority.ALWAYS);

        Label catTitle = new Label("Common Categories (LOST vs. FOUND Counts)");
        catTitle.getStyleClass().add("card-title");
        Label catSub = new Label("Aggregated via SQL GROUP BY and conditional summation.");
        catSub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        categoryAnalyticsTable = new TableView<>();
        categoryAnalyticsTable.setPlaceholder(new Label("No category data available."));
        categoryAnalyticsTable.setPrefHeight(180);

        TableColumn<AdminAnalytics.CategoryStat, String> catNameCol = new TableColumn<>("Category");
        catNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().category()));
        catNameCol.setPrefWidth(150);

        TableColumn<AdminAnalytics.CategoryStat, Number> catTotalCol = new TableColumn<>("Total Reports");
        catTotalCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().totalReports()));
        catTotalCol.setPrefWidth(100);

        TableColumn<AdminAnalytics.CategoryStat, Number> catLostCol = new TableColumn<>("Lost");
        catLostCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().lostCount()));
        catLostCol.setPrefWidth(70);

        TableColumn<AdminAnalytics.CategoryStat, Number> catFoundCol = new TableColumn<>("Found");
        catFoundCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().foundCount()));
        catFoundCol.setPrefWidth(70);

        categoryAnalyticsTable.getColumns().addAll(List.of(catNameCol, catTotalCol, catLostCol, catFoundCol));
        categoriesCard.getChildren().addAll(catTitle, catSub, categoryAnalyticsTable);

        analyticsSection.getChildren().addAll(locationsCard, categoriesCard);

        VBox pendingClaimsCard = new VBox(10);
        pendingClaimsCard.getStyleClass().add("card");

        Label queueTitle = new Label("Pending Claims Verification Queue (Atomic Transactional Resolution)");
        queueTitle.getStyleClass().add("card-title");
        Label queueDesc = new Label("Approval executes an atomic JDBC transaction: locks rows, validates states, marks claim APPROVED, marks target item RECOVERED, and confirms related matches.");
        queueDesc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        pendingClaimsTable = new TableView<>();
        pendingClaimsTable.setPlaceholder(new Label("No pending claims requiring review at this time."));
        pendingClaimsTable.setPrefHeight(160);

        TableColumn<Claim, String> pIdCol = new TableColumn<>("Claim ID");
        pIdCol.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getClaimId()));
        pIdCol.setPrefWidth(80);

        TableColumn<Claim, String> pItemCol = new TableColumn<>("Found Item Target");
        pItemCol.setCellValueFactory(data -> {
            int itemId = data.getValue().getItemId();
            Optional<Item> item = ViewNavigator.getItemService().getItemById(itemId);
            return new SimpleStringProperty(item.map(i -> "#" + i.getItemId() + " " + i.getName()).orElse("Item #" + itemId));
        });
        pItemCol.setPrefWidth(220);

        TableColumn<Claim, String> pClaimantCol = new TableColumn<>("Claimant Student");
        pClaimantCol.setCellValueFactory(data -> {
            int uid = data.getValue().getUserId();
            Optional<User> u = ViewNavigator.getAuthService().getUserById(uid);
            return new SimpleStringProperty(u.map(User::getName).orElse("Student #" + uid));
        });
        pClaimantCol.setPrefWidth(180);

        TableColumn<Claim, String> pDateCol = new TableColumn<>("Date Submitted");
        pDateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getClaimDate() != null ? data.getValue().getClaimDate().format(DATE_FORMATTER) : ""));
        pDateCol.setPrefWidth(140);

        pendingClaimsTable.getColumns().addAll(List.of(pIdCol, pItemCol, pClaimantCol, pDateCol));

        VBox inspectorBox = new VBox(6);
        inspectorBox.setStyle("-fx-background-color: #f8fafc; -fx-padding: 10px; -fx-border-color: #e2e8f0; -fx-border-radius: 6px;");

        claimInspectorMetaLbl = new Label("Select a pending claim above to inspect proof details.");
        claimInspectorMetaLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");

        claimProofInspector = new TextArea();
        claimProofInspector.setEditable(false);
        claimProofInspector.setWrapText(true);
        claimProofInspector.setPrefRowCount(2);
        claimProofInspector.setPromptText("Claimant verification proof will appear here...");

        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        approveBtn = new Button("Approve Claim & Mark Item Recovered (Commit Transaction)");
        approveBtn.getStyleClass().add("btn-success");
        approveBtn.setDisable(true);
        approveBtn.setOnAction(e -> handleClaimDecision(true));

        rejectBtn = new Button("Reject Claim");
        rejectBtn.getStyleClass().add("btn-danger");
        rejectBtn.setDisable(true);
        rejectBtn.setOnAction(e -> handleClaimDecision(false));

        adminActionFeedback = new Label();
        adminActionFeedback.setWrapText(true);

        actionRow.getChildren().addAll(approveBtn, rejectBtn, adminActionFeedback);
        inspectorBox.getChildren().addAll(claimInspectorMetaLbl, claimProofInspector, actionRow);

        pendingClaimsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                claimInspectorMetaLbl.setText("Select a pending claim above to inspect proof details.");
                claimProofInspector.setText("");
                approveBtn.setDisable(true);
                rejectBtn.setDisable(true);
            } else {
                Optional<Item> item = ViewNavigator.getItemService().getItemById(newVal.getItemId());
                Optional<User> claimant = ViewNavigator.getAuthService().getUserById(newVal.getUserId());
                claimInspectorMetaLbl.setText("Reviewing Claim #" + newVal.getClaimId() + " for Item: "
                        + item.map(Item::getName).orElse("Item #" + newVal.getItemId())
                        + " | Claimant: " + claimant.map(User::getName).orElse("User #" + newVal.getUserId()));
                claimProofInspector.setText(newVal.getVerificationDetails());
                approveBtn.setDisable(false);
                rejectBtn.setDisable(false);
                adminActionFeedback.setText("");
            }
        });

        pendingClaimsCard.getChildren().addAll(queueTitle, queueDesc, pendingClaimsTable, inspectorBox);

        VBox recentReportsCard = new VBox(10);
        recentReportsCard.getStyleClass().add("card");
        VBox.setVgrow(recentReportsCard, Priority.ALWAYS);

        Label reportsTitle = new Label("All Campus Reports & Lifecycle Status Monitor");
        reportsTitle.getStyleClass().add("card-title");

        recentItemsTable = new TableView<>();
        recentItemsTable.setPlaceholder(new Label("No items found in system database."));
        recentItemsTable.setPrefHeight(200);

        TableColumn<Item, String> rIdCol = new TableColumn<>("ID");
        rIdCol.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getItemId()));
        rIdCol.setPrefWidth(60);

        TableColumn<Item, String> rTypeCol = new TableColumn<>("Type");
        rTypeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getItemType().name()));
        rTypeCol.setPrefWidth(80);
        rTypeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(type);
                    badge.getStyleClass().add("LOST".equalsIgnoreCase(type) ? "role-tag-admin" : "role-tag-student");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Item, String> rNameCol = new TableColumn<>("Item Name");
        rNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        rNameCol.setPrefWidth(180);

        TableColumn<Item, String> rCatCol = new TableColumn<>("Category");
        rCatCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        rCatCol.setPrefWidth(120);

        TableColumn<Item, String> rDateCol = new TableColumn<>("Date");
        rDateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getItemDate() != null ? data.getValue().getItemDate().toString() : ""));
        rDateCol.setPrefWidth(110);

        TableColumn<Item, String> rLocCol = new TableColumn<>("Location");
        rLocCol.setCellValueFactory(data -> {
            Optional<Location> loc = ViewNavigator.getLocationDAO().findById(data.getValue().getLocationId());
            return new SimpleStringProperty(loc.map(Location::getLocationName).orElse("ID " + data.getValue().getLocationId()));
        });
        rLocCol.setPrefWidth(150);

        TableColumn<Item, String> rStatusCol = new TableColumn<>("Status");
        rStatusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        rStatusCol.setPrefWidth(120);
        rStatusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(status);
                    if ("ACTIVE".equalsIgnoreCase(status)) {
                        badge.getStyleClass().add("badge-active");
                    } else if ("MATCHED".equalsIgnoreCase(status)) {
                        badge.getStyleClass().add("badge-matched");
                    } else if ("CLAIMED".equalsIgnoreCase(status)) {
                        badge.getStyleClass().add("badge-claimed");
                    } else if ("RECOVERED".equalsIgnoreCase(status)) {
                        badge.getStyleClass().add("badge-recovered");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        recentItemsTable.getColumns().addAll(List.of(rIdCol, rTypeCol, rNameCol, rCatCol, rDateCol, rLocCol, rStatusCol));
        recentReportsCard.getChildren().addAll(reportsTitle, recentItemsTable);

        root.getChildren().addAll(topCard, statsRow1, analyticsSection, pendingClaimsCard, recentReportsCard);

        refreshData();
    }

    private VBox createStatCard(String label, Label numberLbl, String colorHex) {
        VBox card = new VBox(4);
        card.getStyleClass().add("stat-card");
        numberLbl.getStyleClass().add("stat-number");
        numberLbl.setStyle("-fx-text-fill: " + colorHex + ";");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("stat-label");
        card.getChildren().addAll(numberLbl, lbl);
        return card;
    }

    private void handleClaimDecision(boolean approve) {
        Claim selected = pendingClaimsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        User admin = SessionManager.getCurrentUser();
        if (admin == null || !admin.isAdmin()) {
            adminActionFeedback.setText("Only authenticated administrators can review claims.");
            adminActionFeedback.getStyleClass().setAll("error-label");
            return;
        }

        try {
            if (approve) {
                boolean success = ViewNavigator.getClaimService().approveClaim(selected.getClaimId(), admin.getUserId());
                if (success) {
                    adminActionFeedback.setText("Claim #" + selected.getClaimId() + " successfully APPROVED. Target item marked RECOVERED via JDBC transaction.");
                    adminActionFeedback.getStyleClass().setAll("success-label");
                }
            } else {
                boolean success = ViewNavigator.getClaimService().rejectClaim(selected.getClaimId(), admin.getUserId());
                if (success) {
                    adminActionFeedback.setText("Claim #" + selected.getClaimId() + " REJECTED.");
                    adminActionFeedback.getStyleClass().setAll("error-label");
                }
            }
            refreshData();
        } catch (Exception e) {
            adminActionFeedback.setText("Action failed: " + e.getMessage());
            adminActionFeedback.getStyleClass().setAll("error-label");
        }
    }

    private void refreshData() {
        try {
            AdminAnalytics analytics = ViewNavigator.getAnalyticsService().getSystemAnalytics();

            totalLostStat.setText(String.valueOf(analytics.getTotalLostItems()));
            totalFoundStat.setText(String.valueOf(analytics.getTotalFoundItems()));
            activeReportsStat.setText(String.valueOf(analytics.getActiveReports()));
            recoveredItemsStat.setText(String.valueOf(analytics.getRecoveredItems()));
            pendingClaimsStat.setText(String.valueOf(analytics.getPendingClaims()));
            approvedClaimsStat.setText(String.valueOf(analytics.getApprovedClaims()));
            recoveryRateStat.setText(String.format("%.1f%%", analytics.getRecoveryRatePercentage()));

            locationAnalyticsTable.setItems(FXCollections.observableArrayList(analytics.getTopLocations()));
            categoryAnalyticsTable.setItems(FXCollections.observableArrayList(analytics.getTopCategories()));

            List<Claim> pendingClaims = ViewNavigator.getClaimService().getPendingClaims();
            List<Item> allItems = ViewNavigator.getItemService().getAllItems();

            pendingClaimsTable.setItems(FXCollections.observableArrayList(pendingClaims));
            recentItemsTable.setItems(FXCollections.observableArrayList(allItems));
        } catch (Exception e) {
            System.err.println("Error refreshing admin dashboard data: " + e.getMessage());
        }
    }

    public Parent getRoot() {
        return scrollPane;
    }
}
