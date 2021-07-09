package RemoteDesktopAdministration.Server.UI.FileSendContents;

import RemoteDesktopAdministration.Server.MainServer;
import RemoteDesktopAdministration.Server.Utils.FileUtils;
import RemoteDesktopAdministration.Server.Utils.ImageUtils;
import RemoteDesktopAdministration.Server.Utils.ListInnerContents;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.ResourceBundle;


public class Controller implements Initializable {

    private static final FileChooser fileChooser = new FileChooser();
    private static final DirectoryChooser folderChooser = new DirectoryChooser();
    private final static Hashtable<HBox, File> filesBoxBoardMap = new Hashtable<>();
    private final static Hashtable<File, ListInnerContents.InnerContents> fileInnerContentsMap = new Hashtable<>();
    @FXML
    private Button chooserBtn, transferBtn;
    @FXML
    private CheckBox chooserType;
    @FXML
    private AnchorPane dragPane;
    @FXML
    private Label filesBoxPlaceholder;
    @FXML
    private VBox fileContentsVBox;
    private Stage stage;
    private MainServer mainServer;
    private List<String> selectedClients;

    private static double transformWindowPointToViewPortPoint(double windowP, double windowMinP, double windowMaxP, double viewPortMinP, double viewPortMaxP) {
        double window_DeltaPoint = windowMaxP - windowMinP;
        double viewPort_DeltaPoint = viewPortMaxP - viewPortMinP;
        double viewport_ScaleFactor = viewPort_DeltaPoint / window_DeltaPoint;
        double viewport_TranslationFactor = (windowMaxP * viewPortMinP - windowMinP * viewPortMaxP) / window_DeltaPoint;
        return (viewport_ScaleFactor * windowP + viewport_TranslationFactor);
    }

    static void clearTable() {
        filesBoxBoardMap.clear();
        fileInnerContentsMap.clear();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        BooleanProperty empty = new SimpleBooleanProperty(true);
        empty.bind(Bindings.isEmpty(fileContentsVBox.getChildren()));
        filesBoxPlaceholder.visibleProperty().bind(empty);

        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        folderChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.setTitle("Choose File(s)");
        folderChooser.setTitle("Choose Folder");

        chooserBtn.textProperty().bind(Bindings.when(chooserType.selectedProperty()).then("Drag and Drop Files Folders or Click to add Files").otherwise("Drag and Drop Files Folders or Click to add Folder"));
        chooserBtn.setOnAction(this::chooserBtnOnAction);
        transferBtn.setOnAction(this::transferBtnOnAction);
        dragPane.setOnDragEntered(this::dragPaneOnDragEntered);
        dragPane.setOnDragOver(this::dragPaneOnDragOver);
        dragPane.setOnDragDropped(this::dragPaneOnDragDropped);
        dragPane.setOnDragExited(this::dragPaneOnDragExited);

    }

    private void transferBtnOnAction(ActionEvent actionEvent) {
        mainServer.addFTPChannelInnerContents(selectedClients, fileInnerContentsMap.values());
        clearTable();
        stage.close();
        actionEvent.consume();
    }

    private void chooserBtnOnAction(ActionEvent actionEvent) {
        if ((chooserType.isSelected())) {
            openMultipleFileChooser(actionEvent);
        } else {
            openSingleFolderChooser(actionEvent);
        }
        actionEvent.consume();
    }

    private void openSingleFolderChooser(ActionEvent actionEvent) {
        final File file = folderChooser.showDialog(stage);
        if (file != null) {
            Platform.runLater(() -> AddInformation.addLayout_ConfigFilesBoard(file, fileContentsVBox));
        }
        actionEvent.consume();
    }

    private void openMultipleFileChooser(ActionEvent actionEvent) {
        final List<File> files = fileChooser.showOpenMultipleDialog(stage);
        if (files != null) {
            files.parallelStream().forEach(file -> Platform.runLater(() -> AddInformation.addLayout_ConfigFilesBoard(file, fileContentsVBox)));
        }
        actionEvent.consume();
    }

    private void dragPaneOnDragEntered(DragEvent dragEvent) {

        dragPane.requestFocus();

        if (dragEvent.getDragboard().hasFiles()) {
            dragPane.setStyle("-fx-background-color: dodgerblue");
        }

        dragEvent.consume();

    }

    private void dragPaneOnDragOver(DragEvent dragEvent) {

        final Dragboard dragboard = dragEvent.getDragboard();

        if (dragboard.hasFiles()) {
            dragEvent.acceptTransferModes(TransferMode.COPY);
            dragEvent.consume();
        }

    }

    private void dragPaneOnDragDropped(DragEvent dragEvent) {
        boolean isTransferDone = false;
        final var dragboard = dragEvent.getDragboard();

        if (dragboard.hasFiles()) {
            isTransferDone = true;
            dragboard.getFiles().parallelStream().forEach(file -> Platform.runLater(() -> AddInformation.addLayout_ConfigFilesBoard(file, fileContentsVBox)));
        }

        dragEvent.setDropCompleted(isTransferDone);
        dragEvent.consume();
    }

    private void dragPaneOnDragExited(DragEvent dragEvent) {

        if (dragEvent.getDragboard().hasFiles()) {
            dragPane.setStyle("-fx-background-color: white");
        }
        dragEvent.consume();

    }

    void setStage(Stage stage) {
        this.stage = stage;
    }

    void setMainServer(MainServer mainServer) {
        this.mainServer = mainServer;
    }

    void setSelectedClients(List<String> selectedClients) {
        this.selectedClients = selectedClients;
    }

    private static class AddInformation {

        private static void addLayout_ConfigFilesBoard(File addedFile, Pane containerPane) {
            ImageView imageView = new ImageView(ImageUtils.getFxImageFromSwingIcon(addedFile));
            imageView.setPickOnBounds(true);
            imageView.setCache(true);
            imageView.setPreserveRatio(true);
            imageView.setFitHeight(32);
            imageView.setFitWidth(32);

            double fittedWidth = Math.min(imageView.getFitWidth(), imageView.getImage().getWidth());
            double fittedHeight = Math.min(imageView.getFitHeight(), imageView.getImage().getHeight());
            double fitDeltaP = Math.min(fittedWidth, fittedHeight);
            final var window = new Rectangle2D(0, 0, 32, 32);
            double viewPortMinX = window.getMinX() + (window.getWidth() - fitDeltaP) / 2;
            double viewPortMaxX = viewPortMinX + fitDeltaP;
            double viewPortMinY = window.getMinY() + (window.getHeight() - fitDeltaP) / 2;
            double viewPortMaxY = viewPortMinY + fitDeltaP;
            double x = transformWindowPointToViewPortPoint(window.getMinX(), window.getMinX(), window.getMaxX(), viewPortMinX, viewPortMaxX);
            double y = transformWindowPointToViewPortPoint(window.getMinY(), window.getMinY(), window.getMaxY(), viewPortMinY, viewPortMaxY);
            imageView.setX(x);
            imageView.setY(y);
            Label name = new Label(addedFile.getName());
            Label size = new Label("Unknown");
            Button remove = new Button("X");

            name.setMaxWidth(Double.MAX_VALUE);
            name.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);

            size.setPrefSize(159, 14);

            remove.setCancelButton(true);
            remove.setTooltip(new Tooltip("Remove item from transfer list"));
            remove.setPrefSize(32, 32);
            remove.setStyle("-fx-background-color: red;");
            remove.setTextFill(Color.WHITE);
            remove.setFont(new Font(20));
            remove.setCursor(Cursor.HAND);


            HBox hBox = new HBox(imageView, name, size, remove);
            hBox.setSpacing(20);
            hBox.setPadding(new Insets(0, 0, 0, 10));
            hBox.setMaxWidth(Double.MAX_VALUE);

            hBox.setAlignment(Pos.CENTER_LEFT);
            hBox.setStyle("-fx-background-color: lightgray;");
            HBox.setHgrow(name, Priority.ALWAYS);
            containerPane.getChildren().add(hBox);
            remove.setOnAction(actionEvent -> {
                containerPane.getChildren().remove(hBox);
                filesBoxBoardMap.computeIfPresent(hBox, (key, value) -> {
                    fileInnerContentsMap.remove(value);
                    return null;
                });
            });
            if (!filesBoxBoardMap.containsValue(addedFile))
                filesBoxBoardMap.computeIfAbsent(hBox, key -> {
                    final var extractInnerContentsService = ExtractInnerContentsService.getInstance();
                    extractInnerContentsService.setFile(addedFile);
                    extractInnerContentsService.start();
                    extractInnerContentsService.setOnSucceeded(e -> {
                        final var value = extractInnerContentsService.getValue();
                        fileInnerContentsMap.putIfAbsent(addedFile, value);
                        size.setText(FileUtils.getReadableFileSize(value.totalSize()));
                        e.consume();
                    });
                    return addedFile;
                });
        }

    }

    private static class ExtractInnerContentsService extends Service<ListInnerContents.InnerContents> {
        private File addedFile;

        private static ExtractInnerContentsService getInstance() {
            return new ExtractInnerContentsService();
        }

        public void setFile(File file) {
            this.addedFile = file;
        }

        @Override
        protected Task<ListInnerContents.InnerContents> createTask() {
            return new Task<>() {
                @Override
                protected ListInnerContents.InnerContents call() {
                    return ListInnerContents.getContents(addedFile.toPath(), false);
                }
            };
        }
    }
}
