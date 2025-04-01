# Java Chat Application

## Overview
This is a real-time multi-user chat application built using Java. It enables users to send private messages to each other, dynamically add or remove users, and switch conversations seamlessly.

## Features
- **Multi-user chat**: Supports multiple users communicating via private messages.
- **Dynamic user management**: Users can join or leave the chat dynamically.
- **Tabbed chat interface**: Users can switch between conversations without losing messages.
- **Automatic user list update**: The user list updates when a user joins or leaves.
- **Single-run execution**: The server and client start together when launching the project.

## Prerequisites
- Java Development Kit (JDK) 8 or later
- Eclipse/IntelliJ or any Java IDE

## Installation
1. Clone the repository:
   ```sh
   git clone https://github.com/yourusername/chat-application.git
   cd chat-application
   ```
2. Open the project in your preferred Java IDE.
3. Compile and run the application.

## Usage
1. **Start the Application**: Running `MainFrame.java` starts both the server and client.
2. **Enter Username**: A prompt will ask for the username.
3. **Send Messages**: Select a user from the list and type a message.
4. **Switch Conversations**: Click on a user's name to switch between chats.
5. **Remove Users**: If a user disconnects, the user list updates automatically.

## Project Structure
```
chat-application/
│-- src/
│   ├── com/mycompany/
│   │   ├── MainFrame.java
│   │   ├── ChatServer.java
│-- README.md
```



