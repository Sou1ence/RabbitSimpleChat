package com.apokalist.telegram_mini;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        System.out.println("Starting application...");
        try {
            LoginDialog loginDialog = new LoginDialog();
            System.out.println("Showing login dialog...");
            loginDialog.showAndWait().ifPresent(result -> {
                String nickname = result.getKey();
                String roomName = result.getValue();
                System.out.println("Nickname: " + nickname + ", Room: " + roomName)ear
                if (nickname.isEmpty() || roomName.isEmpty()) {
                    LoginDialog.showError("Nickname and room cannot be empty!");
                    return;
                }

                ChatClient client = new ChatClient(nickname, roomName, null);
                ChatUI ui = new ChatUI(client, primaryStage);
                client.setUI(ui);
                client.connect();
                System.out.println("Connected to RabbitMQ, showing UI...");
                ui.show();

                ui.getRoomManager().announceRoom(roomName);
            });
        } catch (Exception e) {
            e.printStackTrace();
            LoginDialog.showError("Startup error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
