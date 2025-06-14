package RemoteDesktopAdministration.Client;

import RemoteDesktopAdministration.Client.Logger.ClientLogger;
import RemoteDesktopAdministration.Client.UI.FileReceiveProgress.Model;
import RemoteDesktopAdministration.Client.UI.PrimaryModel;
import RemoteDesktopAdministration.Client.UI.PrimaryView;
import javafx.application.Application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.logging.Level;

import static RemoteDesktopAdministration.Client.FileReceiver.State.*;

public class MainClient {

    public static final int DEFAULT_SERVER_PORT = 35706;
    public static final File DEFAULT_DOWNLOAD_LOCATION = new File(System.getProperty("user.home")).toPath().resolve("Downloads").toFile();
    private final Chat chat;
    private final ControlOps controlOps;
    private final FileReceiver fileReceive;
    private final ScreenSharing screenSharing;
    private final PrimaryModel primaryModel;
    private File downloadLocation;
    private Socket socket;
    private DataInputStream dataInputStream = null;
    private DataOutputStream dataOutputStream = null;

    /**
     * @param primaryModel Model class which is a common class to Front-end and Back-end. Back-end sets Property values of Model class. Front-end listens to such property and acts upon changes.
     */
    public MainClient(PrimaryModel primaryModel) {

        this.primaryModel = primaryModel;
        this.chat = new Chat(this, primaryModel);
        this.controlOps = new ControlOps(this, primaryModel);
        var fileReceiveProgressModel = new Model(primaryModel);
        this.fileReceive = new FileReceiver(this, fileReceiveProgressModel, primaryModel);
        this.screenSharing = new ScreenSharing(this);
        configClientDetails();

    }

    public static void main(String[] args) {
        Application.launch(PrimaryView.class, args);
    }

    public void configClientDetails() {
        try {
            final var hostName = InetAddress.getLocalHost().getHostName();
            primaryModel.setClientName(hostName);
        } catch (UnknownHostException ignored) {
        }
    }

    public boolean connect(String hostIP, Integer portNo) {

        try {
            ClientLogger.CLIENT_LOGGER.info("Connecting to server at IP: " + hostIP + " PortNo: " + portNo + " ...");
            socket = new Socket(hostIP, portNo);
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            ClientLogger.CLIENT_LOGGER.info("Connected to server at IP: " + hostIP + " PortNo: " + portNo + ".");
            return true;

        } catch (UnknownHostException e) {
            primaryModel.setClientSwitchState(false);
            ClientLogger.CLIENT_LOGGER.log(Level.SEVERE, "Failed to connect to Server at PortNo " + portNo + " as host: " + hostIP + " could not be identified", e);
            return false;
        } catch (IOException e) {
            primaryModel.setClientSwitchState(false);
            ClientLogger.CLIENT_LOGGER.log(Level.SEVERE, "Failed to connect to Server at PortNo " + portNo + " due to " + e.getMessage(), e);
            return false;
        }
    }

    public void listenServer() {

        /*
         * do-while loop ends only when client socket endpoint at server side is closed...
         * after then if any Client Operations are active, they are aborted ...
         */
        writeStream(DescriptorCode.CLIENT_ONLINE, getClientName() + "\n" + System.getProperty("os.name"));
        while (!socket.isClosed()) {
            descriptorActionPerformed(readStream());
        }

    }

    public boolean disconnect() {


        if (dataInputStream == null || dataOutputStream == null || socket == null) {
            ClientLogger.CLIENT_LOGGER.log(Level.WARNING, "Cannot disconnect server connection ::: Connection to server was not set up.");
            return false;
        }
        var hostIP = socket.getInetAddress().getHostAddress();
        var portNo = socket.getPort();
        try {
            dataInputStream.close();
            dataOutputStream.close();
            socket.close();
            ClientLogger.CLIENT_LOGGER.severe("Connection to Server at IP: " + hostIP + " PortNo: " + portNo + " Aborted.");
            return true;
        } catch (IOException e) {
            ClientLogger.CLIENT_LOGGER.log(Level.SEVERE, "Failed to disconnect server connection at IP: " + hostIP + " PortNo: " + portNo + " .", e);
            return false;
        }
    }

    private ReadStream readStream() {
        byte[] buffer = new byte[0];
        byte descriptor_byte = DescriptorCode.CONNECTION_LOST;
        try {
            byte[] start = dataInputStream.readNBytes(1);
            if (start.length > 0 && start[0] == (byte) 2/*start*/) {
                byte[] descriptor = dataInputStream.readNBytes(1);
                byte[] byteCount = new byte[0];
                StringBuilder length = new StringBuilder();
                do {
                    if (byteCount.length > 0)
                        length.append(new String(byteCount, StandardCharsets.UTF_8));
                    byteCount = dataInputStream.readNBytes(1);
                } while (byteCount[0] != (byte) 4/*separator*/);
                int data_length = Integer.parseInt(length.toString());
                buffer = dataInputStream.readNBytes(data_length);
                descriptor_byte = descriptor[0];
            }
        } catch (IOException ignored) {
        }
        ClientLogger.CLIENT_LOGGER.finest("Message Type Read :" + descriptor_byte);
        return new ReadStream(descriptor_byte, buffer);
    }

    synchronized void writeStream(byte descriptor, byte[] data) {

        try {
            byte[] buffer = createDataPacket(descriptor, data);
            dataOutputStream.write(buffer);
            dataOutputStream.flush();
            ClientLogger.CLIENT_LOGGER.finest("Message Type Written :" + descriptor);

        } catch (IOException ignored) {
        }

    }

    public synchronized void writeStream(byte descriptor, String data) {
        Objects.requireNonNull(data);
        writeStream(descriptor, data.getBytes(StandardCharsets.UTF_8));
    }

    synchronized void writeStream(byte descriptor) {
        writeStream(descriptor, "");
    }

    /**
     * @param descriptorByte headerByte [One among DescriptorCode class]
     * @param dataBuffer     Actual Data in byteArray
     * @return Data Packet Byte Array [Start Byte (1B) + Descriptor Byte (1B) + ByteArray of String Representation of Data Length (>=1B)+ Data (>=0B) ]
     */
    private byte[] createDataPacket(byte descriptorByte, byte[] dataBuffer) {
        Objects.requireNonNull(dataBuffer);
        byte[] STX = new byte[]{2};//Start of TeXt ASCII Code
        byte[] descriptor = new byte[]{descriptorByte};
        byte[] byteCount = String.valueOf(dataBuffer.length).getBytes(StandardCharsets.UTF_8);
        byte[] EOT = new byte[]{4};//Here End of Transmission ASCII Code
        byte[] packet = new byte[STX.length + descriptor.length + byteCount.length + EOT.length + dataBuffer.length];
        System.arraycopy(STX, 0, packet, 0, STX.length);
        System.arraycopy(descriptor, 0, packet, STX.length, descriptor.length);
        System.arraycopy(byteCount, 0, packet, STX.length + descriptor.length, byteCount.length);
        System.arraycopy(EOT, 0, packet, STX.length + descriptor.length + byteCount.length, EOT.length);
        System.arraycopy(dataBuffer, 0, packet, STX.length + descriptor.length + byteCount.length + EOT.length, dataBuffer.length);
        return Objects.requireNonNull(packet);
    }

    /**
     * @param stream stream read from socket InputStream - contains DescriptorByte and DataBuffer
     */
    void descriptorActionPerformed(ReadStream stream) {
        Objects.requireNonNull(stream);
        var descriptor = stream.descriptorByte;
        var dataBuffer = stream.dataBuffer;

        switch (descriptor) {
            case DescriptorCode.CLIENT_ONLINE:
                primaryModel.setClientId(new String(dataBuffer, StandardCharsets.UTF_8));
                break;

            case DescriptorCode.CONNECTION_LOST:  /* If server disconnects client */
                connectionLost();
                break;

            case DescriptorCode.CHAT_INITIATOR_CLIENT_OK:
                chatStateActionPerformed(Chat.State.SEND, dataBuffer);
                break;

            case DescriptorCode.CHAT_INITIATOR_SERVER:
                chatStateActionPerformed(Chat.State.RECEIVE, dataBuffer);
                break;

            case DescriptorCode.CLIENT_CONTROL_OPERATIONS_REQ:
                controlOpsStateActionPerformed(ControlOps.State.OPS_REQ, dataBuffer);
                break;

            case DescriptorCode.CLIENT_SHUTDOWN:
                controlOpsStateActionPerformed(ControlOps.State.SHUTDOWN, dataBuffer);
                break;

            case DescriptorCode.CLIENT_RESTART:
                controlOpsStateActionPerformed(ControlOps.State.RESTART, dataBuffer);
                break;

            case DescriptorCode.CLIENT_LOGOFF:
                controlOpsStateActionPerformed(ControlOps.State.LOG_OFF, dataBuffer);
                break;

            case DescriptorCode.FTP_OFF_START:
                fileReceiveStateActionPerformed(START, dataBuffer);
                break;

            case DescriptorCode.FTP_ON_NO_PROGRESS:
                fileReceiveStateActionPerformed(NO_PROGRESS, dataBuffer);
                break;

            case DescriptorCode.FTP_EOR:
                fileReceiveStateActionPerformed(EOR, dataBuffer);
                break;

            case DescriptorCode.FTP_EOF:
                fileReceiveStateActionPerformed(EOF, dataBuffer);
                break;

            case DescriptorCode.FTP_COMMIT:
                fileReceiveStateActionPerformed(COMMIT, dataBuffer);
                break;

            case DescriptorCode.FTP_ABORT:
                fileReceiveStateActionPerformed(ABORT, dataBuffer);
                break;

            case DescriptorCode.SCREEN_SHARING_ON_NO_PROGRESS:
                screenSharingStateActionPerformed(ScreenSharing.State.NO_PROGRESS, dataBuffer);
                break;

            case DescriptorCode.SCREEN_SHARING_ON_PROGRESS:
                screenSharingStateActionPerformed(ScreenSharing.State.PROGRESS, dataBuffer);
                break;

            case DescriptorCode.SCREEN_SHARING_ABORT:
                screenSharingStateActionPerformed(ScreenSharing.State.ABORT, dataBuffer);
                break;

            default:
                break;
        }
    }

    private void connectionLost() {

        if (!socket.isClosed()) {//if client has closed client socket but server side client socket is still on.
            primaryModel.connectionLost();
        }

    }

    private void chatStateActionPerformed(Chat.State state, byte[] dataByteBuffer) {
        var dataStringBuffer = new String(dataByteBuffer, StandardCharsets.UTF_8);
        chat.stateActionPerformed(state, dataStringBuffer);
    }

    private void controlOpsStateActionPerformed(ControlOps.State state, byte[] dataByteBuffer) {
        var dataStringBuffer = new String(dataByteBuffer, StandardCharsets.UTF_8);
        controlOps.stateActionPerformed(state, dataStringBuffer);

    }

    private void screenSharingStateActionPerformed(ScreenSharing.State state, byte[] dataByteBuffer) {
        var dataStringBuffer = new String(dataByteBuffer, StandardCharsets.UTF_8);
        screenSharing.stateActionPerformed(state, dataStringBuffer);
    }

    private void fileReceiveStateActionPerformed(FileReceiver.State state, byte[] dataByteBuffer) {
        fileReceive.stateActionPerformed(state, dataByteBuffer);
    }

    String getClientName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return ("");
        }
    }

    public File getDownloadLocation() {
        return downloadLocation;
    }

    public void setDownloadLocation(File downloadLocation) {
        this.downloadLocation = downloadLocation;
    }


    public static final class DescriptorCode {
        public static final byte CHAT_INITIATOR_CLIENT = (byte) 120;
        public static final byte REMOTE_SOFT_INSTALLATION = (byte) 65;
        static final byte CHAT_INITIATOR_CLIENT_OK = (byte) 121;
        static final byte CHAT_INITIATOR_SERVER = (byte) 122;
        static final byte CHAT_INITIATOR_SERVER_OK = (byte) 123;
        static final byte FTP_OFF_START = (byte) 220;//-36
        static final byte FTP_ON_NO_PROGRESS = (byte) 225;//-31
        static final byte FTP_ON_PROGRESS = (byte) 125;
        static final byte FTP_EOR = (byte) 128;//End Of Record Marker//-128
        static final byte FTP_EOF = (byte) 64;
        static final byte FTP_ERROR = (byte) 32;
        static final byte FTP_COMMIT = (byte) 226;//-30
        static final byte FTP_ABORT = (byte) 227;//-29
        static final byte CLIENT_CONTROL_OPERATIONS_REQ = (byte) 129;//-127
        static final byte CLIENT_SHUTDOWN = (byte) 130;//-126
        static final byte CLIENT_RESTART = (byte) 131;//-125
        static final byte CLIENT_LOGOFF = (byte) 132;//-124
        static final byte SCREEN_SHARING_ON_NO_PROGRESS = (byte) 168;
        static final byte SCREEN_SHARING_ON_PROGRESS = (byte) 169;
        static final byte SCREEN_SHARING_ABORT = (byte) 170;
        private static final byte CONNECTION_LOST = (byte) 0;
        private static final byte CLIENT_ONLINE = (byte) 1;
    }

    private record ReadStream(byte descriptorByte, byte[] dataBuffer) {
    }
}

