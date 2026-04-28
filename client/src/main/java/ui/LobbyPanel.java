package ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import protocol.Message;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LobbyPanel extends BasePanel {
    private final AppState appState;
    private final DefaultListModel<String> roomsModel = new DefaultListModel<>();
    private final JList<String> roomsList = new JList<>(roomsModel);
    private final JLabel statusLabel = new JLabel();
    private final JButton readyBtn = new JButton("Ready");

    private static JsonObject pendingRoomList;

    public LobbyPanel(AppState appState) {
        this.appState = appState;
        setLayout(new BorderLayout(8,8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        top.setOpaque(false);
        JButton createBtn = new JButton("Create Room");
        JButton joinBtn = new JButton("Join Room");
        JButton logoutBtn = new JButton("Logout");
        Theme.stylePrimaryButton(createBtn);
        Theme.styleSecondaryButton(joinBtn);
        Theme.styleReadyButtonIdle(readyBtn);
        Theme.styleSecondaryButton(logoutBtn);
        top.add(createBtn);
        top.add(joinBtn);
        top.add(readyBtn);
        top.add(logoutBtn);

        JPanel card = new JPanel(new BorderLayout(8,8));
        Theme.stylePanel(card);
        card.add(top, BorderLayout.NORTH);
        JScrollPane listPane = new JScrollPane(roomsList);
        Theme.styleList(roomsList);
        Theme.styleScrollPane(listPane);
        card.add(listPane, BorderLayout.CENTER);
        Theme.styleStatusLabel(statusLabel);
        card.add(statusLabel, BorderLayout.SOUTH);

        add(card, BorderLayout.CENTER);

        createBtn.addActionListener(e -> onCreateRoom());
        joinBtn.addActionListener(e -> onJoinRoom());
        readyBtn.addActionListener(e -> onReady());
        logoutBtn.addActionListener(e -> onLogout());
    }

    @Override
    public void onShow() {
        statusLabel.setText("");
        Theme.styleReadyButtonIdle(readyBtn);
        readyBtn.setEnabled(true);
        appState.setMessageHandler(this::handleServerMessage);
        if (pendingRoomList != null) {
            updateRoomList(pendingRoomList);
            pendingRoomList = null;
        }
    }

    private void onCreateRoom() {
        String roomName = JOptionPane.showInputDialog(this, "Room name:");
        if (roomName == null || roomName.trim().isEmpty()) return;

        String maxStr = JOptionPane.showInputDialog(this, "Max players (2-8):", "4");
        int max = 4;
        try { max = Integer.parseInt(maxStr); } catch (Exception ignored) {}

        JsonObject payload = new JsonObject();
        payload.addProperty("roomName", roomName.trim());
        payload.addProperty("maxPlayers", max);
        try {
            appState.send(Message.of("CREATE_ROOM", payload, appState.getUsername()));
        } catch (IOException e) {
            showError("Failed to send CREATE_ROOM: " + e.getMessage());
        }
    }

    private void onJoinRoom() {
        String selected = roomsList.getSelectedValue();
        if (selected == null) {
            showError("Select a room to join.");
            return;
        }
        // selected is formatted as "<name> (players/max)" - extract the name part
        String roomName = selected;
        int idx = selected.indexOf(" (");
        if (idx > 0) {
            roomName = selected.substring(0, idx);
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("roomName", roomName);
        try {
            appState.send(Message.of("JOIN_ROOM", payload, appState.getUsername()));
        } catch (IOException e) {
            showError("Failed to send JOIN_ROOM: " + e.getMessage());
        }
    }

    private void onReady() {
        try {
            appState.send(Message.of("READY", new JsonObject(), appState.getUsername()));
            Theme.styleReadyButtonActive(readyBtn);
            readyBtn.setEnabled(false);
            readyBtn.revalidate();
            readyBtn.repaint();
        } catch (IOException e) {
            showError("Failed to send READY: " + e.getMessage());
        }
    }

    private void onLogout() {
        appState.logout();
        setPendingRoomListPayload(null);
        ScorePanel.setPendingGameOverPayload(null);
        SwingNavigator.getInstance().show("login");
    }

    private void handleServerMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            switch (message.getType()) {
                case "ROOM_LIST" -> updateRoomList(message.getDataAsJsonObject());
                case "ROOM_JOINED" -> {
                    // Could display players; switch to game when started.
                    statusLabel.setText("Joined room.");
                }
                case "GAME_START" -> SwingNavigator.getInstance().show("game");
                case "ERROR" -> {
                    JsonObject payload = message.getDataAsJsonObject();
                    showError(payload.has("message") ? payload.get("message").getAsString() : "Error");
                }
                default -> {}
            }
        });
    }

    private void updateRoomList(JsonObject payload) {
        roomsModel.clear();
        if (payload == null) return;
        JsonArray array = payload.has("rooms") ? payload.getAsJsonArray("rooms") : new JsonArray();
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            // server sends 'name', 'players', 'max' in GameManager.buildRoomListPayload()
            String name = obj.has("name") ? obj.get("name").getAsString()
                : obj.has("roomName") ? obj.get("roomName").getAsString() : "";
            int players = obj.has("players") ? obj.get("players").getAsInt() : (obj.has("playerCount") ? obj.get("playerCount").getAsInt() : 0);
            int max = obj.has("max") ? obj.get("max").getAsInt() : (obj.has("maxPlayers") ? obj.get("maxPlayers").getAsInt() : 0);
            String display = String.format("%s (%d/%d)", name, players, max);
            roomsModel.addElement(display);
        }
    }

    private void showError(String text) {
        statusLabel.setForeground(Theme.ERROR);
        statusLabel.setText(text);
    }

    public static void setPendingRoomListPayload(JsonObject payload) {
        pendingRoomList = payload;
    }
}
