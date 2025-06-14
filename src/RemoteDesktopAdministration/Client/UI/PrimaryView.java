package RemoteDesktopAdministration.Client.UI;

import RemoteDesktopAdministration.Client.Logger.ClientLogger;
import RemoteDesktopAdministration.Client.Utils.FxUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.logging.Level;

public class PrimaryView extends Application {

    private static final String TITLE = "Remote Desktop Administration - CLIENT";

    @Override
    public void start(Stage primaryStage) {

        try {
            final var fxmlLoader = new FXMLLoader(getClass().getResource("PrimaryView.fxml"));
            Parent root = fxmlLoader.load();
            primaryStage.setTitle(TITLE);
            final Scene scene = new Scene(root);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("PrimaryView.css")).toExternalForm());
            primaryStage.setScene(scene);
            FxUtils.addAllStageIcons(primaryStage, getClass(), "16.png", "24.png", "32.png", "48.png", "64.png", "96.png", "128.png", "256.png", "512.png", "1024.png");
            primaryStage.setResizable(true);
            final PrimaryController primaryController = fxmlLoader.getController();
            primaryController.setPrimaryStage(primaryStage);
            primaryStage.sizeToScene();
            primaryStage.show();
            ClientLogger.CLIENT_LOGGER.info(MessageCodes.START_OK.getFormattedMessage(null));
            Bounds rootBounds = root.getBoundsInLocal();
            double deltaW = primaryStage.getWidth() - rootBounds.getWidth();
            double deltaH = primaryStage.getHeight() - rootBounds.getHeight();

            Bounds prefBounds = FxUtils.getPrefBounds(root);

            primaryStage.setMinWidth(prefBounds.getWidth() + deltaW);
            primaryStage.setMinHeight(prefBounds.getHeight() + deltaH);
            primaryStage.setOnCloseRequest(evt -> {
                if (primaryController.showConfirmAlertAndWait("Are you sure you want to exit ?"))
                    primaryStage.close();
                evt.consume();
            });
        } catch (Exception e) {
            ClientLogger.CLIENT_LOGGER.log(Level.SEVERE, MessageCodes.START_EXC.getFormattedMessage(e), e);
        }
    }

    @Override
    public void stop() {
        Platform.exit();
        ClientLogger.CLIENT_LOGGER.severe(MessageCodes.STOP_OK.getFormattedMessage(null));
        System.exit(0);
    }

    private enum MessageCodes {
        START_OK(PrimaryView.TITLE + " Application started."),
        START_EXC(PrimaryView.TITLE + " Application failed to start."),
        STOP_OK(PrimaryView.TITLE + " Application Stopped.");

        private final String value;

        MessageCodes(String value) {
            this.value = value;
        }

        String getFormattedMessage(Throwable exc) {
            return (exc == null) ? ("   " + name() + "  : :  " + value) : (name() + "  : :  " + value + " [ " + exc + " ]");
        }
    }


}
