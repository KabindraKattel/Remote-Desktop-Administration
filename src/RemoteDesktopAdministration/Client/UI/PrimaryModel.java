package RemoteDesktopAdministration.Client.UI;

import RemoteDesktopAdministration.Client.Chat;
import RemoteDesktopAdministration.Client.Logger.ClientLogger;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.stage.Stage;

import java.io.File;

public class PrimaryModel {
    private final BooleanProperty clientSwitchState = new SimpleBooleanProperty(false);
    private final ObjectProperty<Chat.UIChatRecord> chatRecord = new SimpleObjectProperty<>();
    private final ObjectProperty<ClientLogger.UILogRecord> clientLogRecord = new SimpleObjectProperty<>();
    private final ObjectProperty<File> fileAddedToShareHistory = new SimpleObjectProperty<>();
    private final StringProperty clientId = new SimpleStringProperty("");
    private final BooleanProperty connection = new SimpleBooleanProperty(false);
    private final StringProperty informationAlert = new SimpleStringProperty("");
    private String clientName = "";
    private Stage primaryStage;

    BooleanProperty clientSwitchStateProperty() {
        return clientSwitchState;
    }

    public boolean getClientSwitchState() {
        return clientSwitchState.get();
    }

    public void setClientSwitchState(boolean clientSwitchState) {
        this.clientSwitchState.set(clientSwitchState);
    }

    public synchronized void setClientId(String clientId) {
        Platform.runLater(() -> this.clientId.set(clientId));
    }

    StringProperty clientIdProperty() {
        return clientId;
    }

    ObjectProperty<ClientLogger.UILogRecord> clientLogRecordProperty() {
        return clientLogRecord;
    }

    public synchronized void setClientLogRecord(ClientLogger.UILogRecord uiLogRecord) {
        Platform.runLater(() -> this.clientLogRecord.set(uiLogRecord));
    }

    public ObjectProperty<Chat.UIChatRecord> chatRecordProperty() {
        return chatRecord;
    }

    public void setChatRecord(Chat.UIChatRecord uiChatRecord) {
        Platform.runLater(() -> this.chatRecord.set(uiChatRecord));
    }

    String getClientName() {
        return this.clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public BooleanProperty connectionProperty() {
        return connection;
    }

    public void connectionLost() {
        if (connection.isBound())
            connection.unbind();
        this.connection.set(false);

    }

    public void setInformationAlert(String information) {
        Platform.runLater(() -> this.informationAlert.set(information));
    }

    StringProperty informationAlertProperty() {
        return informationAlert;
    }

    public ObjectProperty<File> fileAddedToShareHistoryProperty() {
        return fileAddedToShareHistory;
    }

    public void setFileAddedToShareHistory(File file) {
        Platform.runLater(() -> fileAddedToShareHistory.set(file));
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

}
