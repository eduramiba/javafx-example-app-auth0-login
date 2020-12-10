package com.github.eduramiba.javafx.auth0.rest.login;

import com.github.eduramiba.javafx.auth0.utils.I18N;
import com.github.eduramiba.javafx.auth0.Launcher;
import com.github.eduramiba.javafx.auth0.UserInfo;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.jooq.lambda.Unchecked;
import org.slf4j.LoggerFactory;

public class Auth0Login {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(Auth0Login.class);

    private static final int WINDOW_WIDTH = 400;
    private static final int WINDOW_HEIGHT = 550;

    public static CompletableFuture<Optional<UserInfo>> login() {
        final CompletableFuture<Optional<UserInfo>> future = new CompletableFuture<>();

        Platform.runLater(() -> {
            final WebView webView = new WebView();
            final WebEngine engine = webView.getEngine();

            final Scene scene = new Scene(webView);
            final Stage stage = new Stage();

            final Auth0PKCEFlow.FlowInfo flow = Auth0PKCEFlow.createAuthorizationFlow();
            engine.getLoadWorker().stateProperty().addListener((ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    try {
                        final Optional<UserInfo> userInfo = Auth0PKCEFlow.checkURLForLoginSuccess(flow, engine.getLocation());

                        userInfo.ifPresent(user -> {
                            future.complete(Optional.of(user));
                            stage.close();
                        });
                    } catch (IOException ex) {
                        Unchecked.throwChecked(ex);
                    }
                }
            });

            engine.load(flow.getAuthorizeUrl());

            final Image icon = new Image(Launcher.class.getResourceAsStream("/icon.png"));

            stage.setWidth(WINDOW_WIDTH);
            stage.setHeight(WINDOW_HEIGHT);

            stage.getIcons().add(icon);
            stage.titleProperty().set(I18N.getBundle("messages").getString("app.title.login"));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

            stage.addEventHandler(WindowEvent.WINDOW_HIDDEN, (WindowEvent event) -> {
                if (!future.isDone()) {
                    future.complete(Optional.empty());
                }
            });
        });

        return future.whenComplete((userOptional, ex) -> {
            if (ex != null) {
                Unchecked.throwChecked(ex);
            } else {
                userOptional.ifPresentOrElse((UserInfo user) -> {
                    LOG.info("Login succesful! Email: {}", user.getEmail());
                }, () -> {
                    LOG.info("Login canceled by user");
                });
            }
        });
    }

}
