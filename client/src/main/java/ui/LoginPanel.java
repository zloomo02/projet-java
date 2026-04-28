package ui;

import com.google.gson.JsonObject;
import protocol.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class LoginPanel extends BasePanel {
    private final AppState appState;
    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JLabel statusLabel = new JLabel();

    public LoginPanel(AppState appState) {
        this.appState = appState;
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8,8,8,8);

        c.gridx = 0; c.gridy = 0; add(new JLabel("Username:"), c);
        c.gridx = 1; add(usernameField, c);
        c.gridx = 0; c.gridy = 1; add(new JLabel("Password:"), c);
        c.gridx = 1; add(passwordField, c);

        JPanel buttons = new JPanel();
        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");
        buttons.add(loginBtn);
        buttons.add(registerBtn);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; add(buttons, c);
        c.gridy = 3; add(statusLabel, c);

        loginBtn.addActionListener(this::onLogin);
        registerBtn.addActionListener(this::onRegister);
    }

    @Override
    public void onShow() {
        statusLabel.setText("");
        usernameField.setText("");
        passwordField.setText("");

        try {
            appState.connect(this::handleServerMessage);
        } catch (IOException e) {
            showError("Connexion serveur impossible: " + e.getMessage());
        }
    }

    private void onLogin(ActionEvent ev) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        if (username.isBlank() || password.isBlank()) {
            showError("Please enter username and password.");
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("username", username);
        payload.addProperty("password", password);

        try {
            appState.send(Message.of("LOGIN", payload, username));
        } catch (IOException e) {
            showError("Erreur d'envoi LOGIN: " + e.getMessage());
        }
    }

    private void onRegister(ActionEvent ev) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        if (username.isBlank() || password.isBlank()) {
            showError("Please enter username and password.");
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("username", username);
        payload.addProperty("password", password);

        try {
            appState.send(Message.of("REGISTER", payload, username));
        } catch (IOException e) {
            showError("Erreur d'envoi REGISTER: " + e.getMessage());
        }
    }

    private void handleServerMessage(protocol.Message message) {
        SwingUtilities.invokeLater(() -> {
            switch (message.getType()) {
                case "LOGIN_OK" -> {
                    JsonObject payload = message.getDataAsJsonObject();
                    appState.setPlayerId(payload.get("playerId").getAsInt());
                    appState.setUsername(payload.get("username").getAsString());
                    SwingNavigator.getInstance().show("lobby");
                }
                case "LOGIN_FAIL" -> {
                    JsonObject payload = message.getDataAsJsonObject();
                    String reason = payload.has("reason") ? payload.get("reason").getAsString() : "Login failed";
                    showError(reason);
                }
                case "REGISTER_OK" -> statusLabel.setText("Registration succeeded. You may login.");
                case "ROOM_LIST" -> LobbyPanel.setPendingRoomListPayload(message.getDataAsJsonObject());
                case "ERROR" -> {
                    JsonObject payload = message.getDataAsJsonObject();
                    String msg = payload.has("message") ? payload.get("message").getAsString() : "Unknown error";
                    showError(msg);
                }
                default -> {}
            }
        });
    }

    private void showError(String text) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText(text);
    }
}
