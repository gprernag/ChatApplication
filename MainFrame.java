package com.mycompany;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;
import javax.swing.text.*;

public class MainFrame extends JFrame implements ActionListener {
    private JTextField msgText;
    private JButton sendButton;
    private JPanel userPanel;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private JTabbedPane chatTabs;
    private HashMap<String, JTextPane> chatAreas; // Use JTextPane instead of JTextArea
    private String selectedUser = "";

    private static DataInputStream din;
    private static DataOutputStream dout;
    private static Socket socket;
    private String userName;

    public MainFrame(String userName) {
        this.userName = userName;
        chatAreas = new HashMap<>();

        // Setup Frame
        setTitle("Chat - " + userName);
        setSize(700, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Left Panel - User List
        userPanel = new JPanel(new BorderLayout());
        userPanel.setPreferredSize(new Dimension(200, getHeight()));

        JLabel chatLabel = new JLabel("Active Users");
        chatLabel.setFont(new Font("Arial", Font.BOLD, 14));

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.addListSelectionListener(e -> switchChat(userList.getSelectedValue()));

        userPanel.add(chatLabel, BorderLayout.NORTH);
        userPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

        // Right Panel - Chat Tabs
        chatTabs = new JTabbedPane();
        
        // Bottom Panel - Input Field
        JPanel inputPanel = new JPanel(new BorderLayout());
        msgText = new JTextField();
        sendButton = new JButton("Send");
        sendButton.addActionListener(this);
        inputPanel.add(msgText, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Add Components
        add(userPanel, BorderLayout.WEST);
        add(chatTabs, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        setVisible(true);

        startClient();
    }

    private void startClient() {
        try {
            socket = new Socket("127.0.0.1", 6001);
            din = new DataInputStream(socket.getInputStream());
            dout = new DataOutputStream(socket.getOutputStream());

            dout.writeUTF(userName);

            new Thread(() -> {
                try {
                    while (true) {
                        String message = din.readUTF();
                        if (message.startsWith("USERS ")) {
                            updateUserList(message.substring(6));
                        } else {
                            String[] parts = message.split(": ", 2);
                            if (parts.length == 2) {
                                String sender = parts[0];
                                String chatMessage = parts[1];
                                appendMessage(sender, chatMessage, false); // Left-aligned for received messages
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
        });
    }

    private void addChatTab(String user) {
        JTextPane chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatAreas.put(user, chatPane);
        chatTabs.addTab(user, new JScrollPane(chatPane));
    }

    private void switchChat(String user) {
        selectedUser = user;
        chatTabs.setSelectedIndex(userListModel.indexOf(user));
    }

    private void appendMessage(String user, String message, boolean isSentByMe) {
        SwingUtilities.invokeLater(() -> {
            if (!chatAreas.containsKey(user)) {
                addChatTab(user);
            }

            JTextPane chatPane = chatAreas.get(user);
            StyledDocument doc = chatPane.getStyledDocument();

            SimpleAttributeSet style = new SimpleAttributeSet();
            StyleConstants.setFontSize(style, 14);
            StyleConstants.setForeground(style, isSentByMe ? Color.BLUE : Color.BLACK);
//            StyleConstants.setAlignment(style, isSentByMe ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);

            try {
                doc.insertString(doc.getLength(), message + "\n", style);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            String msg = msgText.getText().trim();
            if (!msg.isEmpty() && !selectedUser.isEmpty()) {
                dout.writeUTF("@" + selectedUser + " " + msg);

                // ✅ Show sent message immediately (right-aligned)
                appendMessage(selectedUser, "Me -> " + selectedUser + ": " + msg, true);

                msgText.setText(""); // Clear input field
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Thread(() -> ChatServer.main(args)).start();  // Start server
        String userName = JOptionPane.showInputDialog("Enter your username:");
        if (userName != null && !userName.trim().isEmpty()) {
            new MainFrame(userName.trim());
        }
    }
}
