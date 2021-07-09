package RemoteDesktopAdministration.Server.Utils;

import RemoteDesktopAdministration.Server.Logger.ServerLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.logging.Level;

public class ListInnerContents extends SimpleFileVisitor<Path> {
    private static final ArrayList<File> absolute = new ArrayList<>();
    private static final ArrayList<String> relativeFromParent = new ArrayList<>();
    private static long totalSize = 0L;
    private static Path start;

    public synchronized static InnerContents getContents(Path start, boolean installToAll) {
        try {
            clearContents();
            ListInnerContents.start = start;
            Files.walkFileTree(ListInnerContents.start, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new ListInnerContents());

        } catch (IOException exception) {
            ServerLogger.SERVER_LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
        }
        return new InnerContents(absolute.toArray(File[]::new), relativeFromParent.toArray(String[]::new), totalSize, start, installToAll);
    }

    private static void clearContents() {
        absolute.clear();
        relativeFromParent.clear();
        totalSize = 0L;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
        if (attr.isRegularFile()) {
            absolute.add(file.normalize().toAbsolutePath().toFile());
            Path fromWorking = start.relativize(file);
            final var nameCount = start.getNameCount();
            Path fromParent = (nameCount == 0) ? fromWorking : start.getName(nameCount - 1).resolve(fromWorking);
            relativeFromParent.add(fromParent.toString());
            totalSize += file.toFile().length();
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {

        ServerLogger.SERVER_LOGGER.log(Level.WARNING, exc.getMessage(), exc);
        return FileVisitResult.CONTINUE;

    }

    public record InnerContents(File[] absolute, String[] relative, long totalSize,
                                Path start, boolean installToAll) {

        public boolean isRegularFileInnerContent() {
            return Files.isRegularFile(start);
        }

        public boolean isDirectoryInnerContent() {
            return Files.isDirectory(start);
        }

        public int getTotalItems() {
            return absolute.length;
        }

        public boolean isRemoteSoftwareInstallationMSIFile() throws FileNotFoundException {
            if (!installToAll)
                return false;
            if (start.toFile().isFile()) {
                final var fileNameAndExtension = FileUtils.getFileNameAndExtension(start.toFile());
                return fileNameAndExtension.extension().equals(".msi");
            }
            return false;
        }

        public boolean isRemoteSoftwareInstallationEXEFile() throws FileNotFoundException {
            if (!installToAll)
                return false;
            if (start.toFile().isFile()) {
                final var fileNameAndExtension = FileUtils.getFileNameAndExtension(start.toFile());
                return fileNameAndExtension.extension().equals(".exe");
            }
            return false;
        }
    }
}
