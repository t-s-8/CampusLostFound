package com.campuslostfound.ui;

import com.campuslostfound.model.Item;
import com.campuslostfound.model.ItemStatus;
import com.campuslostfound.model.ItemType;
import com.campuslostfound.model.Location;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;

/**
 * Screen 5: Search Items View
 * Multi-criteria search and filtering connecting to SearchService and ItemDAO.
 */
public class SearchView {

    private final VBox root;
    private final TableView<Item> table;
    private final Label detailNameLbl;
    private final Label detailDescLbl;
    private final Label detailLocLbl;
    private final Button claimActionBtn;
    private Item selectedItem;

    public SearchView() {
        root = new VBox(16);
        root.getStyleClass().add("content-container");

        // Filter Card
        VBox filterCard = new VBox(12);
        filterCard.getStyleClass().add("card");

        Label searchTitle = new Label("Search Campus Lost & Found Database");
        searchTitle.getStyleClass().add("card-title");

        HBox filterRow = new HBox(12);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        TextField keywordField = new TextField();
        keywordField.setPromptText("Search by keyword...");
        HBox.setHgrow(keywordField, Priority.ALWAYS);

        ComboBox<String> catCombo = new ComboBox<>();
        catCombo.getItems().addAll("All Categories", "Electronics", "Accessories", "ID & Cards",
                "Books & Stationery", "Personal Belongings", "Clothing", "Keys", "Other");
        catCombo.setValue("All Categories");

        ComboBox<Location> locCombo = new ComboBox<>();
        Location allLoc = new Location(0, "All Locations", null);
        locCombo.getItems().add(allLoc);
        locCombo.getItems().addAll(ViewNavigator.getLocationDAO().findAll());
        locCombo.setValue(allLoc);
        locCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Location object) {
                return object != null ? object.getFullLocation() : "";
            }

            @Override
            public Location fromString(String string) {
                return null;
            }
        });

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("All Types", "LOST", "FOUND");
        typeCombo.setValue("All Types");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("All Statuses", "ACTIVE", "MATCHED", "CLAIMED", "RECOVERED");
        statusCombo.setValue("All Statuses");

        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("btn-primary");

        Button resetBtn = new Button("Reset");
        resetBtn.getStyleClass().add("btn-secondary");

        filterRow.getChildren().addAll(keywordField, catCombo, locCombo, typeCombo, statusCombo, searchBtn, resetBtn);
        filterCard.getChildren().addAll(searchTitle, filterRow);

        // Results Table
        table = new TableView<>();
        table.setPlaceholder(new Label("No items match your search criteria."));
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Item, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getItemId())));
        idCol.setPrefWidth(50);

        TableColumn<Item, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getItemType().name()));
        typeCol.setPrefWidth(85);

        TableColumn<Item, String> nameCol = new TableColumn<>("Item Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(240);

        TableColumn<Item, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        catCol.setPrefWidth(140);

        TableColumn<Item, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getItemDate().toString()));
        dateCol.setPrefWidth(100);

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
                    badge.getStyleClass().add(StudentDashboardView.getStatusBadgeClass(status));
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        table.getColumns().addAll(List.of(idCol, typeCol, nameCol, catCol, dateCol, statusCol));

        // Details Panel Card at Bottom
        VBox detailCard = new VBox(8);
        detailCard.getStyleClass().add("card");
        detailCard.setPrefHeight(130);

        HBox detailHeader = new HBox(12);
        detailHeader.setAlignment(Pos.CENTER_LEFT);
        detailNameLbl = new Label("Select an item above to inspect detailed description");
        detailNameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e3a8a;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        claimActionBtn = new Button("Claim This Found Item");
        claimActionBtn.getStyleClass().add("btn-success");
        claimActionBtn.setVisible(false);
        claimActionBtn.setOnAction(e -> {
            if (selectedItem != null) {
                ViewNavigator.showClaims();
            }
        });

        detailHeader.getChildren().addAll(detailNameLbl, spacer, claimActionBtn);

        detailDescLbl = new Label();
        detailDescLbl.setStyle("-fx-text-fill: #334155;");
        detailDescLbl.setWrapText(true);

        detailLocLbl = new Label();
        detailLocLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        detailCard.getChildren().addAll(detailHeader, detailDescLbl, detailLocLbl);

        // Table Selection listener
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedItem = newVal;
            if (newVal != null) {
                detailNameLbl.setText("#" + newVal.getItemId() + " - " + newVal.getName() + " (" + newVal.getItemType() + ")");
                detailDescLbl.setText("Description: " + (newVal.getDescription().isEmpty() ? "No description provided." : newVal.getDescription()));
                detailLocLbl.setText("Location ID: " + newVal.getLocationId() + " | Reported Date: " + newVal.getItemDate() + " | Status: " + newVal.getStatus());

                // Show claim button if it is an active found item and user is not admin
                claimActionBtn.setVisible(newVal.getItemType() == ItemType.FOUND && newVal.isClaimable() && !SessionManager.isAdmin());
            } else {
                detailNameLbl.setText("Select an item above to inspect detailed description");
                detailDescLbl.setText("");
                detailLocLbl.setText("");
                claimActionBtn.setVisible(false);
            }
        });

        // Search action
        Runnable performSearch = () -> {
            String kw = keywordField.getText();
            String cat = "All Categories".equals(catCombo.getValue()) ? null : catCombo.getValue();
            Location loc = locCombo.getValue();
            Integer locId = (loc != null && loc.getLocationId() > 0) ? loc.getLocationId() : null;
            ItemType type = "All Types".equals(typeCombo.getValue()) ? null : ItemType.valueOf(typeCombo.getValue());
            ItemStatus status = "All Statuses".equals(statusCombo.getValue()) ? null : ItemStatus.valueOf(statusCombo.getValue());

            List<Item> results = ViewNavigator.getSearchService().search(kw, cat, locId, type, status);
            table.setItems(FXCollections.observableArrayList(results));
        };

        searchBtn.setOnAction(e -> performSearch.run());

        resetBtn.setOnAction(e -> {
            keywordField.clear();
            catCombo.setValue("All Categories");
            locCombo.setValue(allLoc);
            typeCombo.setValue("All Types");
            statusCombo.setValue("All Statuses");
            performSearch.run();
        });

        // Initial search load
        performSearch.run();

        root.getChildren().addAll(filterCard, table, detailCard);
    }

    public Parent getRoot() {
        return root;
    }
}
