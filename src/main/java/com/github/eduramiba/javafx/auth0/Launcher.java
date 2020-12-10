package com.github.eduramiba.javafx.auth0;

import com.github.eduramiba.javafx.auth0.utils.FXUtils;
import java.awt.HeadlessException;
import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class Launcher {

    private static final Logger LOG = LoggerFactory.getLogger(Launcher.class);

    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        setupGlobalErrorHandler();

        setupSwingFallbackLookAndFeel();

        launchApp(args);
    }

    private static void launchApp(String[] args) throws HeadlessException {
        try {
            JavaFXApp.main(args);
        } catch (Throwable ex) {
            LOG.error("Unexpected error launching App", ex);
            final String message = "Unexpected error when running App";

            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    private static void setupSwingFallbackLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Throwable ignored) {
            //NOOP
        }
    }

    private static void setupGlobalErrorHandler() {
        try {
            Thread.setDefaultUncaughtExceptionHandler((Thread thread, Throwable throwable) -> {
                LOG.error("Unhandled error in thread {}", thread.getName(), throwable);

                FXUtils.showExceptionMessage(throwable);
            });
        } catch (Throwable ex) {
            LOG.warn("Unexpected error setting default exception handler", ex);
        }
    }

}
