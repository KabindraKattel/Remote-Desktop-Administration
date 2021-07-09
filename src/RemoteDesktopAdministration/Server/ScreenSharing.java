package RemoteDesktopAdministration.Server;

import RemoteDesktopAdministration.Server.Logger.ServerLogger;
import RemoteDesktopAdministration.Server.UI.PrimaryModel;
import RemoteDesktopAdministration.Server.Utils.ImageUtils;
import javafx.application.Platform;
import javafx.scene.image.Image;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ScreenSharing {

    private final MainServer mainServer;
    private final MainServer.Client client;
    private final RemoteDesktopAdministration.Server.UI.ClientScreenViewer.Model model;
    private double wClient;
    private double hClient;

    public ScreenSharing(MainServer mainServer, RemoteDesktopAdministration.Server.UI.ClientScreenViewer.Model model, PrimaryModel primaryModel, MainServer.Client client) {
        this.mainServer = mainServer;
        this.model = model;
        this.client = client;
    }

    synchronized void stateActionPerformed(State state, byte[] dataByteBuffer) {
        Objects.requireNonNull(state);
        switch (state) {
            case NO_PROGRESS -> noProgress(new String(dataByteBuffer, StandardCharsets.UTF_8));
            case PROGRESS -> progress(dataByteBuffer);
        }
    }

    private void noProgress(String parameters) {

        model.startScreenViewer();
        final var params = parameters.split(",");
        wClient = Double.parseDouble(params[0]);
        hClient = Double.parseDouble(params[1]);
        ServerLogger.SERVER_LOGGER.info("Configuration setup to view " + client.getClientDescriptiveName() + " Screen succeeded.");
        mainServer.writeStream(MainServer.DescriptorCode.SCREEN_SHARING_ON_PROGRESS, "", client.getDataOutputStream());

    }

    void progress(byte[] dataByteBuffer) {
        Platform.runLater(() -> {
            Image image = ImageUtils.fxByteArrayToFxImage((int) wClient, (int) hClient, dataByteBuffer);
            model.setSharedScreen(image);
            mainServer.writeStream(MainServer.DescriptorCode.SCREEN_SHARING_ON_PROGRESS, "", client.getDataOutputStream());
        });
    }

    enum State {
        NO_PROGRESS, PROGRESS
    }

}
