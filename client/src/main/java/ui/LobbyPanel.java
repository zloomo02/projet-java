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

    private static JsonObject pendingRoomList;

    public LobbyPanel(AppState appState) {
        this.appState = appState;
        setLayout(new BorderLayout(8,8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton createBtn = new JButton("Create Room");
        JButton joinBtn = new JButton("Join Room");
        JButton readyBtn = new JButton("Ready");
        top.add(createBtn);
        top.add(joinBtn);
        top.add(readyBtn);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(roomsList), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        createBtn.addActionListener(e -> onCreateRoom());
        joinBtn.addActionListener(e -> onJoinRoom());
        readyBtn.addActionListener(e -> onReady());
    }

    @Override
    public void onShow() {
        statusLabel.setText("");
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
        JsonObject payload = new JsonObject();
        payload.addProperty("roomName", selected);
        try {
            appState.send(Message.of("JOIN_ROOM", payload, appState.getUsername()));
        } catch (IOException e) {
            showError("Failed to send JOIN_ROOM: " + e.getMessage());
        }
    }

    private void onReady() {
        try {
            appState.send(Message.of("READY", new JsonObject(), appState.getUsername()));
        } catch (IOException e) {
            showError("Failed to send READY: " + e.getMessage());
        }
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
        List<String> names = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            String name = obj.has("roomName") ? obj.get("roomName").getAsString() : "";
            names.add(name);
        }
        names.forEach(roomsModel::addElement);
    }

    private void showError(String text) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText(text);
    }

    public static void setPendingRoomListPayload(JsonObject payload) {
        pendingRoomList = payload;
    }
}
