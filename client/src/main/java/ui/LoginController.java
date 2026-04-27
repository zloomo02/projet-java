package ui;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import protocol.Message;

import java.io.IOException;

public class LoginController {
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    private final AppState appState = AppState.getInstance();

    @FXML
    public void initialize() {
        errorLabel.setText("");

        try {
            appState.connect(this::handleServerMessage);
        } catch (IOException e) {
            showError("Connexion serveur impossible: " + e.getMessage());
        }
    }

    /**
     * Envoie une demande de connexion au serveur.
     */
    @FXML
    private void onLoginClicked() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText().trim();

        if (username.isBlank() || password.isBlank()) {
            showError("Veuillez saisir un username et un mot de passe.");
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

    /**
     * Envoie une demande d'inscription au serveur.
     */
    @FXML
    private void onRegisterClicked() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText().trim();

        if (username.isBlank() || password.isBlank()) {
            showError("Veuillez saisir un username et un mot de passe.");
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

    /**
     * Traite les reponses serveur utiles a l'ecran de connexion.
     */
    private void handleServerMessage(Message message) {
        Platform.runLater(() -> {
            switch (message.getType()) {
                case "LOGIN_OK" -> {
                    JsonObject payload = message.getDataAsJsonObject();
                    appState.setPlayerId(payload.get("playerId").getAsInt());
                    appState.setUsername(payload.get("username").getAsString());
                    SceneNavigator.switchTo("/ui/views/lobby.fxml", "Quiz Multijoueur - Lobby");
                }
                case "LOGIN_FAIL" -> {
                    JsonObject payload = message.getDataAsJsonObject();
                    String reason = payload.has("reason") ? payload.get("reason").getAsString() : "Connexion refusee";
                    showError(reason);
                }
                case "REGISTER_OK" -> errorLabel.setText("Inscription reussie, connectez-vous.");
                case "ROOM_LIST" -> LobbyController.setPendingRoomListPayload(message.getDataAsJsonObject());
                case "ERROR" -> {
                    JsonObject payload = message.getDataAsJsonObject();
                    String msg = payload.has("message") ? payload.get("message").getAsString() : "Erreur inconnue";
                    showError(msg);
                }
                default -> {
                    // Ignore les autres messages sur cet ecran.
                }
            }
        });
    }

    private void showError(String text) {
        errorLabel.setStyle("-fx-text-fill: #c0392b;");
        errorLabel.setText(text);
    }
}
