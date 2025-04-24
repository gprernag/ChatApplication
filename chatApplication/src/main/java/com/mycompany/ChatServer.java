// ========================= ChatServer.java =========================

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
            //Start server on port 6001
            serverSocket = new ServerSocket(6001);
            System.out.println("Server started on port 6001...");
            // Wait for clients to connect
            while (true) {
                Socket socket = serverSocket.accept();
                // Handle each client in a new thread
                new ClientHandler(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Handles communication with one client
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
                 // Initialize input and output streams
                din = new DataInputStream(socket.getInputStream());
                dout = new DataOutputStream(socket.getOutputStream());
                // Get client's username
                userName = din.readUTF();
                clients.put(userName, this);
                // Add user to database if new
                saveUserIfNew(userName);
                // Share active user list
                sendUserListToAll();
                // Send previous messages to the client
                sendChatHistoryToClient();
                // Handle messages sent by the user
                while (true) {
                    String message = din.readUTF();
                    if (message.startsWith("@")) {
                        // Split message into recipient and actual message
                        String[] parts = message.split(" ", 2);
                        String recipient = parts[0].substring(1);
                        String privateMessage = parts.length > 1 ? parts[1] : "";
                        
                        // Save message to database
                        saveMessageToDB(userName, recipient, privateMessage);
                        
                        // Send message to recipient and echo to sender
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
        
        // Save new user to database
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
        
        // Store message in database
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
        
        // Send all previous messages to the newly connected user
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
        // Send a message to a specific client
        private void sendPrivateMessage(String recipient, String message) throws IOException {
            if (clients.containsKey(recipient)) {
                clients.get(recipient).dout.writeUTF(message);
            }
        }
        
        // Send current user list to all clients
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