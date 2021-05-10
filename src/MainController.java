import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;

public class MainController {
    @FXML
    private Label frienderror;
    @FXML
    public Label challerror;
    @FXML
    private Label logouterror;
    @FXML
    private TextField username;
    @FXML
    private TextField friendname;
    @FXML
    private TextField challenge;
    @FXML
    public Button challbtn;
    @FXML
    private Button scorebtn;
    @FXML
    private Button friendbtn;
    @FXML
    private Button rankbtn;
    @FXML
    private Button addbtn;
    @FXML
    private Button acceptbtn;
    @FXML
    private Button declinebtn;
    @FXML
    private Button logoutbtn;
    @FXML
    private ListView<String> friendList;
    @FXML
    private Label notifylbl;
    @FXML
    private Label scorelbl;
    @FXML
    private AnchorPane listPane;
    @FXML
    private AnchorPane notifyPane;


    private Client client;
    private Notify notify;
    private int score;
    private LinkedList<Notifier> news = new LinkedList<>();

    public void setClient(Client client, Notify notify, String username) {
        this.client = client;
        this.notify = notify;
        this.username.setText(username);
        scorelbl.setVisible(false);
        listPane.setVisible(false);
        frienderror.setVisible(false);
        challerror.setVisible(false);
        logouterror.setVisible(false);
        acceptbtn.setDisable(true);
        declinebtn.setDisable(true);
        notifyPane.setVisible(false);
    }

    public void setNotifyVisible() {
        // ritorno l'ultima notifica ma non la elimino
        Notifier peek = news.peekLast();
        notifylbl.setText(peek.username);
        notifyPane.setVisible(true);
        acceptbtn.setDisable(false);
        declinebtn.setDisable(false);
    }

    public void setNotifyInvisible() {
        if(news.isEmpty()) {
            notifyPane.setVisible(false);
            acceptbtn.setDisable(true);
            declinebtn.setDisable(true);
        } else
            setNotifyVisible();
    }

    //aggiorno lista amici/ranking
    public void updateList (ArrayList<String> list) {
        ObservableList<String> items = FXCollections.observableArrayList(list);
        friendList.setItems(items);
    }

    public void scoreButton(ActionEvent event) {
        scorelbl.setText("Points: " + client.getScore());
        scorelbl.setVisible(true);
    }

    public void listButton (ActionEvent event){
        updateList(client.getList());
        listPane.setVisible(true);
    }

    public void rankButton (ActionEvent event){
        updateList(client.getRanking());
        listPane.setVisible(true);
    }

    public void addButton (ActionEvent event) {
        Code cd_err;
        cd_err = client.addFriend(friendname.getText());
        frienderror.setText(Client.getMessage(cd_err));
        frienderror.setVisible(true);
        if(listPane.isVisible()){
            updateList(client.getList());
            updateList(client.getRanking());
        }
    }

    public void challengeButton (ActionEvent event) {
        Code cd_err;
        cd_err = client.challengePrcs(challenge.getText());
        challerror.setText(Client.getMessage(cd_err));
        challerror.setVisible(true);
        //se invia la richiesta si deve aspettare o una risposta o il timer
        if (cd_err==Code.SEND_REQUEST)
            challbtn.setDisable(true);
    }

    public void acceptButton(ActionEvent event) {
        Notifier removed = news.removeLast();
        notify.accepted(removed.destIA, removed.UDPport);
        client.challPort = removed.TCPport;
        news.clear();
        client.gotoGame();
    }

    public void declineButton (ActionEvent event) {
        Notifier removed = news.removeLast();
        notify.declined (removed.destIA, removed.UDPport);
        if (news.isEmpty())
            setNotifyInvisible();
        else
            setNotifyVisible();
    }

    public void logoutButton(ActionEvent event) {
        Code cd_err;
        cd_err = client.logoutPrcs();
        logouterror.setText(Client.getMessage(cd_err));
        if (cd_err==Code.SUCCESS) client.gotoLogin();
    }

    //mostra notifica
    public void addNotification(String username, InetAddress destIA, int TCPport, int UDPport) {
        news.addFirst(new Notifier(username, destIA, TCPport, UDPport));
        setNotifyVisible();
    }

    //rimuove la notifica
    public void removeNotification(String username) {
        for (int i = 0; i< news.size(); i++) {
            if (news.get(i).username.equals(username)) {
                news.remove(i);
            }
        }
        if (news.isEmpty())
            setNotifyInvisible();
        else
            setNotifyVisible();
    }

    public static class Notifier {
        InetAddress destIA;
        int TCPport;
        int UDPport;
        String username;

        public Notifier(String username, InetAddress destIA, int TCPport, int UDPport) {
            this.destIA = destIA;
            this.TCPport = TCPport;
            this.UDPport = UDPport;
            this.username = username;
        }
    }
}
