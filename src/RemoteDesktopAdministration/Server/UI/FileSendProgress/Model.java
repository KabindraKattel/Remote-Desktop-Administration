package RemoteDesktopAdministration.Server.UI.FileSendProgress;

import RemoteDesktopAdministration.Server.FileSender;
import RemoteDesktopAdministration.Server.MainServer;
import RemoteDesktopAdministration.Server.UI.PrimaryModel;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class Model {
    private final ObjectProperty<FileSender.SendProgress> fileSendProgress = new SimpleObjectProperty<>();
    private final View view;

    public Model(PrimaryModel primaryModel, MainServer.Client client) {
        view = new View(primaryModel, this, client);
    }

    public void setFileSendProgress(FileSender.SendProgress sendProgress) {
        Platform.runLater(() -> fileSendProgress.set(sendProgress));
    }

    public ObjectProperty<FileSender.SendProgress> fileSendProgressProperty() {
        return fileSendProgress;
    }

    public void startProgressViewer() {
        Platform.runLater(view::start);
    }

    public void stopProgressViewer(View.StopMode stopMode) {
        Platform.runLater(() -> view.stop(stopMode));
    }
}
