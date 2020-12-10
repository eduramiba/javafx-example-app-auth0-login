package com.github.eduramiba.javafx.auth0;

import com.github.eduramiba.javafx.auth0.utils.Utils;
import com.github.eduramiba.javafx.auth0.utils.FXUtils;
import com.github.eduramiba.javafx.auth0.rest.login.JWTUtils;
import com.github.eduramiba.javafx.auth0.rest.login.Auth0Login;
import com.github.eduramiba.javafx.auth0.preferences.AppPreferences;
import com.auth0.jwt.interfaces.DecodedJWT;
import static com.github.eduramiba.javafx.auth0.Constants.PREF_USER_INFO;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.StatusBar;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;

public class MainController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    @FXML
    Pane titleBar;

    @FXML
    Label titleLabel;

    @FXML
    StatusBar statusBar;

    private Glyph menuIcon;
    private Button loginButton;
    private Button logoutButton;

    private final Glyph statusBarWarningIcon;
    private final Glyph statusBarOkIcon;
    private final Glyph statusBarUserIcon;

    //Model:
    private final MainControllerModel model;
    private ResourceBundle bundle;

    public MainController() {
        model = new MainControllerModel();

        statusBarWarningIcon = new Glyph("FontAwesome", FontAwesome.Glyph.EXCLAMATION_TRIANGLE);
        statusBarWarningIcon.setFontSize(20);
        statusBarWarningIcon.setColor(Color.RED);

        statusBarOkIcon = new Glyph("FontAwesome", FontAwesome.Glyph.CHECK_CIRCLE_ALT);
        statusBarOkIcon.setFontSize(20);
        statusBarOkIcon.setColor(Color.DARKGREEN);

        statusBarUserIcon = new Glyph("FontAwesome", FontAwesome.Glyph.USER);
        statusBarUserIcon.setFontSize(20);
        statusBarUserIcon.setColor(Color.BLACK);
    }

    private void initComponents() {
        bindUserSesionToPreferences();

        //Menu icon:
        menuIcon = new Glyph("FontAwesome", FontAwesome.Glyph.BARS);
        menuIcon.setFontSize(32);
        menuIcon.setColor(Color.WHITE);
        menuIcon.setCursor(Cursor.HAND);

        menuIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            showMenu();
        });
        menuIcon.setTooltip(buildTooltip(bundle.getString("show.menu")));
        titleBar.getChildren().add(0, menuIcon);

        final Glyph loginIcon = new Glyph("FontAwesome", FontAwesome.Glyph.SIGN_IN);
        loginIcon.setFontSize(20);
        loginIcon.setColor(Color.WHITE);

        loginButton = new Button(bundle.getString("login"));
        loginButton.getStyleClass().add("loginButton");
        loginButton.setGraphic(loginIcon);
        loginButton.setCursor(Cursor.HAND);

        final Glyph logoutIcon = new Glyph("FontAwesome", FontAwesome.Glyph.SIGN_OUT);
        logoutIcon.setFontSize(20);
        logoutIcon.setColor(Color.WHITE);

        logoutButton = new Button(bundle.getString("logout"));
        logoutButton.getStyleClass().add("logoutButton");
        logoutButton.setGraphic(logoutIcon);
        logoutButton.setCursor(Cursor.HAND);

        logoutButton.addEventHandler(ActionEvent.ACTION, (ActionEvent t) -> {
            hideMenu();
            model.setUserInfo(null);
        });
        loginButton.addEventHandler(ActionEvent.ACTION, (ActionEvent t) -> {
            hideMenu();
            doLogin();
        });

        model.getUserInfo().addListener((binding, oldValue, newValue) -> {
            refreshStatusBar();
        });
        
        refreshStatusBar();
    }

    private Tooltip buildTooltip(final String text) {
        return FXUtils.buildTooltip(text, null);
    }

    @Override
    public void initialize(URL url, ResourceBundle bundle) {
        this.bundle = bundle;
        initComponents();
    }

    public void shutdown() {
        try {
            hideMenu();
        } catch (Exception e) {
            //NOOP
        }
    }

    public void onResize() {
        //NOOP
    }

    private PopOver menuPopover = null;

    private static final Map<String, Image> AVATAR_CACHE = new HashMap<>();

    private void showMenu() {
        final List<Node> nodes = new ArrayList<>();

        getLoggedUserInfo().ifPresentOrElse(user -> {
            final String avatarURL = user.getAvatarURL();
            if (!StringUtils.isBlank(avatarURL)) {
                try {
                    final Rectangle avatarHolder = new Rectangle(0, 0, 128, 128);
                    avatarHolder.setArcWidth(30.0);// Corner radius
                    avatarHolder.setArcHeight(30.0);

                    final int timeoutMillis = 2000;
                    final OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                            .callTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                            .readTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                            .writeTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                            .build();

                    final Request request = new Request.Builder().url(avatarURL)
                            .build();

                    final Image avatarImage;

                    if (AVATAR_CACHE.containsKey(avatarURL)) {
                        avatarImage = AVATAR_CACHE.get(avatarURL);
                    } else {
                        try (Response response = client.newCall(request).execute()) {
                            final ResponseBody body = response.body();
                            if (body == null) {
                                throw new IllegalStateException("Empty response for avatar URL " + avatarURL);
                            }

                            avatarImage = new Image(body.byteStream(), 128, 128, true, true);
                        }

                        AVATAR_CACHE.put(avatarURL, avatarImage);
                    }

                    if (avatarImage != null) {
                        final ImagePattern pattern = new ImagePattern(avatarImage);

                        avatarHolder.setFill(pattern);
                        avatarHolder.setEffect(new DropShadow(5, Color.DARKGREY));// Shadow
                        avatarHolder.getStyleClass().add("userAvatar");
                        nodes.add(avatarHolder);
                    }
                } catch (Throwable ex) {
                    AVATAR_CACHE.put(avatarURL, null);
                }
            }

            nodes.add(new Label(user.getName()));
            nodes.add(new Label(user.getEmail()));
            nodes.add(logoutButton);
        }, () -> {
            nodes.add(loginButton);
        });

        final VBox vbox = new VBox(nodes.toArray(new Node[0]));
        vbox.getStyleClass().add("menu-popover");

        menuPopover = new PopOver(vbox);
        menuPopover.setDetachable(false);
        menuPopover.show(menuIcon);
        menuPopover.setArrowLocation(PopOver.ArrowLocation.LEFT_TOP);
        menuPopover.setHideOnEscape(true);
    }

    private boolean hideMenu() {
        if (menuPopover != null && menuPopover.isShowing()) {
            menuPopover.hide(Duration.ZERO);
            menuPopover = null;

            return true;
        }

        return false;
    }

    private Optional<UserInfo> getLoggedUserInfo() {
        final UserInfo userInfo = model.getUserInfo().getValue();

        if (userInfo != null) {
            final Optional<DecodedJWT> validToken = JWTUtils.verifyToken(userInfo.getJwtToken());

            return validToken.map(jwt -> userInfo);
        } else {
            return Optional.empty();
        }
    }

    private class StatusBarInfo {

        private final Glyph icon;
        private final String text;

        public StatusBarInfo(Glyph icon, String text) {
            this.icon = icon;
            this.text = text;
        }

        public Glyph getIcon() {
            return icon;
        }

        public String getText() {
            return text;
        }

    }

    private void refreshStatusBar() {
        Platform.runLater(() -> {
            final UserInfo user = model.getUserInfo().getValue();
            statusBar.getRightItems().clear();
            if (user != null) {
                final HBox userInfoNode = new HBox(
                        statusBarUserIcon,
                        new Label(user.getName())
                );
                userInfoNode.setAlignment(Pos.CENTER);
                userInfoNode.setSpacing(8);

                statusBar.getRightItems().add(userInfoNode);
            }
        });
    }

    private CompletableFuture<Optional<UserInfo>> ensureLoggedIn() {
        return getLoggedUserInfo()
                .map(userInfo -> CompletableFuture.completedFuture(Optional.of(userInfo)))
                .orElseGet(() -> doLogin());
    }

    private CompletableFuture<Optional<UserInfo>> doLogin() {
        return Auth0Login.login().thenApply(userInfo -> {
            userInfo.ifPresent(model::setUserInfo);

            return userInfo;
        });
    }

    private void bindUserSesionToPreferences() {
        AppPreferences.getGlobalPreference(PREF_USER_INFO, Unchecked.function(json -> Utils.parseJSON(json, UserInfo.class)))
                .ifPresent(userInfo -> {
                    LOG.info("User already logged in: {}", userInfo.getEmail());
                    model.setUserInfo(userInfo);
                });

        model.getUserInfo().addListener((ObservableValue<? extends UserInfo> binding, UserInfo oldValue, UserInfo newValue) -> {
            if (newValue != null) {
                AppPreferences.saveGlobalPreference(PREF_USER_INFO, Utils.toJSON(newValue));
            } else {
                AppPreferences.removeGlobalPreference(PREF_USER_INFO);
            }
        });
    }
}
