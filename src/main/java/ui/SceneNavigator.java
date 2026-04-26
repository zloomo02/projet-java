package ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public final class SceneNavigator {
    private static Stage primaryStage;

    private SceneNavigator() {
    }

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    /**
     * Charge une vue FXML et la place dans la scene principale.
     */
    public static void switchTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(SceneNavigator.class.getResource("/style.css").toExternalForm());

            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger la vue " + fxmlPath, e);
        }
    }
}
