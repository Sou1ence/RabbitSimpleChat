# Telegram Mini - JavaFX Chat Application

A robust real-time chat application developed using JavaFX and RabbitMQ, supporting multiple chat rooms, private messaging, and a modern dark theme interface.

![Java](https://img.shields.io/badge/Java-17+-orange.svg)
![JavaFX](https://img.shields.io/badge/JavaFX-17+-blue.svg)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.8+-green.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

## Features

- **Multi-Room Support**: Dynamically create and join chat rooms.
- **Private Messaging**: Send direct messages using the `/pm` command.
- **User Management**: Real-time user list with join/leave notifications.
- **Dark Theme Interface**: Professional design with smooth animations.
- **Real-Time Updates**: Instant message delivery via RabbitMQ.
- **Message Persistence**: Durable queues to prevent message loss.
- **Desktop Notifications**: Title bar alerts for new messages.
- **Chat Commands**: Built-in commands for enhanced functionality.

## Design & Color Palette

The application features a professional dark theme with a cohesive color palette:

### Primary Colors
- **Background**: `#2c2c2a` - Deep charcoal for the main interface.
- **Secondary**: `#272726` - Darker shade for panels and lists.
- **Surface**: `#3f3f3c` - Medium gray for input fields and chat areas.

### Accent Colors
- **Primary Accent**: `#ca7b5d` - Warm terracotta for buttons and highlights.
- **Secondary Accent**: `#7f72c3` - Soft purple for account details.
- **Text Primary**: `#dfdfdc` - Light cream for primary text.
- **Text Secondary**: `#a19e96` - Muted beige for secondary text.

### Design Philosophy
The interface adopts a modern minimalist approach, featuring:
- Clean typography and balanced whitespace.
- Subtle hover effects and visual feedback.
- Consistent spacing and alignment.
- Color harmony inspired by contemporary messaging applications.

## Quick Start

### Prerequisites
- Java 17 or higher.
- JavaFX SDK (if not included with JDK).
- RabbitMQ Server running locally.
- Maven for dependency management (optional).

### RabbitMQ Setup
1. **Install RabbitMQ**:
   - **Windows (Chocolatey)**: `choco install rabbitmq`
   - **macOS (Homebrew)**: `brew install rabbitmq`
   - **Ubuntu/Debian**: `sudo apt-get install rabbitmq-server`
2. **Start RabbitMQ Server**: `rabbitmq-server`
   - Ensure the server runs on `localhost:5672` (default port).
3. **Verify Installation**: Access the management interface at `http://localhost:15672` (default credentials: `guest`/`guest`).

### Application Setup
1. **Clone the Repository**:
   ```bash
   git clone https://github.com/Sou1ence/RabbitSimpleChat.git
   cd telegram-mini
   ```
2. **Compile the Application**:
   ```bash
   javac -cp "path/to/javafx/lib/*:path/to/rabbitmq-client.jar" src/main/java/com/apokalist/telegram_mini/*.java
   ```
3. **Run the Application**:
   ```bash
   java -cp "path/to/javafx/lib/*:path/to/rabbitmq-client.jar:src/main/java" --module-path "path/to/javafx/lib" --add-modules javafx.controls,javafx.fxml com.apokalist.telegram_mini.Main
   ```

### Using an IDE
1. Import the project into an IDE (IntelliJ IDEA, Eclipse, VS Code).
2. Add dependencies: JavaFX Controls, RabbitMQ Java Client.
3. Configure VM options:
   ```
   --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml
   ```
4. Run the `Main.java` class.

## How to Use

### Getting Started
1. Launch the application.
2. Enter a nickname and room name in the login dialog.
3. Click "Login" to join the chat.

### Basic Commands
- **Send Message**: Type and press Enter.
- **Private Message**: `/pm username message`
- **Clear Chat**: `/clear`
- **List Users**: `/users`

### Room Management
- **Create Room**: Right-click the room list and select "Add Room."
- **Switch Room**: Click a room in the left panel.
- **Current Room**: Highlighted in bold.

### Private Messaging
- **Via Command**: `/pm username message`
- **Via Context Menu**: Right-click a user and select "Send Private Message."

## Architecture

### Core Components
- **`ChatClient.java`**: Manages RabbitMQ connections and message processing.
- **`ChatUI.java`**: Implements the JavaFX user interface with responsive design.
- **`RoomManager.java`**: Handles room discovery and management.
- **`LoginDialog.java`**: Provides authentication and room selection.
- **`Main.java`**: Application entry point.

### Message Flow
1. User input is captured by the UI.
2. `ChatClient` formats and timestamps the message.
3. RabbitMQ routes the message to appropriate queues.
4. Messages are delivered to room members.
5. The UI updates to display new messages.

### Queue Architecture
- **Room Queues**: `user_{nickname}_room_{roomname}` for each user per room.
- **Private Queues**: `private_{nickname}` for direct messaging.
- **Room Discovery**: Fanout exchange for real-time room list updates.

## Configuration

### RabbitMQ Settings
Default connection settings:
- **Host**: `localhost`
- **Port**: `5672`
- **Virtual Host**: `/`
Modify settings in `ChatClient.java` and `RoomManager.java` as needed.

### Styling
Customize the `style.css` file to adjust:
- Color scheme.
- Font sizes.
- Component spacing.
- Hover effects.

## Contributing
1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/AmazingFeature`
3. Commit changes: `git commit -m 'Add some AmazingFeature'`
4. Push to the branch: `git push origin feature/AmazingFeature`
5. Open a Pull Request.

## License
Licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Author
**Kostiantyn Feniuk** - *Initial work* - [@apokalist](https://github.com/apokalist)

## Troubleshooting

### Common Issues
- **RabbitMQ Connection Failed**:
  - Verify the server is running and port 5672 is open.
  - Check firewall settings.
- **UI Not Loading**:
  - Ensure JavaFX is installed and module paths are correct.
  - Confirm `style.css` is in the resources folder.
- **Messages Not Appearing**:
  - Verify queue creation in RabbitMQ.
  - Check network connectivity and console logs.

---

*Developed by Era*
