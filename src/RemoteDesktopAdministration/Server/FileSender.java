package RemoteDesktopAdministration.Server;

import RemoteDesktopAdministration.Server.Logger.ServerLogger;
import RemoteDesktopAdministration.Server.UI.FileSendProgress.Model;
import RemoteDesktopAdministration.Server.UI.FileSendProgress.View;
import RemoteDesktopAdministration.Server.UI.PrimaryModel;
import RemoteDesktopAdministration.Server.Utils.FileUtils;
import RemoteDesktopAdministration.Server.Utils.ListInnerContents;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class FileSender {

    private final Model model;
    private final PrimaryModel primaryModel;
    private final AtomicBoolean innerContentScheduled = new AtomicBoolean(false);
    private final MainServer.Client client;
    private final MainServer mainServer;
    private final AtomicBoolean innerContentFileSendStarted = new AtomicBoolean(false);
    private final FileUtils.SHA256Digest sha256Digest = new FileUtils.SHA256Digest();
    private int innerContentsFileIndex = 0;
    private String mainFileName;
    private ListInnerContents.InnerContents innerContents;
    private RandomAccessFile raf;
    private double sendRateValue;
    private long startTime;
    private String relativePath;
    private long uploadSize;
    private long prevUploadSize;

    public FileSender(MainServer mainServer, Model model, PrimaryModel primaryModel, MainServer.Client client) {
        this.mainServer = mainServer;
        this.model = model;
        this.primaryModel = primaryModel;
        this.client = client;
    }

    private static byte[] generateRemoteSoftwareInstallPayload(String dataStringBuffer) {
        byte[] descriptor = new byte[]{MainServer.DescriptorCode.REMOTE_SOFT_INSTALLATION};
        byte[] dataByteBuffer = dataStringBuffer.getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[descriptor.length + dataByteBuffer.length];
        System.arraycopy(descriptor, 0, payload, 0, descriptor.length);
        System.arraycopy(dataByteBuffer, 0, payload, descriptor.length, dataByteBuffer.length);
        return payload;
    }

    void stateActionPerformed(State state, byte[] dataByteBuffer) {

        switch (state) {
            case NO_PROGRESS -> noProgress();
            case PROGRESS -> progress(Long.parseLong(new String(dataByteBuffer, StandardCharsets.UTF_8)));
            case ERROR -> error(new String(dataByteBuffer, StandardCharsets.UTF_8));
            case COMMIT -> commit();
            case ABORT -> abort();
        }

    }

    //<editor-fold defaultstate="collapsed" desc="noProgress()">
    private void noProgress() {

        client.setFTPScheduled(true);
        if (!innerContentScheduled.get()) {
            this.innerContents = client.getFTPChannelFirstInnerContentsAndRemove();
            innerContentFileSendStarted.set(false);
        }
        if (innerContents == null) {
            client.setFTPScheduled(false);
            innerContentScheduled.set(false);
            innerContentFileSendStarted.set(false);
            return;
        }
        innerContentScheduled.set(true);
        relativePath = innerContents.relative()[innerContentsFileIndex];
        if (!innerContentFileSendStarted.get()) {
            try {
                sha256Digest.resetSHA256Digest();
                raf = new RandomAccessFile(innerContents.absolute()[innerContentsFileIndex], "r");
                mainFileName = Paths.get(relativePath).getName(0).toString();
                if (prevUploadSize == 0L) {
                    String fileOrDir = innerContents.isRegularFileInnerContent() ? "File " : innerContents.isDirectoryInnerContent() ? "Folder " : "";
                    ServerLogger.SERVER_LOGGER.info(mainFileName + " " + fileOrDir + "Send to " + client.getClientDescriptiveName() + " started.");
                }
            } catch (IOException e) {
                String fileOrDir = innerContents.isRegularFileInnerContent() ? "File " : innerContents.isDirectoryInnerContent() ? "Folder " : "";
                ServerLogger.SERVER_LOGGER.log(Level.SEVERE, fileOrDir + "Content Send to " + client.getClientDescriptiveName() + " Failed to start for " + innerContents.absolute()[innerContentsFileIndex] + " [" + e.getMessage() + "]", e);
                mainServer.writeStream(MainServer.DescriptorCode.FTP_OFF_START, new byte[]{(byte) (isInnerContentProcessedFully() ? 1 : 0)}, client.getDataOutputStream());
                return;
            }
        }

        if (!innerContents.isDirectoryInnerContent() && !innerContents.isRegularFileInnerContent()) {
            return;
        }
        byte dirFile = 0;
        if (innerContents.isRegularFileInnerContent())
            dirFile = 1;
        else if (innerContents.isDirectoryInnerContent())
            dirFile = 2;
        String separator = new String(new byte[]{4}, StandardCharsets.UTF_8);
        String payload = dirFile + separator + innerContents.totalSize() + separator + relativePath;
        innerContentsFileIndex++;
        mainServer.writeStream(MainServer.DescriptorCode.FTP_ON_NO_PROGRESS, payload, client.getDataOutputStream());

    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="progress(filePointer)">
    private void progress(long fp) {
        try {
            long remainingLength = raf.length() - fp;
            final var bufferMaxSize = 1048576;
            int buffer_length = (remainingLength < bufferMaxSize) ? (int) remainingLength : bufferMaxSize;
            byte[] buffer = new byte[buffer_length];
            if (uploadSize == 0L) {
                this.startTime = System.nanoTime();
            }
            raf.seek(fp);
            raf.read(buffer, 0, buffer.length);
            sha256Digest.updateSHA256Digest(buffer, 0, buffer.length);
            if (uploadSize == 0L) {
                model.startProgressViewer();
            }
            uploadSize = raf.getFilePointer();
            model.setFileSendProgress(getSendProgress(relativePath, innerContents.totalSize(), uploadSize, prevUploadSize, startTime));

            if (fp != raf.length()) {
                mainServer.writeStream(MainServer.DescriptorCode.FTP_EOR, buffer, client.getDataOutputStream());

            } else {
                prevUploadSize += uploadSize;
                final var sha256Digest = this.sha256Digest.getSHA256Digest();
                mainServer.writeStream(MainServer.DescriptorCode.FTP_EOF, sha256Digest, client.getDataOutputStream());
                releaseResourcesAndClose();
            }
        } catch (IOException e) {
            String fileOrDir = innerContents.isRegularFileInnerContent() ? "File " : innerContents.isDirectoryInnerContent() ? "Folder " : "";
            ServerLogger.SERVER_LOGGER.log(Level.SEVERE, fileOrDir + "Content Send to " + client.getClientDescriptiveName() + " Failed to progress for " + innerContents.absolute()[innerContentsFileIndex] + " [" + e.getMessage() + "]", e);
            mainServer.writeStream(MainServer.DescriptorCode.FTP_OFF_START, new byte[]{(byte) (isInnerContentProcessedFully() ? 1 : 0)}, client.getDataOutputStream());
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="error(fileChecksum)">
    private void error(String fileChecksum) {
        if (fileChecksum.equals(sha256Digest.getSHA256Digest())) {
            uploadSize = 0L;
            innerContentsFileIndex = innerContentsFileIndex - 1;
            innerContentFileSendStarted.set(false);
            releaseResourcesAndClose();
            noProgress();
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="commit()">
    private void commit() {

        if (isInnerContentProcessedFully()) {
            String fileOrDir = innerContents.isRegularFileInnerContent() ? "File " : innerContents.isDirectoryInnerContent() ? "Folder " : "";
            ServerLogger.SERVER_LOGGER.info(mainFileName + " " + fileOrDir + "Send to " + client.getClientDescriptiveName() + " succeeded. ");
            primaryModel.setFileAddedToShareHistory(innerContents.start().toFile());
            byte[] dataByteBuffer = new byte[0];
            try {
                if (innerContents.isRemoteSoftwareInstallationMSIFile()) {
                    dataByteBuffer = generateRemoteSoftwareInstallPayload("cmd.exe /c msiexec.exe /i \"FILE\" /quiet /norestart");
                } else if (innerContents.isRemoteSoftwareInstallationEXEFile()) {
                    dataByteBuffer = generateRemoteSoftwareInstallPayload("cmd.exe /c \"FILE\" -s install");
                }
            } catch (FileNotFoundException ignored) {
            }
            mainServer.writeStream(MainServer.DescriptorCode.FTP_COMMIT, dataByteBuffer, client.getDataOutputStream());
            mainServer.writeStream(MainServer.DescriptorCode.FTP_OFF_START, new byte[]{(byte) 1}, client.getDataOutputStream());
            return;
        }
        mainServer.writeStream(MainServer.DescriptorCode.FTP_OFF_START, new byte[]{(byte) 0}, client.getDataOutputStream());
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="abort()">
    private void abort() {
        innerContentScheduled.set(false);
        innerContentsFileIndex = 0;
        uploadSize = 0L;
        String fileOrDir = innerContents.isRegularFileInnerContent() ? "File " : innerContents.isDirectoryInnerContent() ? "Folder " : "";
        ServerLogger.SERVER_LOGGER.severe(mainFileName + " " + fileOrDir + "Send to " + client.getClientDescriptiveName() + " aborted due to Client's File System Error.");
        mainServer.writeStream(MainServer.DescriptorCode.FTP_ABORT, "", client.getDataOutputStream());
        releaseResourcesAndClose();
        mainServer.writeStream(MainServer.DescriptorCode.FTP_OFF_START, new byte[]{(byte) (isInnerContentProcessedFully() ? 1 : 0)}, client.getDataOutputStream());
    }
    //</editor-fold>

    private boolean isInnerContentProcessedFully() {
        final var b = (innerContents.getTotalItems()) == innerContentsFileIndex;
        innerContentFileSendStarted.set(false);
        if (b) {
            innerContentScheduled.set(false);
            uploadSize = 0L;
            prevUploadSize = 0L;
            model.stopProgressViewer(View.StopMode.NORMAL_STOP);
            innerContentsFileIndex = 0;
        } else {
            innerContentScheduled.set(true);
        }
        return b;
    }

    private void releaseResourcesAndClose() {
        try {
            raf.close();
        } catch (IOException ignored) {

        }
    }

    //<editor-fold defaultstate="collapsed" desc="SendProgressCalculations">
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

    private SendProgress getSendProgress(String currentFile, long total, long currentDown, long prevDown, long startTime) {

        final String totalSize = FileUtils.getReadableFileSize(total);
        final String uploadedFileSize = FileUtils.getReadableFileSize(currentDown + prevDown);
        final String remainingSize = FileUtils.getReadableFileSize(total - currentDown - prevDown);
        final String receiptRate = getReceiptRateString(currentDown + prevDown, startTime);
        final String timeLeft = getTimeToCommitReceipting(total, currentDown + prevDown);
        final Double per = getFileReceiptedPercentage(currentDown + prevDown, total);
        return new SendProgress(timeLeft, remainingSize, currentFile, totalSize, uploadedFileSize, receiptRate, per);
    }

    private String getReceiptRateString(long afterFilePointer, long beforeTime) {
        long afterTime = System.nanoTime();
        long differenceTime = afterTime - beforeTime;
        double ratePerSec = afterFilePointer * 1000000000.0 / differenceTime;
        this.sendRateValue = ratePerSec;
        if (Double.isInfinite(ratePerSec))
            return "(unknown)";
        return FileUtils.getReadableFileSize(ratePerSec) + "/sec";
    }

    private String getTimeToCommitReceipting(double total, double uploaded) {
        double leftTime = (total - uploaded) / sendRateValue;
        if (Double.isInfinite(leftTime))
            return ("(unknown)");
        return getReadableTime(leftTime);
    }

    private Double getFileReceiptedPercentage(long uploaded, long total) {
        double percent = ((double) uploaded / total);
        if (Double.isNaN(percent)) {
            return percent;
        }
        return (new BigDecimal(percent).setScale(2, RoundingMode.HALF_UP)).doubleValue();
    }
    //</editor-fold>

    enum State {

        NO_PROGRESS, PROGRESS, ERROR, COMMIT, ABORT
    }

    //<editor-fold defaultstate="collapsed" desc="SendProgress Record Class"
    public record SendProgress(String timeLeft, String remainingSize, String currentFile, String totalSize,
                               String uploadedFileSize, String speed, Double per) {
    }
    //</editor-fold>

}
