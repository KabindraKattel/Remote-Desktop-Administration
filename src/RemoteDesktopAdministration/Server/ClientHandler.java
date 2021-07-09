package RemoteDesktopAdministration.Server;

import RemoteDesktopAdministration.Server.UI.PrimaryModel;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final MainServer mainServer;
    private final Chat chat;
    private final FileSender fileSender;
    private final ScreenSharing screenSharing;
    private final ControlOps controlOps;
    private final DataInputStream dataInputStream;

    ClientHandler(Socket socket, MainServer mainServer, PrimaryModel primaryModel) throws IOException, MainServer.ClientConfigurationFailedException {
        this.socket = socket;
        dataInputStream = new DataInputStream(socket.getInputStream());
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        this.mainServer = mainServer;
        mainServer.setClientConfig(socket, dataOutputStream);
        MainServer.Client client = mainServer.getClient(socket);
        if (client == null)
            throw new MainServer.ClientConfigurationFailedException("Setting client configuration failed.");
        this.chat = new Chat(this.mainServer, primaryModel, client);
        var fileSendProgressModel = new RemoteDesktopAdministration.Server.UI.FileSendProgress.Model(primaryModel, client);
        this.fileSender = new FileSender(this.mainServer, fileSendProgressModel, primaryModel, client);
        var screenViewerModel = new RemoteDesktopAdministration.Server.UI.ClientScreenViewer.Model(this.mainServer, primaryModel, client);
        this.screenSharing = new ScreenSharing(this.mainServer, screenViewerModel, primaryModel, client);
        this.controlOps = new ControlOps(this.mainServer, client);
    }

    @Override
    public void run() {
        listenClient();
    }

    private void listenClient() {
        while (!socket.isClosed()) {
            descriptorActionPerformed(mainServer.readStream(dataInputStream));
        }

    }

    private void descriptorActionPerformed(MainServer.ReadStream stream) {

        Objects.requireNonNull(stream);
        var descriptor = stream.descriptorByte();
        var dataBuffer = stream.dataBuffer();

        switch (descriptor) {
            case MainServer.DescriptorCode.CLIENT_ONLINE -> mainServer.generateIdAndUpdateClientConfig(socket, dataBuffer);
            case MainServer.DescriptorCode.CONNECTION_LOST -> connectionLost();

            case MainServer.DescriptorCode.CHAT_INITIATOR_SERVER_OK -> chatStateActionPerformed(Chat.State.SEND, dataBuffer);
            case MainServer.DescriptorCode.CHAT_INITIATOR_CLIENT -> chatStateActionPerformed(Chat.State.RECEIVE, dataBuffer);

            case MainServer.DescriptorCode.CLIENT_SHUTDOWN -> controlOpsStateActionPerformed(ControlOps.State.SHUTDOWN);
            case MainServer.DescriptorCode.CLIENT_RESTART -> controlOpsStateActionPerformed(ControlOps.State.RESTART);
            case MainServer.DescriptorCode.CLIENT_LOGOFF -> controlOpsStateActionPerformed(ControlOps.State.LOG_OFF);

            case MainServer.DescriptorCode.FTP_ON_NO_PROGRESS -> fileTransferStateActionPerformed(FileSender.State.NO_PROGRESS, dataBuffer);
            case MainServer.DescriptorCode.FTP_ON_PROGRESS -> fileTransferStateActionPerformed(FileSender.State.PROGRESS, dataBuffer);
            case MainServer.DescriptorCode.FTP_COMMIT -> fileTransferStateActionPerformed(FileSender.State.COMMIT, dataBuffer);
            case MainServer.DescriptorCode.FTP_ABORT -> fileTransferStateActionPerformed(FileSender.State.ABORT, dataBuffer);
            case MainServer.DescriptorCode.FTP_ERROR -> fileTransferStateActionPerformed(FileSender.State.ERROR, dataBuffer);

            case MainServer.DescriptorCode.SCREEN_SHARING_ON_NO_PROGRESS -> screenSharingStateActionPerformed(ScreenSharing.State.NO_PROGRESS, dataBuffer);
            case MainServer.DescriptorCode.SCREEN_SHARING_ON_PROGRESS -> screenSharingStateActionPerformed(ScreenSharing.State.PROGRESS, dataBuffer);
        }

    }

    private void connectionLost() {
        if (!socket.isClosed())
            mainServer.removeClient(socket);
    }

    private void chatStateActionPerformed(Chat.State state, byte[] dataByteBuffer) {
        var dataStringBuffer = new String(dataByteBuffer, StandardCharsets.UTF_8);
        chat.stateActionPerformed(state, dataStringBuffer);

    }

    private void controlOpsStateActionPerformed(ControlOps.State state) {
        controlOps.stateActionPerformed(state);
    }

    private void screenSharingStateActionPerformed(ScreenSharing.State state, byte[] dataBuffer) {
        screenSharing.stateActionPerformed(state, dataBuffer);
    }

    private void fileTransferStateActionPerformed(FileSender.State state, byte[] dataByteBuffer) {
        fileSender.stateActionPerformed(state, dataByteBuffer);
    }
}
