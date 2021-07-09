package RemoteDesktopAdministration.Client;

import RemoteDesktopAdministration.Client.Logger.ClientLogger;
import RemoteDesktopAdministration.Client.UI.FileReceiveProgress.Model;
import RemoteDesktopAdministration.Client.UI.FileReceiveProgress.View;
import RemoteDesktopAdministration.Client.UI.PrimaryModel;
import RemoteDesktopAdministration.Client.Utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;

import static RemoteDesktopAdministration.Client.MainClient.DescriptorCode.FTP_ON_NO_PROGRESS;
import static RemoteDesktopAdministration.Client.MainClient.DescriptorCode.FTP_ON_PROGRESS;

public class FileReceiver {

    private final MainClient mainClient;
    private final PrimaryModel primaryModel;
    private final FileUtils.SHA256Digest sha256Digest = new FileUtils.SHA256Digest();
    private final Model model;
    private File absolute;
    private double receiptRateValue;
    private long totalSize;
    private String relativePath;
    private long prevDownSize = 0L;
    private long downSize = 0L;
    private boolean isFolderContent = false;
    private RandomAccessFile raf;
    private String mainFileName;
    private File downloadLocation;
    private long startTime;


    public FileReceiver(MainClient mainClient, Model model, PrimaryModel primaryModel) {
        this.mainClient = mainClient;
        this.model = model;
        this.primaryModel = primaryModel;
    }

    private static String getRemoteSoftInstallPayloadDataBuffer(byte[] eventPayload) {
        byte[] payloadData = new byte[eventPayload.length - 1];
        System.arraycopy(eventPayload, 1, payloadData, 0, eventPayload.length - 1);
        return new String(payloadData, StandardCharsets.UTF_8);
    }

    void stateActionPerformed(State state, byte[] dataByteBuffer) {
        Objects.requireNonNull(state);
        switch (state) {
            case START -> start(dataByteBuffer);
            case NO_PROGRESS -> noProgress(new String(dataByteBuffer, StandardCharsets.UTF_8));
            case EOR -> EOR(dataByteBuffer);
            case EOF -> EOF(new String(dataByteBuffer, StandardCharsets.UTF_8));
            case COMMIT -> commit(dataByteBuffer);
            case ABORT -> abort();
        }
    }

    //<editor-fold defaultstate="collapsed" desc="start(dataByteBuffer)">
    private void start(byte[] dataByteBuffer) {
        byte b = dataByteBuffer[0];
        switch (b) {
            case 0 -> mainClient.writeStream(FTP_ON_NO_PROGRESS);
            case 1 -> {
                prevDownSize = 0L;
                downSize = 0L;
                mainClient.writeStream(FTP_ON_NO_PROGRESS);
            }
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="noProgress(fileParams)">
    private void noProgress(String fileParams) {

        Objects.requireNonNull(fileParams);
        long newFp = 0;

        String separator = new String(new byte[]{4}, StandardCharsets.UTF_8);
        String[] data = fileParams.split(separator);
        byte dirOrFile = Byte.parseByte(data[0]);
        if (dirOrFile == 0)
            return;
        if ((dirOrFile == 1)) {
            this.isFolderContent = false;
        }
        if (dirOrFile == 2) {
            this.isFolderContent = true;
        }
        this.totalSize = Long.parseLong(data[1]);
        this.relativePath = data[2];

        File parentPathFile = mainClient.getDownloadLocation();
        downloadLocation = parentPathFile.toPath().resolve("Remote Desktop Administration").toFile();
        final var absoluteFile = this.downloadLocation.toPath().resolve(relativePath).toFile();
        if (absoluteFile.exists()) {
            this.absolute = FileUtils.getUniqueFile(absoluteFile);
        } else {
            try {
                FileUtils.createPath(absoluteFile, true);
                this.absolute = absoluteFile;
            } catch (IOException e) {
                ClientLogger.CLIENT_LOGGER.log(Level.SEVERE, relativePath + " File Receive failed [" + e.getMessage() + "]", e);
                mainClient.writeStream(MainClient.DescriptorCode.FTP_ABORT);
                return;
            }
        }
        try {
            sha256Digest.resetSHA256Digest();
            this.raf = new RandomAccessFile(absolute, "rw");
            this.mainFileName = Paths.get(relativePath).getName(0).toString();
            if (prevDownSize == 0L)
                ClientLogger.CLIENT_LOGGER.info(mainFileName + (isFolderContent ? " FOLDER" : " FILE") + " Receive Started");
        } catch (IOException e) {
            ClientLogger.CLIENT_LOGGER.log(Level.SEVERE, "File Receive failed [" + e.getMessage() + "]", e);
            mainClient.writeStream(MainClient.DescriptorCode.FTP_ABORT);
            return;
        }
        mainClient.writeStream(FTP_ON_PROGRESS, String.valueOf(newFp));

    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="EOR(dataByteBuffer)">
    private void EOR(byte[] dataByteBuffer) {
        try {
            long fp = raf.getFilePointer();
            if (downSize == 0L) {
                this.startTime = System.nanoTime();
            }
            raf.seek(fp);
            raf.write(dataByteBuffer);
            sha256Digest.updateSHA256Digest(dataByteBuffer, 0, dataByteBuffer.length);
            if (downSize == 0L) {
                model.startProgressViewer();
            }
            downSize = raf.getFilePointer();
            model.setFileReceiveProgress(getReceiveProgress(relativePath, totalSize, downSize, prevDownSize, startTime));
            mainClient.writeStream(FTP_ON_PROGRESS, String.valueOf(downSize));
        } catch (IOException e) {
            ClientLogger.CLIENT_LOGGER.log(Level.SEVERE, "File Receive failed [" + e.getMessage() + "]", e);
            mainClient.writeStream(MainClient.DescriptorCode.FTP_ABORT);
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="EOF(fileChecksum)">
    private void EOF(String fileChecksum) {
        final var sha256Digest = this.sha256Digest.getSHA256Digest();
        if (!sha256Digest.equals(fileChecksum)) {
            downSize = 0L;
            model.setFileReceiveProgress(getReceiveProgress(relativePath, totalSize, downSize, prevDownSize, startTime));
            releaseResourcesAndClose();
            absolute.delete();
            ClientLogger.CLIENT_LOGGER.log(Level.SEVERE, "File " + absolute + " is corrupted while sending. Removing and Requesting for re-send.");
            mainClient.writeStream(MainClient.DescriptorCode.FTP_ERROR, fileChecksum);
            return;
        }
        releaseResourcesAndClose();
        prevDownSize += downSize;
        mainClient.writeStream(MainClient.DescriptorCode.FTP_COMMIT);

    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="commit(dataByteBuffer)">
    private void commit(byte[] dataByteBuffer) {
        if (!Arrays.equals(dataByteBuffer, new byte[0])) {
            if (dataByteBuffer[0] == MainClient.DescriptorCode.REMOTE_SOFT_INSTALLATION) {
                String command = getRemoteSoftInstallPayloadDataBuffer(dataByteBuffer);
                command = command.replace("FILE", downloadLocation.toPath().resolve(mainFileName).toString());
                try {
                    Runtime.getRuntime().exec(command);
                } catch (IOException e) {
                    ClientLogger.CLIENT_LOGGER.log(Level.SEVERE, "Remote Software Installation Failed [" + e.getMessage() + "]");
                }
            }
        }
        downSize = 0L;
        prevDownSize = 0L;
        model.stopProgressViewer(View.StopMode.NORMAL_STOP);
        ClientLogger.CLIENT_LOGGER.info(mainFileName + (isFolderContent ? " FOLDER" : " FILE") + " Receive Complete");
        ClientLogger.CLIENT_LOGGER.info(mainFileName + (isFolderContent ? " FOLDER" : " FILE") + " Saved to: \"" + downloadLocation + "\"");
        primaryModel.setFileAddedToShareHistory(downloadLocation.toPath().resolve(mainFileName).toFile());
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="abort()">
    private void abort() {
        model.stopProgressViewer(View.StopMode.NORMAL_STOP);
        downSize = 0L;
        releaseResourcesAndClose();
        absolute.delete();
        ClientLogger.CLIENT_LOGGER.log(Level.SEVERE, mainFileName + (isFolderContent ? " FOLDER" : " FILE") + " Received Failed due to Server's File System Error.");
    }
    //</editor-fold>

    private void releaseResourcesAndClose() {
        try {
            raf.close();
        } catch (IOException ignored) {
        }
    }

    //<editor-fold defaultstate="collapsed" desc="ReceiveProgressCalculations">
    String getReadableTime(double leftTime) {

        int hr = 0, min = 0, sec = (int) leftTime;
        if (leftTime >= 60) {
            min = (int) (leftTime / 60);
            sec = (int) (leftTime % 60);
        }
        if (min >= 60) {
            hr = min / 60;
            min = min % 60;
        }
        if (hr > 0)
            return (hr + " hr " + min + " min " + sec + " sec");
        else if (min > 0)
            return (min + " min " + sec + " sec");
        else
            return (sec + " sec");
    }

    private ReceiveProgress getReceiveProgress(String currentFile, long total, long currentDown, long prevDown, long startTime) {

        final String totalSize = FileUtils.getReadableFileSize(total);
        final String downloadedFileSize = FileUtils.getReadableFileSize(currentDown + prevDown);
        final String remainingSize = FileUtils.getReadableFileSize(total - currentDown - prevDown);
        final String receiptRate = getReceiptRateString(currentDown + prevDown, startTime);
        final String timeLeft = getTimeToCommitReceipting(total, currentDown + prevDown);
        final Double per = getFileReceiptedPercentage(currentDown + prevDown, total);
        return new ReceiveProgress(timeLeft, remainingSize, currentFile, totalSize, downloadedFileSize, receiptRate, per);
    }

    private String getReceiptRateString(long afterFilePointer, long beforeTime) {
        long afterTime = System.nanoTime();
        long differenceTime = afterTime - beforeTime;
        double ratePerSec = afterFilePointer * 1000000000.0 / differenceTime;
        this.receiptRateValue = ratePerSec;
        if (Double.isInfinite(ratePerSec))
            return "(unknown)";
        return FileUtils.getReadableFileSize(ratePerSec) + "/sec";
    }

    private String getTimeToCommitReceipting(double total, double downloaded) {
        double leftTime = (total - downloaded) / receiptRateValue;
        if (Double.isInfinite(leftTime))
            return ("(unknown)");
        return getReadableTime(leftTime);
    }

    private Double getFileReceiptedPercentage(long downloaded, long total) {
        double percent = ((double) downloaded / total);
        if (Double.isNaN(percent)) {
            return percent;
        }
        return (new BigDecimal(percent).setScale(2, RoundingMode.HALF_UP)).doubleValue();
    }
    //</editor-fold>

    enum State {
        START, NO_PROGRESS, EOR, EOF, COMMIT, ABORT
    }

    //<editor-fold defaultstate="collapsed" desc="ReceiveProgress Data Class">
    public record ReceiveProgress(String timeLeft, String remainingSize, String currentFile,
                                  String totalSize, String downloadedFileSize,
                                  String receiptRate, double per) {
    }
    //</editor-fold>

}