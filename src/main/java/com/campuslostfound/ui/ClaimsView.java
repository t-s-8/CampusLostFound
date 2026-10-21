package com.campuslostfound.ui;

import com.campuslostfound.exception.InvalidClaimException;
import com.campuslostfound.model.Claim;
import com.campuslostfound.model.ClaimStatus;
import com.campuslostfound.model.Item;
import com.campuslostfound.model.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Screen 7: Claims View
 * Handles ownership claim submission for found items, display of claim statuses,
 * and administrative verification/resolution.
 */
public class ClaimsView {

    private final VBox root;
    private final TableView<Claim> claimsTable;
    private final TextField itemIdField;
    private final ComboBox<Item> foundItemDropdown;
    private final TextArea proofArea;
    private final Label feedbackLabel;

    // Details panel components
    private final Label detailClaimIdLbl;
    private final Label detailItemLbl;
    private final Label detailStatusLbl;
    private final Label detailDateLbl;
    private final Label detailReviewerLbl;
    private final TextArea detailProofText;
    private final HBox adminActionBox;
    private final Button approveBtn;
    private final Button rejectBtn;
    private final Label adminActionFeedback;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ClaimsView() {
        root = new VBox(16);
        root.getStyleClass().add("content-container");

        // 1. Informative Header Card
        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label headerTitle = new Label("Item Ownership Claims");
        headerTitle.getStyleClass().add("card-title");
        Label headerSub = new Label("If a found item belongs to you, submit an ownership claim with identifying proof. Administrators review submitted claims before handing over property.");
        headerSub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        headerCard.getChildren().addAll(headerTitle, headerSub);

        // 2. Main Content Split: Submit Form (Left) & Claims Table / Inspector (Right)
        HBox mainLayout = new HBox(16);
        VBox.setVgrow(mainLayout, Priority.ALWAYS);

        // ----------------- LEFT: Submission Form -----------------
        VBox submitCard = new VBox(12);
        submitCard.getStyleClass().add("card");
        submitCard.setPrefWidth(380);
        submitCard.setMinWidth(340);

        Label formTitle = new Label("Submit Ownership Claim");
        formTitle.getStyleClass().add("card-title");

        Label formDesc = new Label("Select or enter the Found Item ID you are claiming.");
        formDesc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        Label itemSelectLbl = new Label("Select Available Found Item:");
        itemSelectLbl.getStyleClass().add("form-label");

        foundItemDropdown = new ComboBox<>();
        foundItemDropdown.setMaxWidth(Double.MAX_VALUE);
        foundItemDropdown.setPromptText("Choose from active found items...");
        configureFoundItemDropdown();

        Label idLabel = new Label("Or Enter Found Item ID Directly:");
        idLabel.getStyleClass().add("form-label");
        itemIdField = new TextField();
        itemIdField.setPromptText("e.g. 2");

        foundItemDropdown.setOnAction(e -> {
            Item selected = foundItemDropdown.getValue();
            if (selected != null) {
                itemIdField.setText(String.valueOf(selected.getItemId()));
            }
        });

        Label proofLabel = new Label("Ownership Proof & Verification Details *:");
        proofLabel.getStyleClass().add("form-label");
        proofArea = new TextArea();
        proofArea.setPromptText("Describe unique markings, scratches, screen locks, serial numbers, or contents not in the public report...");
        proofArea.setPrefRowCount(5);
        proofArea.setWrapText(true);

        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);

        Button submitBtn = new Button("Submit Claim for Review");
        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setOnAction(e -> handleSubmitClaim());

        submitCard.getChildren().addAll(
                formTitle, formDesc,
                itemSelectLbl, foundItemDropdown,
                idLabel, itemIdField,
                proofLabel, proofArea,
                feedbackLabel, submitBtn
        );

        // ----------------- RIGHT: Claims Table & Inspection -----------------
        VBox rightCard = new VBox(12);
        rightCard.getStyleClass().add("card");
        HBox.setHgrow(rightCard, Priority.ALWAYS);

        HBox tableHeader = new HBox(12);
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        Label tableTitle = new Label(SessionManager.isAdmin() ? "Campus Claims Review Queue" : "My Submitted Claims");
        tableTitle.getStyleClass().add("card-title");
        HBox.setHgrow(tableTitle, Priority.ALWAYS);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> refreshClaims());
        tableHeader.getChildren().addAll(tableTitle, refreshBtn);

        claimsTable = new TableView<>();
        claimsTable.setPlaceholder(new Label("No claims found."));
        VBox.setVgrow(claimsTable, Priority.ALWAYS);

        TableColumn<Claim, String> idCol = new TableColumn<>("Claim ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getClaimId()));
        idCol.setPrefWidth(80);

        TableColumn<Claim, String> itemCol = new TableColumn<>("Item Claimed");
        itemCol.setCellValueFactory(data -> {
            int itemId = data.getValue().getItemId();
            Optional<Item> item = ViewNavigator.getItemService().getItemById(itemId);
            return new SimpleStringProperty(item.map(i -> "#" + i.getItemId() + " - " + i.getName()).orElse("Item #" + itemId));
        });
        itemCol.setPrefWidth(200);

        TableColumn<Claim, String> dateCol = new TableColumn<>("Claim Date");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getClaimDate() != null ? data.getValue().getClaimDate().format(DATE_FORMATTER) : ""));
        dateCol.setPrefWidth(140);

        TableColumn<Claim, String> statusCol = new TableColumn<>("Claim Status");
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
                    if ("PENDING".equalsIgnoreCase(status)) {
                        badge.getStyleClass().add("badge-pending");
                    } else if ("APPROVED".equalsIgnoreCase(status)) {
                        badge.getStyleClass().add("badge-approved");
                    } else if ("REJECTED".equalsIgnoreCase(status)) {
                        badge.getStyleClass().add("badge-rejected");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        claimsTable.getColumns().addAll(List.of(idCol, itemCol, dateCol, statusCol));

        // Details / Inspector Box below table
        VBox inspectorBox = new VBox(8);
        inspectorBox.setStyle("-fx-background-color: #f8fafc; -fx-padding: 12px; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px;");

        Label inspectorTitle = new Label("Claim Details & Verification Inspector");
        inspectorTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");

        detailClaimIdLbl = new Label("Claim: None selected");
        detailItemLbl = new Label("Item: -");
        detailStatusLbl = new Label("Status: -");
        detailDateLbl = new Label("Date: -");
        detailReviewerLbl = new Label("Reviewer: -");

        detailProofText = new TextArea();
        detailProofText.setEditable(false);
        detailProofText.setWrapText(true);
        detailProofText.setPrefRowCount(3);
        detailProofText.setPromptText("Select a claim from the table to view the verification proof details...");

        adminActionFeedback = new Label();
        adminActionFeedback.setWrapText(true);

        adminActionBox = new HBox(10);
        adminActionBox.setAlignment(Pos.CENTER_LEFT);
        approveBtn = new Button("Approve Claim (Mark Recovered)");
        approveBtn.getStyleClass().add("btn-success");
        rejectBtn = new Button("Reject Claim");
        rejectBtn.getStyleClass().add("btn-danger");
        adminActionBox.getChildren().addAll(approveBtn, rejectBtn, adminActionFeedback);
        adminActionBox.setVisible(false);

        approveBtn.setOnAction(e -> handleReviewClaim(true));
        rejectBtn.setOnAction(e -> handleReviewClaim(false));

        HBox metaRow1 = new HBox(20, detailClaimIdLbl, detailItemLbl, detailStatusLbl);
        HBox metaRow2 = new HBox(20, detailDateLbl, detailReviewerLbl);

        inspectorBox.getChildren().addAll(inspectorTitle, metaRow1, metaRow2, new Label("Verification Proof:"), detailProofText, adminActionBox);

        claimsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> showClaimDetails(newVal));

        rightCard.getChildren().addAll(tableHeader, claimsTable, inspectorBox);
        mainLayout.getChildren().addAll(submitCard, rightCard);

        root.getChildren().addAll(headerCard, mainLayout);

        // Initial Data Load
        refreshClaims();
        loadFoundItems();
    }

    private void configureFoundItemDropdown() {
        foundItemDropdown.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "#" + item.getItemId() + " - " + item.getName() + " (" + item.getCategory() + ")");
            }
        });
        foundItemDropdown.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "#" + item.getItemId() + " - " + item.getName());
            }
        });
    }

    private void loadFoundItems() {
        try {
            List<Item> foundItems = ViewNavigator.getItemService().getActiveFoundItems();
            foundItemDropdown.setItems(FXCollections.observableArrayList(foundItems));
        } catch (Exception e) {
            System.err.println("Could not load found items for dropdown: " + e.getMessage());
        }
    }

    private void handleSubmitClaim() {
        feedbackLabel.setText("");
        String itemIdStr = itemIdField.getText().trim();
        String proof = proofArea.getText().trim();

        if (itemIdStr.isEmpty()) {
            feedbackLabel.setText("Please enter or select a Found Item ID.");
            feedbackLabel.getStyleClass().setAll("error-label");
            return;
        }

        int itemId;
        try {
            itemId = Integer.parseInt(itemIdStr);
        } catch (NumberFormatException e) {
            feedbackLabel.setText("Item ID must be a positive integer.");
            feedbackLabel.getStyleClass().setAll("error-label");
            return;
        }

        if (proof.isEmpty()) {
            feedbackLabel.setText("Please provide verification details to prove ownership.");
            feedbackLabel.getStyleClass().setAll("error-label");
            return;
        }

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            feedbackLabel.setText("You must be logged in to submit a claim.");
            feedbackLabel.getStyleClass().setAll("error-label");
            return;
        }

        try {
            Claim claim = ViewNavigator.getClaimService().submitClaim(itemId, currentUser.getUserId(), proof);
            feedbackLabel.setText("Claim #" + claim.getClaimId() + " submitted successfully! Status: PENDING review.");
            feedbackLabel.getStyleClass().setAll("success-label");

            itemIdField.clear();
            proofArea.clear();
            foundItemDropdown.getSelectionModel().clearSelection();

            refreshClaims();
        } catch (InvalidClaimException e) {
            feedbackLabel.setText(e.getMessage());
            feedbackLabel.getStyleClass().setAll("error-label");
        } catch (Exception e) {
            feedbackLabel.setText("Error submitting claim: " + e.getMessage());
            feedbackLabel.getStyleClass().setAll("error-label");
        }
    }

    private void refreshClaims() {
        try {
            User currentUser = SessionManager.getCurrentUser();
            if (currentUser == null) {
                return;
            }

            List<Claim> claims;
            if (SessionManager.isAdmin()) {
                claims = ViewNavigator.getClaimService().getAllClaims();
            } else {
                claims = ViewNavigator.getClaimService().getClaimsForUser(currentUser.getUserId());
            }

            claimsTable.setItems(FXCollections.observableArrayList(claims));
        } catch (Exception e) {
            System.err.println("Error loading claims: " + e.getMessage());
        }
    }

    private void showClaimDetails(Claim claim) {
        if (claim == null) {
            detailClaimIdLbl.setText("Claim: None selected");
            detailItemLbl.setText("Item: -");
            detailStatusLbl.setText("Status: -");
            detailDateLbl.setText("Date: -");
            detailReviewerLbl.setText("Reviewer: -");
            detailProofText.setText("");
            adminActionBox.setVisible(false);
            return;
        }

        detailClaimIdLbl.setText("Claim ID: #" + claim.getClaimId());
        Optional<Item> item = ViewNavigator.getItemService().getItemById(claim.getItemId());
        detailItemLbl.setText("Item: " + item.map(i -> "#" + i.getItemId() + " (" + i.getName() + ")").orElse("ID " + claim.getItemId()));
        detailStatusLbl.setText("Status: " + claim.getStatus().name());
        detailDateLbl.setText("Date: " + (claim.getClaimDate() != null ? claim.getClaimDate().format(DATE_FORMATTER) : "N/A"));

        if (claim.getReviewedBy() != null && claim.getReviewedBy() > 0) {
            Optional<User> reviewer = ViewNavigator.getAuthService().getUserById(claim.getReviewedBy());
            String reviewerName = reviewer.map(User::getName).orElse("Admin #" + claim.getReviewedBy());
            detailReviewerLbl.setText("Reviewer: " + reviewerName + (claim.getReviewedAt() != null ? " on " + claim.getReviewedAt().format(DATE_FORMATTER) : ""));
        } else {
            detailReviewerLbl.setText("Reviewer: Not reviewed yet");
        }

        detailProofText.setText(claim.getVerificationDetails());

        // Show admin actions if logged in as Admin and claim is PENDING
        if (SessionManager.isAdmin() && claim.getStatus() == ClaimStatus.PENDING) {
            adminActionBox.setVisible(true);
            adminActionFeedback.setText("");
        } else {
            adminActionBox.setVisible(false);
        }
    }

    private void handleReviewClaim(boolean approve) {
        Claim selected = claimsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        User admin = SessionManager.getCurrentUser();
        if (admin == null || !admin.isAdmin()) {
            adminActionFeedback.setText("Only administrators can review claims.");
            adminActionFeedback.getStyleClass().setAll("error-label");
            return;
        }

        try {
            if (approve) {
                boolean success = ViewNavigator.getClaimService().approveClaim(selected.getClaimId(), admin.getUserId());
                if (success) {
                    adminActionFeedback.setText("Claim #" + selected.getClaimId() + " APPROVED. Item marked RECOVERED.");
                    adminActionFeedback.getStyleClass().setAll("success-label");
                }
            } else {
                boolean success = ViewNavigator.getClaimService().rejectClaim(selected.getClaimId(), admin.getUserId());
                if (success) {
                    adminActionFeedback.setText("Claim #" + selected.getClaimId() + " REJECTED.");
                    adminActionFeedback.getStyleClass().setAll("error-label");
                }
            }
            refreshClaims();
            loadFoundItems();
        } catch (Exception e) {
            adminActionFeedback.setText("Action failed: " + e.getMessage());
            adminActionFeedback.getStyleClass().setAll("error-label");
        }
    }

    public Parent getRoot() {
        return root;
    }
}
