package ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class ScoreController {
    private static JsonObject rankingPayload;

    @FXML
    private Label titleLabel;

    @FXML
    private TableView<RankingRow> rankingTable;

    @FXML
    private TableColumn<RankingRow, Number> rankColumn;

    @FXML
    private TableColumn<RankingRow, String> usernameColumn;

    @FXML
    private TableColumn<RankingRow, Number> scoreColumn;

    private final ObservableList<RankingRow> rows = FXCollections.observableArrayList();

    public static void setRankingPayload(JsonObject payload) {
        rankingPayload = payload;
    }

    @FXML
    public void initialize() {
        rankColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().rank()));
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().username()));
        scoreColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().score()));
        rankingTable.setItems(rows);

        loadRanking();
    }

    /**
     * Recharge le classement final recu dans GAME_OVER.
     */
    private void loadRanking() {
        rows.clear();

        if (rankingPayload == null || !rankingPayload.has("ranking")) {
            titleLabel.setText("Resultats indisponibles");
            return;
        }

        JsonArray ranking = rankingPayload.getAsJsonArray("ranking");
        for (int i = 0; i < ranking.size(); i++) {
            JsonObject row = ranking.get(i).getAsJsonObject();
            rows.add(new RankingRow(
                row.get("rank").getAsInt(),
                row.get("username").getAsString(),
                row.get("score").getAsInt()
            ));
        }
    }

    @FXML
    private void onBackToLobbyClicked() {
        SceneNavigator.switchTo("/ui/views/lobby.fxml", "Quiz Multijoueur - Lobby");
    }

    public record RankingRow(int rank, String username, int score) {
    }
}
