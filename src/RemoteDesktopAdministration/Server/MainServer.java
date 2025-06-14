package RemoteDesktopAdministration.Server;

import RemoteDesktopAdministration.Server.Logger.ServerLogger;
import RemoteDesktopAdministration.Server.UI.PrimaryModel;
import RemoteDesktopAdministration.Server.UI.PrimaryView;
import RemoteDesktopAdministration.Server.Utils.ListInnerContents;
import javafx.application.Application;
import org.hashids.Hashids;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static RemoteDesktopAdministration.Server.MainServer.DescriptorCode.FTP_OFF_START;

public class MainServer {

    public static final int DEFAULT_PORT = 35706;
    private final PrimaryModel uiMainModel;
    private final Hashtable<Socket, Client> socketClientHashtable = new Hashtable<>();
    private final ArrayList<Client> clientOnlineList = new ArrayList<>();
    private ServerSocket serverSocket = null;
    private long clientId = 0L;

    /**
     * @param uiMainModel Model class which is a common class to Front-end and Back-end. Back-end sets Property values of Model class. Front-end listens to such property and acts upon changes.
     */
    public MainServer(PrimaryModel uiMainModel) {
        this.uiMainModel = uiMainModel;
        configServerDetails();
    }

    public static void main(String[] args) {
        Application.launch(PrimaryView.class, args);
    }

    public void configServerDetails() {
        try {
            final var hostName = InetAddress.getLocalHost().getHostName();
            uiMainModel.setHostName(hostName);
            final var inetAddresses = InetAddress.getAllByName(hostName);
            var ipv4 = Stream.of(inetAddresses).filter(inetAddress -> inetAddress instanceof Inet4Address).map(inetAddress -> (Inet4Address) inetAddress).collect(Collectors.toList());
            var ipv6 = Stream.of(inetAddresses).filter(inetAddress -> inetAddress instanceof Inet6Address).map(inetAddress -> (Inet6Address) inetAddress).collect(Collectors.toList());
            uiMainModel.setIPv4Addresses(ipv4);
            uiMainModel.setIPv6Addresses(ipv6);
        } catch (UnknownHostException ignored) {
        }
    }

    public boolean startServer(Integer port) {
        try {
            serverSocket = new ServerSocket(port);
            ServerLogger.SERVER_LOGGER.info("Server Started at PortNo: " + port);
            return true;
        } catch (IOException e) {
            ServerLogger.SERVER_LOGGER.log(Level.SEVERE, "Server failed to start due to : " + e.getMessage(), e);
            return false;
        }
    }

    public void listenClients() {
        if (serverSocket == null)
            return;

        final ExecutorService clientPool = Executors.newCachedThreadPool();
        ServerLogger.SERVER_LOGGER.info("Listening new Clients... ");

        while (true) {
            if (serverSocket.isClosed()) {
                clientPool.shutdownNow();
                return;
            }
            try {
                final var acceptedClient = serverSocket.accept();
                var result = clientPool.submit(new ClientHandler(acceptedClient, MainServer.this, uiMainModel));
                if (result.isCancelled() || result.isDone())
                    result.get();
            } catch (IOException | ClientConfigurationFailedException e) {
                ServerLogger.SERVER_LOGGER.log(Level.SEVERE, "Listening new Clients failed", e);
            } catch (InterruptedException | ExecutionException e) {
                ServerLogger.SERVER_LOGGER.log(Level.SEVERE, "Exception occurred : " + e, e);
            }
        }
    }

    public boolean stopServer() {
        if (serverSocket == null) {
            ServerLogger.SERVER_LOGGER.log(Level.WARNING, "Cannot stop server ::: Server was not started.");
            return false;
        }
        try {
            serverSocket.close();
            clientOnlineList.parallelStream().forEach(client -> writeStream(DescriptorCode.CONNECTION_LOST, "", client.dataOutputStream));
            ServerLogger.SERVER_LOGGER.severe("Server is offline.");
            return true;
        } catch (IOException ex) {
            ServerLogger.SERVER_LOGGER.log(Level.SEVERE, "Server failed to stop due to : " + ex.getMessage(), ex);
            return false;
        }
    }


    ReadStream readStream(InputStream inputStream) {
        byte[] buffer = new byte[0];
        byte descriptor_byte = MainServer.DescriptorCode.CONNECTION_LOST;
        try {
            byte[] start = inputStream.readNBytes(1);
            if (start.length > 0 && start[0] == (byte) 2) {
                byte[] descriptor = inputStream.readNBytes(1);
                byte[] byteCount = new byte[0];
                StringBuilder length = new StringBuilder();
                do {
                    if (byteCount.length != 0)
                        length.append(new String(byteCount, StandardCharsets.UTF_8));
                    byteCount = inputStream.readNBytes(1);
                } while (byteCount[0] != (byte) 4);
                int data_length = Integer.parseInt(length.toString());
                buffer = inputStream.readNBytes(data_length);
                descriptor_byte = descriptor[0];
            }
        } catch (IOException ignored) {
        }
        ServerLogger.SERVER_LOGGER.finest("Message Type Read :" + descriptor_byte);
        return (new ReadStream(descriptor_byte, buffer));
    }

    synchronized void writeStream(byte descriptor, byte[] data, OutputStream outputStream) {

        try {
            byte[] buffer = createDataPacket(descriptor, data);
            outputStream.write(buffer);
            outputStream.flush();
            ServerLogger.SERVER_LOGGER.finest("Message Type Written :" + descriptor);
        } catch (IOException ignored) {
        }
    }

    public void writeStream(byte descriptor, String data, OutputStream outputStream) {
        writeStream(descriptor, data.getBytes(StandardCharsets.UTF_8), outputStream);
    }

    public void writeStream(byte descriptor, String data, List<String> selectedClients) {
        selectedClients.forEach(selectedClient -> writeStream(descriptor, data.getBytes(StandardCharsets.UTF_8), selectedClient));
    }

    public void writeStream(byte descriptor, String data, String selectedClient) {
        writeStream(descriptor, data.getBytes(StandardCharsets.UTF_8), selectedClient);
    }

    public void writeStream(byte descriptor, byte[] data, String selectedClient) {
        clientOnlineList.stream().filter(client -> client.getClientDescriptiveName().equals(selectedClient)).forEach(client -> writeStream(descriptor, data, client.getDataOutputStream()));
    }

    /**
     * @param descriptorByte headerByte [One among DescriptorCode class]
     * @param dataBuffer     Actual Data in byteArray
     * @return Data Packet Byte Array [Start Byte (1B) + Descriptor Byte (1B) + ByteArray of String Representation of Data Length (>=1B)+ Data (>=0B) ]
     */
    private byte[] createDataPacket(byte descriptorByte, byte[] dataBuffer) {
        Objects.requireNonNull(dataBuffer);
        byte[] packet;
        var STX = new byte[]{2};
        var descriptor = new byte[]{descriptorByte};
        var byteCount = String.valueOf(dataBuffer.length).getBytes(StandardCharsets.UTF_8);
        byte[] EOT = new byte[]{4};
        packet = new byte[STX.length + descriptor.length + byteCount.length + EOT.length + dataBuffer.length];
        System.arraycopy(STX, 0, packet, 0, STX.length);
        System.arraycopy(descriptor, 0, packet, STX.length, descriptor.length);
        System.arraycopy(byteCount, 0, packet, STX.length + descriptor.length, byteCount.length);
        System.arraycopy(EOT, 0, packet, STX.length + descriptor.length + byteCount.length, EOT.length);
        System.arraycopy(dataBuffer, 0, packet, STX.length + descriptor.length + byteCount.length + EOT.length, dataBuffer.length);
        return Objects.requireNonNull(packet);
    }

    public void setClientConfig(Socket socket, DataOutputStream dataOutputStream) {

        socketClientHashtable.computeIfAbsent(socket, clientSocket -> {
            Client newClient = new Client(dataOutputStream);
            clientOnlineList.add(newClient);
            return newClient;
        });

    }

    void generateIdAndUpdateClientConfig(Socket socket, byte[] dataBuffer) {

        socketClientHashtable.computeIfAbsent(socket, clientSocket -> {
            try {
                throw new ClientConfigurationFailedException("Generating unique client id failed as Client connection was not setup.");
            } catch (ClientConfigurationFailedException e) {
                ServerLogger.SERVER_LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
            return null;
        });

        socketClientHashtable.computeIfPresent(socket, (clientSocket, client) -> {

            String regex = "\n";
            String[] subBuffers = new String(dataBuffer, StandardCharsets.UTF_8).split(regex);
            var name = subBuffers[0];
            var platform = subBuffers[1];
            final String encodedId = new Hashids("", 8).encode(++clientId);
            client.clientName = name;
            client.clientId = encodedId;
            client.clientPlatformName = platform;
            final String descriptiveName = client.getClientDescriptiveName();
            final DataOutputStream dataOutputStream = client.getDataOutputStream();
            uiMainModel.setClientOnline(descriptiveName);
            ServerLogger.SERVER_LOGGER.info(descriptiveName + " is Online under platform " + client.getClientPlatformName());
            writeStream(DescriptorCode.CLIENT_ONLINE, ("#" + encodedId).getBytes(StandardCharsets.UTF_8), dataOutputStream);
            return client;

        });

    }

    void removeClient(Socket socket) {

        socketClientHashtable.computeIfAbsent(socket, clientSocket -> {
            try {
                throw new ClientConfigurationFailedException("Client disconnect failed as Client connection was not setup.");
            } catch (ClientConfigurationFailedException e) {
                ServerLogger.SERVER_LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
            return null;
        });
        socketClientHashtable.computeIfPresent(socket, (clientSocket, client) -> {
            final String descriptiveClientName = client.getClientDescriptiveName();
            try {
                ServerLogger.SERVER_LOGGER.severe(descriptiveClientName + " is Offline.");
                uiMainModel.setClientOffline(getClient(socket).getClientDescriptiveName());
                socket.close();
                clientOnlineList.remove(client);
                return null;
            } catch (IOException ex) {
                ServerLogger.SERVER_LOGGER.log(Level.SEVERE, "Cannot disconnect " + client.getClientDescriptiveName() + " due to: " + ex.getMessage(), ex);
                return client;
            }
        });

    }

    public void addFTPChannelInnerContents(List<String> selectedClients, Collection<ListInnerContents.InnerContents> innerContentsCollection) {
        selectedClients.parallelStream().forEach(clientName -> clientOnlineList.stream().filter(client -> client.getClientDescriptiveName().equals(clientName)).forEach(client -> {
            client.addFTPChannelInnerContents(innerContentsCollection);
            if (!client.isFTPScheduled())
                writeStream(FTP_OFF_START, new byte[]{1}, client.getDataOutputStream());
            if (!innerContentsCollection.isEmpty())
                ServerLogger.SERVER_LOGGER.info("Selected files are added to send-queue.");
        }));
    }

    public Client getClient(Socket socket) {
        return socketClientHashtable.get(socket);
    }

    public static class Client {

        private final DataOutputStream dataOutputStream;
        private final AtomicBoolean ftpScheduled = new AtomicBoolean(false);
        private final List<ListInnerContents.InnerContents> innerContentsList = new ArrayList<>();
        private String clientName = "Unknown";
        private String clientId = "Unknown";
        private String clientPlatformName = "Unknown";

        Client(DataOutputStream dataOutputStream) {
            this.dataOutputStream = dataOutputStream;
        }

        public String getClientPlatformName() {
            return clientPlatformName;
        }

        public Platform getPlatform() {
            final var windows = clientPlatformName.contains("Windows");
            final var linux = clientPlatformName.contains("Linux");
            final var macOs = clientPlatformName.contains("Mac OS");
            return windows ? Platform.WINDOWS : linux ? Platform.LINUX : macOs ? Platform.MAC_OS : Platform.OTHERS;
        }

        public DataOutputStream getDataOutputStream() {
            return dataOutputStream;
        }

        public String getClientDescriptiveName() {
            return clientName + " (#" + clientId + ")";
        }

        public void addFTPChannelInnerContents(Collection<ListInnerContents.InnerContents> innerContentsCollection) {
            innerContentsList.addAll(innerContentsCollection);
        }

        public boolean isFTPScheduled() {
            return ftpScheduled.get();
        }

        public void setFTPScheduled(boolean ftpScheduled) {
            this.ftpScheduled.set(ftpScheduled);
        }

        public ListInnerContents.InnerContents getFTPChannelFirstInnerContentsAndRemove() {
            if (innerContentsList.isEmpty()) {
                return null;
            }
            return innerContentsList.remove(0);
        }

        enum Platform {
            WINDOWS, LINUX, MAC_OS, OTHERS
        }

    }

    record ReadStream(byte descriptorByte, byte[] dataBuffer) {
    }

    public static final class DescriptorCode {

        public static final byte CONNECTION_LOST = (byte) 0;
        public static final byte CHAT_INITIATOR_SERVER = (byte) 122;
        public static final byte FTP_OFF_START = (byte) 220;
        public static final byte REMOTE_SOFT_INSTALLATION = (byte) 65;
        public static final byte CLIENT_CONTROL_OPERATIONS_REQ = (byte) 129;//-127
        public static final byte CLIENT_SHUTDOWN = (byte) 130;
        public static final byte CLIENT_RESTART = (byte) 131;
        public static final byte CLIENT_LOGOFF = (byte) 132;
        public static final byte SCREEN_SHARING_ON_NO_PROGRESS = (byte) 168;
        public static final byte SCREEN_SHARING_ON_PROGRESS = (byte) 169;
        public static final byte SCREEN_SHARING_ABORT = (byte) 170;
        static final byte CLIENT_ONLINE = (byte) 1;
        static final byte CHAT_INITIATOR_CLIENT = (byte) 120;
        static final byte CHAT_INITIATOR_CLIENT_OK = (byte) 121;
        static final byte CHAT_INITIATOR_SERVER_OK = (byte) 123;
        static final byte FTP_ON_NO_PROGRESS = (byte) 225;
        static final byte FTP_ON_PROGRESS = (byte) 125;
        static final byte FTP_EOR = (byte) 128;
        static final byte FTP_EOF = (byte) 64;
        static final byte FTP_ERROR = (byte) 32;
        static final byte FTP_COMMIT = (byte) 226;
        static final byte FTP_ABORT = (byte) 227;
    }

    static class ClientConfigurationFailedException extends Exception {

        public ClientConfigurationFailedException(String message) {
            super(message);
        }
    }

    static class UnsupportedOSException extends Exception {

        public UnsupportedOSException(String message) {
            super("Unsupported OS: " + message);
        }
    }
}
