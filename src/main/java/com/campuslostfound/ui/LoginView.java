package com.campuslostfound.ui;

import com.campuslostfound.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class LoginView {

    private final StackPane root;

    public LoginView() {
        root = new StackPane();
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));

        VBox card = new VBox(16);
        card.getStyleClass().add("card");
        card.setMaxWidth(420);
        card.setAlignment(Pos.TOP_LEFT);

        Label headerTitle = new Label("Campus Lost & Found");
        headerTitle.getStyleClass().add("card-title");
        headerTitle.setStyle("-fx-font-size: 20px; -fx-text-fill: #1e3a8a;");

        Label headerSub = new Label("Sign in with your campus email account");
        headerSub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        Label emailLbl = new Label("Campus Email");
        emailLbl.getStyleClass().add("form-label");
        TextField emailField = new TextField();
        emailField.setPromptText("e.g. alice@campus.edu or admin@campus.edu");

        Label pwdLbl = new Label("Password");
        pwdLbl.getStyleClass().add("form-label");
        PasswordField pwdField = new PasswordField();
        pwdField.setPromptText("Enter your password");

        Label errorLbl = new Label();
        errorLbl.getStyleClass().add("error-label");
        errorLbl.setVisible(false);
        errorLbl.setWrapText(true);

        Button loginBtn = new Button("Sign In");
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        HBox registerLinkBox = new HBox(5);
        registerLinkBox.setAlignment(Pos.CENTER);
        Label noAccLbl = new Label("Don't have an account?");
        noAccLbl.setStyle("-fx-text-fill: #64748b;");
        Hyperlink registerLink = new Hyperlink("Register here");
        registerLink.setOnAction(e -> ViewNavigator.showRegister());
        registerLinkBox.getChildren().addAll(noAccLbl, registerLink);

        VBox devHintBox = new VBox(4);
        devHintBox.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 6px; -fx-padding: 10px;");
        Label devTitle = new Label("Local Development Accounts (DevPass@123):");
        devTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #334155;");
        Label devAdmin = new Label("• Admin: admin@campus.edu");
        devAdmin.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        Label devStudent = new Label("• Student: alice@campus.edu");
        devStudent.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        devHintBox.getChildren().addAll(devTitle, devAdmin, devStudent);

        loginBtn.setOnAction(e -> {
            String email = emailField.getText();
            String pwd = pwdField.getText();

            if (email == null || email.trim().isEmpty() || pwd == null || pwd.trim().isEmpty()) {
                errorLbl.setText("Please enter both email and password.");
                errorLbl.setVisible(true);
                return;
            }

            try {
                User authenticated = ViewNavigator.getAuthService().authenticate(email.trim(), pwd);
                SessionManager.login(authenticated);

                if (authenticated.isAdmin()) {
                    ViewNavigator.showAdminDashboard();
                } else {
                    ViewNavigator.showStudentDashboard();
                }
            } catch (Exception ex) {
                errorLbl.setText(ex.getMessage());
                errorLbl.setVisible(true);
            }
        });

        card.getChildren().addAll(
                headerTitle, headerSub,
                emailLbl, emailField,
                pwdLbl, pwdField,
                errorLbl,
                loginBtn,
                registerLinkBox,
                devHintBox
        );

        root.getChildren().add(card);
    }

    public Parent getRoot() {
        return root;
    }
}
