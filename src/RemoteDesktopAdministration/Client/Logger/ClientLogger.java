package RemoteDesktopAdministration.Client.Logger;

import RemoteDesktopAdministration.Client.UI.PrimaryModel;
import RemoteDesktopAdministration.Client.Utils.FileUtils;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;

public class ClientLogger {

    public static final Logger CLIENT_LOGGER = Logger.getLogger(ClientLogger.class.getName());

    public static void initLogger(PrimaryModel uiPrimaryModel) {
        try {
            LogManager.getLogManager().reset();
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO);
            CLIENT_LOGGER.addHandler(consoleHandler);
            UiHandler uiHandler = new UiHandler(uiPrimaryModel);
            uiHandler.setLevel(Level.INFO);
            CLIENT_LOGGER.addHandler(uiHandler);
            final StringBuilder fileNameBuilder = new StringBuilder();
            fileNameBuilder.append(LocalDate.now().format(DateTimeFormatter.ofPattern("u-LLL-dd-E-"))).append("clientLogger").append(".log");
            final var serverLoggerDirectory = FileUtils.getProjectDirectory().toPath().resolve(".log/RDA_Client").resolve(fileNameBuilder.toString()).toFile();
            final var uniqueFile = FileUtils.getUniqueFile(serverLoggerDirectory);
            FileUtils.createPath(uniqueFile, true);
            final var fileHandler = new FileHandler(uniqueFile.getAbsolutePath(), true);
            fileHandler.setLevel(Level.FINEST);
            CLIENT_LOGGER.addHandler(fileHandler);
        } catch (SecurityException e) {
            CLIENT_LOGGER.log(Level.WARNING, "Unable to create logger handler", e);
        } catch (IOException e) {
            CLIENT_LOGGER.log(Level.WARNING, "File logger not working", e);
        }

    }

    private static class UiHandler extends Handler {

        private final PrimaryModel uiPrimaryModel;

        public UiHandler(PrimaryModel uiPrimaryModel) {
            this.uiPrimaryModel = uiPrimaryModel;
        }

        @Override
        public void publish(LogRecord logRecord) {
            final ZonedDateTime zdt = ZonedDateTime.ofInstant(logRecord.getInstant(), ZoneId.systemDefault());
            uiPrimaryModel.setClientLogRecord(new UILogRecord(zdt, logRecord.getLevel(), logRecord.getMessage()));
        }

        @Override
        public void flush() {

        }

        @Override
        public void close() throws SecurityException {

        }
    }

    public static class UILogRecord {
        private final Text recordDate;
        private final Text recordMessage;

        public UILogRecord(ZonedDateTime zdt, Level level, String message) {
            Color color = (level == Level.SEVERE) ? Color.RED : (level == Level.WARNING) ? Color.ORANGE : (level == Level.INFO) ? Color.GREEN : Color.BLACK;
            this.recordDate = getText(zdt.format(DateTimeFormatter.ofPattern("E dd LLL u hh : mm : ss a ")), Color.BLUE);
            this.recordMessage = getText(message, color);
        }

        private Text getText(String string, Paint paint) {
            Text text = new Text(string);
            text.setFill(paint);
            text.setFontSmoothingType(FontSmoothingType.LCD);
            return text;
        }

        public Text getRecordDate() {
            return recordDate;
        }

        public Text getRecordMessage() {
            return recordMessage;
        }
    }
}

