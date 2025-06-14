package RemoteDesktopAdministration.Client;

import RemoteDesktopAdministration.Client.Logger.ClientLogger;
import RemoteDesktopAdministration.Client.UI.PrimaryModel;

import java.time.ZonedDateTime;


public class Chat {
    private final MainClient mainClient;
    private final PrimaryModel uiPrimaryModel;

    public Chat(MainClient mainClient, PrimaryModel uiPrimaryModel) {
        this.mainClient = mainClient;
        this.uiPrimaryModel = uiPrimaryModel;
    }

    void stateActionPerformed(State state, String message) {

        if (state == State.SEND) {
            sendToServer(message);
        } else if (state == State.RECEIVE) {
            receiveFromServer(message);
        }

    }

    private void sendToServer(String message) {
        ClientLogger.CLIENT_LOGGER.info("Message Sent");
        uiPrimaryModel.setChatRecord(new UIChatRecord(State.SEND, ZonedDateTime.now(), message));
    }

    private void receiveFromServer(String message) {
        mainClient.writeStream(MainClient.DescriptorCode.CHAT_INITIATOR_SERVER_OK, message);
        uiPrimaryModel.setChatRecord(new UIChatRecord(State.RECEIVE, ZonedDateTime.now(), message));
        ClientLogger.CLIENT_LOGGER.info("Message Received");

    }

    public enum State {
        SEND, RECEIVE
    }

    public static class UIChatRecord {
        private final ZonedDateTime recordDate;
        private final String recordMessage;
        private final State state;

        public UIChatRecord(Chat.State state, ZonedDateTime zdt, String message) {

            this.state = state;
            this.recordDate = zdt;
            this.recordMessage = message;
        }

        public boolean isSent() {
            return (state == State.SEND);
        }

        public ZonedDateTime getRecordDate() {
            return recordDate;
        }

        public String getRecordMessage() {
            return recordMessage;
        }
    }
}