package RemoteDesktopAdministration.Server.UI.FileSendProgress;

import RemoteDesktopAdministration.Server.MainServer;
import RemoteDesktopAdministration.Server.UI.PrimaryModel;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    private final PrimaryModel primaryModel;
    private final Model model;
    private final MainServer.Client client;
    private Stage stage;
    private double expandableHeight;

    @FXML
    private GridPane expandableGrid;
    @FXML
    private DialogPane dialogPane;
    @FXML
    private Label remTime, remSize, totalSize, currentFile, uploaded, speed;
    @FXML
    private ProgressBar progressBar;

    public Controller(PrimaryModel primaryModel, Model model, MainServer.Client client) {
        this.primaryModel = primaryModel;
        this.model = model;
        this.client = client;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        primaryModel.serverSwitchStateProperty().addListener((obs, wasConnected, isConnected) -> {
            if (!isConnected)
                model.stopProgressViewer(View.StopMode.SERVER_OFFLINE_STOP);
        });
        primaryModel.clientOfflineProperty().addListener((obs, oldClient, newClient) -> {
            if (newClient.equals(client.getClientDescriptiveName()))
                model.stopProgressViewer(View.StopMode.CLIENT_OFFLINE_STOP);
        });
        dialogPane.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
            if (!isExpanded)
                expandableHeight = expandableGrid.getHeight();
            double expandableH = expandableHeight;
            expandableH = isExpanded ? expandableH : -expandableH;
            stage.setHeight(stage.getHeight() + expandableH);
        });
        model.fileSendProgressProperty().addListener((obs, oldProgress, newProgress) -> {
            remTime.setText(newProgress.timeLeft());
            remSize.setText("[" + newProgress.remainingSize() + "]");
            totalSize.setText(newProgress.totalSize());
            currentFile.setText(newProgress.currentFile());
            uploaded.setText(newProgress.uploadedFileSize());
            speed.setText(newProgress.speed());
            progressBar.progressProperty().set(newProgress.per());
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

}
