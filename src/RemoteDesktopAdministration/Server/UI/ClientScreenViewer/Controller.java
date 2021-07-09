package RemoteDesktopAdministration.Server.UI.ClientScreenViewer;

import RemoteDesktopAdministration.Server.MainServer;
import RemoteDesktopAdministration.Server.UI.PrimaryModel;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    private final PrimaryModel primaryModel;
    private final Model model;
    private final MainServer mainServer;
    private final MainServer.Client client;
    @FXML
    private ImageView screenImageView;

    public Controller(PrimaryModel primaryModel, Model model, MainServer mainServer, MainServer.Client client) {
        this.primaryModel = primaryModel;
        this.mainServer = mainServer;
        this.model = model;
        this.client = client;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        model.sharedScreenProperty().addListener((obs, oldImage, newImage) -> screenImageView.setImage(newImage));
        screenImageView.addEventHandler(MouseEvent.ANY, this::screenViewOnMouseEvent);
        screenImageView.addEventHandler(KeyEvent.ANY, this::screenViewOnKeyEvent);
        screenImageView.addEventHandler(ScrollEvent.ANY, this::screenViewOnScrollEvent);
        primaryModel.serverSwitchStateProperty().addListener((obs, wasConnected, isConnected) -> {
            if (!isConnected)
                model.stopScreenViewer(View.StopMode.SERVER_OFFLINE_STOP);
        });
        primaryModel.clientOfflineProperty().addListener((obs, oldClient, newClient) -> {
            if (newClient.equals(client.getClientDescriptiveName()))
                model.stopScreenViewer(View.StopMode.CLIENT_OFFLINE_STOP);
        });
    }

    private void screenViewOnScrollEvent(ScrollEvent scrollEvent) {
        if (ScrollEvent.SCROLL.equals(scrollEvent.getEventType())) {
            mainServer.writeStream(MainServer.DescriptorCode.SCREEN_SHARING_ON_PROGRESS, scrollEvent.getEventType().getName() + "," + scrollEvent.getTouchCount(), client.getDataOutputStream());
        }
    }

    private void screenViewOnKeyEvent(KeyEvent keyEvent) {
        if (KeyEvent.KEY_PRESSED.equals(keyEvent.getEventType())) {
            mainServer.writeStream(MainServer.DescriptorCode.SCREEN_SHARING_ON_PROGRESS, keyEvent.getEventType().getName() + "," + keyEvent.getCode().name(), client.getDataOutputStream());
        }
        if (KeyEvent.KEY_RELEASED.equals(keyEvent.getEventType())) {
            mainServer.writeStream(MainServer.DescriptorCode.SCREEN_SHARING_ON_PROGRESS, keyEvent.getEventType().getName() + "," + keyEvent.getCode().name(), client.getDataOutputStream());
        }
    }

    private void screenViewOnMouseEvent(MouseEvent mouseEvent) {

        if (MouseEvent.MOUSE_PRESSED.equals(mouseEvent.getEventType())) {
            mainServer.writeStream(MainServer.DescriptorCode.SCREEN_SHARING_ON_PROGRESS, mouseEvent.getEventType().getName() + "," + mouseEvent.getButton().name(), client.getDataOutputStream());
        }
        if (MouseEvent.MOUSE_RELEASED.equals(mouseEvent.getEventType())) {
            mainServer.writeStream(MainServer.DescriptorCode.SCREEN_SHARING_ON_PROGRESS, mouseEvent.getEventType().getName() + "," + mouseEvent.getButton().name(), client.getDataOutputStream());
        }
        if (MouseEvent.MOUSE_MOVED.equals(mouseEvent.getEventType())) {
            mainServer.writeStream(MainServer.DescriptorCode.SCREEN_SHARING_ON_PROGRESS, mouseEvent.getEventType().getName() + "," + mouseEvent.getX() + "," + mouseEvent.getY(), client.getDataOutputStream());
        }

    }

}
