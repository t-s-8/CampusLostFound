package com.campuslostfound;

import com.campuslostfound.ui.ViewNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Primary JavaFX Application entrypoint.
 * Coordinates stage initialization and delegates view presentation to ViewNavigator.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        ViewNavigator.setPrimaryStage(primaryStage);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(550);

        // Display the initial Login screen
        ViewNavigator.showLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
