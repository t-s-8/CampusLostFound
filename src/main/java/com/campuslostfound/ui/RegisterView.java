package com.campuslostfound.ui;

import com.campuslostfound.model.User;
import com.campuslostfound.model.UserRole;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class RegisterView {

    private final StackPane root;

    public RegisterView() {
        root = new StackPane();
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));

        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setMaxWidth(440);
        card.setAlignment(Pos.TOP_LEFT);

        Label headerTitle = new Label("Create an Account");
        headerTitle.getStyleClass().add("card-title");
        headerTitle.setStyle("-fx-font-size: 20px; -fx-text-fill: #1e3a8a;");

        Label headerSub = new Label("Register to report items, search, and file claims");
        headerSub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        Label nameLbl = new Label("Full Name");
        nameLbl.getStyleClass().add("form-label");
        TextField nameField = new TextField();
        nameField.setPromptText("e.g. John Doe");

        Label emailLbl = new Label("Campus Email Address");
        emailLbl.getStyleClass().add("form-label");
        TextField emailField = new TextField();
        emailField.setPromptText("e.g. john@campus.edu");

        Label pwdLbl = new Label("Password (minimum 6 characters)");
        pwdLbl.getStyleClass().add("form-label");
        PasswordField pwdField = new PasswordField();
        pwdField.setPromptText("Create a password");

        Label roleLbl = new Label("Account Role");
        roleLbl.getStyleClass().add("form-label");
        ComboBox<UserRole> roleCombo = new ComboBox<>(FXCollections.observableArrayList(UserRole.STUDENT, UserRole.ADMIN));
        roleCombo.setValue(UserRole.STUDENT);
        roleCombo.setMaxWidth(Double.MAX_VALUE);

        Label messageLbl = new Label();
        messageLbl.getStyleClass().add("error-label");
        messageLbl.setVisible(false);
        messageLbl.setWrapText(true);

        Button registerBtn = new Button("Register Account");
        registerBtn.getStyleClass().add("btn-primary");
        registerBtn.setMaxWidth(Double.MAX_VALUE);

        HBox backLinkBox = new HBox(5);
        backLinkBox.setAlignment(Pos.CENTER);
        Label haveAccLbl = new Label("Already registered?");
        haveAccLbl.setStyle("-fx-text-fill: #64748b;");
        Hyperlink loginLink = new Hyperlink("Sign In here");
        loginLink.setOnAction(e -> ViewNavigator.showLogin());
        backLinkBox.getChildren().addAll(haveAccLbl, loginLink);

        registerBtn.setOnAction(e -> {
            String name = nameField.getText();
            String email = emailField.getText();
            String pwd = pwdField.getText();
            UserRole role = roleCombo.getValue();

            try {
                User registered = ViewNavigator.getAuthService().register(name, email, pwd, role);
                SessionManager.login(registered);

                if (registered.isAdmin()) {
                    ViewNavigator.showAdminDashboard();
                } else {
                    ViewNavigator.showStudentDashboard();
                }
            } catch (Exception ex) {
                messageLbl.setText(ex.getMessage());
                messageLbl.setVisible(true);
            }
        });

        card.getChildren().addAll(
                headerTitle, headerSub,
                nameLbl, nameField,
                emailLbl, emailField,
                pwdLbl, pwdField,
                roleLbl, roleCombo,
                messageLbl,
                registerBtn,
                backLinkBox
        );

        root.getChildren().add(card);
    }

    public Parent getRoot() {
        return root;
    }
}
