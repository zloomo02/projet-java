package ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import protocol.Message;

import javax.swing.*;
import java.awt.*;

public class ScorePanel extends BasePanel {
    private final AppState appState;
    private final JTextArea rankingArea = new JTextArea();

    public ScorePanel(AppState appState) {
        this.appState = appState;
        setLayout(new BorderLayout(8,8));
        rankingArea.setEditable(false);
        add(new JScrollPane(rankingArea), BorderLayout.CENTER);
    }

    @Override
    public void onShow() {
        appState.setMessageHandler(this::handleServerMessage);
    }

    private void handleServerMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            if ("GAME_OVER".equals(message.getType())) {
                JsonObject payload = message.getDataAsJsonObject();
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
        });
    }
}
