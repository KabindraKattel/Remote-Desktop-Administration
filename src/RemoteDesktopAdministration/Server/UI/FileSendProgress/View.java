package RemoteDesktopAdministration.Server.UI.FileSendProgress;

import RemoteDesktopAdministration.Server.Logger.ServerLogger;
import RemoteDesktopAdministration.Server.MainServer;
import RemoteDesktopAdministration.Server.UI.PrimaryModel;
import RemoteDesktopAdministration.Server.Utils.FxUtils;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class View {

    private final Controller controller;
    private final PrimaryModel primaryModel;
    private final MainServer.Client client;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private Stage stage;

    public View(PrimaryModel primaryModel, Model model, MainServer.Client client) {
        this.primaryModel = primaryModel;
        this.client = client;
        this.controller = new Controller(primaryModel, model, client);

    }

    public void start() {
        if (started.get())
            return;
        try {
            this.stage = new Stage();
            stage.initOwner(primaryModel.getPrimaryStage());
            stage.initModality(Modality.NONE);
            stage.setTitle("File Send Progress for " + client.getClientDescriptiveName());
            controller.setStage(stage);
            final var fxmlLoader = new FXMLLoader(getClass().getResource("View.fxml"));
            fxmlLoader.setController(controller);
            Parent root = fxmlLoader.load();
            final Scene scene = new Scene(root);
            stage.setScene(scene);
            FxUtils.addAllStageIcons(stage, getClass(), "16.png", "24.png", "32.png", "48.png", "64.png", "96.png", "128.png", "256.png", "512.png", "1024.png");
            stage.setResizable(false);
            stage.sizeToScene();
            stage.show();
            started.set(true);
            stage.setOnCloseRequest(Event::consume);
        } catch (Exception e) {
            ServerLogger.SERVER_LOGGER.log(Level.SEVERE, "Transfer Progress UI failed to load [" + e.getMessage() + "]", e);
            started.set(false);
        }

    }

    public void stop(StopMode stopMode) {
        if (!started.get())
            return;
        started.set(false);
        stage.close();
        if (stopMode == StopMode.NORMAL_STOP)
            return;
        ServerLogger.SERVER_LOGGER.log(Level.SEVERE, stopMode.name() + " | File Send for " + client.getClientDescriptiveName() + " failed.");

    }

    public enum StopMode {
        NORMAL_STOP(),
        CLIENT_OFFLINE_STOP(),
        SERVER_OFFLINE_STOP()
    }

}
