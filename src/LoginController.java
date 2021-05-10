import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

public class LoginController {
    @FXML
    private Button loginbtn;
    @FXML
    private PasswordField pswf;
    @FXML
    private TextField usrnf;
    @FXML
    private Label errorlbl;
    @FXML
    private Label reglbl;

    private Client client;

    public void setClient(Client client) {
        this.client = client;
    }

    @FXML
    private void loginButton(ActionEvent event) {
        if (usrnf.getText().isEmpty() || pswf.getText().isEmpty()) {
            errorlbl.setText("Username o password non inserita!");
        } else {
            Code cd_err;
            cd_err = client.loginPrcs(usrnf.getText(), pswf.getText());
            errorlbl.setText(Client.getMessage(cd_err));
            if (cd_err==Code.SUCCESS) client.gotoMain();
        }
    }

    @FXML
    private void regLabel(MouseEvent event) {
        client.gotoSignIn();
    }
}

