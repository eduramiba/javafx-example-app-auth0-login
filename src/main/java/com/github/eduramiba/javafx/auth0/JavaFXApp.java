package com.github.eduramiba.javafx.auth0;

import com.github.eduramiba.javafx.auth0.utils.I18N;
import java.io.IOException;
import java.util.ResourceBundle;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class JavaFXApp extends Application {

    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;

    @Override
    public void start(final Stage primaryStage) {
        final FXMLLoader loader;
        final Parent root;
        final ResourceBundle languageResource;
        try {
            languageResource = I18N.getBundle("messages");
            loader = new FXMLLoader(getClass().getResource("/fxml/MainController.fxml"), languageResource);
            root = loader.load();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        final MainController controller = loader.getController();

        final Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

        primaryStage.getIcons().add(new Image(JavaFXApp.class.getResourceAsStream("/icon.png")));
        scene.getStylesheets().add(JavaFXApp.class.getResource("/fxml/styles.css").toExternalForm());
        primaryStage.titleProperty().set(languageResource.getString("app.title"));

        primaryStage.setScene(scene);

        primaryStage.setMinHeight(WINDOW_HEIGHT);
        primaryStage.setMinWidth(WINDOW_WIDTH);

        primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            controller.onResize();
        });

        primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            controller.onResize();
        });

        primaryStage.setOnHidden(e -> {
            controller.shutdown();
            Platform.exit();
        });

        primaryStage.setMaximized(true);
        primaryStage.show();
        primaryStage.requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
