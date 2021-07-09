package RemoteDesktopAdministration.Server.Utils;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FxUtils {

    public static void addAllStageIcons(Stage stage, Class<?> _class, String... iconNames) {
        List<Image> iconImages = Arrays.stream(iconNames).map(iconName -> new Image(Objects.requireNonNull(_class.getResource("/icons/" + iconName)).toExternalForm())).collect(Collectors.toList());
        stage.getIcons().addAll(iconImages);
    }

    public static Bounds getPrefBounds(Node node) {
        double prefWidth;
        double prefHeight;

        Orientation bias = node.getContentBias();
        if (bias == Orientation.HORIZONTAL) {
            prefWidth = node.prefWidth(-1);
            prefHeight = node.prefHeight(prefWidth);
        } else if (bias == Orientation.VERTICAL) {
            prefHeight = node.prefHeight(-1);
            prefWidth = node.prefWidth(prefHeight);
        } else {
            prefWidth = node.prefWidth(-1);
            prefHeight = node.prefHeight(-1);
        }
        return new BoundingBox(0, 0, prefWidth, prefHeight);
    }
}
