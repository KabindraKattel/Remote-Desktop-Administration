package RemoteDesktopAdministration.Server.UI.ClientScreenViewer;

import RemoteDesktopAdministration.Server.MainServer;
import RemoteDesktopAdministration.Server.UI.PrimaryModel;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;

public class Model {
    private final ObjectProperty<Image> sharedScreen = new SimpleObjectProperty<>();
    private final View view;

    public Model(MainServer mainServer, PrimaryModel primaryModel, MainServer.Client client) {
        view = new View(mainServer, primaryModel, this, client);
    }

    public ObjectProperty<Image> sharedScreenProperty() {
        return sharedScreen;
    }

    public void setSharedScreen(Image image) {
        sharedScreen.set(image);
    }

    public void startScreenViewer() {
        Platform.runLater(view::start);
    }

    void stopScreenViewer(View.StopMode stopMode) {

        Platform.runLater(() -> view.stop(stopMode));
    }
}
