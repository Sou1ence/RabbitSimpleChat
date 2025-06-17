/**
 * @author Kostiantyn Feniuk
 */

package com.apokalist.telegram_mini;

import com.rabbitmq.client.*;
import javafx.application.Platform;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * Manages room list using RabbitMQ
 *
 * pub-sub model for real-time updates
 */
public class RoomManager {
    private static final String ROOM_LIST_EXCHANGE = "room_list_exchange";
//    private static final String ROOM_LIST_QUEUE = "room_list_updates";

    private Connection connection; // RabbitMQ connection
    private Channel channel; // RabbitMQ channel
    private final Set<String> rooms = new HashSet<>(); // List of rooms
    private ChatUI ui; // UI for updating room list
    private String consumerTag; // Tag for message consumer

    /**
     * Constructor, sets up default rooms.
     * @param ui ChatUI instance
     */
    public RoomManager(ChatUI ui) {
        this.ui = ui;
        // Add default rooms
        rooms.add("room1");
        rooms.add("room2");
        rooms.add("room3");
    }

    /**
     * Connects to RabbitMQ and sets up room updates.
     */
    public void connect() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            factory.setPort(5672);
            connection = factory.newConnection();
            channel = connection.createChannel();

            // Set up exchange for room updates
            //
            channel.exchangeDeclare(ROOM_LIST_EXCHANGE, "fanout", true);

            // Create temp queue for updates
            String queueName = channel.queueDeclare("", false, true, true, null).getQueue();
            channel.queueBind(queueName, ROOM_LIST_EXCHANGE, "");

            // Listen for room updates
            DeliverCallback callback = (consumerTag, delivery) -> {
                String roomName = new String(delivery.getBody(), "UTF-8");
                synchronized (rooms) {
                    if (rooms.add(roomName)) { // Add new room
                        Platform.runLater(() -> {
                            if (ui != null) {
                                ui.addRoomToList(roomName); // Update UI
                            }
                        });
                    }
                }
            };

            consumerTag = channel.basicConsume(queueName, true, callback, tag -> {});

        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    /**
     * Announces a new room to all clients.
     * @param roomName Name of the room
     */
    public void announceRoom(String roomName) {
        if (roomName == null || roomName.trim().isEmpty()) return;

        synchronized (rooms) {
            if (rooms.add(roomName)) { // Add locally
                try {
                    // Send to other clients
                    channel.basicPublish(ROOM_LIST_EXCHANGE, "", null, roomName.getBytes("UTF-8"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Gets current list of rooms.
     * @return Set of room names
     */
    public Set<String> getRooms() {
        synchronized (rooms) {
            return new HashSet<>(rooms);
        }
    }

    /**
     * Closes RabbitMQ connection and channel.
     */
    public void close() {
        try {
            if (consumerTag != null && channel != null && channel.isOpen())
                channel.basicCancel(consumerTag);

            if (channel != null && channel.isOpen())
                channel.close();

            if (connection != null && connection.isOpen())
                connection.close();

        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
