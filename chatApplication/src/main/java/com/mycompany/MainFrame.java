package com.mycompany;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class MainFrame extends JFrame implements ActionListener {
    private JTextField msgText;
    private JButton sendButton;
    private JPanel userPanel;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private JTabbedPane chatTabs;
    private HashMap<String, JTextPane> chatAreas;
    private String selectedUser = "";

    private static DataInputStream din;
    private static DataOutputStream dout;
    private static Socket socket;
    private String userName;

    public MainFrame(String userName) {
        this.userName = userName;
        chatAreas = new HashMap<>();

        // Frame setup
        setTitle("Chat - " + userName);
        setSize(800, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 🟪 Left Panel - User List
        userPanel = new JPanel(new BorderLayout());
        userPanel.setPreferredSize(new Dimension(200, getHeight()));
        userPanel.setBackground(new Color(230, 230, 250)); // Lavender

        JLabel chatLabel = new JLabel("Active Users", SwingConstants.CENTER);
        chatLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        chatLabel.setForeground(Color.BLACK);
        chatLabel.setBorder(new EmptyBorder(10, 0, 10, 0));

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userList.setSelectionBackground(new Color(128, 0, 128)); // Purple
        userList.setBorder(new EmptyBorder(5, 10, 5, 10));
        userList.setFixedCellHeight(30);
        userList.setOpaque(false);
        userList.setBackground(new Color(230, 230, 250)); // Lavender

        userList.addListSelectionListener(e -> switchChat(userList.getSelectedValue()));

        userPanel.add(chatLabel, BorderLayout.NORTH);
        userPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

        // 🟦 Center - Chat Tabs
        chatTabs = new JTabbedPane();
        chatTabs.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // 🟧 Bottom - Input Panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        msgText = new JTextField();
        msgText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        msgText.setBorder(new EmptyBorder(10, 10, 10, 10));

        sendButton = new JButton("Send");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sendButton.setBackground(new Color(128, 0, 128)); // Purple
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(new EmptyBorder(10, 20, 10, 20));
        sendButton.addActionListener(this);

        inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        inputPanel.add(msgText, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Frame Layout
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
                                appendMessage(sender, chatMessage, false);
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
        chatPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatPane.setBackground(new Color(250, 250, 250));
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

            Color textColor = isSentByMe ? new Color(128, 0, 128) : Color.DARK_GRAY; // Purple for sent
            StyleConstants.setForeground(style, textColor);

            StyleConstants.setAlignment(style, isSentByMe ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
            StyleConstants.setLeftIndent(style, 5);
            StyleConstants.setRightIndent(style, 5);

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
                appendMessage(selectedUser, "Me: " + msg, true);
                msgText.setText("");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String userName = JOptionPane.showInputDialog("Enter your username:");
        if (userName != null && !userName.trim().isEmpty()) {
            new MainFrame(userName.trim());
        }
    }
}
