package ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import protocol.Message;

import javax.swing.*;
import java.awt.*;

public class ScorePanel extends BasePanel {
    private final AppState appState;
    private final JTextArea rankingArea = new JTextArea();
    private final JButton logoutButton = new JButton("Logout");
    private static JsonObject pendingGameOverPayload;

    public ScorePanel(AppState appState) {
        this.appState = appState;
        setLayout(new BorderLayout(8,8));
        rankingArea.setEditable(false);
        add(new JScrollPane(rankingArea), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(logoutButton);
        add(actions, BorderLayout.SOUTH);

        logoutButton.addActionListener(e -> onLogout());
    }

    @Override
    public void onShow() {
        appState.setMessageHandler(this::handleServerMessage);
        if (pendingGameOverPayload != null) {
            renderRanking(pendingGameOverPayload);
            pendingGameOverPayload = null;
        } else if (rankingArea.getText().isBlank()) {
            rankingArea.setText("Waiting for results...");
        }
    }

    private void handleServerMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            if ("GAME_OVER".equals(message.getType())) {
                renderRanking(message.getDataAsJsonObject());
            }
        });
    }

    private void renderRanking(JsonObject payload) {
        if (payload == null || !payload.has("ranking")) {
            rankingArea.setText("No results yet.");
            return;
        }
        JsonArray arr = payload.getAsJsonArray("ranking");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.size(); i++) {
            JsonObject row = arr.get(i).getAsJsonObject();
            sb.append(row.get("rank").getAsInt())
              .append(". ")
              .append(row.get("username").getAsString())
              .append(" - ")
              .append(row.get("score").getAsInt())
              .append('\n');
        }
        rankingArea.setText(sb.toString());
    }

    public static void setPendingGameOverPayload(JsonObject payload) {
        pendingGameOverPayload = payload;
    }

    private void onLogout() {
        appState.logout();
        LobbyPanel.setPendingRoomListPayload(null);
        setPendingGameOverPayload(null);
        SwingNavigator.getInstance().show("login");
    }
}
