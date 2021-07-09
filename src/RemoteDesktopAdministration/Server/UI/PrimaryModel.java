package RemoteDesktopAdministration.Server.UI;

import RemoteDesktopAdministration.Server.Chat;
import RemoteDesktopAdministration.Server.Logger.ServerLogger;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;

import java.io.File;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.List;
import java.util.stream.Collectors;

public class PrimaryModel {
    private final SimpleBooleanProperty serverSwitchState = new SimpleBooleanProperty(false);
    private final SimpleStringProperty clientOnline = new SimpleStringProperty();
    private final SimpleStringProperty clientOffline = new SimpleStringProperty();
    private final SimpleObjectProperty<Chat.UIChatRecord> chatRecord = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<ServerLogger.UILogRecord> serverLogRecord = new SimpleObjectProperty<>();
    private final ObservableList<Inet4Address> iPv4Addresses = FXCollections.observableArrayList();
    private final ObservableList<Inet6Address> iPv6Addresses = FXCollections.observableArrayList();
    private final ObjectProperty<File> fileAddedToShareHistory = new SimpleObjectProperty<>();
    private String hostName = "";
    private Stage primaryStage;

    public boolean getServerSwitchState() {
        return serverSwitchState.get();
    }

    public void setServerSwitchState(boolean serverSwitchState) {
        this.serverSwitchState.set(serverSwitchState);
    }

    public SimpleBooleanProperty serverSwitchStateProperty() {
        return serverSwitchState;
    }

    SimpleStringProperty clientOnlineProperty() {
        return clientOnline;
    }

    public synchronized void setClientOnline(String clientOnline) {
        Platform.runLater(() -> this.clientOnline.set(clientOnline)); //switches other thread to application Thread
    }

    public SimpleStringProperty clientOfflineProperty() {
        return clientOffline;
    }

    public synchronized void setClientOffline(String clientOffline) {
        Platform.runLater(() -> this.clientOffline.set(clientOffline));
    }

    SimpleObjectProperty<ServerLogger.UILogRecord> serverLogRecordProperty() {
        return serverLogRecord;
    }

    public synchronized void setServerLogRecord(ServerLogger.UILogRecord uiLogRecord) {
        Platform.runLater(() -> this.serverLogRecord.set(uiLogRecord));
    }

    public SimpleObjectProperty<Chat.UIChatRecord> chatRecordProperty() {
        return chatRecord;
    }

    public synchronized void setChatRecord(Chat.UIChatRecord uiChatRecord) {
        Platform.runLater(() ->
                this.chatRecord.set(uiChatRecord)
        );
    }

    ObservableList<String> getIPv4Addresses() {
        return FXCollections.observableList(iPv4Addresses.stream().map(Inet4Address::getHostAddress).collect(Collectors.toList()));
    }

    public void setIPv4Addresses(List<Inet4Address> inet4Addresses) {
        iPv4Addresses.setAll(inet4Addresses);
    }

    ObservableList<String> getIPv6Addresses() {
        return FXCollections.observableList(iPv6Addresses.stream().map(Inet6Address::getHostAddress).collect(Collectors.toList()));

    }

    public void setIPv6Addresses(List<Inet6Address> inet6Addresses) {
        iPv6Addresses.setAll(inet6Addresses);
    }

    String getHostName() {
        return this.hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public ObjectProperty<File> fileAddedToShareHistoryProperty() {
        return fileAddedToShareHistory;
    }

    public synchronized void setFileAddedToShareHistory(File file) {
        Platform.runLater(() -> fileAddedToShareHistory.set(file));
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

}
