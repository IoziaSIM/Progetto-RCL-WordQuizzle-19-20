import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class StartController {
    @FXML
    private Button startbtn;

    private Client client;

    public void setClient(Client client) {
        this.client = client;
    }

    @FXML
    private void startButton(ActionEvent event) {
            client.gotoLogin();
    }
}
