package RemoteDesktopAdministration.Server;

import RemoteDesktopAdministration.Server.Logger.ServerLogger;

import java.io.DataOutputStream;
import java.util.logging.Level;

public class ControlOps {

    private static final String WINDOWS_SHUTDOWN_CMD = "shutdown -s -t 0 -f";
    private static final String WINDOWS_RESTART_CMD = "shutdown -r -t 0 -f";
    private static final String WINDOWS_LOG_OFF_CMD = "shutdown -l";
    private static final String WINDOWS_ABORT_CMD = "shutdown -a";
    private static final String LINUX_MAC_SHUTDOWN_CMD = "shutdown -h +0 ";
    private static final String LINUX_MAC_RESTART_CMD = "shutdown -r +0 ";
    private static final String LINUX_MAC_LOG_OFF_CMD = "gnome-session-quit --logout --no-prompt";
    private static final String LINUX_MAC_ABORT_CMD = "shutdown -c";
    private final MainServer serverMain;
    private final MainServer.Client client;
    private String shutdownCmd = "";
    private String restartCmd = "";
    private String logOffCmd = "";
    private String abortCmd = "";
    private boolean commandSetConfigured = false;

    public ControlOps(MainServer serverMain, MainServer.Client client) {
        this.serverMain = serverMain;
        this.client = client;
    }

    private void configureCommandSet() {
        switch (this.client.getPlatform()) {
            case WINDOWS -> {
                shutdownCmd = WINDOWS_SHUTDOWN_CMD;
                restartCmd = WINDOWS_RESTART_CMD;
                logOffCmd = WINDOWS_LOG_OFF_CMD;
                abortCmd = WINDOWS_ABORT_CMD;
                commandSetConfigured = true;
            }
            case LINUX, MAC_OS -> {
                shutdownCmd = LINUX_MAC_SHUTDOWN_CMD;
                restartCmd = LINUX_MAC_RESTART_CMD;
                logOffCmd = LINUX_MAC_LOG_OFF_CMD;
                abortCmd = LINUX_MAC_ABORT_CMD;
                commandSetConfigured = true;
            }
        }
    }

    public void stateActionPerformed(State state) {
        if (!commandSetConfigured)
            configureCommandSet();
        switch (state) {
            case SHUTDOWN -> sendCommand(state, MainServer.DescriptorCode.CLIENT_SHUTDOWN, shutdownCmd);
            case RESTART -> sendCommand(state, MainServer.DescriptorCode.CLIENT_RESTART, restartCmd);
            case LOG_OFF -> sendCommand(state, MainServer.DescriptorCode.CLIENT_LOGOFF, logOffCmd);
        }
    }

    private void sendCommand(State state, byte opCode, String cmd) {

        if (client.getPlatform() == MainServer.Client.Platform.OTHERS) {
            try {
                throw new MainServer.UnsupportedOSException(client.getClientPlatformName());
            } catch (MainServer.UnsupportedOSException e) {
                ServerLogger.SERVER_LOGGER.log(Level.SEVERE, "Preparing " + state.name() + " Request failed [" + e + "]", e);
            }
            return;
        }
        cmd = abortCmd + "\n" + cmd;
        DataOutputStream out = client.getDataOutputStream();
        String desc = client.getClientDescriptiveName();
        serverMain.writeStream(opCode, cmd, out);
        ServerLogger.SERVER_LOGGER.info(desc + " is scheduled to " + state.name());
    }

    public enum State {

        SHUTDOWN, RESTART, LOG_OFF
    }
}
