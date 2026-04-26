package ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import protocol.Message;

import java.io.IOException;

public class LobbyController {
    private static JsonObject pendingRoomListPayload;

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private TextField roomNameField;

    @FXML
    private TextField maxPlayersField;

    @FXML
    private TableView<RoomRow> roomsTable;

    @FXML
    private TableColumn<RoomRow, String> roomNameColumn;

    @FXML
    private TableColumn<RoomRow, Number> playersColumn;

    @FXML
    private TableColumn<RoomRow, Number> maxColumn;

    private final ObservableList<RoomRow> roomRows = FXCollections.observableArrayList();
    private final AppState appState = AppState.getInstance();

    public static void setPendingRoomListPayload(JsonObject payload) {
        pendingRoomListPayload = payload;
    }

    @FXML
    public void initialize() {
        welcomeLabel.setText("Bienvenue, " + appState.getUsername());

        roomNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
        playersColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().players()));
        maxColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().max()));

        roomsTable.setItems(roomRows);

        appState.setMessageHandler(this::handleServerMessage);

        if (pendingRoomListPayload != null) {
            refreshRoomTable(pendingRoomListPayload);
            pendingRoomListPayload = null;
        }

        setStatus("En attente des rooms disponibles...", false);
    }

    /**
     * Cree une nouvelle room sur le serveur.
     */
    @FXML
    private void onCreateRoomClicked() {
        String roomName = roomNameField.getText() == null ? "" : roomNameField.getText().trim();
        int maxPlayers = 4;

        if (roomName.isBlank()) {
            setStatus("Nom de room obligatoire.", true);
            return;
        }

        try {
            if (maxPlayersField.getText() != null && !maxPlayersField.getText().isBlank()) {
                maxPlayers = Integer.parseInt(maxPlayersField.getText().trim());
            }
        } catch (NumberFormatException e) {
            setStatus("maxPlayers doit etre un nombre.", true);
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("roomName", roomName);
        payload.addProperty("maxPlayers", maxPlayers);

        try {
            appState.send(Message.of("CREATE_ROOM", payload, appState.getUsername()));
        } catch (IOException e) {
            setStatus("Erreur CREATE_ROOM: " + e.getMessage(), true);
        }
    }

    /**
     * Rejoint la room selectionnee dans le tableau.
     */
    @FXML
    private void onJoinRoomClicked() {
        RoomRow selected = roomsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Selectionnez une room.", true);
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("roomName", selected.name());

        try {
            appState.send(Message.of("JOIN_ROOM", payload, appState.getUsername()));
        } catch (IOException e) {
            setStatus("Erreur JOIN_ROOM: " + e.getMessage(), true);
        }
    }

    /**
     * Marque le joueur pret a demarrer.
     */
    @FXML
    private void onReadyClicked() {
        try {
            appState.send(Message.of("READY", new JsonObject(), appState.getUsername()));
            setStatus("Statut: pret, en attente des autres joueurs...", false);
        } catch (IOException e) {
            setStatus("Erreur READY: " + e.getMessage(), true);
        }
    }

    /**
     * Traite les messages lobby et navigation vers la partie.
     */
    private void handleServerMessage(Message message) {
        Platform.runLater(() -> {
            switch (message.getType()) {
                case "ROOM_LIST" -> refreshRoomTable(message.getDataAsJsonObject());
                case "ROOM_JOINED" -> setStatus("Room rejointe: " + message.getDataAsJsonObject().get("roomName").getAsString(), false);
                case "PLAYER_JOINED" -> setStatus("Un joueur a rejoint la room.", false);
                case "QUESTION" -> {
                    GameController.setPendingQuestionPayload(message.getDataAsJsonObject());
                    SceneNavigator.switchTo("/ui/views/game.fxml", "Quiz Multijoueur - Partie");
                }
                case "GAME_START" -> SceneNavigator.switchTo("/ui/views/game.fxml", "Quiz Multijoueur - Partie");
                case "ERROR" -> {
                    JsonObject payload = message.getDataAsJsonObject();
                    String msg = payload.has("message") ? payload.get("message").getAsString() : "Erreur serveur";
                    setStatus(msg, true);
                }
                default -> {
                    // Ignore.
                }
            }
        });
    }

    private void refreshRoomTable(JsonObject payload) {
        roomRows.clear();
        if (!payload.has("rooms")) {
            return;
        }

        JsonArray rooms = payload.getAsJsonArray("rooms");
        for (JsonElement element : rooms) {
            JsonObject room = element.getAsJsonObject();
            roomRows.add(new RoomRow(
                room.get("name").getAsString(),
                room.get("players").getAsInt(),
                room.get("max").getAsInt()
            ));
        }
    }

    private void setStatus(String text, boolean error) {
        statusLabel.setStyle(error ? "-fx-text-fill: #c0392b;" : "-fx-text-fill: #1d3557;");
        statusLabel.setText(text);
    }

    public record RoomRow(String name, int players, int max) {
    }
}
