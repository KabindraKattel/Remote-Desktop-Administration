package RemoteDesktopAdministration.Client.UI.FileReceiveProgress;

import RemoteDesktopAdministration.Client.FileReceiver;
import RemoteDesktopAdministration.Client.UI.PrimaryModel;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class Model {

    private final ObjectProperty<FileReceiver.ReceiveProgress> fileReceiveProgress = new SimpleObjectProperty<>();
    private final View view;

    public Model(PrimaryModel primaryModel) {
        view = new View(primaryModel, this);
    }

    public ObjectProperty<FileReceiver.ReceiveProgress> fileReceiveProgressProperty() {
        return fileReceiveProgress;
    }

    public void setFileReceiveProgress(FileReceiver.ReceiveProgress receiveProgress) {
        Platform.runLater(() -> fileReceiveProgress.set(receiveProgress));
    }

    public void startProgressViewer() {
        Platform.runLater(view::start);
    }

    public void stopProgressViewer(View.StopMode stopMode) {
        Platform.runLater(() -> view.stop(stopMode));
    }
}
