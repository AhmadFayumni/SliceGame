package client;

import javax.swing.*;
import java.awt.*;

public class SplashScreen extends JWindow {
    private JProgressBar progressBar;
    private int progress = 0;

    public SplashScreen() {
        setSize(400, 300);
        setLocationRelativeTo(null);
        setBackground(new Color(20, 30, 45));

        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(15, 25, 45),
                        0, getHeight(), new Color(5, 10, 20));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(new Color(255, 200, 50));
                g2d.setFont(new Font("Segoe UI Emoji", Font.BOLD, 60));
                g2d.drawString("🍉", 170, 120);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 24));
                g2d.setColor(Color.WHITE);
                g2d.drawString("NINJA DOJO", 120, 180);
            }
        };
        panel.setOpaque(false);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(255, 140, 0));
        progressBar.setBackground(new Color(50, 50, 70));
        progressBar.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        panel.add(progressBar, BorderLayout.SOUTH);

        setContentPane(panel);
        setVisible(true);

        new Thread(() -> {
            while (progress < 100) {
                progress++;
                SwingUtilities.invokeLater(() -> progressBar.setValue(progress));
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            }
            SwingUtilities.invokeLater(() -> {
                dispose();
                new FruitNinjaClientGUI().setVisible(true);
            });
        }).start();
    }
}