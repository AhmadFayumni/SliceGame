package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import javax.imageio.ImageIO;

public class FruitNinjaClientGUI extends JFrame {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String currentUser;
    private boolean connected = false;
    private boolean mfaActive = false;
    private boolean mfaSolved = false;
    private String mfaOrder = "";

    private CardLayout cardLayout;
    private JPanel mainPanel;

    // Login components
    private JTextField userField;
    private JPasswordField passField;
    private JTextArea statusArea;
    private JButton loginBtn;
    private JButton registerBtn;

    // MFA panel components
    private JButton[] tileButtons = new JButton[9];
    private int[] tileOrder = new int[9];
    private int selectedTileIndex = -1;
    private BufferedImage puzzleImage;
    private int tileSize = 80;

    // Game components
    private GamePanel gamePanel;
    private JButton retryBtn;
    private JButton leaderboardBtn;

    // Chat components
    private JTextArea chatHistory;
    private JTextField chatInput;
    private JButton sendBtn;

    // Retry fade timer
    private Timer fadeTimer;
    private float retryOpacity = 0f;

    public FruitNinjaClientGUI() {
        setTitle("🍉 Fruit Ninja - Dojo Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 620);
        setLocationRelativeTo(null);
        setResizable(false);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(new Color(20, 30, 45));

        mainPanel.add(createLoginPanel(), "Login");
        mainPanel.add(createMFAPanel(), "MFA");
        mainPanel.add(createGamePanel(), "Game");

        add(mainPanel);
        connectToServer();
    }

    // ======================== LOGIN PANEL ========================
    private JPanel createLoginPanel() {
        JPanel container = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(15, 25, 45),
                        0, getHeight(), new Color(5, 10, 20));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        container.setBorder(BorderFactory.createEmptyBorder(50, 30, 50, 30));

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2d.setColor(new Color(255, 255, 255, 50));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2d.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("🍉 NINJA DOJO", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(new Color(255, 200, 50));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;
        card.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        userLabel.setForeground(Color.WHITE);
        card.add(userLabel, gbc);

        userField = new JTextField(15);
        userField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        userField.setBackground(new Color(255, 255, 255, 200));
        userField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 100), 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        gbc.gridx = 1;
        card.add(userField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        passLabel.setForeground(Color.WHITE);
        card.add(passLabel, gbc);

        passField = new JPasswordField(15);
        passField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        passField.setBackground(new Color(255, 255, 255, 200));
        passField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 100), 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        gbc.gridx = 1;
        card.add(passField, gbc);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.setOpaque(false);

        loginBtn = new JButton("🗡️ SLICE IN") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2d.setColor(new Color(200, 100, 0));
                } else if (getModel().isRollover() && isEnabled()) {
                    g2d.setColor(new Color(255, 180, 50));
                } else {
                    g2d.setColor(isEnabled() ? new Color(255, 140, 0) : new Color(100, 100, 100));
                }
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2d.setColor(Color.WHITE);
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2d.drawString(getText(), x, y);
                g2d.dispose();
            }
        };
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setBorderPainted(false);
        loginBtn.setContentAreaFilled(false);
        loginBtn.setFocusPainted(false);
        loginBtn.setEnabled(false);
        loginBtn.addActionListener(e -> attemptLogin());

        registerBtn = new JButton("📝 Register") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2d.setColor(new Color(0, 100, 150));
                } else if (getModel().isRollover() && isEnabled()) {
                    g2d.setColor(new Color(0, 180, 255));
                } else {
                    g2d.setColor(isEnabled() ? new Color(0, 120, 200) : new Color(100, 100, 100));
                }
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2d.setColor(Color.WHITE);
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2d.drawString(getText(), x, y);
                g2d.dispose();
            }
        };
        registerBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setBorderPainted(false);
        registerBtn.setContentAreaFilled(false);
        registerBtn.setFocusPainted(false);
        registerBtn.setEnabled(false);
        registerBtn.addActionListener(e -> showRegisterDialog());

        buttonPanel.add(loginBtn);
        buttonPanel.add(registerBtn);

        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.insets = new Insets(20, 8, 8, 8);
        card.add(buttonPanel, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(10, 8, 8, 8);
        statusArea = new JTextArea(3, 20);
        statusArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusArea.setEditable(false);
        statusArea.setOpaque(false);
        statusArea.setForeground(new Color(200, 200, 200));
        statusArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        statusArea.setText("⚔️ Connecting to dojo...\n");
        JScrollPane scrollPane = new JScrollPane(statusArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        card.add(scrollPane, gbc);

        container.add(card);
        getRootPane().setDefaultButton(loginBtn);
        return container;
    }

    // ======================== REGISTER DIALOG ========================
    private void showRegisterDialog() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JTextField regUser = new JTextField();
        JPasswordField regPass = new JPasswordField();
        JPasswordField regConfirm = new JPasswordField();
        panel.add(new JLabel("Username:"));
        panel.add(regUser);
        panel.add(new JLabel("Password:"));
        panel.add(regPass);
        panel.add(new JLabel("Confirm Password:"));
        panel.add(regConfirm);

        int result = JOptionPane.showConfirmDialog(this, panel, "Register New Account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String user = regUser.getText().trim();
            String pass = new String(regPass.getPassword());
            String confirm = new String(regConfirm.getPassword());
            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username and password cannot be empty.");
                return;
            }
            if (!pass.equals(confirm)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match.");
                return;
            }
            sendMessage("REGISTER|" + user + "|" + pass);
        }
    }

    // ======================== MFA PANEL ========================
    private JPanel createMFAPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(20, 30, 45));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel instruction = new JLabel("🔐 Security Check: Arrange the puzzle correctly", SwingConstants.CENTER);
        instruction.setFont(new Font("Segoe UI", Font.BOLD, 18));
        instruction.setForeground(new Color(255, 200, 50));
        panel.add(instruction, BorderLayout.NORTH);

        JPanel gridPanel = new JPanel(new GridLayout(3, 3, 4, 4));
        gridPanel.setBackground(new Color(20, 30, 45));
        gridPanel.setPreferredSize(new Dimension(260, 260));
        for (int i = 0; i < 9; i++) {
            tileButtons[i] = new JButton();
            tileButtons[i].setFocusPainted(false);
            tileButtons[i].setBackground(Color.GRAY);
            tileButtons[i].setPreferredSize(new Dimension(tileSize, tileSize));
            tileButtons[i].addActionListener(new TileActionListener(i));
            gridPanel.add(tileButtons[i]);
        }
        panel.add(gridPanel, BorderLayout.CENTER);

        JLabel statusLabel = new JLabel("Click two tiles to swap them.", SwingConstants.CENTER);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(statusLabel, BorderLayout.SOUTH);

        puzzleImage = loadSafePuzzleImage();
        if (puzzleImage != null && puzzleImage.getWidth() >= 240) {
            tileSize = puzzleImage.getWidth() / 3;
        } else {
            tileSize = 80;
        }
        return panel;
    }

    private BufferedImage loadSafePuzzleImage() {
        BufferedImage img = null;
        String[] paths = {
            "/client/resources/image1.jpg",
            "/resources/image1.jpg",
            "src/client/resources/image1.jpg",
            "image1.jpg"
        };
        for (String path : paths) {
            try {
                InputStream is = getClass().getResourceAsStream(path);
                if (is == null) {
                    File f = new File(path);
                    if (f.exists()) is = new FileInputStream(f);
                }
                if (is != null) {
                    img = ImageIO.read(is);
                    if (img != null) break;
                }
            } catch (Exception ignored) {}
        }

        if (img == null) {
            System.out.println("⚠️ No image found. Using dummy puzzle.");
            img = new BufferedImage(240, 240, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    int x = j * 80, y = i * 80;
                    g.setColor(new Color(50 + i * 70, 100 + j * 50, 200));
                    g.fillRect(x, y, 80, 80);
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Arial", Font.BOLD, 24));
                    int num = i * 3 + j;
                    g.drawString(String.valueOf(num + 1), x + 30, y + 50);
                    g.setColor(Color.BLACK);
                    g.drawRect(x, y, 80, 80);
                }
            }
            g.dispose();
            return img;
        }

        if (img.getWidth() != img.getHeight() || img.getWidth() % 3 != 0) {
            BufferedImage resized = new BufferedImage(240, 240, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, 240, 240, null);
            g.dispose();
            img = resized;
        }
        return img;
    }

    private class TileActionListener implements ActionListener {
        private int index;

        public TileActionListener(int index) {
            this.index = index;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!mfaActive || mfaSolved) return;

            if (selectedTileIndex == -1) {
                selectedTileIndex = index;
                tileButtons[index].setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
            } else if (selectedTileIndex == index) {
                tileButtons[index].setBorder(null);
                selectedTileIndex = -1;
            } else {
                // Swap
                int temp = tileOrder[selectedTileIndex];
                tileOrder[selectedTileIndex] = tileOrder[index];
                tileOrder[index] = temp;
                updateTileImages();
                tileButtons[selectedTileIndex].setBorder(null);
                selectedTileIndex = -1;

                // Check if solved
                boolean solved = true;
                for (int i = 0; i < 9; i++) {
                    if (tileOrder[i] != i) { solved = false; break; }
                }
                if (solved && !mfaSolved) {
                    mfaSolved = true;
                    mfaActive = false;
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 9; i++) {
                        if (i > 0) sb.append(",");
                        sb.append(tileOrder[i]);
                    }
                    sendMessage("MFA_CONFIRM|" + sb.toString());
                    JOptionPane.showMessageDialog(FruitNinjaClientGUI.this,
                            "✅ Puzzle solved! Waiting for server...",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }

    private void updateTileImages() {
        if (puzzleImage == null) return;
        int w = puzzleImage.getWidth();
        int h = puzzleImage.getHeight();
        if (w != h || w % 3 != 0) {
            BufferedImage resized = new BufferedImage(240, 240, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.drawImage(puzzleImage, 0, 0, 240, 240, null);
            g.dispose();
            puzzleImage = resized;
        }
        int tileW = puzzleImage.getWidth() / 3;
        int tileH = puzzleImage.getHeight() / 3;
        for (int i = 0; i < 9; i++) {
            int tileIndex = tileOrder[i];
            int row = tileIndex / 3;
            int col = tileIndex % 3;
            int x = col * tileW;
            int y = row * tileH;
            BufferedImage tile = puzzleImage.getSubimage(x, y, tileW, tileH);
            ImageIcon icon = new ImageIcon(tile.getScaledInstance(tileSize, tileSize, Image.SCALE_SMOOTH));
            tileButtons[i].setIcon(icon);
            tileButtons[i].setText("");
            tileButtons[i].setBackground(Color.WHITE);
        }
    }

    // ======================== GAME PANEL ========================
    private JPanel createGamePanel() {
        JPanel mainGamePanel = new JPanel(new BorderLayout());
        mainGamePanel.setBackground(new Color(20, 30, 45));

        // Top Bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        topBar.setOpaque(false);
        topBar.setBackground(new Color(0, 0, 0, 50));
        topBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel scoreLabel = new JLabel("⚔️ SLICE MASTER");
        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        scoreLabel.setForeground(new Color(255, 200, 50));

        leaderboardBtn = new JButton("🏆 Leaderboard");
        leaderboardBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        leaderboardBtn.setForeground(Color.WHITE);
        leaderboardBtn.setBackground(new Color(30, 40, 60));
        leaderboardBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 200, 50, 100), 1),
                BorderFactory.createEmptyBorder(6, 18, 6, 18)
        ));
        leaderboardBtn.setFocusPainted(false);
        leaderboardBtn.addActionListener(e -> {
            sendMessage("GET_LEADERBOARD");
            appendChat("System", "Fetching leaderboard...");
        });

        retryBtn = new JButton("🔄 Retry");
        retryBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        retryBtn.setForeground(Color.WHITE);
        retryBtn.setBackground(new Color(0, 150, 50, 0));
        retryBtn.setOpaque(true);
        retryBtn.setContentAreaFilled(true);
        retryBtn.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));
        retryBtn.setFocusPainted(false);
        retryBtn.setEnabled(false);
        retryBtn.addActionListener(e -> {
            sendMessage("RESTART");
            retryBtn.setEnabled(false);
            retryOpacity = 0f;
            retryBtn.setBackground(new Color(0, 150, 50, 0));
        });

        topBar.add(scoreLabel);
        topBar.add(leaderboardBtn);
        topBar.add(retryBtn);
        mainGamePanel.add(topBar, BorderLayout.NORTH);

        // Center: GamePanel + Chat Sidebar
        JPanel centerPanel = new JPanel(new BorderLayout(5, 0));
        centerPanel.setOpaque(false);

        gamePanel = new GamePanel(this);
        centerPanel.add(gamePanel, BorderLayout.CENTER);

        JPanel chatSidebar = createChatSidebar();
        centerPanel.add(chatSidebar, BorderLayout.EAST);

        mainGamePanel.add(centerPanel, BorderLayout.CENTER);

        fadeTimer = new Timer(30, e -> {
            retryOpacity += 0.05f;
            if (retryOpacity >= 1.0f) {
                retryOpacity = 1.0f;
                fadeTimer.stop();
            }
            int alpha = (int) (retryOpacity * 255);
            retryBtn.setBackground(new Color(0, 150, 50, alpha));
            retryBtn.repaint();
        });
        fadeTimer.setRepeats(true);

        return mainGamePanel;
    }

    // ======================== CHAT SIDEBAR ========================
    private JPanel createChatSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 5));
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBackground(new Color(15, 25, 40));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(255, 255, 255, 30)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel chatTitle = new JLabel("💬 DOJO CHAT", SwingConstants.CENTER);
        chatTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        chatTitle.setForeground(new Color(255, 200, 50));
        chatTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        sidebar.add(chatTitle, BorderLayout.NORTH);

        chatHistory = new JTextArea();
        chatHistory.setEditable(false);
        chatHistory.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chatHistory.setBackground(new Color(10, 18, 30));
        chatHistory.setForeground(new Color(220, 220, 240));
        chatHistory.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JScrollPane scrollPane = new JScrollPane(chatHistory);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        sidebar.add(scrollPane, BorderLayout.CENTER);

        // Input
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setOpaque(false);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        chatInput = new JTextField();
        chatInput.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatInput.setBackground(new Color(255, 255, 255, 180));
        chatInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 80), 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        chatInput.setEnabled(false);
        chatInput.addActionListener(e -> sendChatMessage());
        inputPanel.add(chatInput, BorderLayout.CENTER);

        sendBtn = new JButton("Send");
        sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setBackground(new Color(255, 140, 0));
        sendBtn.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        sendBtn.setFocusPainted(false);
        sendBtn.addActionListener(e -> sendChatMessage());
        inputPanel.add(sendBtn, BorderLayout.EAST);

        sidebar.add(inputPanel, BorderLayout.SOUTH);
        return sidebar;
    }

    // ======================== CHAT LOGIC ========================
    private void sendChatMessage() {
        String msg = chatInput.getText().trim();
        if (msg.isEmpty()) return;
        if (!gamePanel.isGameActive()) {
            gamePanel.setLastMessage("⏳ Wait for the game to start!");
            return;
        }
        sendMessage("CHAT|" + msg);
        chatInput.setText("");
        chatInput.requestFocusInWindow();
    }

    private void appendChat(String sender, String message) {
        chatHistory.append(sender + ": " + message + "\n");
        chatHistory.setCaretPosition(chatHistory.getDocument().getLength());
    }

    // ======================== NETWORKING ========================
    private void connectToServer() {
        new Thread(() -> {
            try {
                socket = new Socket("127.0.0.1", 12345);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                connected = true;
                SwingUtilities.invokeLater(() -> {
                    loginBtn.setEnabled(true);
                    registerBtn.setEnabled(true);
                    appendStatus("✅ Connected! Enter credentials or register.\n");
                });
                new Thread(this::listenForMessages).start();
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    appendStatus("❌ ERROR: Server not running. Start Server.java first.\n");
                    loginBtn.setEnabled(false);
                    registerBtn.setEnabled(false);
                });
            }
        }).start();
    }

    private void attemptLogin() {
        if (!connected) {
            appendStatus("⏳ Waiting for connection...\n");
            return;
        }
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword()).trim();
        if (user.isEmpty() || pass.isEmpty()) {
            appendStatus("⚠️ Enter username and password.\n");
            return;
        }
        currentUser = user;
        sendMessage("LOGIN|" + user + "|" + pass);
        appendStatus("⚔️ Logging in as " + user + "...\n");
    }

    private void listenForMessages() {
        try {
            String encrypted;
            while ((encrypted = in.readLine()) != null) {
                String plain = CryptoUtil.decrypt(encrypted);
                String[] parts = plain.split("\\|", 2);
                String cmd = parts[0];
                String data = (parts.length > 1) ? parts[1] : "";
                SwingUtilities.invokeLater(() -> handleServerCommand(cmd, data));
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> appendStatus("❌ Connection lost.\n"));
        }
    }

    // ======================== COMMAND HANDLER ========================
    private void handleServerCommand(String cmd, String data) {
        switch (cmd) {
            case "AUTH_FAIL" -> appendStatus("❌ Login failed: " + data + "\n");
            case "REGISTER_SUCCESS" -> {
                appendStatus("✅ " + data + "\n");
                JOptionPane.showMessageDialog(this, "Account created! Please login.");
            }
            case "REGISTER_FAIL" -> {
                appendStatus("❌ " + data + "\n");
                JOptionPane.showMessageDialog(this, "Registration failed: " + data);
            }
            case "MFA_REQUIRED" -> {
                mfaOrder = data;
                String[] orderStr = data.split(",");
                for (int i = 0; i < 9; i++) {
                    tileOrder[i] = Integer.parseInt(orderStr[i]);
                }
                mfaActive = true;
                mfaSolved = false;
                selectedTileIndex = -1;
                cardLayout.show(mainPanel, "MFA");
                updateTileImages();
                for (JButton btn : tileButtons) btn.setBorder(null);
                appendStatus("🔐 MFA challenge required. Arrange the puzzle.\n");
            }
            case "MFA_FAIL" -> {
                JOptionPane.showMessageDialog(this, "Puzzle not solved. Try again.");
                mfaActive = true;
                mfaSolved = false;
            }
            case "AUTH_SUCCESS" -> {
                appendStatus("✅ Welcome, " + currentUser + "!\n");
                cardLayout.show(mainPanel, "Game");
                chatInput.setEnabled(true);
                sendBtn.setEnabled(true);
                chatInput.requestFocusInWindow();
                appendChat("System", "You joined the dojo!");
                retryBtn.setEnabled(false);
                retryOpacity = 0f;
                retryBtn.setBackground(new Color(0, 150, 50, 0));
                gamePanel.setWaitingMessage("");
                gamePanel.setGameActive(true);
            }
            case "FRUIT_SPAWN" -> {
                String[] d = data.split("\\|");
                if (d.length == 7) {
                    int id = Integer.parseInt(d[0]);
                    double x = Double.parseDouble(d[1]);
                    double y = Double.parseDouble(d[2]);
                    double vx = Double.parseDouble(d[3]);
                    double vy = Double.parseDouble(d[4]);
                    int radius = Integer.parseInt(d[5]);
                    int type = Integer.parseInt(d[6]);
                    gamePanel.addFruit(id, x, y, vx, vy, radius, type);
                }
            }
            case "FRUIT_SLICED" -> {
                String[] parts = data.split("\\|");
                if (parts.length >= 6) {
                    int fruitId = Integer.parseInt(parts[0]);
                    String who = parts[1];
                    int combo = Integer.parseInt(parts[2]);
                    int points = Integer.parseInt(parts[3]);
                    String bonus = parts[4];
                    gamePanel.onFruitSliced(fruitId, who, combo, points, bonus);
                }
            }
            case "SLASH_RESULT" -> {
                String[] d = data.split("\\|");
                if (d[0].equals("SUCCESS")) gamePanel.setLastMessage("✅ " + d[1]);
                else gamePanel.setLastMessage("❌ " + d[1]);
            }
            case "SCORE" -> {
                try { gamePanel.updateScore(Integer.parseInt(data)); } catch (NumberFormatException ignored) {}
            }
            case "CHAT" -> {
                String[] parts = data.split("\\|", 2);
                if (parts.length == 2) appendChat(parts[0], parts[1]);
            }
            case "PRIVATE" -> {
                String[] parts = data.split("\\|", 2);
                if (parts.length == 2) appendChat("💬 " + parts[0] + " (private)", parts[1]);
            }
            case "PRIVATE_SENT" -> gamePanel.setLastMessage("📨 " + data);
            case "ACCOUNT_DELETED" -> {
                JOptionPane.showMessageDialog(this, "Your account has been deleted. Goodbye!");
                System.exit(0);
            }
            case "TIME" -> {
                try { gamePanel.updateTime(Integer.parseInt(data)); } catch (NumberFormatException ignored) {}
            }
            case "WAITING" -> {
                gamePanel.setWaitingMessage(data);
                gamePanel.setGameActive(false);
                gamePanel.setLastMessage("⏳ " + data);
            }
            case "GAME_OVER" -> {
                gamePanel.setGameActive(false);
                gamePanel.setLastMessage("⏰ " + data);
                chatInput.setEnabled(false);
                sendBtn.setEnabled(false);
                appendChat("System", "⏰ Time's up! Click Retry to start again.");
                retryBtn.setEnabled(true);
                retryOpacity = 0f;
                retryBtn.setBackground(new Color(0, 150, 50, 0));
                fadeTimer.start();
                sendMessage("GET_LEADERBOARD");
            }
            case "GAME_STARTING" -> {
                gamePanel.setGameActive(true);
                gamePanel.setWaitingMessage("");
                gamePanel.setLastMessage("🍉 " + data);
                chatInput.setEnabled(true);
                sendBtn.setEnabled(true);
                chatInput.requestFocusInWindow();
                appendChat("System", "🍉 New round started! Slice fruits!");
                retryBtn.setEnabled(false);
                retryOpacity = 0f;
                retryBtn.setBackground(new Color(0, 150, 50, 0));
                fadeTimer.stop();
            }
            case "LEADERBOARD_DATA" -> {
                if (data.isEmpty()) appendChat("System", "🏆 No scores yet.");
                else {
                    String[] entries = data.split(",");
                    StringBuilder sb = new StringBuilder("🏆 LEADERBOARD 🏆\n");
                    int rank = 1;
                    for (String entry : entries) {
                        String[] u = entry.split(":");
                        sb.append(rank++).append(". ").append(u[0]).append(" - ").append(u[1]).append(" pts\n");
                    }
                    appendChat("System", sb.toString());
                }
            }
            default -> appendStatus("Unknown command: " + cmd);
        }
    }

    public void sendMessage(String plainText) {
        try {
            if (out == null) return;
            String encrypted = CryptoUtil.encrypt(plainText);
            out.println(encrypted);
        } catch (Exception e) {
            appendStatus("Encryption error.\n");
        }
    }

    private void appendStatus(String text) {
        if (statusArea != null) {
            statusArea.append(text);
            statusArea.setCaretPosition(statusArea.getDocument().getLength());
        }
    }

    // ======================== GETTER FOR CURRENT USER ========================
    public String getCurrentUser() {
        return currentUser;
    }

    // ======================== MAIN ========================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SplashScreen());
    }
}