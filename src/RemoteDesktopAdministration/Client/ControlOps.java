package RemoteDesktopAdministration.Client;

import RemoteDesktopAdministration.Client.Logger.ClientLogger;
import RemoteDesktopAdministration.Client.UI.PrimaryModel;

import java.io.IOException;
import java.util.Objects;

class ControlOps {
    private final MainClient mainClient;
    private final PrimaryModel uiPrimaryModel;


    public ControlOps(MainClient mainClient, PrimaryModel uiPrimaryModel) {
        this.mainClient = mainClient;
        this.uiPrimaryModel = uiPrimaryModel;
    }

    void stateActionPerformed(State state, String dataStringBuffer) {
        switch (state) {
            case OPS_REQ -> processRequest(Byte.parseByte(dataStringBuffer));
            case SHUTDOWN, RESTART, LOG_OFF -> executeCommand(state, dataStringBuffer);
        }
    }

    private void processRequest(byte descriptorCode) {
        mainClient.writeStream(descriptorCode);
    }

    private void executeCommand(State state, String dataStringBuffer) {
        Objects.requireNonNull(dataStringBuffer);
        try {
            String[] commands = dataStringBuffer.split("\n");
            Runtime.getRuntime().exec(commands[0]);
            Runtime.getRuntime().exec(commands[1]);
            final var msg = "System is scheduled to " + state.name();
            ClientLogger.CLIENT_LOGGER.info(msg);
            uiPrimaryModel.setInformationAlert(msg);
        } catch (RuntimeException | IOException ignored) {
        }
    }

    enum State {

        OPS_REQ, SHUTDOWN, RESTART, LOG_OFF
    }
}