package RemoteDesktopAdministration.Client.Utils;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;

public class ImageUtils {

    public static byte[] fxImageToFxByteArray(Image i) {

        final int width = (int) i.getWidth();
        final int height = (int) i.getHeight();

        byte[] buffer = new byte[(width * height * 4)];
        i.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getByteBgraInstance(), buffer, 0, width * 4);
        return buffer;
    }

//    public static byte[] fxImageToStandardByteArray(Image image) throws IOException {
//        final var bImage = SwingFXUtils.fromFXImage(image, new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_RGB));
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        ImageIO.write(bImage, "JPG", baos);
//        byte[] res = baos.toByteArray();
//        baos.close();
//        return res;
//    }

}
