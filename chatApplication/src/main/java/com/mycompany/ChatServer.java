package com.mycompany;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static ServerSocket serverSocket;
    private static HashMap<String, ClientHandler> clients = new HashMap<>();

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
                sendUserListToAll();

                while (true) {
                    String message = din.readUTF();
                    if (message.startsWith("@")) {
                        String[] parts = message.split(" ", 2);
                        String recipient = parts[0].substring(1);
                        String privateMessage = parts.length > 1 ? parts[1] : "";
                        sendPrivateMessage(recipient, userName + ": " + privateMessage);
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
