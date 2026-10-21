package com.campuslostfound.ui;

import com.campuslostfound.model.Item;
import com.campuslostfound.model.ItemStatus;
import com.campuslostfound.model.ItemType;
import com.campuslostfound.model.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Screen 3: Student Dashboard
 * Hub for students to launch actions (Report, Search, Matches, Claims) and inspect their activity.
 */
public class StudentDashboardView {

    private final VBox root;

    public StudentDashboardView() {
        root = new VBox(20);
        root.getStyleClass().add("content-container");

        User user = SessionManager.getCurrentUser();
        String studentName = user != null ? user.getName() : "Student";

        // Welcome banner
        VBox banner = new VBox(4);
        Label welcomeLbl = new Label("Welcome back, " + studentName + "!");
        welcomeLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label welcomeSub = new Label("Campus Lost & Found Portal — Manage your reports, search for items, and track claims.");
        welcomeSub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        banner.getChildren().addAll(welcomeLbl, welcomeSub);

        // Metrics Row
        List<Item> myItems = user != null ? ViewNavigator.getItemService().getItemsByReporter(user.getUserId()) : List.of();
        List<Item> activeLost = ViewNavigator.getItemService().getActiveLostItems();
        List<Item> activeFound = ViewNavigator.getItemService().getActiveFoundItems();

        HBox statsRow = new HBox(16);
        statsRow.getChildren().addAll(
                createStatCard("MY REPORTS", String.valueOf(myItems.size()), "#2563eb"),
                createStatCard("ACTIVE CAMPUS LOST REPORTS", String.valueOf(activeLost.size()), "#ea580c"),
                createStatCard("ACTIVE CAMPUS FOUND ITEMS", String.valueOf(activeFound.size()), "#059669")
        );

        // Action Cards Grid
        GridPane actionGrid = new GridPane();
        actionGrid.setHgap(16);
        actionGrid.setVgap(16);

        actionGrid.add(createActionCard("Report Lost or Found Item",
                "Submit a formal report for an item you misplaced or turned in on campus.",
                "Report Item", e -> ViewNavigator.showReportItem()), 0, 0);

        actionGrid.add(createActionCard("Search Campus Database",
                "Search active lost and found listings by keyword, category, date, and location.",
                "Search Items", e -> ViewNavigator.showSearch()), 1, 0);

        actionGrid.add(createActionCard("View Possible Matches",
                "Inspect algorithmic similarity matches connecting your reports with potential finds.",
                "View Matches", e -> ViewNavigator.showMatches()), 0, 1);

        actionGrid.add(createActionCard("My Ownership Claims",
                "Check the status of claims you submitted for items turned in across campus.",
                "My Claims", e -> ViewNavigator.showClaims()), 1, 1);

        // Table of User's Reports
        VBox tableCard = new VBox(12);
        tableCard.getStyleClass().add("card");

        Label tableTitle = new Label("My Reported Items");
        tableTitle.getStyleClass().add("card-title");

        TableView<Item> table = new TableView<>();
        table.setPlaceholder(new Label("You have not reported any items yet. Use 'Report Item' to create one."));
        table.setPrefHeight(220);

        TableColumn<Item, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getItemId())));
        idCol.setPrefWidth(60);

        TableColumn<Item, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getItemType().name()));
        typeCol.setPrefWidth(90);

        TableColumn<Item, String> nameCol = new TableColumn<>("Item Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(220);

        TableColumn<Item, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        catCol.setPrefWidth(140);

        TableColumn<Item, String> dateCol = new TableColumn<>("Report Date");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getItemDate().toString()));
        dateCol.setPrefWidth(110);

        TableColumn<Item, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        statusCol.setPrefWidth(120);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(status);
                    badge.getStyleClass().add(getStatusBadgeClass(status));
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        table.getColumns().addAll(List.of(idCol, typeCol, nameCol, catCol, dateCol, statusCol));
        table.setItems(FXCollections.observableArrayList(myItems));

        tableCard.getChildren().addAll(tableTitle, table);

        root.getChildren().addAll(banner, statsRow, actionGrid, tableCard);
    }

    private VBox createStatCard(String label, String value, String colorHex) {
        VBox card = new VBox(4);
        card.getStyleClass().add("stat-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label numLbl = new Label(value);
        numLbl.getStyleClass().add("stat-number");
        numLbl.setStyle("-fx-text-fill: " + colorHex + ";");

        Label nameLbl = new Label(label);
        nameLbl.getStyleClass().add("stat-label");

        card.getChildren().addAll(nameLbl, numLbl);
        return card;
    }

    private VBox createActionCard(String title, String desc, String btnText, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        GridPane.setHgrow(card, Priority.ALWAYS);
        card.setPrefWidth(500);

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("card-title");

        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        descLbl.setWrapText(true);

        Button btn = new Button(btnText);
        btn.getStyleClass().add("btn-secondary");
        btn.setOnAction(handler);

        card.getChildren().addAll(titleLbl, descLbl, btn);
        return card;
    }

    public static String getStatusBadgeClass(String status) {
        if (status == null) return "badge-active";
        return switch (status) {
            case "ACTIVE" -> "badge-active";
            case "MATCHED" -> "badge-matched";
            case "CLAIMED" -> "badge-claimed";
            case "RECOVERED", "APPROVED" -> "badge-recovered";
            case "PENDING" -> "badge-pending";
            case "REJECTED" -> "badge-rejected";
            default -> "badge-active";
        };
    }

    public Parent getRoot() {
        return root;
    }
}
