import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import java.rmi.RemoteException;

public class RegisterController {

    @FXML
    private Button regbtn;
    @FXML
    private PasswordField pswf;
    @FXML
    private TextField usrnf;
    @FXML
    private Label errorlbl;
    @FXML
    private Label backlbl;

    private Client client;

    public void setClient(Client client) {
        this.client = client;
    }

    @FXML
    private void regButton(ActionEvent event) {
        if (pswf.getText().isEmpty() || usrnf.getText().isEmpty()) {
            errorlbl.setText("Controlla i campi vuoti");
            errorlbl.setVisible(true);
        } else {
            try {
                Code cd_err;
                cd_err = Client.remReg.userRegistration(usrnf.getText(), pswf.getText());
                errorlbl.setText(Client.getMessage(cd_err));
                errorlbl.setVisible(true);
            } catch (RemoteException | NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void backLabel(MouseEvent event) {
        client.gotoLogin();
    }
}
