package ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Duration;
import protocol.Message;

import java.io.IOException;

public class GameController {
    private static JsonObject pendingQuestionPayload;

    @FXML
    private Label questionLabel;

    @FXML
    private Label feedbackLabel;

    @FXML
    private Button optionAButton;

    @FXML
    private Button optionBButton;

    @FXML
    private Button optionCButton;

    @FXML
    private Button optionDButton;

    @FXML
    private ProgressBar timerProgress;

    @FXML
    private TableView<ScoreRow> scoresTable;

    @FXML
    private TableColumn<ScoreRow, String> usernameColumn;

    @FXML
    private TableColumn<ScoreRow, Number> scoreColumn;

    private final ObservableList<ScoreRow> scores = FXCollections.observableArrayList();
    private final AppState appState = AppState.getInstance();

    private Timeline timerTimeline;
    private int timeLimitSeconds;
    private String selectedAnswer;

    @FXML
    public void initialize() {
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().username()));
        scoreColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().score()));
        scoresTable.setItems(scores);

        feedbackLabel.setText("");
        disableAnswerButtons(true);

        appState.setMessageHandler(this::handleServerMessage);

        if (pendingQuestionPayload != null) {
            displayQuestion(pendingQuestionPayload);
            pendingQuestionPayload = null;
        }
    }

    public static void setPendingQuestionPayload(JsonObject payload) {
        pendingQuestionPayload = payload;
    }

    /**
     * Gere le clic sur une reponse: envoi ANSWER et verrouillage des boutons.
     */
    @FXML
    private void onAnswerAClicked() {
        submitAnswer("A", optionAButton);
    }

    @FXML
    private void onAnswerBClicked() {
        submitAnswer("B", optionBButton);
    }

    @FXML
    private void onAnswerCClicked() {
        submitAnswer("C", optionCButton);
    }

    @FXML
    private void onAnswerDClicked() {
        submitAnswer("D", optionDButton);
    }

    private void submitAnswer(String answer, Button clickedButton) {
        selectedAnswer = answer;
        disableAnswerButtons(true);

        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("answer", answer);
            appState.send(Message.of("ANSWER", payload, appState.getUsername()));
        } catch (IOException e) {
            feedbackLabel.setText("Erreur envoi reponse: " + e.getMessage());
        }

        clickedButton.getStyleClass().add("selected-answer");
    }

    /**
     * Route les messages de partie: question, resultats, scores, fin de jeu.
     */
    private void handleServerMessage(Message message) {
        Platform.runLater(() -> {
            switch (message.getType()) {
                case "QUESTION" -> displayQuestion(message.getDataAsJsonObject());
                case "ANSWER_RESULT" -> displayAnswerResult(message.getDataAsJsonObject());
                case "SCORES_UPDATE" -> refreshScores(message.getDataAsJsonObject());
                case "PLAYER_LEFT" -> {
                    JsonObject payload = message.getDataAsJsonObject();
                    String username = payload.has("username") ? payload.get("username").getAsString() : "Un joueur";
                    feedbackLabel.setText(username + " a quitte la partie.");
                }
                case "GAME_OVER" -> {
                    ScoreController.setRankingPayload(message.getDataAsJsonObject());
                    SceneNavigator.switchTo("/ui/views/score.fxml", "Quiz Multijoueur - Resultats");
                }
                case "ERROR" -> feedbackLabel.setText("Erreur: " + message.getDataAsJsonObject().get("message").getAsString());
                default -> {
                    // Ignore.
                }
            }
        });
    }

    private void displayQuestion(JsonObject payload) {
        questionLabel.setText(payload.get("text").getAsString());

        optionAButton.setText("A. " + payload.get("optionA").getAsString());
        optionBButton.setText("B. " + payload.get("optionB").getAsString());
        optionCButton.setText("C. " + payload.get("optionC").getAsString());
        optionDButton.setText("D. " + payload.get("optionD").getAsString());

        selectedAnswer = null;
        feedbackLabel.setText("");

        clearAnswerStyles();
        disableAnswerButtons(false);

        timeLimitSeconds = payload.get("timeLimit").getAsInt();
        startTimerAnimation(timeLimitSeconds);
    }

    private void displayAnswerResult(JsonObject payload) {
        boolean correct = payload.get("correct").getAsBoolean();
        String correctAnswer = payload.get("correctAnswer").getAsString();
        int points = payload.get("pointsEarned").getAsInt();

        feedbackLabel.setText(correct ? "Bonne reponse ! +" + points : "Mauvaise reponse. Bonne reponse: " + correctAnswer);
        colorAnswerButtons(correct, correctAnswer);
    }

    private void refreshScores(JsonObject payload) {
        scores.clear();
        if (!payload.has("scores")) {
            return;
        }

        JsonArray array = payload.getAsJsonArray("scores");
        for (JsonElement element : array) {
            JsonObject row = element.getAsJsonObject();
            scores.add(new ScoreRow(row.get("username").getAsString(), row.get("score").getAsInt()));
        }
    }

    private void startTimerAnimation(int seconds) {
        if (timerTimeline != null) {
            timerTimeline.stop();
        }

        timerProgress.setProgress(1.0);
        timerTimeline = new Timeline(new KeyFrame(Duration.millis(100), event -> {
            double decrement = 0.1 / seconds;
            double next = Math.max(0.0, timerProgress.getProgress() - decrement);
            timerProgress.setProgress(next);
        }));
        timerTimeline.setCycleCount(seconds * 10);
        timerTimeline.playFromStart();
    }

    private void colorAnswerButtons(boolean correct, String correctAnswer) {
        Button selectedButton = switch (selectedAnswer == null ? "" : selectedAnswer) {
            case "A" -> optionAButton;
            case "B" -> optionBButton;
            case "C" -> optionCButton;
            case "D" -> optionDButton;
            default -> null;
        };

        if (selectedButton != null) {
            selectedButton.getStyleClass().removeAll("answer-correct", "answer-wrong");
            selectedButton.getStyleClass().add(correct ? "answer-correct" : "answer-wrong");
        }

        Button correctButton = switch (correctAnswer) {
            case "A" -> optionAButton;
            case "B" -> optionBButton;
            case "C" -> optionCButton;
            case "D" -> optionDButton;
            default -> null;
        };

        if (correctButton != null && correctButton != selectedButton) {
            correctButton.getStyleClass().removeAll("answer-correct", "answer-wrong");
            correctButton.getStyleClass().add("answer-correct");
        }
    }

    private void clearAnswerStyles() {
        optionAButton.getStyleClass().removeAll("selected-answer", "answer-correct", "answer-wrong");
        optionBButton.getStyleClass().removeAll("selected-answer", "answer-correct", "answer-wrong");
        optionCButton.getStyleClass().removeAll("selected-answer", "answer-correct", "answer-wrong");
        optionDButton.getStyleClass().removeAll("selected-answer", "answer-correct", "answer-wrong");
    }

    private void disableAnswerButtons(boolean disable) {
        optionAButton.setDisable(disable);
        optionBButton.setDisable(disable);
        optionCButton.setDisable(disable);
        optionDButton.setDisable(disable);
    }

    public record ScoreRow(String username, int score) {
    }
}
