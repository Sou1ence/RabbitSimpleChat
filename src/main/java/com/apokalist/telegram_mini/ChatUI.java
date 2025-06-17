package com.apokalist.telegram_mini;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.application.Platform;

import java.util.Arrays;

public class ChatUI {
    private ChatClient client;
    private final Stage stage;
    private TextArea chatArea;
    private TextField inputField;
    private ListView<String> roomList;
    private ListView<String> userList;
    private Label accountLabel;

    public ChatUI(ChatClient client, Stage stage) {
        this.client = client;
        this.stage = stage;
    }

    public void show() {
        System.out.println("Building UI...");
        BorderPane root = new BorderPane();

        // Left: Room list
        roomList = new ListView<>();
        roomList.setId("roomList");
        roomList.getItems().addAll(Arrays.asList("room1", "room2", "room3"));
        roomList.getSelectionModel().select(client.getRoomName());
        roomList.setPrefWidth(150);
        roomList.getSelectionModel().selectedItemProperty().addListener((obs, oldRoom, newRoom) -> {
            if (newRoom != null && !newRoom.equals(client.getRoomName())) {
                // Properly close old client
                client.close();

                // Wait a bit for clean shutdown
                new Thread(() -> {
                    try {
                        Thread.sleep(500); // Wait for proper shutdown
                        Platform.runLater(() -> {
                            // Create new client for new room
                            client = new ChatClient(client.getNickname(), newRoom, this);
                            this.client = client;
                            client.connect();

                            // Clear UI and update
                            chatArea.clear();
                            userList.getItems().clear();
                            accountLabel.setText("Logged in as: " + client.getNickname());
                            stage.setTitle("Chat - " + newRoom + " (" + client.getNickname() + ")");

                            // Update room selection styling
                            roomList.refresh();
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        });

        ContextMenu roomMenu = new ContextMenu();
        MenuItem addRoom = new MenuItem("Add Room");
        addRoom.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("New Room");
            dialog.setHeaderText("Enter new room name");
            dialog.showAndWait().ifPresent(roomList.getItems()::add);
        });
        roomMenu.getItems().add(addRoom);
        roomList.setContextMenu(roomMenu);

        roomList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                }
            }
        });

        VBox leftPane = new VBox(roomList);
        leftPane.setPadding(new Insets(10));
        root.setLeft(leftPane);

        // Center: Chat area
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        inputField = new TextField();
        inputField.setPromptText("Enter message...");
        inputField.setOnAction(e -> sendMessage());

        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage());

        HBox inputBox = new HBox(10, inputField, sendButton);
        inputBox.setPadding(new Insets(10));
        HBox.setHgrow(inputField, Priority.ALWAYS);

        // Right: User list
        userList = new ListView<>();
        userList.setId("userList");
        userList.setPrefWidth(150);

        ContextMenu userMenu = new ContextMenu();
        MenuItem sendPrivate = new MenuItem("Send Private Message");
        sendPrivate.setOnAction(e -> {
            String selectedUser = userList.getSelectionModel().getSelectedItem();
            if (selectedUser != null && !selectedUser.equals(client.getNickname())) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Private Message");
                dialog.setHeaderText("Message to " + selectedUser);
                dialog.showAndWait().ifPresent(msg -> client.sendPrivateMessage(selectedUser, msg));
            }
        });
        userMenu.getItems().add(sendPrivate);
        userList.setContextMenu(userMenu);

        VBox chatPane = new VBox(10, chatArea, inputBox);
        chatPane.setPadding(new Insets(10));
        VBox.setVgrow(chatArea, Priority.ALWAYS);

        HBox centerPane = new HBox(10, chatPane, userList);
        centerPane.setPadding(new Insets(10));
        HBox.setHgrow(chatPane, Priority.ALWAYS);
        root.setCenter(centerPane);

        // Bottom: Account info
        accountLabel = new Label("Logged in as: " + client.getNickname());
        accountLabel.setId("accountLabel");

        Button logoutButton = new Button("Change Account");
        logoutButton.setOnAction(e -> {
            client.close();
            stage.close();
            new Main().start(new Stage());
        });

        HBox accountBox = newategories(10, accountLabel, logoutButton);
        accountBox.setPadding(new Insets(10));
        root.setBottom(accountBox);

        Scene scene = new Scene(root, 700, 500);
        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Failed to load styles.css: " + e.getMessage());
        }

        stage.setScene(scene);
        stage.setTitle("Chat - " + client.getRoomName() + " (" + client.getNickname() + ")");
        stage.setOnCloseRequest(e -> client.close());
        System.out.println("Showing stage...");
        stage.show();

        // Focus на поле ввода
        Platform.runLater(() -> inputField.requestFocus());
    }

    private HBox newategories(int i, Label accountLabel, Button logoutButton) {
        HBox hbox = new HBox(i, accountLabel, logoutButton);
        hbox.setPadding(new Insets(10));
        return hbox;
    }

    public void appendMessage(String message) {
        Platform.runLater(() -> {
            if (message.contains(client.getNickname() + ":")) {
                chatArea.appendText("**" + message + "**\n");
            } else {
                chatArea.appendText(message + "\n");
            }
            chatArea.setScrollTop(Double.MAX_VALUE);

            // Обновление списка пользователей
            if (message.contains("joined the chat")) {
                String[] parts = message.split(" ");
                if (parts.length > 1) {
                    String user = parts[1];
                    if (!userList.getItems().contains(user) && !user.equals("System:")) {
                        userList.getItems().add(user);
                    }
                }
            } else if (message.contains("left the chat")) {
                String[] parts = message.split(" ");
                if (parts.length > 1) {
                    String user = parts[1];
                    userList.getItems().remove(user);
                }
            }

            // Notification если окно не в фокусе
            if (!stage.isFocused()) {
                notifyNewMessage();
            }
        });
    }

    private void notifyNewMessage() {
        new Thread(() -> {
            try {
                String originalTitle = stage.getTitle();
                for (int i = 0; i < 3; i++) {
                    Platform.runLater(() -> stage.setTitle("🔔 New Message! 🔔"));
                    Thread.sleep(500);
                    Platform.runLater(() -> stage.setTitle(originalTitle));
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            // Проверка на команды
            if (message.startsWith("/pm ")) {
                // Приватное сообщение: /pm username message
                String[] parts = message.substring(4).split(" ", 2);
                if (parts.length >= 2) {
                    String recipient = parts[0];
                    String privateMessage = parts[1];
                    client.sendPrivateMessage(recipient, privateMessage);
                    chatArea.appendText("[Private to " + recipient + "] " + privateMessage + "\n");
                } else {
                    chatArea.appendText("Usage: /pm <username> <message>\n");
                }
            } else if (message.startsWith("/help")) {
                // Показать справку
                showHelp();
            } else if (message.startsWith("/clear")) {
                // Очистить чат
                chatArea.clear();
            } else if (message.startsWith("/users")) {
                // Показать список пользователей
                chatArea.appendText("Users in room: " + String.join(", ", userList.getItems()) + "\n");
            } else {
                // Обычное сообщение
                client.sendMessage(message);
            }
            inputField.clear();
        }
    }

    private void showHelp() {
        StringBuilder help = new StringBuilder();
        help.append("Available commands:\n");
        help.append("/pm <username> <message> - Send private message\n");
        help.append("/clear - Clear chat area\n");
        help.append("/users - Show users in current room\n");
        help.append("/help - Show this help\n");
        help.append("Right-click on room list to add new room\n");
        help.append("Right-click on user list to send private message\n");
        chatArea.appendText(help.toString());
    }

    public void setClient(ChatClient client) {
        this.client = client;
    }
}
