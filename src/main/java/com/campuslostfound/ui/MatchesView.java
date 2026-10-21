package com.campuslostfound.ui;

import com.campuslostfound.model.Item;
import com.campuslostfound.model.Match;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * Screen 6: Matches View
 * Displays candidate pairs ranked by MatchPriorityQueue and MatchAlgorithm.
 */
public class MatchesView {

    private final VBox root;
    private final TableView<Match> table;
    private final Label lostDetailsLbl;
    private final Label foundDetailsLbl;
    private final Label scoreExplanationLbl;

    public MatchesView() {
        root = new VBox(16);
        root.getStyleClass().add("content-container");

        // Informational Banner Card
        VBox banner = new VBox(6);
        banner.getStyleClass().add("card");
        banner.setStyle("-fx-border-color: #bfdbfe; -fx-background-color: #eff6ff;");

        Label bannerTitle = new Label("Candidate Matches (Algorithmic Ranking)");
        bannerTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e40af;");

        Label bannerText = new Label("Matches are computed locally using weighted criteria: Category (25), Location (25), Date (20), Name (20), and Description (10). A score >= 70.0% is flagged as a possible match. NOTE: A match does NOT automatically mark an item as recovered; recovery requires claim verification.");
        bannerText.setStyle("-fx-text-fill: #1e3a8a; -fx-font-size: 12px;");
        bannerText.setWrapText(true);

        banner.getChildren().addAll(bannerTitle, bannerText);

        // Matches Table
        table = new TableView<>();
        table.setPlaceholder(new Label("No match candidates found in database. Report complementary LOST and FOUND items to generate matches."));
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Match, String> idCol = new TableColumn<>("Match ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getMatchId())));
        idCol.setPrefWidth(80);

        TableColumn<Match, String> lostCol = new TableColumn<>("Lost Item (Target)");
        lostCol.setCellValueFactory(data -> {
            int lostId = data.getValue().getLostItemId();
            Optional<Item> item = ViewNavigator.getItemService().getItemById(lostId);
            return new SimpleStringProperty(item.map(i -> "#" + i.getItemId() + " " + i.getName()).orElse("Item #" + lostId));
        });
        lostCol.setPrefWidth(240);

        TableColumn<Match, String> foundCol = new TableColumn<>("Found Item (Candidate)");
        foundCol.setCellValueFactory(data -> {
            int foundId = data.getValue().getFoundItemId();
            Optional<Item> item = ViewNavigator.getItemService().getItemById(foundId);
            return new SimpleStringProperty(item.map(i -> "#" + i.getItemId() + " " + i.getName()).orElse("Item #" + foundId));
        });
        foundCol.setPrefWidth(240);

        TableColumn<Match, String> scoreCol = new TableColumn<>("Similarity Score");
        scoreCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.1f%%", data.getValue().getScore())));
        scoreCol.setPrefWidth(160);
        scoreCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String scoreStr, boolean empty) {
                super.updateItem(scoreStr, empty);
                if (empty || scoreStr == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    double score = Double.parseDouble(scoreStr.replace("%", ""));
                    Label badge = new Label(score >= 70.0 ? scoreStr + " (POSSIBLE MATCH)" : scoreStr + " (LOW)");
                    badge.getStyleClass().add(score >= 70.0 ? "badge-possible-match" : "badge-low-match");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Match, String> statusCol = new TableColumn<>("Match Status");
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
                    badge.getStyleClass().add(StudentDashboardView.getStatusBadgeClass(status));
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        table.getColumns().addAll(List.of(idCol, lostCol, foundCol, scoreCol, statusCol));

        // Side-by-Side Comparison Inspector Card
        VBox inspectorCard = new VBox(10);
        inspectorCard.getStyleClass().add("card");
        inspectorCard.setPrefHeight(150);

        Label inspectTitle = new Label("Side-by-Side Match Inspector");
        inspectTitle.getStyleClass().add("card-title");

        HBox splitBox = new HBox(20);
        VBox leftBox = new VBox(4);
        leftBox.setPrefWidth(450);
        Label lostHeader = new Label("LOST ITEM DETAILS:");
        lostHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #ea580c; -fx-font-size: 11px;");
        lostDetailsLbl = new Label("Select a match record from the table above.");
        lostDetailsLbl.setWrapText(true);
        leftBox.getChildren().addAll(lostHeader, lostDetailsLbl);

        VBox rightBox = new VBox(4);
        rightBox.setPrefWidth(450);
        Label foundHeader = new Label("FOUND ITEM DETAILS:");
        foundHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #059669; -fx-font-size: 11px;");
        foundDetailsLbl = new Label("Select a match record from the table above.");
        foundDetailsLbl.setWrapText(true);
        rightBox.getChildren().addAll(foundHeader, foundDetailsLbl);

        splitBox.getChildren().addAll(leftBox, rightBox);

        scoreExplanationLbl = new Label();
        scoreExplanationLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        inspectorCard.getChildren().addAll(inspectTitle, splitBox, scoreExplanationLbl);

        // Selection Listener
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, match) -> {
            if (match != null) {
                Optional<Item> lostOpt = ViewNavigator.getItemService().getItemById(match.getLostItemId());
                Optional<Item> foundOpt = ViewNavigator.getItemService().getItemById(match.getFoundItemId());

                lostDetailsLbl.setText(lostOpt.map(i -> i.getName() + " | Category: " + i.getCategory() + " | Date: " + i.getItemDate() + "\nDesc: " + i.getDescription())
                        .orElse("Lost item record not found."));

                foundDetailsLbl.setText(foundOpt.map(i -> i.getName() + " | Category: " + i.getCategory() + " | Date: " + i.getItemDate() + "\nDesc: " + i.getDescription())
                        .orElse("Found item record not found."));

                scoreExplanationLbl.setText("Algorithm Score: " + match.getScore() + "/100.0. " +
                        (match.isPossibleMatch() ? "High confidence possible match. Verify proof in claims." : "Score is below the 70.0% threshold."));
            } else {
                lostDetailsLbl.setText("Select a match record from the table above.");
                foundDetailsLbl.setText("Select a match record from the table above.");
                scoreExplanationLbl.setText("");
            }
        });

        // Load data
        List<Match> matches = ViewNavigator.getMatchService().getAllMatches();
        table.setItems(FXCollections.observableArrayList(matches));

        root.getChildren().addAll(banner, table, inspectorCard);
    }

    public Parent getRoot() {
        return root;
    }
}
