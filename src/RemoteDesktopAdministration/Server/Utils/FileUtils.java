package RemoteDesktopAdministration.Server.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class FileUtils {

    public static String getReadableFileSize(double size) {
        String[] units = {"bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
        double runningSize = size;
        String sizeUnit = units[0];
        for (int i = 1; runningSize >= 1000.00 && i < units.length; i++) {

            runningSize /= 1024;
            sizeUnit = units[i];

        }
        return new BigDecimal(runningSize).setScale(2, RoundingMode.HALF_UP) + " " + sizeUnit;

    }


    public static File getProjectDirectory() {
        return Paths.get(".").normalize().toAbsolutePath().toFile();
    }

    public static void createFilePath(File file, boolean containsFile) throws IOException {
        if (containsFile) {
            file.getParentFile().mkdirs(); //it is done to ensure if not exists creates folder
            file.createNewFile();
        } else {
            file.mkdirs();
        }
    }

    public static RegularFile getFileNameAndExtension(File file) throws FileNotFoundException {
        if (file.exists()) {
            final var fullName = file.getName();
            final var name = (file.isFile() && !file.isDirectory()) ? (fullName.contains(".")) ? fullName.substring(0, fullName.lastIndexOf(".")) : fullName : fullName;
            final var extension = (file.isFile() && !file.isDirectory()) ? (fullName.contains(".")) ? fullName.substring(fullName.lastIndexOf(".")) : "" : "";
            return new RegularFile(name, extension);
        } else {
            throw new FileNotFoundException(file + " does not exists");
        }
    }

    private static Path insertAtLastToFileName(File file, String key) throws FileNotFoundException {
        final var fileNameAndExtension = getFileNameAndExtension(file);
        final var fileNameWithoutExtension = fileNameAndExtension.name();
        final var fileExtension = fileNameAndExtension.extension();
        final var parentPath = file.toPath().getParent();
        return parentPath.resolve(fileNameWithoutExtension + "_" + key + fileExtension);
    }

    public static File getUniqueFile(File file) {
        long i = 0L;
        File tempFile = file;
        while (true) {
            try {
                tempFile = insertAtLastToFileName(file, String.valueOf(i++)).toFile();
                if (!tempFile.exists()) {
                    throw new FileNotFoundException(file + " does not exists");
                }
            } catch (FileNotFoundException e) {
                break;
            }

        }
        return tempFile;
    }

    public static class SHA256Digest {
        private final MessageDigest sha256Digest;

        public SHA256Digest() {
            MessageDigest sha256Digest = null;
            try {
                sha256Digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException ignored) {
            }
            this.sha256Digest = sha256Digest;
        }

        public void resetSHA256Digest() {
            if (sha256Digest == null)
                return;
            sha256Digest.reset();
        }

        public void updateSHA256Digest(byte[] input, int offset, int len) {
            if (sha256Digest == null)
                return;
            sha256Digest.update(input, offset, len);
        }

        public String getSHA256Digest() {
            if (sha256Digest == null)
                return "";
            final var stringBuilder = new StringBuilder();
            for (byte b : sha256Digest.digest()) {
                stringBuilder.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            //has 32 bytes - 64 HEX
            return stringBuilder.toString();
        }
    }


    public record RegularFile(String name, String extension) {

    }

}