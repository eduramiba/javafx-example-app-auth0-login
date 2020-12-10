package com.github.eduramiba.javafx.auth0.utils;

import com.github.eduramiba.javafx.auth0.rest.ApiRESTException;
import com.github.eduramiba.javafx.auth0.rest.ApiRESTIOException;
import java.awt.Desktop;
import java.net.SocketException;
import java.net.URI;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FXUtils {

    private static final Logger LOG = LoggerFactory.getLogger(FXUtils.class);

    public static Tooltip buildTooltip(final String text) {
        return buildTooltip(text, null);
    }

    public static Tooltip buildTooltip(final String text, final Duration duration) {
        final Tooltip tooltip = new Tooltip(text);
        tooltip.setStyle("-fx-font-size: 16");
        if (duration != null) {
            tooltip.setShowDelay(duration);
        } else {
            tooltip.setShowDelay(Duration.millis(200));
        }

        tooltip.setMaxWidth(240);
        tooltip.setWrapText(true);

        return tooltip;
    }

    public static void openURL(final URI url) {
        final Desktop desktop = Desktop.getDesktop();
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(
                        url
                );
            } catch (Throwable ex) {
                LOG.error("Error opening browser", ex);
            }
        }
    }

    public static Optional<Window> findActiveWindow() {
        return Stage.getWindows().stream().filter(Window::isFocused).findFirst()
                .or(() -> Stage.getWindows().stream().filter(Window::isShowing).findFirst());
    }

    public static void showErrorMessage(final String message) {
        showAlert(message, Alert.AlertType.ERROR);
    }

    public static void showAlert(final String message, final Alert.AlertType alertType) {
        Platform.runLater(() -> {
            final Alert alert = new Alert(
                    alertType,
                    message,
                    ButtonType.OK
            );

            findActiveWindow().ifPresent(alert::initOwner);
            alert.show();
        });
    }

    public static void showExceptionMessage(final Throwable throwable) {
        final ResourceBundle bundle = I18N.getBundle("messages");
        final String message;
        if (throwable instanceof ApiRESTException) {
            final ApiRESTException error = (ApiRESTException) throwable;

            final int httpStatus = error.getStatus();
            if (error.getErrorResponse() != null && error.getErrorResponse().getErrorCode() != null) {
                final String errorCode = error.getErrorResponse().getErrorCode();

                if (bundle.containsKey("error.code." + errorCode)) {
                    message = bundle.getString("error.code." + errorCode);
                } else {
                    message = String.format(bundle.getString("error.code.generic"), errorCode);
                }
            } else if (bundle.containsKey("error.http." + httpStatus)) {
                message = bundle.getString("error.http." + httpStatus);
            } else {
                message = error.getFriendlyMessage();
            }
        } else if (throwable instanceof ApiRESTIOException || throwable instanceof SocketException) {
            message = bundle.getString("error.network");
        } else {
            message = String.format(
                    bundle.getString("error.generic"), throwable != null ? throwable.getMessage() : ""
            );
        }

        FXUtils.showErrorMessage(message);
    }
}
