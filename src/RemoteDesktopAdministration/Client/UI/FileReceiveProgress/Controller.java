package RemoteDesktopAdministration.Client.UI.FileReceiveProgress;

import RemoteDesktopAdministration.Client.UI.PrimaryModel;
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
    private Stage stage;
    private double expandableHeight;

    @FXML
    private GridPane expandableGrid;
    @FXML
    private DialogPane dialogPane;
    @FXML
    private Label remTime, remSize, totalSize, currentFile, downloaded, speed;
    @FXML
    private ProgressBar progressBar;

    public Controller(PrimaryModel primaryModel, Model model) {
        this.primaryModel = primaryModel;
        this.model = model;
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        primaryModel.connectionProperty().addListener((obs, wasConnected, isConnected) -> {
            if (!isConnected)
                model.stopProgressViewer(View.StopMode.CONNECTION_ERROR_STOP);

        });

        dialogPane.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
            if (!isExpanded) {
                expandableHeight = expandableGrid.getHeight();
            }
            double expandableH = expandableHeight;
            expandableH = isExpanded ? expandableH : -expandableH;
            stage.setHeight(stage.getHeight() + expandableH);
        });
        model.fileReceiveProgressProperty().addListener((obs, oldProgress, newProgress) -> {
            remTime.setText(newProgress.timeLeft());
            remSize.setText("[" + newProgress.remainingSize() + "]");
            totalSize.setText(newProgress.totalSize());
            currentFile.setText(newProgress.currentFile());
            downloaded.setText(newProgress.downloadedFileSize());
            speed.setText(newProgress.receiptRate());
            progressBar.progressProperty().set(newProgress.per());
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

}
