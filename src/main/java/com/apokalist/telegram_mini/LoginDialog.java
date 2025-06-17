package com.apokalist.telegram_mini;

import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

import java.util.Optional;

public class LoginDialog {
    private final Dialog<Pair<String, String>> dialog;

    public LoginDialog() {
        dialog = new Dialog<>();
        dialog.setTitle("Вход в чат");
        dialog.setHeaderText("Введите ник и название комнаты");

        // Настройка GridPane
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // Поля ввода и метки
        TextField nicknameField = new TextField();
        nicknameField.setPromptText("Введите ник...");
        TextField roomField = new TextField();
        roomField.setPromptText("Введите комнату...");
        grid.add(new Label("Ник:"), 0, 0);
        grid.add(nicknameField, 1, 0);
        grid.add(new Label("Комната:"), 0, 1);
        grid.add(roomField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        ButtonType okButton = new ButtonType("Войти", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == okButton) {
                return new Pair<>(nicknameField.getText(), roomField.getText());
            }
            return null;
        });

        // Подключение CSS
        try {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Failed to load styles.css: " + e.getMessage());
        }
    }

    public Optional<Pair<String, String>> showAndWait() {
        return dialog.showAndWait();
    }

    public static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        try {
            alert.getDialogPane().getStylesheets().add(LoginDialog.class.getResource("/style.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Failed to load styles.css: " + e.getMessage());
        }
        alert.showAndWait();
    }
}
