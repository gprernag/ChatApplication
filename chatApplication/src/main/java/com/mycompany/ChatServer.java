package com.mycompany;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
public class ChatServer {
    private static ServerSocket serverSocket;
    private static HashMap<String, ClientHandler> clients = new HashMap<>();
    // Database credentials
    private static final String DB_URL = "jdbc:mysql://localhost:3306/chat_app";
    private static final String DB_USER = "root"; // change if needed
    private static final String DB_PASSWORD = "123pgkg222#P"; // change if needed
    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(6001);
            System.out.println("Server started on port 6001...");
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    static class ClientHandler extends Thread {
        private Socket socket;
        private DataInputStream din;
        private DataOutputStream dout;
        private String userName;
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        public void run() {
            try {
                din = new DataInputStream(socket.getInputStream());
                dout = new DataOutputStream(socket.getOutputStream());
                userName = din.readUTF();
                clients.put(userName, this);
                // Save user to DB if not exists
                saveUserIfNew(userName);
                // Send user list
                sendUserListToAll();
                // Send chat history to the user
                sendChatHistoryToClient();
                // Listen for messages
                while (true) {
                    String message = din.readUTF();
                    if (message.startsWith("@")) {
                        String[] parts = message.split(" ", 2);
                        String recipient = parts[0].substring(1);
                        String privateMessage = parts.length > 1 ? parts[1] : "";
                        saveMessageToDB(userName, recipient, privateMessage);
                        sendPrivateMessage(recipient, userName + ": " + privateMessage);
                        sendPrivateMessage(userName, userName + ": " + privateMessage); // Echo to sender
                    }
                }
            } catch (IOException e) {
                System.out.println(userName + " disconnected.");
            } finally {
                clients.remove(userName);
                sendUserListToAll();
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        private void saveUserIfNew(String username) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String query = "INSERT IGNORE INTO users (username) VALUES (?)";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, username);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        private void saveMessageToDB(String sender, String receiver, String message) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String query = "INSERT INTO messages (sender, receiver, message) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, sender);
                    stmt.setString(2, receiver);
                    stmt.setString(3, message);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        private void sendChatHistoryToClient() {
    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
        String query = """
            SELECT sender, receiver, message
            FROM messages
            WHERE sender = ? OR receiver = ?
            ORDER BY sent_at ASC
        """;
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, userName);
            stmt.setString(2, userName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String sender = rs.getString("sender");
                String receiver = rs.getString("receiver");
                String message = rs.getString("message");
                dout.writeUTF("HISTORY " + sender + "->" + receiver + ": " + message);
            }
        }
    } catch (SQLException | IOException e) {
        e.printStackTrace();
    }
}

        private void sendPrivateMessage(String recipient, String message) throws IOException {
            if (clients.containsKey(recipient)) {
                clients.get(recipient).dout.writeUTF(message);
            }
        }
        private void sendUserListToAll() {
            String userList = "USERS " + String.join(",", clients.keySet());
            for (ClientHandler client : clients.values()) {
                try {
                    client.dout.writeUTF(userList);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}