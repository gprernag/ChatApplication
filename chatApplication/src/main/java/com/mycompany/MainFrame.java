// ========================= MainFrame.java =========================

package com.mycompany;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

// Main class that creates the GUI and handles user interaction
public class MainFrame extends JFrame implements ActionListener {
    private JTextField msgText; // For typing message
    private JButton sendButton; // Send button
    private JPanel userPanel; // Left panel for user list
    private DefaultListModel<String> userListModel; // Holds active usernames
    private JList<String> userList; // Displays usernames
    private JTabbedPane chatTabs; // Separate tabs for each conversation
    private HashMap<String, JTextPane> chatAreas; // Stores chat text areas by user
    private String selectedUser = ""; // Currently selected user to chat with

    private static DataInputStream din;
    private static DataOutputStream dout;
    private static Socket socket;
    private String userName;

    public MainFrame(String userName) {
        this.userName = userName;
        chatAreas = new HashMap<>();

        // Setup GUI window
        setTitle("Chat - " + userName);
        setSize(500, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Left panel for users
        userPanel = new JPanel(new BorderLayout());
        userPanel.setPreferredSize(new Dimension(200, getHeight()));
        userPanel.setBackground(new Color(230, 230, 250));

        JLabel chatLabel = new JLabel("Active Users", SwingConstants.CENTER);
        chatLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        chatLabel.setBorder(new EmptyBorder(10, 0, 10, 0));

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userList.setSelectionBackground(new Color(128, 0, 128));
        userList.setBackground(new Color(230, 230, 250));
        
        // Change chat tab when user clicks on another username
        userList.addListSelectionListener(e -> switchChat(userList.getSelectedValue()));

        userPanel.add(chatLabel, BorderLayout.NORTH);
        userPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

        // Chat area
        chatTabs = new JTabbedPane();
        chatTabs.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Input and send button at bottom
        JPanel inputPanel = new JPanel(new BorderLayout());
        msgText = new JTextField();
        msgText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        msgText.setBorder(new EmptyBorder(10, 10, 10, 10));

        sendButton = new JButton("Send");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sendButton.setBackground(new Color(128, 0, 128));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(new EmptyBorder(10, 20, 10, 20));
        sendButton.addActionListener(this);

        inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        inputPanel.add(msgText, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(userPanel, BorderLayout.WEST);
        add(chatTabs, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        setVisible(true);
        startClient(); // Connect to server
    }

   
// Setup socket connection and start reading messages
 private void startClient() {
    try {
        socket = new Socket("127.0.0.1", 6001);
        din = new DataInputStream(socket.getInputStream());
        dout = new DataOutputStream(socket.getOutputStream());
        dout.writeUTF(userName); // Send own username to server

        // New thread to listen for incoming messages
        new Thread(() -> {
            try {
                while (true) {
                    String message = din.readUTF();

                    if (message.startsWith("USERS ")) {
                        updateUserList(message.substring(6));
                    } else if (message.startsWith("HISTORY ")) {
                        // Format: HISTORY sender->receiver: message
                        String body = message.substring(8);
                        String[] parts = body.split("->|: ", 3); // sender, receiver, message
                        if (parts.length == 3) {
                            String sender = parts[0].trim();
                            String receiver = parts[1].trim();
                            String chatMsg = parts[2].trim();

                            String chatPartner = sender.equals(userName) ? receiver : sender;
                            boolean isSentByMe = sender.equals(userName);

                            appendMessage(chatPartner, (isSentByMe ? "Me: " : sender + ": ") + chatMsg, isSentByMe);
                        }
                    } else {
                        // Real-time message: sender: message
                        // Live incoming message
                        String[] parts = message.split(": ", 2);
                        if (parts.length == 2) {
                            String sender = parts[0];
                            String chatMsg = parts[1];

                            String chatPartner = sender.equals(userName) ? selectedUser : sender;
                            boolean isSentByMe = sender.equals(userName);

                            appendMessage(chatPartner, (isSentByMe ? "Me: " : sender + ": ") + chatMsg, isSentByMe);
                        }
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

    } catch (Exception e) {
        e.printStackTrace();
    }
}
 
  // Update list of active users on the left
 private void updateUserList(String users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            String[] userArray = users.split(",");
            for (String user : userArray) {
                if (!user.equals(userName)) {
                    userListModel.addElement(user);
                    if (!chatAreas.containsKey(user)) {
                        addChatTab(user);
                    }
                }
            }
            
            // Auto-select first user if none selected
            if (selectedUser.isEmpty() && userListModel.size() > 0) {
                String firstUser=userListModel.getElementAt(0);
                if(firstUser!= null && !firstUser.equals(userName)){
                    switchChat(firstUser);
                }
            }
        });
    }

 // Add a new tab for chatting with a user
    private void addChatTab(String user) {
        if(user.equals(userName)) 
            return;
        if(!chatAreas.containsKey(user)){
        JTextPane chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatPane.setBackground(new Color(250, 250, 250));
        chatAreas.put(user, chatPane);
        chatTabs.addTab(user, new JScrollPane(chatPane));
    }
    }
    
    // Switch the chat tab to the selected user
    private void switchChat(String user) {
    selectedUser = user;
    for (int i = 0; i < chatTabs.getTabCount(); i++) {
        if (chatTabs.getTitleAt(i).equals(user)) {
            chatTabs.setSelectedIndex(i);
            break;
        }
    }
}

// Append message to the correct chat tab
    private void appendMessage(String user, String message, boolean isSentByMe) {
        // This makes sure the chat message is added safely to the screen (because Swing needs to update UI on a special thread)
        SwingUtilities.invokeLater(() -> {
            
            // If the chat tab for this user doesn't exist yet, create it
            if (!chatAreas.containsKey(user)) {
                addChatTab(user); // Create a new chat tab for this user
            }
            // Get the chat screen for the selected user
            JTextPane chatPane = chatAreas.get(user);
            // Get the document (text area) inside the chat screen so we can insert new messages
            StyledDocument doc = chatPane.getStyledDocument();

            // Set how the message will look
            SimpleAttributeSet style = new SimpleAttributeSet();
            StyleConstants.setFontSize(style, 14);
            StyleConstants.setForeground(style, isSentByMe ? new Color(128, 0, 128) : Color.DARK_GRAY);
            StyleConstants.setAlignment(style, isSentByMe ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
            StyleConstants.setLeftIndent(style, 5);
            StyleConstants.setRightIndent(style, 5);

            try {
                // Add the message text to the chat screen
                doc.insertString(doc.getLength(), message + "\n", style);
            } catch (BadLocationException e) {
                // If something goes wrong while adding the message, print the error
                e.printStackTrace();
            }
        });
    }

    // Handle send button click
    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            String msg = msgText.getText().trim();
            if (!msg.isEmpty() && !selectedUser.isEmpty()) {
                dout.writeUTF("@" + selectedUser + " " + msg);// Send message to server
                msgText.setText(""); // Clear input field
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
// Start the app by asking for username
    public static void main(String[] args) {
        String userName = JOptionPane.showInputDialog("Enter your username:");
        if (userName != null && !userName.trim().isEmpty()) {
            new MainFrame(userName.trim());
        }
    }
}
