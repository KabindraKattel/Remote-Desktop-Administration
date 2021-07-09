package RemoteDesktopAdministration.Server.Utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import javax.swing.filechooser.FileSystemView;
import java.awt.image.BufferedImage;
import java.io.File;

public class ImageUtils {

    public static Image getFxImageFromSwingIcon(File file) {
        final var swingIcon = FileSystemView.getFileSystemView().getSystemIcon(file);
        final var swingBufferedImage = new BufferedImage(swingIcon.getIconWidth(), swingIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        swingIcon.paintIcon(null, swingBufferedImage.getGraphics(), 0, 0);
        return SwingFXUtils.toFXImage(swingBufferedImage, null);
    }

//    public static Image standardByteArrayToFxImage(byte[] buffer) throws IOException {
//        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
//        BufferedImage bImage = ImageIO.read(bais);
//        bais.close();
//        return SwingFXUtils.toFXImage(bImage, null);
//    }

    public static Image fxByteArrayToFxImage(int width, int height, byte[] bytes) {
        final var writableImage = new WritableImage(width, height);
        writableImage.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getByteBgraInstance(), bytes, 0, width * 4);
        return writableImage;
    }

}
