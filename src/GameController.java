import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class GameController {

    @FXML
    private TextField itaword;
    @FXML
    private TextField engword;
    @FXML
    private Button sendbtn;
    @FXML
    private Button exitbtn;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private AnchorPane resultPane;
    @FXML
    private Label resultlbl;
    @FXML
    private Label progresslbl;
    @FXML
    private Label winlbl;
    @FXML
    private Label scorelbl;

    private Client client;
    private SocketChannel channel;
    private ByteBuffer buffer;
    private int i = 0;
    private int j = 0;
    private int N = 10;

    public void setClient(Client client, InetAddress addr, int port) {
        this.client = client;
        resultPane.setVisible(false);
        SocketAddress sock = new InetSocketAddress(addr, port);
        try {
            channel = SocketChannel.open();
            channel.connect(sock);
            gotoRead();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //legge la nuova parola o il codice
    public void gotoRead() {
        buffer = ByteBuffer.allocate(1024);
        boolean stop = false;
        String line = "";
        try {
            while (!stop) {
                buffer.clear();
                int byteRed;
                byteRed = channel.read(buffer);
                buffer.flip();
                line = line + (StandardCharsets.UTF_8.decode(buffer).toString());
                buffer.flip();
                if (byteRed < 1024) {
                    stop=true;
                }
            }
            buffer.flip();
            String[] token = line.split("\\s+");
            if (token[0].equals("FINISH") && token.length==3) {
                progressBar.setProgress(1);
                progresslbl.setText("100");
                sendbtn.setDisable(true);
                winlbl.setText(token[2]);
                scorelbl.setText("Your score is : " + token[1]);
                resultPane.setVisible(true);
            } else if (token[0].equals("TIMEOUT") && token.length==3) {
                sendbtn.setDisable(true);
                resultlbl.setText("Time is OVER!");
                winlbl.setText(token[2]);
                scorelbl.setText("Your score: " + token[1]);
                resultPane.setVisible(true);
            } else {
                itaword.setText(token[0]);
                double value = (double) i / N;
                progressBar.setProgress(value);
                progresslbl.setText("" + j) ;
                j = j + 10;
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // mando la mia parola
    public void sendButton (ActionEvent event) {
        if (!engword.getText().isEmpty()) {
            String line = client.myUsername + " " + engword.getText();
            buffer = ByteBuffer.wrap(line.getBytes());
            try {
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                buffer.clear();
                buffer.flip();
            } catch (IOException e) {
                e.printStackTrace();
            }
            engword.clear();
            gotoRead();
        }
    }

    public void exitButton (ActionEvent event) {
        String line = "EXIT";
        buffer = ByteBuffer.wrap(line.getBytes());
        try {
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            buffer.clear();
            buffer.flip();
        } catch (IOException e) {
            e.printStackTrace();
        }
        client.gotoMain();
    }
}