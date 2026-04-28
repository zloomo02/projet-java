package ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import protocol.Message;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class GamePanel extends BasePanel {
    private final AppState appState;
    private final JLabel questionLabel = new JLabel("Waiting...");
    private final JButton[] optionButtons = new JButton[4];
    private final JTextArea scoresArea = new JTextArea();

    public GamePanel(AppState appState) {
        this.appState = appState;
        setLayout(new BorderLayout(8,8));

        JPanel qPanel = new JPanel(new BorderLayout(8,8));
        Theme.stylePanel(qPanel);
        Theme.styleTitle(questionLabel);
        qPanel.add(questionLabel, BorderLayout.NORTH);

        JPanel options = new JPanel(new GridLayout(2,2,8,8));
        options.setOpaque(false);
        for (int i = 0; i < 4; i++) {
            optionButtons[i] = new JButton("Option " + (char)('A'+i));
            Theme.stylePrimaryButton(optionButtons[i]);
            int idx = i;
            optionButtons[i].addActionListener(e -> onAnswer((char)('A'+idx) + ""));
            options.add(optionButtons[i]);
        }

        qPanel.add(options, BorderLayout.CENTER);
        add(qPanel, BorderLayout.CENTER);

        scoresArea.setEditable(false);
        Theme.styleTextArea(scoresArea);
        JScrollPane scoresPane = new JScrollPane(scoresArea);
        Theme.styleScrollPane(scoresPane);
        JPanel scoresCard = new JPanel(new BorderLayout());
        Theme.stylePanel(scoresCard);
        scoresCard.add(scoresPane, BorderLayout.CENTER);
        add(scoresCard, BorderLayout.EAST);
    }

    @Override
    public void onShow() {
        appState.setMessageHandler(this::handleServerMessage);
        questionLabel.setText("Waiting for game to start...");
        scoresArea.setText("");
    }

    private void onAnswer(String answer) {
        JsonObject payload = new JsonObject();
        payload.addProperty("answer", answer);
        try {
            appState.send(Message.of("ANSWER", payload, appState.getUsername()));
        } catch (IOException e) {
            // ignore
        }
    }

    private void handleServerMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            switch (message.getType()) {
                case "QUESTION" -> {
                    JsonObject payload = message.getDataAsJsonObject();
                    questionLabel.setText(payload.get("text").getAsString());
                    optionButtons[0].setText("A: " + payload.get("optionA").getAsString());
                    optionButtons[1].setText("B: " + payload.get("optionB").getAsString());
                    optionButtons[2].setText("C: " + payload.get("optionC").getAsString());
                    optionButtons[3].setText("D: " + payload.get("optionD").getAsString());
                }
                case "SCORES_UPDATE" -> {
                    JsonObject payload = message.getDataAsJsonObject();
                    JsonArray arr = payload.getAsJsonArray("scores");
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject obj = arr.get(i).getAsJsonObject();
                        sb.append(obj.get("username").getAsString())
                          .append(": ")
                          .append(obj.get("score").getAsInt())
                          .append('\n');
                    }
                    scoresArea.setText(sb.toString());
                }
                case "ROOM_LIST" -> LobbyPanel.setPendingRoomListPayload(message.getDataAsJsonObject());
                case "GAME_OVER" -> {
                    ScorePanel.setPendingGameOverPayload(message.getDataAsJsonObject());
                    SwingNavigator.getInstance().show("score");
                }
                default -> {}
            }
        });
    }
}
