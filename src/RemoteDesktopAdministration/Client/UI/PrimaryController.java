package RemoteDesktopAdministration.Client.UI;

import RemoteDesktopAdministration.Client.Logger.ClientLogger;
import RemoteDesktopAdministration.Client.MainClient;
import RemoteDesktopAdministration.Client.UI.FileReceiveProgress.View;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;


public class PrimaryController implements Initializable {

    private static final int MIN = 1024;
    private static final int MAX = 65535;
    private static final int DEFAULT = MainClient.DEFAULT_SERVER_PORT;
    private static final Clipboard SYSTEM_CLIPBOARD = Clipboard.getSystemClipboard();
    private static final ClipboardContent CLIPBOARD_CONTENT = new ClipboardContent();
    private static final DirectoryChooser folderChooser = new DirectoryChooser();
    private final TranslateTransition translateTransition = new TranslateTransition();
    private final FillTransition fillTransition = new FillTransition();
    private final StrokeTransition strokeTransition = new StrokeTransition();
    private final ParallelTransition parallelTransition = new ParallelTransition();
    private final StringProperty trimmedIPInput = new SimpleStringProperty("");
    private final StringProperty trimmedMessageInput = new SimpleStringProperty("");
    private final Service<Boolean> serverConnectService = new ServerConnectService();
    private final Service<Void> serverListenerService = new ServerListenerService();
    private final ArrayList<String> yearMonDates = new ArrayList<>();
    private final ObservableList<File> sharedFiles = FXCollections.observableArrayList();
    private final ObservableMap<Integer, File> listViewIndexFileMapper = FXCollections.observableHashMap();
    private final MainClient mainClient;
    private final PrimaryModel primaryModel = new PrimaryModel();
    private Stage primaryStage;
    private View fileReceiveProgressView;
    @FXML
    private GridPane chatGridPane;
    @FXML
    private ScrollPane chatScrollPane, eventScrollPane;
    @FXML
    private StackPane clientToggleSwitch;
    @FXML
    private Label idLabel, nameLabel;
    @FXML
    private ListView<String> fileSharedList;
    @FXML
    private TextFlow eventFlow;
    @FXML
    private TextField ipInput, downLocationField;
    @FXML
    private TextArea messageInput;
    @FXML
    private Spinner<Integer> portInput;
    @FXML
    private Button defaultPortBtn, sendBtn, changeDownLocation;

    public PrimaryController() {
        ClientLogger.initLogger(primaryModel);
        this.mainClient = new MainClient(primaryModel);
    }

    private static VBox getChatRecordVBox(Node... nodes) {
        final var vBox = new VBox(nodes);
        vBox.setPadding(new Insets(10));
        return vBox;
    }

    public void initialize(URL url, ResourceBundle resourceBundle) {

        mainClient.setDownloadLocation(MainClient.DEFAULT_DOWNLOAD_LOCATION);
        downLocationField.setText(mainClient.getDownloadLocation().getAbsolutePath());
        changeDownLocation.setOnAction(this::changeDownLocationBtnOnAction);
        installTooltip("Connect / Disconnect To Server", clientToggleSwitch);
        installTooltip("Default Server Port", defaultPortBtn);
        installTooltip("Server Port [" + MIN + " , " + MAX + "]", portInput);

        configureIntegerSpinner(portInput);

        bindToNodeDisableProperty((portInput.getEditor().textProperty().isEqualTo(DEFAULT + "")).or(primaryModel.clientSwitchStateProperty()), defaultPortBtn);
        defaultPortBtn.setOnAction(this::defaultPortBtnOnAction);

        primaryModel.connectionProperty().bind(primaryModel.clientSwitchStateProperty());

        bindToTextFieldTrimmedTextProperty(ipInput, trimmedIPInput);
        final var emptyTrimmedIpInputProperty = Bindings.isEmpty(trimmedIPInput);
        bindToNodeDisableProperty(emptyTrimmedIpInputProperty, clientToggleSwitch);

        bindToNodeDisableProperty(primaryModel.clientSwitchStateProperty(), ipInput, portInput);

        final var reversedClientStateProperty = primaryModel.clientSwitchStateProperty().not();
        bindToNodeDisableProperty(reversedClientStateProperty, messageInput); //ipInput

        bindToTextAreaTrimmedTextProperty(messageInput, trimmedMessageInput);
        final var emptyTrimmedMsgInputProperty = Bindings.isEmpty(trimmedMessageInput);
        bindToNodeDisableProperty(reversedClientStateProperty.or(emptyTrimmedMsgInputProperty), sendBtn);

        autoScrollVBarEnclosingLayoutPane(eventScrollPane, eventFlow);
        autoScrollVBarEnclosingLayoutPane(chatScrollPane, chatGridPane);

        nameLabel.setText(primaryModel.getClientName());

        final ObservableList<Node> children = clientToggleSwitch.getChildren();

        final Circle sliderNode = (Circle) children.get(1);
        final Rectangle baseNode = (Rectangle) children.get(0);

        translateTransition.setNode(sliderNode);
        fillTransition.setShape(baseNode);
        strokeTransition.setShape(baseNode);
        parallelTransition.getChildren().setAll(translateTransition, fillTransition, strokeTransition);

        clientSwitchOnStateChanged(sliderNode, baseNode);
        clientLogRecordOnChanged();
        chatRecordOnChanged();
        clientIDOnChanged();
        clientSwitchOnServerOffline();
        informationAlertOnChanged();

        ipInput.setOnAction(actionEvent -> {
            portInput.requestFocus();
            actionEvent.consume();
        });
        portInput.getEditor().setOnAction(actionEvent -> {
            clientToggleSwitch.fireEvent(new ActionEvent(clientToggleSwitch, clientToggleSwitch));
            actionEvent.consume();
        });
        clientToggleSwitch.setOnMouseClicked(this::clientSwitchOnMouseClicked);
        clientToggleSwitch.addEventHandler(ActionEvent.ACTION, this::clientSwitchOnAction);

//        messageInput.setOnAction(actionEvent -> {
//            sendBtn.fire();
//            actionEvent.consume();
//        });
        fileAddedToShareHistoryPropertyOnChanged();
        sendBtn.setOnAction(this::sendBtnOnAction);
    }

    private void changeDownLocationBtnOnAction(ActionEvent actionEvent) {
        final File file = folderChooser.showDialog(primaryStage);
        if (file != null) {
            Platform.runLater(() -> {
                downLocationField.setText(file.getAbsolutePath());
                mainClient.setDownloadLocation(file);
            });
        }
        actionEvent.consume();
    }

    private void fileAddedToShareHistoryPropertyOnChanged() {
        primaryModel.fileAddedToShareHistoryProperty().addListener((obs, oldFile, newFile) -> {
            if (!sharedFiles.contains(newFile)) {
                sharedFiles.add(newFile);
                int index = fileSharedList.getItems().size();
                fileSharedList.getItems().add((newFile.isDirectory() ? "[FOLDER] " : "[FILE] ") + newFile.getName());
                listViewIndexFileMapper.put(index, newFile);
            }
        });
    }

    private void informationAlertOnChanged() {
        primaryModel.informationAlertProperty().addListener((obs, oldMsg, newMsg) -> {
            if (!newMsg.isEmpty()) {
                primaryModel.setInformationAlert("");
                showInformationAlert(Modality.WINDOW_MODAL, "Shutdown Option Alert", "Information", newMsg, 30);
            }
        });
    }

    private void defaultPortBtnOnAction(ActionEvent actionEvent) {
        portInput.getEditor().setText(DEFAULT + "");
        portInput.commitValue();
        portInput.requestFocus();
        actionEvent.consume();
    }

    void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryModel.setPrimaryStage(primaryStage);
    }

    private void configureIntegerSpinner(Spinner<Integer> integerSpinner) {
        integerSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(PrimaryController.MIN, PrimaryController.MAX, PrimaryController.DEFAULT));
        integerSpinner.getEditor().textProperty().addListener((observable, oldTxt, newTxt) -> {
            if (newTxt.isEmpty())
                integerSpinner.getEditor().setText(PrimaryController.DEFAULT + "");
            if (!newTxt.matches("\\d*")) {
                var txt = newTxt.replaceAll("[^\\d]", "");
                integerSpinner.getEditor().setText(txt);
            }
        });
    }

    private void clientSwitchOnServerOffline() {
        primaryModel.connectionProperty().addListener((observableValue, wasConnection, isConnection) -> {
            if (!isConnection) {
                if (primaryModel.getClientSwitchState()) {
                    clientToggleSwitch.fireEvent(new ActionEvent(clientToggleSwitch, clientToggleSwitch));
                    if (!primaryModel.connectionProperty().isBound()) {
                        primaryModel.connectionProperty().bind(primaryModel.clientSwitchStateProperty());
                    }
                }
            }
        });
    }

    private void installTooltip(String tooltipText, Node node) {
        Tooltip tooltip = new Tooltip(tooltipText);
        Tooltip.install(node, tooltip);
    }

    boolean showConfirmAlertAndWait(String contentText) {
        AtomicBoolean ok = new AtomicBoolean(false);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, contentText);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.initOwner(primaryStage);
        alert.showAndWait().filter(response -> response == ButtonType.OK).ifPresent(response -> ok.set(true));
        return ok.get();
    }

    private void bindToNodeDisableProperty(ObservableBooleanValue toBind, Node... nodesBindTo) {
        Stream.of(nodesBindTo).forEach(nodeBindTo -> nodeBindTo.disableProperty().bind(toBind));
    }

    private void bindToTextAreaTrimmedTextProperty(TextArea textArea, StringProperty trimmedStringProperty) {
        trimmedStringProperty.bind(Bindings.createStringBinding(() -> textArea.getText().trim(), textArea.textProperty()));
    }

    private void bindToTextFieldTrimmedTextProperty(TextField textField, StringProperty trimmedStringProperty) {
        trimmedStringProperty.bind(Bindings.createStringBinding(() -> textField.getText().trim(), textField.textProperty()));
    }

    private void autoScrollVBarEnclosingLayoutPane(ScrollPane scrollPane, Pane pane) {
        scrollPane.vvalueProperty().bind(pane.heightProperty());
    }

    private void clientLogRecordOnChanged() {
        primaryModel.clientLogRecordProperty().addListener((observableValue, oldRecord, newRecord) -> {
            final var children = eventFlow.getChildren();
            if (!children.isEmpty()) {
                children.add(new Text(System.lineSeparator()));
            }
            children.addAll(newRecord.getRecordDate(), newRecord.getRecordMessage());
            eventFlow.setLineSpacing(1.5);
        });
    }

    private void chatRecordOnChanged() {
        primaryModel.chatRecordProperty().addListener((observableValue, oldRecord, newRecord) -> {

            final var recordMessage = new Text(newRecord.getRecordMessage());
            final var messageFlow = new TextFlow(recordMessage);

            addCssClassToChatMessage(newRecord, recordMessage, messageFlow);
            messageFlow.setLineSpacing(2);

            final var zonedDateTime = newRecord.getRecordDate();
            final var ymd = zonedDateTime.format(DateTimeFormatter.ofPattern("u LLL dd"));
            final var hrMin = zonedDateTime.format(DateTimeFormatter.ofPattern("hh : mm a "));

            addDateToChatPane(ymd);

            final var columnIndex = newRecord.isSent() ? 1 : 0;
            final var rowIndex = chatGridPane.getRowCount();

            final VBox vBox = getChatRecordVBox(messageFlow, new Text(hrMin));
            chatGridPane.add(vBox, columnIndex, rowIndex, 2, 1);
            addCopyContextMenuAndSetOnAction(messageFlow, newRecord.getRecordMessage());
        });
    }

    private void addCssClassToChatMessage(RemoteDesktopAdministration.Client.Chat.UIChatRecord newRecord, Text recordMessage, TextFlow messageFlow) {
        if (newRecord.isSent()) {
            recordMessage.getStyleClass().add("sentMessageContent");
            messageFlow.getStyleClass().addAll("sentMessage", "message");
        } else {
            recordMessage.getStyleClass().add("receivedMessageContent");
            messageFlow.getStyleClass().addAll("receivedMessage", "message");
        }
    }

    private void addDateToChatPane(String ymd) {
        if (!yearMonDates.contains(ymd)) {
            yearMonDates.add(ymd);
            final var rowCount = chatGridPane.getRowCount();
            chatGridPane.add(new Separator(), 0, rowCount, 1, 1);
            chatGridPane.add(new BorderPane(new Text(ymd)), 1, rowCount, 1, 1);
            chatGridPane.add(new Separator(), 2, rowCount, 1, 1);
        }
    }

    private void addCopyContextMenuAndSetOnAction(Node node, String message) {

        final var copyMenuItem = new MenuItem("Copy");
        final var contextMenu = new ContextMenu(copyMenuItem);
        node.setOnContextMenuRequested(e -> {
            contextMenu.show(node, e.getScreenX(), e.getScreenY());
            contextMenu.setAutoHide(true);
        });
        copyMenuItem.setOnAction(e -> messageCopyMenuItemOnAction(message));
    }

    private void messageCopyMenuItemOnAction(String toCopy) {
        copyToolAlert(toCopy, "Message Copy Tool Alert");
    }

    private void copyToolAlert(String message, String title) {

        CLIPBOARD_CONTENT.putString(message);
        SYSTEM_CLIPBOARD.setContent(CLIPBOARD_CONTENT);
        showInformationAlert(Modality.NONE, title, "SUCCESS | Copied to system clipboard", message, 5);
    }

    private void showInformationAlert(Modality modality, String title, String headerText, String message, int autoCloseAfterInSec) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.initModality(modality);
        alert.initOwner(primaryStage);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.show();
        if (alert.isShowing()) {
            PauseTransition pauseTransition = new PauseTransition(Duration.seconds(autoCloseAfterInSec));
            pauseTransition.setOnFinished(e -> alert.hide());
            pauseTransition.play();
        }
    }

    private void clientIDOnChanged() {
        primaryModel.clientIdProperty().addListener((observableValue, oldID, newID) -> {
            idLabel.setText(newID);
            if (!newID.isEmpty())
                ClientLogger.CLIENT_LOGGER.info("Client Unique Id : " + newID);
        });
    }

    private void sendBtnOnAction(ActionEvent actionEvent) {
        mainClient.writeStream(MainClient.DescriptorCode.CHAT_INITIATOR_CLIENT, trimmedMessageInput.get());
        messageInput.clear();
        messageInput.requestFocus();
        actionEvent.consume();
    }

    private void clientSwitchOnStateChanged(Circle sliderNode, Rectangle baseNode) {
        primaryModel.clientSwitchStateProperty().addListener((observableValue, oldState, newState) -> {
            final var baseNodeWidth = baseNode.getWidth();
            final var sliderNodeRadius = sliderNode.getRadius();
            final double moveToX = newState ? baseNodeWidth - 2 * sliderNodeRadius : 0;
            final var colorFromValue = (newState) ? Color.RED : Color.GREEN;
            final var colorToValue = (newState) ? Color.GREEN : Color.RED;
            setAnimationOnClientSwitchState(moveToX, colorFromValue, colorToValue);
        });
    }

    private void setAnimationOnClientSwitchState(double moveToX, Color colorFromValue, Color colorToValue) {
        translateTransition.setToX(moveToX);

        fillTransition.setFromValue(colorFromValue);
        fillTransition.setToValue(colorToValue);

        strokeTransition.setFromValue(colorFromValue);
        strokeTransition.setToValue(colorToValue);

        parallelTransition.setAutoReverse(true);
        parallelTransition.play();
    }

    private void clientSwitchOnMouseClicked(MouseEvent mouseEvent) {
        final StackPane source = (StackPane) mouseEvent.getSource();
        final Rectangle baseNode = (Rectangle) source.getChildren().get(0);
        if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 1) {
            setClientSwitchPaletteColor(baseNode, Color.WHITE);
            if (primaryModel.getClientSwitchState()) {
                final var alertConfirmed = showConfirmAlertAndWait("Are you sure you want to disconnect connection with Server ?");
                if (!alertConfirmed) {
                    setClientSwitchPaletteColor(baseNode, Color.GREEN);
                    mouseEvent.consume();
                    return;
                }
            }
            source.fireEvent(new ActionEvent(source, mouseEvent.getTarget()));
        }
        mouseEvent.consume();
    }

    private void clientSwitchOnAction(ActionEvent actionEvent) {

        final var source = (StackPane) actionEvent.getSource();
        final Rectangle baseNode = (Rectangle) source.getChildren().get(0);
        if (!primaryModel.getClientSwitchState()) {
            serverConnectService.restart();
            serverConnectService.setOnSucceeded(workerStateEvent -> {
                if (serverConnectService.getValue()) {
                    primaryModel.setClientSwitchState(true);
                    serverListenerService.restart();
                } else {
                    setClientSwitchPaletteColor(baseNode, Color.RED);
                }
            });
        } else {
            if (mainClient.disconnect()) {
                primaryModel.setClientId("");
                primaryModel.setClientSwitchState(false);
            } else {
                setClientSwitchPaletteColor(baseNode, Color.GREEN);
            }
        }
        source.requestFocus();
        actionEvent.consume();
    }

    private void setClientSwitchPaletteColor(Rectangle baseNode, Color color) {
        baseNode.setFill(color);
        baseNode.setStroke(color);
    }

    private class ServerListenerService extends Service<Void> {

        @Override
        protected Task<Void> createTask() {
            return new Task<>() {
                @Override
                protected Void call() {
                    mainClient.listenServer();
                    return null;
                }
            };
        }
    }

    private class ServerConnectService extends Service<Boolean> {

        @Override
        protected Task<Boolean> createTask() {
            return new Task<>() {
                @Override
                protected Boolean call() {
                    portInput.commitValue();
                    return mainClient.connect(trimmedIPInput.get(), portInput.getValue());
                }
            };
        }
    }

}
