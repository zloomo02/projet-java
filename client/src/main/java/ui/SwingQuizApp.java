package ui;

import javax.swing.*;

public class SwingQuizApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SwingNavigator nav = SwingNavigator.getInstance();

            AppState state = AppState.getInstance();
            LoginPanel login = new LoginPanel(state);
            LobbyPanel lobby = new LobbyPanel(state);
            GamePanel game = new GamePanel(state);
            ScorePanel score = new ScorePanel(state);

            nav.register("login", login);
            nav.register("lobby", lobby);
            nav.register("game", game);
            nav.register("score", score);

            nav.show("login");
        });
    }
}
