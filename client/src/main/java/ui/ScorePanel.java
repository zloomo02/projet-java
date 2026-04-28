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
    private final JButton backToLobbyButton = new JButton("Back to Lobby");
    private static JsonObject pendingGameOverPayload;

    public ScorePanel(AppState appState) {
        this.appState = appState;
        setLayout(new BorderLayout(8,8));
        rankingArea.setEditable(false);
        Theme.styleTextArea(rankingArea);
        JScrollPane rankingPane = new JScrollPane(rankingArea);
        Theme.styleScrollPane(rankingPane);

        JPanel card = new JPanel(new BorderLayout(8,8));
        Theme.stylePanel(card);
        JLabel title = new JLabel("Final Ranking");
        Theme.styleTitle(title);
        card.add(title, BorderLayout.NORTH);
        card.add(rankingPane, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        Theme.styleSecondaryButton(backToLobbyButton);
        Theme.styleSecondaryButton(logoutButton);
        actions.add(backToLobbyButton);
        actions.add(logoutButton);
        card.add(actions, BorderLayout.SOUTH);

        add(card, BorderLayout.CENTER);

        logoutButton.addActionListener(e -> onLogout());
        backToLobbyButton.addActionListener(e -> onBackToLobby());
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
            switch (message.getType()) {
                case "GAME_OVER" -> renderRanking(message.getDataAsJsonObject());
                case "ROOM_LIST" -> LobbyPanel.setPendingRoomListPayload(message.getDataAsJsonObject());
                default -> {}
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

    private void onBackToLobby() {
        setPendingGameOverPayload(null);
        SwingNavigator.getInstance().show("lobby");
    }
}
