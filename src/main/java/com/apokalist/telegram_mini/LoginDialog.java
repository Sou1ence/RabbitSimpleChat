package com.apokalist.telegram_mini;

import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

import java.util.Objects;
import java.util.Optional;

/**
 * Dialog for user login, collects nickname and room.
 */
public class LoginDialog {
    private final Dialog<Pair<String, String>> dialog; // Login dialog

    /**
     * Constructor, sets up login dialog.
     */
    public LoginDialog() {
        dialog = new Dialog<>();
        dialog.setTitle("Login to Chat");
        dialog.setHeaderText("Enter nickname and room name");

        // Set up GridPane
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // Input fields and labels
        TextField nicknameField = new TextField();
        nicknameField.setPromptText("Enter nickname...");
        TextField roomField = new TextField();
        roomField.setPromptText("Enter room...");
        grid.add(new Label("Nickname:"), 0, 0);
        grid.add(nicknameField, 1, 0);
        grid.add(new Label("Room:"), 0, 1);
        grid.add(roomField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        ButtonType okButton = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == okButton) {
                return new Pair<>(nicknameField.getText(), roomField.getText());
            }
            return null;
        });

        // Load CSS
        try {
            dialog.getDialogPane().getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
        } catch (Exception e) {
            System.err.println("Failed to load styles.css: " + e.getMessage());
        }
    }

    /**
     * Shows dialog and waits for input.
     * @return Optional pair of nickname and room
     */
    public Optional<Pair<String, String>> showAndWait() {
        return dialog.showAndWait();
    }

    /**
     * Shows error alert.
     * @param message Error message to display
     */
    public static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        try {
            alert.getDialogPane().getStylesheets().add(LoginDialog.class.getResource("/com/apokalist/telegram_mini/styles.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Failed to load styles.css: " + e.getMessage());
        }
        alert.showAndWait();
    }
}
