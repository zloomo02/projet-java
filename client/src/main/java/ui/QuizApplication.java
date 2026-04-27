package ui;

import javafx.application.Application;
import javafx.stage.Stage;

public class QuizApplication extends Application {
    @Override
    public void start(Stage stage) {
        SceneNavigator.init(stage);
        SceneNavigator.switchTo("/ui/views/login.fxml", "Quiz Multijoueur - Connexion");
    }

    @Override
    public void stop() {
        AppState.getInstance().disconnect();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
