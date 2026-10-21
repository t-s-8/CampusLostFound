package com.campuslostfound.ui;

import com.campuslostfound.model.Item;
import com.campuslostfound.model.Location;
import com.campuslostfound.model.User;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.List;

/**
 * Screen 4: Report Item View
 * Submits LOST or FOUND reports to ItemService, validating input and kicking off automated matching.
 */
public class ReportItemView {

    private final VBox root;

    public ReportItemView() {
        root = new VBox(20);
        root.getStyleClass().add("content-container");

        VBox card = new VBox(16);
        card.getStyleClass().add("card");
        card.setMaxWidth(680);

        Label titleLbl = new Label("Report an Item");
        titleLbl.getStyleClass().add("card-title");
        titleLbl.setStyle("-fx-font-size: 18px;");

        Label subLbl = new Label("Fill in the item details accurately to increase the probability of matching.");
        subLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        // Type selection toggle
        Label typeLbl = new Label("What are you reporting?");
        typeLbl.getStyleClass().add("form-label");

        ToggleGroup typeGroup = new ToggleGroup();
        RadioButton lostRadio = new RadioButton("I Lost an Item (Missing Property)");
        lostRadio.setToggleGroup(typeGroup);
        lostRadio.setSelected(true);

        RadioButton foundRadio = new RadioButton("I Found an Item (Turned In / Discovered)");
        foundRadio.setToggleGroup(typeGroup);

        HBox typeBox = new HBox(20, lostRadio, foundRadio);

        // Name
        Label nameLbl = new Label("Item Title / Name *");
        nameLbl.getStyleClass().add("form-label");
        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Dell XPS 15 Charger, Hydro Flask Bottle, Student ID Card");

        // Category
        Label catLbl = new Label("Category *");
        catLbl.getStyleClass().add("form-label");
        ComboBox<String> catCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Electronics", "Accessories", "ID & Cards", "Books & Stationery",
                "Personal Belongings", "Clothing", "Keys", "Other"
        ));
        catCombo.setValue("Electronics");
        catCombo.setMaxWidth(Double.MAX_VALUE);

        // Location
        Label locLbl = new Label("Campus Location *");
        locLbl.getStyleClass().add("form-label");
        ComboBox<Location> locCombo = new ComboBox<>();
        List<Location> locations = ViewNavigator.getLocationDAO().findAll();
        locCombo.setItems(FXCollections.observableArrayList(locations));
        if (!locations.isEmpty()) {
            locCombo.setValue(locations.get(0));
        }
        locCombo.setMaxWidth(Double.MAX_VALUE);
        locCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Location loc) {
                return loc != null ? loc.getFullLocation() : "";
            }

            @Override
            public Location fromString(String string) {
                return null;
            }
        });

        // Date
        Label dateLbl = new Label("Date (Lost or Found) *");
        dateLbl.getStyleClass().add("form-label");
        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setMaxWidth(Double.MAX_VALUE);

        // Description
        Label descLbl = new Label("Description & Identifying Marks");
        descLbl.getStyleClass().add("form-label");
        TextArea descArea = new TextArea();
        descArea.setPromptText("Detail color, brand, stickers, unique scratches, contents, or circumstances...");
        descArea.setPrefRowCount(3);

        // Status messages
        Label feedbackLbl = new Label();
        feedbackLbl.setWrapText(true);
        feedbackLbl.setVisible(false);

        // Submit button
        Button submitBtn = new Button("Submit Report");
        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setPrefWidth(160);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> {
            if (SessionManager.isAdmin()) {
                ViewNavigator.showAdminDashboard();
            } else {
                ViewNavigator.showStudentDashboard();
            }
        });

        HBox btnBox = new HBox(12, submitBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER_LEFT);

        submitBtn.setOnAction(e -> {
            feedbackLbl.setVisible(false);
            User currentUser = SessionManager.getCurrentUser();
            if (currentUser == null) {
                feedbackLbl.setText("Session expired. Please log in again.");
                feedbackLbl.getStyleClass().setAll("error-label");
                feedbackLbl.setVisible(true);
                return;
            }

            String name = nameField.getText();
            String category = catCombo.getValue();
            Location location = locCombo.getValue();
            LocalDate date = datePicker.getValue();
            String description = descArea.getText();
            boolean isLost = lostRadio.isSelected();

            if (location == null) {
                feedbackLbl.setText("Please select a valid campus location.");
                feedbackLbl.getStyleClass().setAll("error-label");
                feedbackLbl.setVisible(true);
                return;
            }

            try {
                Item created;
                if (isLost) {
                    created = ViewNavigator.getItemService().reportLostItem(
                            currentUser.getUserId(), location.getLocationId(), name, category, description, date
                    );
                } else {
                    created = ViewNavigator.getItemService().reportFoundItem(
                            currentUser.getUserId(), location.getLocationId(), name, category, description, date
                    );
                }

                feedbackLbl.setText("Report submitted successfully! (Item ID: #" + created.getItemId() + "). Matching algorithm executed.");
                feedbackLbl.getStyleClass().setAll("success-label");
                feedbackLbl.setVisible(true);

                // Clear input fields
                nameField.clear();
                descArea.clear();
            } catch (Exception ex) {
                feedbackLbl.setText(ex.getMessage());
                feedbackLbl.getStyleClass().setAll("error-label");
                feedbackLbl.setVisible(true);
            }
        });

        card.getChildren().addAll(
                titleLbl, subLbl,
                typeLbl, typeBox,
                nameLbl, nameField,
                catLbl, catCombo,
                locLbl, locCombo,
                dateLbl, datePicker,
                descLbl, descArea,
                feedbackLbl,
                btnBox
        );

        root.getChildren().add(card);
    }

    public Parent getRoot() {
        return root;
    }
}
