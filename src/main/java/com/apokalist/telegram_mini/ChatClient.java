/**
 * @author Feniuk Kostiantyn
 *
 *
 *
 * слушатели
 * ( channel.basicConsume(queueName, false, roomCallback, tag -> {});
channel.basicConsume(privateQueueName, true, privateCallback, tag -> {});
 )
 */

package com.apokalist.telegram_mini;

import com.rabbitmq.client.*;
import javafx.application.Platform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Manages chat client connection to RabbitMQ.
 */
public class ChatClient {
    private final String nickname; // User nickname
    private final String roomName; // Room name
    private Connection connection; // RabbitMQ connection
    private Channel channel; // RabbitMQ channel
    private String queueName; // Queue for messages
    private ChatUI ui; // UI instance
    private final List<String> messageHistory = new ArrayList<>(); // Message history
    private String consumerTag; // Consumer tag

    /**
     * Constructor, sets up client.
     * @param nickname User nickname
     * @param roomName Room name  (ROUTING KEY)
     * @param ui ChatUI instance
     */
    public ChatClient(String nickname, String roomName, ChatUI ui) {
        this.nickname = nickname;
        this.roomName = roomName;
        this.ui = ui;
    }

    /**
     * Sets UI instance.
     * @param ui ChatUI instance
     */
    public void setUI(ChatUI ui) {
        this.ui = ui;
    }

    /**
     * Connects to RabbitMQ and sets up queues.
     */
    public void connect() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            factory.setPort(5672);
            connection = factory.newConnection();
            channel = connection.createChannel();

            // Declare exchange (use unique name to avoid conflicts)
            String exchangeName = "chat_exchange_v2";
            channel.exchangeDeclare(exchangeName, "topic", true);

            // Create DURABLE queue for EACH USER in EACH ROOM
            // Ensures every user gets ALL messages
            queueName = "user_" + nickname + "_room_" + roomName;


//            NOTE_(DO_not_forget)_____________________________________________________________
            // Создание очереди:
            // durable = true — очередь сохраняется при перезапуске сервера
            // exclusive = false — доступна другим каналам
            // autoDelete = false — не удаляется после отключения клиента
//            _______________________________________________________________

//_________ CHANNEL.QUEUEDECLARE(STRING QUEUE, BOOLEAN DURABLE, BOOLEAN EXCLUSIVE, BOOLEAN AUTODELETE, MAP<STRING, OBJECT> ARGUMENTS)__________________|
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, exchangeName, roomName);

            // Private message queue
            String privateQueueName = "private_" + nickname;
            channel.queueDeclare(privateQueueName, true, false, false, null);

            // Load message history first
            loadExistingMessages();

            // Subscribe to new messages
            DeliverCallback roomCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                if (!messageHistory.contains(message)) { // Avoid duplicates
                    messageHistory.add(message);
                    Platform.runLater(() -> {
                        if (ui != null) ui.appendMessage(message);
                    });
                }
            };

            DeliverCallback privateCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                messageHistory.add(message);
                Platform.runLater(() -> {
                    if (ui != null) ui.appendMessage(message);
                });
            };

            // Start consuming with manual acknowledgment
            consumerTag = channel.basicConsume(queueName, false, roomCallback, tag -> {});
            channel.basicConsume(privateQueueName, true, privateCallback, tag -> {});

            // Send join notification
            sendSystemMessage(nickname + " joined the chat");

        } catch (IOException | TimeoutException e) {
            Platform.runLater(() -> LoginDialog.showError("Failed to connect to RabbitMQ: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    /**
     * Loads existing messages from queue.
     */
    private void loadExistingMessages() {
        try {
            // Get all messages from queue without removing
            boolean autoAck = false;
            GetResponse response;

            List<String> existingMessages = new ArrayList<>();

            // Read all messages from queue
            while ((response = channel.basicGet(queueName, autoAck)) != null) {
                String message = new String(response.getBody(), StandardCharsets.UTF_8);
                existingMessages.add(message);

                // Acknowledge the message
                channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
            }

            // Add to UI
            Platform.runLater(() -> {
                if (ui != null) {
                    for (String msg : existingMessages) {
                        ui.appendMessage(msg);
                        messageHistory.add(msg);
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a chat message to the room.
     * @param message Message to send
     */
    public void sendMessage(String message) {
        String fullMessage = "[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + nickname + ": " + message;
        try {
            // Send with persistent delivery mode
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .deliveryMode(2) // persistent (2)  transient (1)
                    .build();

            channel.basicPublish("chat_exchange_v2", roomName, props, fullMessage.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Platform.runLater(() -> LoginDialog.showError("Failed to send message: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    /**
     * Sends a private message to a user.
     *
     * @param recipient Recipient's nickname
     * @param message Message to send
     */
    public void sendPrivateMessage(String recipient, String message) {
        String fullMessage = "[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] [Private from " + nickname + "] " + message;
        try {

            // reference to queue for private messages
            String privateQueueName = "private_" + recipient;
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .deliveryMode(2)
                    .build();
            channel.basicPublish("", privateQueueName,true, props, fullMessage.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Platform.runLater(() -> LoginDialog.showError("Failed to send private message: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    /**
     * Sends a system message to the room.
     * @param message System message to send
     */
    private void sendSystemMessage(String message) {
        try {
            //Exchange "chat_exchange_v2" is used for all messages
            String fullMessage = "[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] System: " + message;
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .deliveryMode(2)
                    .build();
            channel.basicPublish("chat_exchange_v2", roomName, props, fullMessage.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes RabbitMQ connection and channel.
     */
    public void close() {
        try {
            // Cancel consumer
            if (consumerTag != null && channel != null && channel.isOpen()) {
                channel.basicCancel(consumerTag);
            }

            // Send leave message
            sendSystemMessage(nickname + " left the chat");

            // Wait for message sent
            Thread.sleep(200);

            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (IOException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets user nickname.
     * @return Nickname
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Gets room name.
     * @return Room name
     */
    public String getRoomName() {
        return roomName;
    }

    /**
     * Gets message history.
     * @return List of messages
     */
    public List<String> getMessageHistory() {
        return new ArrayList<>(messageHistory);
    }
}
