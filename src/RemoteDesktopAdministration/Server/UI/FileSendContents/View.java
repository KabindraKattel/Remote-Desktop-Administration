package RemoteDesktopAdministration.Server.UI.FileSendContents;

import RemoteDesktopAdministration.Server.Logger.ServerLogger;
import RemoteDesktopAdministration.Server.MainServer;
import RemoteDesktopAdministration.Server.Utils.FxUtils;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class View {
    private static final String TITLE = "Add Files or Folders";
    private final Stage primaryStage;
    private final MainServer mainServer;
    private final List<String> selectedClients;
    private Stage stage;

    public View(Stage primaryStage, MainServer mainServer, List<String> selectedClients) {
        this.selectedClients = selectedClients;
        this.mainServer = mainServer;
        this.primaryStage = primaryStage;
    }

    public void start() {

        try {
            this.stage = new Stage();
            this.stage.initOwner(primaryStage);
            this.stage.initModality(Modality.APPLICATION_MODAL);
            final var fxmlLoader = new FXMLLoader(getClass().getResource("View.fxml"));
            Parent root = fxmlLoader.load();
            stage.setTitle(TITLE);
            final Scene scene = new Scene(root);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("View.css")).toExternalForm());
            stage.setScene(scene);
            FxUtils.addAllStageIcons(stage, getClass(), "16.png", "24.png", "32.png", "48.png", "64.png", "96.png", "128.png", "256.png", "512.png", "1024.png");
            stage.setResizable(true);
            final Controller controller = fxmlLoader.getController();
            controller.setStage(stage);
            controller.setMainServer(mainServer);
            controller.setSelectedClients(selectedClients);
            stage.show();
            ServerLogger.SERVER_LOGGER.info(View.MessageCodes.START_OK.getFormattedMessage(null));
            Bounds rootBounds = root.getBoundsInLocal();
            double deltaW = stage.getWidth() - rootBounds.getWidth();
            double deltaH = stage.getHeight() - rootBounds.getHeight();

            Bounds prefBounds = FxUtils.getPrefBounds(root);

            stage.setMinWidth(prefBounds.getWidth() + deltaW);
            stage.setMinHeight(prefBounds.getHeight() + deltaH);
            stage.setOnCloseRequest(evt -> {
                Controller.clearTable();
                stop();
                stage.close();
                evt.consume();
            });
        } catch (Exception e) {
            ServerLogger.SERVER_LOGGER.log(Level.SEVERE, MessageCodes.START_EXC.getFormattedMessage(e), e);
        }
    }

    private void stop() {
        ServerLogger.SERVER_LOGGER.severe(MessageCodes.STOP_OK.getFormattedMessage(null));
    }


    private enum MessageCodes {
        START_OK(View.TITLE + " Application started."),
        START_EXC(View.TITLE + " Application failed to start."),
        STOP_OK(View.TITLE + " Application Stopped.");

        private final String value;

        MessageCodes(String value) {
            this.value = value;
        }

        String getFormattedMessage(Throwable exc) {
            return (exc == null) ? (name() + " | " + value) : (name() + " | " + value + " [ " + exc + " ]");
        }
    }
}
