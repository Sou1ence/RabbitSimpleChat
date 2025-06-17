package com.apokalist.telegram_mini;

import com.rabbitmq.client.*;
import javafx.application.Platform;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class ChatClient {
    private final String nickname;
    private final String roomName;
    private Connection connection;
    private Channel channel;
    private String queueName;
    private ChatUI ui;
    private final List<String> messageHistory = new ArrayList<>();
    private String consumerTag;

    public ChatClient(String nickname, String roomName, ChatUI ui) {
        this.nickname = nickname;
        this.roomName = roomName;
        this.ui = ui;
    }

    public void setUI(ChatUI ui) {
        this.ui = ui;
    }

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
            // Это гарантирует, что каждый пользователь получит ВСЕ сообщения
            queueName = "user_" + nickname + "_room_" + roomName;
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, exchangeName, roomName);

            // Private message queue
            String privateQueueName = "private_" + nickname;
            channel.queueDeclare(privateQueueName, true, false, false, null);

            // Сначала загружаем историю (все непрочитанные сообщения)
            loadExistingMessages();

            // Потом подписываемся на новые сообщения
            DeliverCallback roomCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                if (!messageHistory.contains(message)) { // Избегаем дубликатов
                    messageHistory.add(message);
                    Platform.runLater(() -> {
                        if (ui != null) ui.appendMessage(message);
                    });
                }
            };

            DeliverCallback privateCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
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

    private void loadExistingMessages() {
        try {
            // Получаем все сообщения из очереди БЕЗ удаления
            boolean autoAck = false;
            GetResponse response;

            List<String> existingMessages = new ArrayList<>();

            // Читаем все сообщения из очереди
            while ((response = channel.basicGet(queueName, autoAck)) != null) {
                String message = new String(response.getBody(), "UTF-8");
                existingMessages.add(message);

                // Acknowledge the message
                channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
            }

            // Добавляем в UI
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

    public void sendMessage(String message) {
        String fullMessage = "[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + nickname + ": " + message;
        try {
            // Отправляем с persistent delivery mode
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .deliveryMode(2) // persistent
                    .build();

            channel.basicPublish("chat_exchange_v2", roomName, props, fullMessage.getBytes("UTF-8"));

            // НЕ добавляем в messageHistory здесь - добавим когда получим через consumer

        } catch (IOException e) {
            Platform.runLater(() -> LoginDialog.showError("Failed to send message: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    public void sendPrivateMessage(String recipient, String message) {
        String fullMessage = "[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] [Private from " + nickname + "] " + message;
        try {
            String privateQueueName = "private_" + recipient;
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .deliveryMode(2)
                    .build();
            channel.basicPublish("", privateQueueName, props, fullMessage.getBytes("UTF-8"));
        } catch (IOException e) {
            Platform.runLater(() -> LoginDialog.showError("Failed to send private message: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private void sendSystemMessage(String message) {
        try {
            String fullMessage = "[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] System: " + message;
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .deliveryMode(2)
                    .build();
            channel.basicPublish("chat_exchange_v2", roomName, props, fullMessage.getBytes("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            // Cancel consumer
            if (consumerTag != null && channel != null && channel.isOpen()) {
                channel.basicCancel(consumerTag);
            }

            // Send leave message
            sendSystemMessage(nickname + " left the chat");

            Thread.sleep(200); // Wait for message to be sent

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

    public String getNickname() {
        return nickname;
    }

    public String getRoomName() {
        return roomName;
    }

    public List<String> getMessageHistory() {
        return new ArrayList<>(messageHistory);
    }
}
