package RemoteDesktopAdministration.Client;

import RemoteDesktopAdministration.Client.Logger.ClientLogger;
import RemoteDesktopAdministration.Client.Utils.ImageUtils;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.robot.Robot;
import javafx.stage.Screen;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

class ScreenSharing {
    private static Rectangle2D SCREEN_SIZE;
    private final MainClient mainClient;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private Robot robot;
    private byte[] viewBytes;

    public ScreenSharing(MainClient mainClient) {
        this.mainClient = mainClient;
    }

    void stateActionPerformed(State state, String dataStringBuffer) {
        Objects.requireNonNull(state);
        switch (state) {
            case NO_PROGRESS -> Platform.runLater(this::noProgress);
            case PROGRESS -> Platform.runLater(() -> progress(dataStringBuffer));
            case ABORT -> abort();
        }
    }

    private void noProgress() {
        started.set(true);
        SCREEN_SIZE = Screen.getPrimary().getBounds();
        robot = new Robot();
        mainClient.writeStream(MainClient.DescriptorCode.SCREEN_SHARING_ON_NO_PROGRESS, SCREEN_SIZE.getWidth() + "," + SCREEN_SIZE.getHeight());
        ClientLogger.CLIENT_LOGGER.info("Server is ready to access this PC screen.");
    }

    private void progress(String dataStringBuffer) {

        if (!started.get())
            return;
        if (!dataStringBuffer.equals("")) {
            String[] eventParams = dataStringBuffer.split(",");
            String eventType = eventParams[0];
            if (MouseEvent.MOUSE_PRESSED.getName().equals(eventType)) {
                robot.mousePress(MouseButton.valueOf(eventParams[1]));
            }
            if (MouseEvent.MOUSE_RELEASED.getName().equals(eventType)) {
                robot.mouseRelease(MouseButton.valueOf(eventParams[1]));
            }
            if (MouseEvent.MOUSE_MOVED.getName().equals(eventType)) {
                robot.mouseMove(Double.parseDouble(eventParams[1]), Double.parseDouble(eventParams[2]));
            }
            if (ScrollEvent.SCROLL.getName().equals(eventType)) {
                robot.mouseWheel(Integer.parseInt(eventParams[1]));
            }
            if (KeyEvent.KEY_PRESSED.getName().equals(eventType)) {
                robot.keyPress(KeyCode.valueOf(eventParams[1]));
            }
            if (KeyEvent.KEY_RELEASED.getName().equals(eventType)) {
                robot.keyRelease(KeyCode.valueOf(eventParams[1]));
            }
        }
        WritableImage newImage = robot.getScreenCapture(null, SCREEN_SIZE);
        final var newBytes = ImageUtils.fxImageToFxByteArray(newImage);
        if (!Arrays.equals(viewBytes, newBytes)) {
            viewBytes = newBytes;
            mainClient.writeStream(MainClient.DescriptorCode.SCREEN_SHARING_ON_PROGRESS, newBytes);
        }

    }

    private void abort() {
        ClientLogger.CLIENT_LOGGER.log(Level.SEVERE, "Screen sharing session ended.");
        started.set(false);
    }

    enum State {
        NO_PROGRESS, PROGRESS, ABORT
    }

}