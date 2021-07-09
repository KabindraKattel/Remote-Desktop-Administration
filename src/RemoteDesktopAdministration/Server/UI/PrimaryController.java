package RemoteDesktopAdministration.Server.UI;

import RemoteDesktopAdministration.Server.ControlOps;
import RemoteDesktopAdministration.Server.Logger.ServerLogger;
import RemoteDesktopAdministration.Server.MainServer;
import RemoteDesktopAdministration.Server.UI.FileSendContents.View;
import RemoteDesktopAdministration.Server.Utils.ListInnerContents;
import javafx.animation.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
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
import javafx.scene.control.cell.CheckBoxListCell;
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
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class PrimaryController implements Initializable {
    private static final int MIN = 1024;
    private static final int MAX = 65535;
    private static final int DEFAULT = MainServer.DEFAULT_PORT;
    private static final Clipboard SYSTEM_CLIPBOARD = Clipboard.getSystemClipboard();
    private static final ClipboardContent CLIPBOARD_CONTENT = new ClipboardContent();
    private static final FileChooser fileChooser = new FileChooser();
    private final TranslateTransition translateTransition = new TranslateTransition();
    private final FillTransition fillTransition = new FillTransition();
    private final StrokeTransition strokeTransition = new StrokeTransition();
    private final ParallelTransition parallelTransition = new ParallelTransition();
    private final StringProperty trimmedMessageInput = new SimpleStringProperty("");
    private final Service<Void> clientsListenerService = new ClientListenerService();
    private final ArrayList<String> yearMonDates = new ArrayList<>();
    private final ObservableList<File> sharedFiles = FXCollections.observableArrayList();
    private final ObservableMap<Integer, File> listViewIndexFileMapper = FXCollections.observableHashMap();
    private final MainServer mainServer;
    private final PrimaryModel primaryModel = new PrimaryModel();
    private Stage primaryStage;
    @FXML
    private GridPane chatGridPane;
    @FXML
    private ScrollPane chatScrollPane, eventScrollPane;
    @FXML
    private StackPane serverToggleSwitch;
    @FXML
    private Label nameLabel, ipLabel;
    @FXML
    private ComboBox<String> ipComboBox;
    @FXML
    private Spinner<Integer> portInput;
    @FXML
    private ListView<String> clientOnlineList, fileSharedList;
    @FXML
    private TextFlow eventFlow;
    @FXML
    private TextArea messageInput;
    @FXML
    private Button sendBtn, defaultPortBtn, disconnectClientBtn, fileTransferBtn, readClient, clientShutdownBtn, clientRestartBtn, clientLogoffBtn, installSoftwareBtn;
    @FXML
    private SplitMenuButton ipPortCopyBtn;


    public PrimaryController() {
        ServerLogger.initLogger(primaryModel);
        this.mainServer = new MainServer(primaryModel);
    }

    private static VBox getChatRecordVBox(Node... nodes) {
        final var vBox = new VBox(nodes);
        vBox.setPadding(new Insets(10));
        return vBox;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        installTooltip("Start / Stop Server", serverToggleSwitch);
        installTooltip("Copy IP or Server Port ", ipPortCopyBtn);
        installTooltip("Default Port", defaultPortBtn);
        installTooltip("Server Port [" + MIN + " , " + MAX + "]", portInput);

        configureIntegerSpinner(portInput);

        bindToNodeDisableProperty((portInput.getEditor().textProperty().isEqualTo(DEFAULT + "")).or(primaryModel.serverSwitchStateProperty()), defaultPortBtn);
        defaultPortBtn.setOnAction(this::defaultPortBtnOnAction);

        ipPortCopyBtn.setOnAction(this::ipPortCopyBtnOnAction);
        ipPortCopyBtn.getItems().addAll(new MenuItem("Server _IP"), new MenuItem("Server _Port"));
        ipPortCopyBtn.getItems().forEach(menuItem -> {
            menuItem.setStyle("-fx-text-fill:black");
            menuItem.setOnAction(this::ipPortCopyBtnMenuItemsOnAction);
        });

        bindToNodeDisableProperty(primaryModel.serverSwitchStateProperty(), portInput);

        final var reversedServerStateProperty = primaryModel.serverSwitchStateProperty().not();
        final var noClientSelectedProperty = Bindings.isEmpty(clientOnlineList.getSelectionModel().getSelectedItems());
        bindToNodeDisableProperty(reversedServerStateProperty.or(noClientSelectedProperty), fileTransferBtn, disconnectClientBtn, readClient, clientShutdownBtn, clientRestartBtn, clientLogoffBtn, installSoftwareBtn, messageInput);

        bindToTextAreaTrimmedTextProperty(messageInput, trimmedMessageInput);
        final var emptyTrimmedMsgInputProperty = Bindings.isEmpty(trimmedMessageInput);
        bindToNodeDisableProperty(reversedServerStateProperty.or(noClientSelectedProperty).or(emptyTrimmedMsgInputProperty), sendBtn);

        autoScrollVBarEnclosingLayoutPane(eventScrollPane, eventFlow);
        autoScrollVBarEnclosingLayoutPane(chatScrollPane, chatGridPane);

        nameLabel.setText(primaryModel.getHostName());
        ipLabel.setLabelFor(ipComboBox);

        ipComboBox.setItems(FXCollections.observableArrayList("localhost"));
        ipComboBox.getSelectionModel().select(0);
        ipComboBox.getItems().addAll(primaryModel.getIPv4Addresses());
        ipComboBox.getItems().addAll(primaryModel.getIPv6Addresses());

        clientOnlineList.setCellFactory(CustomCheckBoxListCell::new);

        final ObservableList<Node> children = serverToggleSwitch.getChildren();

        final Circle sliderNode = (Circle) children.get(1);
        final Rectangle baseNode = (Rectangle) children.get(0);

        translateTransition.setNode(sliderNode);
        fillTransition.setShape(baseNode);
        strokeTransition.setShape(baseNode);
        parallelTransition.getChildren().setAll(translateTransition, fillTransition, strokeTransition);

        clientOnlineList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        fileSharedList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        serverSwitchOnStateChanged(sliderNode, baseNode);
        clientOnlineListOnChanged();
        serverLogRecordOnChanged();
        chatRecordOnChanged();
        fileAddedToShareHistoryPropertyOnChanged();

        portInput.getEditor().setOnAction(actionEvent -> {
            serverToggleSwitch.fireEvent(new ActionEvent(serverToggleSwitch, serverToggleSwitch));
            actionEvent.consume();
        });
        serverToggleSwitch.setOnMouseClicked(this::serverSwitchOnMouseClicked);
        serverToggleSwitch.addEventHandler(ActionEvent.ACTION, this::serverSwitchOnAction);
        installSoftwareBtn.setOnAction(this::remoteSoftwareInstallationBtnOnAction);
        sendBtn.setOnAction(this::sendBtnOnAction);
        readClient.setOnAction(this::readClientBtnOnAction);
        clientShutdownBtn.setOnAction(e -> clientControlOpsBtnOnAction(e, ControlOps.State.SHUTDOWN));
        clientRestartBtn.setOnAction(e -> clientControlOpsBtnOnAction(e, ControlOps.State.RESTART));
        clientLogoffBtn.setOnAction(e -> clientControlOpsBtnOnAction(e, ControlOps.State.LOG_OFF));

        disconnectClientBtn.setOnAction(this::disconnectClientBtnOnAction);
        fileTransferBtn.setOnAction(this::fileTransferBtnOnAction);
    }

    private void remoteSoftwareInstallationBtnOnAction(ActionEvent actionEvent) {
        final var selectedClients = clientOnlineList.getSelectionModel().getSelectedItems();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Windows Setup Files (MSI, EXE)", "*.msi", "*.exe");
        fileChooser.getExtensionFilters().add(extFilter);
        final File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            final List<ListInnerContents.InnerContents> innerContents = new ArrayList<>();
            innerContents.add(ListInnerContents.getContents(file.toPath(), true));
            mainServer.addFTPChannelInnerContents(selectedClients, innerContents);
        }
        actionEvent.consume();
    }

    private void readClientBtnOnAction(ActionEvent actionEvent) {
        final var selectedClients = clientOnlineList.getSelectionModel().getSelectedItems();
        mainServer.writeStream(MainServer.DescriptorCode.SCREEN_SHARING_ON_NO_PROGRESS, "", selectedClients);
        actionEvent.consume();
    }

    private void clientControlOpsBtnOnAction(ActionEvent actionEvent, ControlOps.State state) {
        final var selectedClients = clientOnlineList.getSelectionModel().getSelectedItems();
        Byte descriptorCode = switch (state) {
            case SHUTDOWN -> MainServer.DescriptorCode.CLIENT_SHUTDOWN;
            case RESTART -> MainServer.DescriptorCode.CLIENT_RESTART;
            case LOG_OFF -> MainServer.DescriptorCode.CLIENT_LOGOFF;
            default -> null;
        };
        if (descriptorCode == null)
            return;
        selectedClients.parallelStream().forEachOrdered(client -> ServerLogger.SERVER_LOGGER.info("Preparing " + state.name() + " Request for " + client));
        mainServer.writeStream(MainServer.DescriptorCode.CLIENT_CONTROL_OPERATIONS_REQ, String.valueOf(descriptorCode), selectedClients);
        actionEvent.consume();
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
            if (newTxt.equals(""))
                integerSpinner.getEditor().setText(PrimaryController.DEFAULT + "");
            if (!newTxt.matches("\\d*")) {
                var txt = newTxt.replaceAll("[^\\d]", "");
                integerSpinner.getEditor().setText(txt);
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

    private void bindToTextAreaTrimmedTextProperty(TextArea textArea, StringProperty trimmedIPInputProperty) {
        trimmedIPInputProperty.bind(Bindings.createStringBinding(() -> textArea.getText().trim(), textArea.textProperty()));
    }

    private void autoScrollVBarEnclosingLayoutPane(ScrollPane scrollPane, Pane pane) {
        scrollPane.vvalueProperty().bind(pane.heightProperty());
    }

    private void fileTransferBtnOnAction(ActionEvent actionEvent) {
        final var selectedItems = clientOnlineList.getSelectionModel().getSelectedItems();
        new View(primaryStage, mainServer, selectedItems).start();

    }

    private void sendBtnOnAction(ActionEvent actionEvent) {
        final var selectedItems = clientOnlineList.getSelectionModel().getSelectedItems();
        mainServer.writeStream(MainServer.DescriptorCode.CHAT_INITIATOR_SERVER, trimmedMessageInput.get(), selectedItems);
        messageInput.clear();
        sendBtn.requestFocus();
        actionEvent.consume();
    }

    private void disconnectClientBtnOnAction(ActionEvent actionEvent) {

        final var selectedItems = clientOnlineList.getSelectionModel().getSelectedItems();
        mainServer.writeStream(MainServer.DescriptorCode.CONNECTION_LOST, "", selectedItems);
        selectedItems.forEach(primaryModel::setClientOffline);

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

    private void clientOnlineListOnChanged() {
        primaryModel.clientOnlineProperty().addListener((observableValue, oldClient, newClient) -> {
            final var items = clientOnlineList.getItems();
            items.add(newClient);
        });
        primaryModel.clientOfflineProperty().addListener((observableValue, oldClient, newClient) -> {
            final var items = clientOnlineList.getItems();
            final var selectionModel = clientOnlineList.getSelectionModel();
            final var index = items.indexOf(newClient);
            final var traversalIndex = (index < items.size() - 1) ? index + 1 : index - 1;
            final var traversalIndexWasSelected = (traversalIndex >= 0) && selectionModel.isSelected(traversalIndex);
            boolean removed = items.remove(newClient);
            final var newIndex = Math.min(traversalIndex, index);
            if (!traversalIndexWasSelected && removed) {
                selectionModel.clearSelection(newIndex);
            }
        });
    }

    private void serverLogRecordOnChanged() {
        primaryModel.serverLogRecordProperty().addListener((observableValue, oldRecord, newRecord) -> {
            final var children = eventFlow.getChildren();
            if (!children.isEmpty()) {
                children.add(new Text("\n"));
            }
            children.addAll(newRecord.getRecordDate(), newRecord.getRecordMessage());
            eventFlow.setLineSpacing(1.5);
        });
    }

    private void chatRecordOnChanged() {
        primaryModel.chatRecordProperty().addListener((observableValue, oldRecord, newRecord) -> {

            final var recordAgent = new Text(newRecord.getRecordAgent());
            final var recordMessage = new Text(newRecord.getRecordMessage());
            final var messageFlow = new TextFlow(recordMessage);

            addCssClassToChatMessage(newRecord, recordMessage, messageFlow);
            messageFlow.setLineSpacing(2);

            final var zonedDateTime = newRecord.getRecordDate();
            final var ymd = zonedDateTime.format(DateTimeFormatter.ofPattern("u LLL dd"));
            final var hrMin = zonedDateTime.format(DateTimeFormatter.ofPattern("hh : mm a "));

            addDateToChatPane(ymd);

            final var recordDate = new Text(hrMin);

            final var columnIndex = newRecord.isSent() ? 1 : 0;
            final var rowIndex = chatGridPane.getRowCount();

            final VBox vBox = getChatRecordVBox(recordAgent, messageFlow, recordDate);
            chatGridPane.add(vBox, columnIndex, rowIndex, 2, 1);
            addCopyContextMenuAndSetOnAction(messageFlow, newRecord.getRecordMessage());
        });
    }

    private void addCssClassToChatMessage(RemoteDesktopAdministration.Server.Chat.UIChatRecord newRecord, Text recordMessage, TextFlow messageFlow) {
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

    private void serverSwitchOnStateChanged(Circle sliderNode, Rectangle baseNode) {
        primaryModel.serverSwitchStateProperty().addListener((observableValue, oldState, newState) -> {
            final var baseNodeWidth = baseNode.getWidth();
            final var sliderNodeRadius = sliderNode.getRadius();
            final double moveToX = newState ? baseNodeWidth - 2 * sliderNodeRadius : 0;
            final var colorFromValue = (newState) ? Color.RED : Color.GREEN;
            final var colorToValue = (newState) ? Color.GREEN : Color.RED;
            setAnimationOnServerSwitchState(moveToX, colorFromValue, colorToValue);
        });
    }

    private void setAnimationOnServerSwitchState(double moveToX, Color colorFromValue, Color colorToValue) {
        translateTransition.setToX(moveToX);

        fillTransition.setFromValue(colorFromValue);
        fillTransition.setToValue(colorToValue);

        strokeTransition.setFromValue(colorFromValue);
        strokeTransition.setToValue(colorToValue);

        parallelTransition.setAutoReverse(true);
        parallelTransition.play();
    }

    private void ipPortCopyBtnMenuItemsOnAction(ActionEvent actionEvent) {
        final var menuText = ((MenuItem) actionEvent.getSource()).getText();
        String title = menuText + " Copy Tool Alert";
        switch (menuText) {
            case ("Server _IP") -> copyToolAlert(ipComboBox.getSelectionModel().getSelectedItem(), title);
            case ("Server _Port") -> {
                portInput.commitValue();
                copyToolAlert(portInput.getEditor().getText(), title);
            }
        }
    }

    private void ipPortCopyBtnOnAction(ActionEvent actionEvent) {
        portInput.commitValue();
        final var message = ipComboBox.getSelectionModel().getSelectedItem() + System.lineSeparator() + portInput.getEditor().getText();
        copyToolAlert(message, "Server IP and Port Copy Tool Alert");
    }

    private void copyToolAlert(String message, String title) {

        CLIPBOARD_CONTENT.putString(message);
        SYSTEM_CLIPBOARD.setContent(CLIPBOARD_CONTENT);
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.initModality(Modality.NONE);
        alert.initOwner(primaryStage);
        alert.setTitle(title);
        alert.setHeaderText("SUCCESS | Copied to system clipboard");
        alert.show();
        if (alert.isShowing()) {
            PauseTransition pauseTransition = new PauseTransition(Duration.seconds(5));
            pauseTransition.setOnFinished(e -> alert.hide());
            pauseTransition.play();
        }
    }

    private void serverSwitchOnMouseClicked(MouseEvent mouseEvent) {
        final StackPane source = (StackPane) mouseEvent.getSource();
        final Rectangle baseNode = (Rectangle) source.getChildren().get(0);
        if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 1) {
            setServerSwitchPaletteColor(baseNode, Color.WHITE);
            if (primaryModel.getServerSwitchState()) {
                final var alertConfirmed = showConfirmAlertAndWait("Are you sure you want to stop Server ?");
                if (!alertConfirmed) {
                    setServerSwitchPaletteColor(baseNode, Color.GREEN);
                    mouseEvent.consume();
                    return;
                }
            }
            source.fireEvent(new ActionEvent(source, mouseEvent.getTarget()));
        }
        mouseEvent.consume();
    }

    private void serverSwitchOnAction(ActionEvent actionEvent) {
        final var source = (StackPane) actionEvent.getSource();
        final Rectangle baseNode = (Rectangle) source.getChildren().get(0);
        if (!primaryModel.getServerSwitchState()) {
            portInput.commitValue();
            if (!mainServer.startServer(portInput.getValue())) {
                setServerSwitchPaletteColor(baseNode, Color.RED);
            } else {
                primaryModel.setServerSwitchState(true);
                clientsListenerService.restart();
            }
        } else {
            if (!mainServer.stopServer()) {
                setServerSwitchPaletteColor(baseNode, Color.GREEN);
            } else {
                primaryModel.setServerSwitchState(false);
            }
        }
        source.requestFocus();
        actionEvent.consume();
    }

    private void setServerSwitchPaletteColor(Rectangle baseNode, Color color) {
        baseNode.setFill(color);
        baseNode.setStroke(color);
    }

    private static class CustomCheckBoxListCell extends CheckBoxListCell<String> {

        private final ListView<String> stringListView;
        private final SimpleBooleanProperty checked = new SimpleBooleanProperty();

        public CustomCheckBoxListCell(ListView<String> stringListView) {
            this.stringListView = stringListView;
            super.setSelectedStateCallback(s -> {
                checked.set(isSelected());
                return checked;
            });
            onChecked();
        }

        private void onChecked() {
            checked.addListener((observableValue, wasChecked, isChecked) -> {
                if (isChecked && !isSelected()) {
                    this.stringListView.requestFocus();
                    this.stringListView.getSelectionModel().select(getIndex());
                    this.stringListView.getFocusModel().focus(getIndex());
                } else if (!isChecked && isSelected()) {
                    this.stringListView.requestFocus();
                    this.stringListView.getSelectionModel().clearSelection(getIndex());
                    this.stringListView.getFocusModel().focus(getIndex());
                }
            });
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, item == null || item.trim().equals(""));
        }
    }

    private class ClientListenerService extends Service<Void> {

        @Override
        protected Task<Void> createTask() {
            return new Task<>() {
                @Override
                protected Void call() {
                    mainServer.listenClients();
                    return null;
                }
            };
        }
    }

}
