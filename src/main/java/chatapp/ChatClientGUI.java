package chatapp;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;

public class ChatClientGUI extends JFrame {
    
    // Connection settings
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5000;
    
    // UI Components
    private JTextPane chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JLabel statusLabel;
    private JTextField usernameField;
    
    // Date tracking for separators
    private String lastMessageDate = "";
    
    // Store all messages as HTML
    private StringBuilder messagesHtml = new StringBuilder();
    // Network components
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username = "User";
    private boolean connected = false;
    
    // Colors - Modern dark theme
    private static final Color BACKGROUND_DARK = new Color(17, 24, 39);
    private static final Color SIDEBAR_COLOR = new Color(24, 32, 48);
    private static final Color CHAT_BG = new Color(31, 41, 55);
    private static final Color INPUT_BG = new Color(55, 65, 81);
    private static final Color ACCENT_COLOR = new Color(59, 130, 246);
    private static final Color ACCENT_HOVER = new Color(37, 99, 235);
    private static final Color TEXT_PRIMARY = new Color(243, 244, 246);
    private static final Color TEXT_SECONDARY = new Color(156, 163, 175);
    private static final Color TEXT_MUTED = new Color(107, 114, 128);
    private static final Color ONLINE_COLOR = new Color(34, 197, 94);
    private static final Color BORDER_COLOR = new Color(55, 65, 81);
    private static final Color MESSAGE_SENT = new Color(59, 130, 246);
    private static final Color MESSAGE_RECEIVED = new Color(55, 65, 81);
    
    private static final String PLACEHOLDER_TEXT = "Your Name";
    
    public ChatClientGUI() {
        initializeUI();
        setupEventListeners();
    }
    
    private void initializeUI() {
        // Frame settings
        setTitle("Chat Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        
        // Main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(BACKGROUND_DARK);
        
        // Create components
        mainPanel.add(createSidebar(), BorderLayout.WEST);
        mainPanel.add(createChatPanel(), BorderLayout.CENTER);
        
        setContentPane(mainPanel);
    }
    
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 0));
        sidebar.setPreferredSize(new Dimension(280, 0));
        sidebar.setBackground(SIDEBAR_COLOR);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setBackground(SIDEBAR_COLOR);
        headerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("Messages");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(TEXT_PRIMARY);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        sidebar.add(headerPanel, BorderLayout.NORTH);
        
        // User connection panel
        JPanel connectionPanel = new JPanel(new BorderLayout(10, 10));
        connectionPanel.setBackground(SIDEBAR_COLOR);
        connectionPanel.setBorder(new EmptyBorder(0, 20, 15, 20));
        
        usernameField = new JTextField(PLACEHOLDER_TEXT);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.setBackground(INPUT_BG);
        usernameField.setForeground(TEXT_MUTED);
        usernameField.setCaretColor(TEXT_PRIMARY);
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(10, 15, 10, 15)
        ));
        
        // placeholder behavior - clear on focus/click
        usernameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (usernameField.getText().equals(PLACEHOLDER_TEXT)) {
                    usernameField.setText("");
                    usernameField.setForeground(TEXT_PRIMARY);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (usernameField.getText().isEmpty()) {
                    usernameField.setText(PLACEHOLDER_TEXT);
                    usernameField.setForeground(TEXT_MUTED);
                }
            }
        });
        
        JButton connectBtn = createStyledButton("Connect", ACCENT_COLOR);
        connectBtn.addActionListener(e -> toggleConnection());
        
        connectionPanel.add(usernameField, BorderLayout.CENTER);
        connectionPanel.add(connectBtn, BorderLayout.EAST);
        
        sidebar.add(connectionPanel, BorderLayout.SOUTH);
        
        // User list
        userListModel = new DefaultListModel<>();
        // Users will be added dynamically when they connect
        
        userList = new JList<>(userListModel);
        userList.setBackground(SIDEBAR_COLOR);
        userList.setForeground(TEXT_PRIMARY);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userList.setSelectionBackground(new Color(59, 130, 246, 30));
        userList.setSelectionForeground(TEXT_PRIMARY);
        userList.setFixedCellHeight(60);
        userList.setCellRenderer(new UserListRenderer());
        userList.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setBorder(null);
        userScrollPane.setBackground(SIDEBAR_COLOR);
        userScrollPane.getViewport().setBackground(SIDEBAR_COLOR);
        
        sidebar.add(userScrollPane, BorderLayout.CENTER);
        
        return sidebar;
    }
    
    private JPanel createChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout(0, 0));
        chatPanel.setBackground(CHAT_BG);
        
        // Chat header
        JPanel chatHeader = new JPanel(new BorderLayout(15, 0));
        chatHeader.setBackground(CHAT_BG);
        chatHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
            new EmptyBorder(15, 20, 15, 20)
        ));
        
        JPanel userInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        userInfo.setOpaque(false);
        
        JLabel avatarLabel = new JLabel("CR");
        avatarLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        avatarLabel.setForeground(Color.WHITE);
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setPreferredSize(new Dimension(40, 40));
        avatarLabel.setOpaque(true);
        avatarLabel.setBackground(ACCENT_COLOR);
        
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel("Chat Room");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        nameLabel.setForeground(TEXT_PRIMARY);
        
        statusLabel = new JLabel("● Offline");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(TEXT_MUTED);
        
        namePanel.add(nameLabel);
        namePanel.add(statusLabel);
        
        userInfo.add(avatarLabel);
        userInfo.add(namePanel);
        
        chatHeader.add(userInfo, BorderLayout.WEST);
        
        chatPanel.add(chatHeader, BorderLayout.NORTH);
        
        // Chat area
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setBackground(CHAT_BG);
        chatArea.setForeground(TEXT_PRIMARY);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatArea.setBorder(new EmptyBorder(20, 20, 20, 20));
        chatArea.setContentType("text/html");
        
        // Start with empty chat
        chatArea.setText("<html><body style='font-family: Segoe UI; background-color: #1f2937; color: #f3f4f6;'>" +
            "<div style='text-align: center; padding: 50px; color: #6b7280;'>" +
            "<p style='font-size: 16px;'>Welcome to Chat</p>" +
            "<p style='font-size: 12px;'>Connect to start messaging</p>" +
            "</div></body></html>");
        
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setBorder(null);
        chatScrollPane.setBackground(CHAT_BG);
        chatScrollPane.getViewport().setBackground(CHAT_BG);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        
        // Input panel 
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(CHAT_BG);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
            new EmptyBorder(15, 20, 15, 20)
        ));
        
        messageField = new JTextField();
        messageField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageField.setBackground(INPUT_BG);
        messageField.setForeground(TEXT_PRIMARY);
        messageField.setCaretColor(TEXT_PRIMARY);
        messageField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(12, 15, 12, 15)
        ));
        messageField.putClientProperty("JTextField.placeholderText", "Type a message...");
        
        sendButton = createStyledButton("Send", ACCENT_COLOR);
        sendButton.setPreferredSize(new Dimension(80, 44));
        
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        
        return chatPanel;
    }
    
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 20, 10, 20));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(ACCENT_HOVER);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }
    
    private String createMessageBubble(String message, String sender, String time, boolean isSent) {
        String bgColor = isSent ? "#3b82f6" : "#374151";
        String align = isSent ? "right" : "left";
        String margin = isSent ? "margin-left: 100px;" : "margin-right: 100px;";
        String senderText = isSent ? "" : "<p style='margin: 0 0 4px 0; font-size: 11px; color: #60a5fa; font-weight: bold;'>" + sender + "</p>";
        
        return String.format(
            "<div style='text-align: %s; margin: 10px 0;'>" +
            "<div style='display: inline-block; background-color: %s; padding: 12px 16px; border-radius: 18px; %s max-width: 70%%; text-align: left;'>" +
            "%s" +
            "<p style='margin: 0; color: #f3f4f6;'>%s</p>" +
            "<p style='margin: 5px 0 0 0; font-size: 10px; color: #9ca3af;'>%s</p>" +
            "</div></div>",
            align, bgColor, margin, senderText, message, time
        );
    }
    
    private void setupEventListeners() {
        // Send button click
        sendButton.addActionListener(e -> sendMessage());
        
        // Enter key to send
        messageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });
        
        // Window closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
    }
    
    private void toggleConnection() {
        if (!connected) {
            connect();
        } else {
            disconnect();
        }
    }
    
    private void connect() {
        String inputName = usernameField.getText().trim();
        // Treat placeholder as empty
        if (inputName.isEmpty() || inputName.equals(PLACEHOLDER_TEXT)) {
            username = "User";
        } else {
            username = inputName;
        }
        
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            
            connected = true;
            statusLabel.setText("● Connected");
            statusLabel.setForeground(ONLINE_COLOR);
            
            // Start message receiver thread
            new Thread(this::receiveMessages).start();
            
            // Send join message
            out.println(username + " has joined the chat");
            
            JOptionPane.showMessageDialog(this, 
                "Connected to server successfully!", 
                "Connected", 
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                "Could not connect to server.\nMake sure the server is running on " + SERVER_ADDRESS + ":" + SERVER_PORT, 
                "Connection Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void disconnect() {
        if (connected) {
            try {
                connected = false;
                if (out != null) {
                    out.println(username + " has left the chat");
                }
                if (socket != null) {
                    socket.close();
                }
                statusLabel.setText("● Offline");
                statusLabel.setForeground(TEXT_MUTED);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            if (connected && out != null) {
                // Send to server - it will broadcast back to everyone including us
                out.println(username + ": " + message);
            } else {
                // Only show locally if not connected 
                String timestamp = new SimpleDateFormat("h:mm a").format(new Date());
                appendMessage(message, "You", timestamp, true);
            }
            messageField.setText("");
        }
    }
    
    private void receiveMessages() {
        try {
            String line;
            while (connected && (line = in.readLine()) != null) {
                final String msg = line;
                SwingUtilities.invokeLater(() -> {
                    String timestamp = new SimpleDateFormat("h:mm a").format(new Date());
                    
                    // Check if user joined
                    if (msg.contains("has joined the chat")) {
                        String joinedUser = msg.replace(" has joined the chat", "").trim();
                        if (!joinedUser.equals(username)) {
                            addUserToList(joinedUser);
                        }
                        appendMessage(msg, "System", timestamp, false);
                    }
                    // Check if user left
                    else if (msg.contains("has left the chat")) {
                        String leftUser = msg.replace(" has left the chat", "").trim();
                        removeUserFromList(leftUser);
                        appendMessage(msg, "System", timestamp, false);
                    }
                    // Regular message
                    else if (msg.contains(": ")) {
                        String[] parts = msg.split(": ", 2);
                        String sender = parts[0];
                        String content = parts.length > 1 ? parts[1] : msg;
                        boolean isOwnMessage = sender.equals(username);
                        appendMessage(content, isOwnMessage ? "You" : sender, timestamp, isOwnMessage);
                    }
                });
            }
        } catch (IOException e) {
            if (connected) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("● Disconnected");
                    statusLabel.setForeground(new Color(239, 68, 68));
                });
            }
        }
    }
    
    private void addUserToList(String userName) {
        // Check if user already exists
        for (int i = 0; i < userListModel.size(); i++) {
            if (userListModel.get(i).contains(userName)) {
                return; // User already in list
            }
        }
        userListModel.addElement(userName);
    }
    
    private void removeUserFromList(String userName) {
        for (int i = 0; i < userListModel.size(); i++) {
            if (userListModel.get(i).contains(userName)) {
                userListModel.remove(i);
                break;
            }
        }
    }
    
    private void appendMessage(String message, String sender, String time, boolean isSent) {
        // Get today's date for separator
        String todayDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String displayDate = getDateLabel(todayDate);
        
        // Add date separator if new day
        if (!todayDate.equals(lastMessageDate)) {
            messagesHtml.append(createDateSeparator(displayDate));
            lastMessageDate = todayDate;
        }
        
        messagesHtml.append(createMessageBubble(message, sender, time, isSent));
        
        // Update chat area
        chatArea.setText("<html><body style='font-family: Segoe UI; background-color: #1f2937; color: #f3f4f6; padding: 10px;'>" 
            + messagesHtml.toString() + "</body></html>");
        
        // Scroll to bottom
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    private String getDateLabel(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date messageDate = sdf.parse(dateStr);
            Calendar msgCal = Calendar.getInstance();
            msgCal.setTime(messageDate);
            
            Calendar today = Calendar.getInstance();
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DAY_OF_YEAR, -1);
            
            if (isSameDay(msgCal, today)) {
                return "Today";
            } else if (isSameDay(msgCal, yesterday)) {
                return "Yesterday";
            } else {
                return new SimpleDateFormat("MMMM d, yyyy").format(messageDate);
            }
        } catch (Exception e) {
            return dateStr;
        }
    }
    
    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
    
    private String createDateSeparator(String dateLabel) {
        return String.format(
            "<div style='text-align: center; margin: 20px 0;'>" +
            "<span style='background-color: #374151; color: #9ca3af; padding: 6px 16px; border-radius: 12px; font-size: 12px;'>%s</span>" +
            "</div>",
            dateLabel
        );
    }
    
    // Custom cell renderer for user list
    class UserListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            
            JPanel panel = new JPanel(new BorderLayout(10, 0));
            panel.setBackground(isSelected ? new Color(59, 130, 246, 30) : SIDEBAR_COLOR);
            panel.setBorder(new EmptyBorder(10, 10, 10, 10));
            
            JLabel label = new JLabel(value.toString());
            label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            label.setForeground(TEXT_PRIMARY);
            
            panel.add(label, BorderLayout.CENTER);
            
            return panel;
        }
    }
    
    public static void main(String[] args) {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Run on EDT
        SwingUtilities.invokeLater(() -> {
            ChatClientGUI client = new ChatClientGUI();
            client.setVisible(true);
        });
    }
}
