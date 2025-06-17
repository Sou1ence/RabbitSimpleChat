/**
 * @author Kostiantyn Feniuk
 */

package com.apokalist.telegram_mini;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.application.Platform;

import java.util.Objects;

/**
 * UI for the chat app, sets up JavaFX components
 */
public class ChatUI {
    private ChatClient client; // Client for RabbitMQ
    private final Stage stage; // Main window
    private TextArea chatArea; // Where messages show up
    private TextField inputField; // Input for messages
    private ListView<String> roomList; // List of rooms
    private ListView<String> userList; // List of users
    private Label accountLabel; // Shows logged-in user
    private RoomManager roomManager; // Manages room list

    /**
     * Constructor, sets up client and stage.
     * @param client Chat client
     * @param stage Main stage
     */
    public ChatUI(ChatClient client, Stage stage) {
        this.client = client;
        this.stage = stage;
        this.roomManager = new RoomManager(this);
    }

    /**
     * Shows the UI, builds all components.
     */
    public void show() {
        System.out.println("Building UI...");
        BorderPane root = new BorderPane(); // Main layout

        /// Room list on the left
        roomList = new ListView<>();
        roomList.setId("roomList"); // For CSS

        // Load rooms from manager
        roomList.getItems().addAll(roomManager.getRooms());
        roomList.getSelectionModel().select(client.getRoomName()); // Select current room
        roomList.setPrefWidth(150); // Width

        // Connect room manager
        roomManager.connect();

        // Switch room when selected
        roomList.getSelectionModel().selectedItemProperty().addListener((obs, oldRoom, newRoom) -> {
            if (newRoom != null && !newRoom.equals(client.getRoomName())) {
                client.close(); // Close old client

                // Wait a bit, then switch
                new Thread(() -> {
                    try {
                        Thread.sleep(500); // Give it time to shutdown
                        Platform.runLater(() -> {
                            client = new ChatClient(client.getNickname(), newRoom, this); // New client
                            this.client = client;
                            client.connect(); // Connect to new room

                            // Reset UI
                            chatArea.clear();
                            userList.getItems().clear();
                            accountLabel.setText("Logged in as: " + client.getNickname());
                            stage.setTitle("Chat - " + newRoom + " (" + client.getNickname() + ")");

                            roomList.refresh(); // Update list
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        });

        // Context menu for adding rooms
        ContextMenu roomMenu = new ContextMenu();
        MenuItem addRoom = new MenuItem("Add Room");
        addRoom.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("New Room");
            dialog.setHeaderText("Enter new room name");
            dialog.showAndWait().ifPresent(newRoomName -> {
                if (!newRoomName.trim().isEmpty()) {
                    roomManager.announceRoom(newRoomName.trim());
                    // Room added via callback
                }
            });
        });
        roomMenu.getItems().add(addRoom);
        roomList.setContextMenu(roomMenu);

        // Custom cell for room list
        roomList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("  " + item); // Add padding
                    if (item.equals(client.getRoomName())) {
                        setStyle("-fx-font-weight: bold;"); // Bold current room
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        VBox leftPane = new VBox(roomList);
        leftPane.setPadding(new Insets(10));
        root.setLeft(leftPane);

        /// Center: Chat area
        chatArea = new TextArea();
        chatArea.setEditable(false); // Read-only
        chatArea.setWrapText(true); // Wrap text

        inputField = new TextField();
        inputField.setPromptText("Enter message...");
        inputField.setOnAction(e -> sendMessage()); // Send on enter

        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage()); // Send on click

        HBox inputBox = new HBox(10, inputField, sendButton);
        inputBox.setPadding(new Insets(10));
        HBox.setHgrow(inputField, Priority.ALWAYS); // Stretch input field

        /// Right: User list
        userList = new ListView<>();
        userList.setId("userList"); // For CSS
        userList.setPrefWidth(150);

        // Context menu for private messages
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
        VBox.setVgrow(chatArea, Priority.ALWAYS); // Stretch chat area

        HBox centerPane = new HBox(10, chatPane, userList);
        centerPane.setPadding(new Insets(10));
        HBox.setHgrow(chatPane, Priority.ALWAYS);
        root.setCenter(centerPane);

        /// Bottom: Account info
        accountLabel = new Label("Logged in as: " + client.getNickname());
        accountLabel.setId("accountLabel"); // For CSS

        Button logoutButton = new Button("Change Account");
        logoutButton.setOnAction(e -> {
            client.close(); // Close client
            roomManager.close(); // Close room manager
            stage.close(); // Close window
            new Main().start(new Stage()); // Open login
        });

        HBox accountBox = new HBox(10, accountLabel, logoutButton);
        accountBox.setPadding(new Insets(10));
        root.setBottom(accountBox);

        // Set up scene
        Scene scene = new Scene(root, 700, 500);
        try {
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
        } catch (Exception e) {
            System.err.println("Failed to load styles.css: " + e.getMessage());
        }

        stage.setScene(scene);
        stage.setTitle("Chat - " + client.getRoomName() + " (" + client.getNickname() + ")");
        stage.setOnCloseRequest(e -> {
            client.close();
            roomManager.close();
        });
        System.out.println("Showing stage...");
        stage.show();

        // Focus input field
        Platform.runLater(() -> inputField.requestFocus());
    }

    /**
     * Adds room to list (called from RoomManager).
     * @param roomName Room name to add
     */
    public void addRoomToList(String roomName) {
        if (!roomList.getItems().contains(roomName)) {
            roomList.getItems().add(roomName);
        }
    }

    /**
     * Adds message to chat area.
     * @param message Message to display
     */
    public void appendMessage(String message) {
        Platform.runLater(() -> {
            /// Bold own messages
            if (message.contains(client.getNickname() + ":")) {
                chatArea.appendText("**" + message + "**\n");
            } else {
                chatArea.appendText(message + "\n");
            }
            chatArea.setScrollTop(Double.MAX_VALUE); // Scroll to bottom

            // Update user list
            if (message.contains("joined the chat")) {
                String[] parts = message.split(" ");
                if (parts.length > 1) {
                    String user = parts[1];
                    if (!userList.getItems().contains(user) && !user.equals("System:")) {
                        userList.getItems().add(user); // Add new user
                    }
                }
            } else if (message.contains("left the chat")) {
                String[] parts = message.split(" ");
                if (parts.length > 1) {
                    String user = parts[1];
                    userList.getItems().remove(user); // Remove user
                }
            }

            // Notify if window not focused
            if (!stage.isFocused()) {
                notifyNewMessage();
            }
        });
    }

    /**
     * Flashes title for new messages.
     */
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

    /**
     * Sends message or handles commands.
     */
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            if (message.startsWith("/pm ")) {
                /// Private message: /pm username message
                String[] parts = message.substring(4).split(" ", 2);
                if (parts.length >= 2) {
                    String recipient = parts[0];
                    String privateMessage = parts[1];
                    client.sendPrivateMessage(recipient, privateMessage);
                    chatArea.appendText("[Private to " + recipient + "] " + privateMessage + "\n");
                } else {
                    chatArea.appendText("Usage: /pm <username> <message>\n");
                }
            } else if (message.startsWith("/clear")) {
                chatArea.clear(); // Clear chat
            } else if (message.startsWith("/users")) {
                chatArea.appendText("Users in room: " + String.join(", ", userList.getItems()) + "\n"); // List users
            } else {
                client.sendMessage(message); // Normal message
            }
            inputField.clear(); // Clear input
        }
    }


    /**
     * Sets new client.
     * @param client New chat client
     */
    public void setClient(ChatClient client) {
        this.client = client;
    }

    /**
     * Gets room manager.
     * @return Room manager instance
     */
    public RoomManager getRoomManager() {
        return roomManager;
    }
}
