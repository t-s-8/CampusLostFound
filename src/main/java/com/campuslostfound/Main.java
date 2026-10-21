package com.campuslostfound;

/**
 * Standard launcher entrypoint for the Campus Lost & Found desktop application.
 *
 * This wrapper class forwards execution to MainApp.launch(), avoiding the JavaFX
 * runtime component checks that occur when a class directly extends javafx.application.Application.
 */
public class Main {

    public static void main(String[] args) {
        MainApp.main(args);
    }
}
