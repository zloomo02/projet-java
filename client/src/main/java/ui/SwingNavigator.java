package ui;

import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class SwingNavigator {
    private static SwingNavigator INSTANCE;

    private final JFrame frame;
    private final JPanel container;
    private final CardLayout cardLayout;
    private final Map<String, BasePanel> panels = new HashMap<>();

    private SwingNavigator() {
        frame = new JFrame("Quiz Multijoueur");
        cardLayout = new CardLayout();
        container = new JPanel(cardLayout);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.add(container);
    }

    public static synchronized SwingNavigator getInstance() {
        if (INSTANCE == null) INSTANCE = new SwingNavigator();
        return INSTANCE;
    }

    public void register(String name, BasePanel panel) {
        panels.put(name, panel);
        container.add(panel, name);
    }

    public void show(String name) {
        BasePanel panel = panels.get(name);
        if (panel != null) {
            cardLayout.show(container, name);
            panel.onShow();
        }
        frame.setVisible(true);
    }

    public JFrame getFrame() {
        return frame;
    }
}
