package RemoteDesktopAdministration.Server;

import RemoteDesktopAdministration.Server.Logger.ServerLogger;
import RemoteDesktopAdministration.Server.UI.PrimaryModel;

import java.time.ZonedDateTime;

public class Chat {

    private final MainServer mainServer;
    private final PrimaryModel primaryModel;
    private final MainServer.Client client;

    public Chat(MainServer mainServer, PrimaryModel primaryModel, MainServer.Client client) {
        this.mainServer = mainServer;
        this.primaryModel = primaryModel;
        this.client = client;
    }

    void stateActionPerformed(State state, String message) {

        if (state == State.SEND) {
            sendToClient(message);
        } else if (state == State.RECEIVE) {
            receiveFromClient(message);
        }

    }

    private void sendToClient(String message) {
        ServerLogger.SERVER_LOGGER.info("Message delivered to " + client.getClientDescriptiveName());
        primaryModel.setChatRecord(new UIChatRecord(State.SEND, ZonedDateTime.now(), client, message));
    }

    private void receiveFromClient(String message) {
        mainServer.writeStream(MainServer.DescriptorCode.CHAT_INITIATOR_CLIENT_OK, message, client.getDataOutputStream());
        primaryModel.setChatRecord(new UIChatRecord(State.RECEIVE, ZonedDateTime.now(), client, message));
        ServerLogger.SERVER_LOGGER.info("Message Received from " + client.getClientDescriptiveName());
    }

    enum State {

        SEND, RECEIVE
    }

    public static class UIChatRecord {
        private final ZonedDateTime recordDate;
        private final String recordAgent;
        private final String recordMessage;
        private final State state;


        public UIChatRecord(State state, ZonedDateTime zdt, MainServer.Client client, String message) {

            this.state = state;
            this.recordAgent = client.getClientDescriptiveName();
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

        public String getRecordAgent() {
            return recordAgent;
        }
    }
}
