package RemoteDesktopAdministration.Server.Logger;

import RemoteDesktopAdministration.Server.UI.PrimaryModel;
import RemoteDesktopAdministration.Server.Utils.FileUtils;
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

public class ServerLogger {

    public static final Logger SERVER_LOGGER = Logger.getLogger(ServerLogger.class.getName());

    public static void initLogger(PrimaryModel uiMainModel) {
        try {
            LogManager.getLogManager().reset();
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO);
            SERVER_LOGGER.addHandler(consoleHandler);
            UiHandler uiHandler = new UiHandler(uiMainModel);
            uiHandler.setLevel(Level.INFO);
            SERVER_LOGGER.addHandler(uiHandler);
            final StringBuilder fileNameBuilder = new StringBuilder();
            fileNameBuilder.append(LocalDate.now().format(DateTimeFormatter.ofPattern("u-LLL-dd-E-"))).append("serverLogger").append(".log");
            final var serverLoggerDirectory = FileUtils.getProjectDirectory().toPath().resolve(".log/RDA_Server").resolve(fileNameBuilder.toString()).toFile();
            final var uniqueFile = FileUtils.getUniqueFile(serverLoggerDirectory);
            FileUtils.createFilePath(uniqueFile, true);
            final var fileHandler = new FileHandler(uniqueFile.getAbsolutePath(), true);
            fileHandler.setLevel(Level.FINEST);
            SERVER_LOGGER.addHandler(fileHandler);
        } catch (SecurityException e) {
            SERVER_LOGGER.log(Level.WARNING, "Unable to create logger handler", e);
        } catch (IOException e) {
            SERVER_LOGGER.log(Level.WARNING, "File logger not working", e);
        }

    }

    private static class UiHandler extends Handler {

        private final PrimaryModel uiMainModel;

        public UiHandler(PrimaryModel uiMainModel) {
            this.uiMainModel = uiMainModel;
        }

        @Override
        public void publish(LogRecord logRecord) {
            final ZonedDateTime zdt = ZonedDateTime.ofInstant(logRecord.getInstant(), ZoneId.systemDefault());
            uiMainModel.setServerLogRecord(new UILogRecord(zdt, logRecord.getLevel(), logRecord.getMessage()));
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

