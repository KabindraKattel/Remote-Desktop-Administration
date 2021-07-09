package RemoteDesktopAdministration.Server.UI.ClientScreenViewer;

import RemoteDesktopAdministration.Server.Logger.ServerLogger;
import RemoteDesktopAdministration.Server.MainServer;
import RemoteDesktopAdministration.Server.UI.PrimaryModel;
import RemoteDesktopAdministration.Server.Utils.FxUtils;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class View {

    private final PrimaryModel primaryModel;
    private final Controller controller;
    private final MainServer mainServer;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final MainServer.Client client;
    private Stage stage;

    public View(MainServer mainServer, PrimaryModel primaryModel, Model model, MainServer.Client client) {
        this.mainServer = mainServer;
        this.primaryModel = primaryModel;
        this.client = client;
        this.controller = new Controller(primaryModel, model, mainServer, client);
    }

    public void start() {

        if (started.get())
            return;
        try {
            this.stage = new Stage();
            stage.initOwner(primaryModel.getPrimaryStage());
            stage.initModality(Modality.NONE);
            stage.setTitle("Screen Viewer of " + client.getClientDescriptiveName());
            final var fxmlLoader = new FXMLLoader(getClass().getResource("View.fxml"));
            fxmlLoader.setController(controller);
            Parent root = fxmlLoader.load();
            final Scene scene = new Scene(root);
            stage.setScene(scene);
            FxUtils.addAllStageIcons(stage, getClass(), "16.png", "24.png", "32.png", "48.png", "64.png", "96.png", "128.png", "256.png", "512.png", "1024.png");
            stage.setResizable(true);
            stage.sizeToScene();
            stage.show();
            started.set(true);
            Bounds rootBounds = root.getBoundsInLocal();
            double deltaW = stage.getWidth() - rootBounds.getWidth();
            double deltaH = stage.getHeight() - rootBounds.getHeight();

            Bounds prefBounds = FxUtils.getPrefBounds(root);

            stage.setMinWidth(prefBounds.getWidth() + deltaW);
            stage.setMinHeight(prefBounds.getHeight() + deltaH);
            stage.setOnCloseRequest(evt -> {
                stop(StopMode.NORMAL_STOP);
                evt.consume();
            });
        } catch (Exception e) {
            ServerLogger.SERVER_LOGGER.log(Level.SEVERE, "Shared Screen Viewer UI failed to load [" + e.getMessage() + "]", e);
            started.set(false);
        }

    }

    public void stop(StopMode stopMode) {
        if (!started.get())
            return;
        mainServer.writeStream(MainServer.DescriptorCode.SCREEN_SHARING_ABORT, "", client.getDataOutputStream());
        started.set(false);
        stage.close();
        ServerLogger.SERVER_LOGGER.log(Level.SEVERE, stopMode.name() + " | Screen receiving of " + client.getClientDescriptiveName() + " terminated.");
    }

    enum StopMode {
        NORMAL_STOP(),
        CLIENT_OFFLINE_STOP(),
        SERVER_OFFLINE_STOP()
    }
}
