import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;

public class Client extends Application {

        private static Stage window;
        private FXMLLoader loader;
        public static RegInt remReg;
        private Socket TCPsock;
        private DatagramSocket UDPsock;
        private int UDPport;
        public int challPort;
        private BufferedReader reader;
        private BufferedWriter writer;
        public String myUsername;
        private Notify notify;
        private final JSONParser parser = new JSONParser();

        public static void main(String[] args) {
        Registry r;
        //connessione col registry
        try {
            r = LocateRegistry.getRegistry(8989);
            remReg = (RegInt) r.lookup("REG-SERVER");

        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
        //avvio interfaccia grafica
        launch();
    }

    @Override
    public void start(Stage primaryStage) {
        window = primaryStage;
        primaryStage.setTitle("Word Quizzle");
        gotoStart();
        primaryStage.show();
    }

    //pagina iniziale
    public void gotoStart() {
        try{
            //caricamento file fxml
            loader = new FXMLLoader(Client.class.getResource("StartView.fxml"));
            Parent root = loader.load();

            //caricamento controlli interfaccia
            StartController logincontroller = loader.getController();
            logincontroller.setClient(this);

            //setto la nuova faccia
            Scene scene = new Scene(root);
            window.setScene(scene);
            window.sizeToScene();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //pagina del login
    public void gotoLogin() {
        try {
            loader = new FXMLLoader(Client.class.getResource("LoginView.fxml"));
            Parent root = loader.load();

            LoginController logincontroller = loader.getController();
            logincontroller.setClient(this);

            Scene scene = new Scene(root);
            window.setScene(scene);
            window.sizeToScene();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void gotoSignIn() {
        try {
            loader = new FXMLLoader(Client.class.getResource("RegisterView.fxml"));
            Parent root = loader.load();

            RegisterController logincontroller = loader.getController();
            logincontroller.setClient(this);

            Scene scene = new Scene(root);
            window.setScene(scene);
            window.sizeToScene();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void gotoMain() {
        try {
            loader = new FXMLLoader(Client.class.getResource("MainView.fxml"));
            Parent root = loader.load();

            MainController maincontroller = loader.getController();

            maincontroller.setClient(this, notify, myUsername);
            notify.setController(maincontroller);

            Scene scene = new Scene(root);
            window.setScene(scene);
            window.sizeToScene();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void gotoGame() {
        try {
            loader = new FXMLLoader(Client.class.getResource("GameView.fxml"));
            Parent root = loader.load();
            //setta i bottoni del  profilo
            GameController gamecontroller = loader.getController();
            //passo il riferimento di questa classe
            gamecontroller.setClient(this, TCPsock.getInetAddress(), challPort);
            Scene scene = new Scene(root);
            window.setScene(scene);
            window.sizeToScene();
            window.sizeToScene();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Code loginPrcs(String username, String password) {
        Code cd_err = null;
        try {
            //collegamento tcp al server
            String serverName = "localhost";
            TCPsock = new Socket(serverName, 6767);
            //creo porta delle notifiche
            UDPport = (int) ((Math.random() * ((65535 - 1024) + 1)) + 1024);

            reader = new BufferedReader(new InputStreamReader(TCPsock.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(TCPsock.getOutputStream()));

            writer.write("LOGIN " + username + " " + password + " " + TCPsock.getInetAddress().getHostAddress() + " " + UDPport);
            writer.newLine();
            writer.flush();
            cd_err = Code.valueOf(reader.readLine());

            //in caso di errori socket chiusa
            if (cd_err != Code.SUCCESS) {
                System.out.println("WQClient | login failed. Cleaning...");
                TCPsock.close();
                writer.close();
                reader.close();
                return cd_err;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // creo la socket udp per le notifiche
        try {
            UDPsock = new DatagramSocket(UDPport);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        // attivo il thread che gestisce le notifiche
        notify = new Notify(this, UDPsock);
        notify.start();
        myUsername = username;
        return cd_err;
    }

    public int getScore() {
        try {
            writer.write("SCORE " + myUsername);
            writer.newLine();
            writer.flush();
            return Integer.parseInt(reader.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public ArrayList<String> getList() {
        try {
            writer.write("LIST " + myUsername);
            writer.newLine();
            writer.flush();
            String jfile = reader.readLine();
            JSONArray jlist = null;
            ArrayList<String> list= new ArrayList<>();
            try {
                jlist = (JSONArray) parser.parse(jfile);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            assert jlist != null;
            for (Object o : jlist)
                list.add(o.toString());

            return list;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ArrayList<String> getRanking() {
        try {
            writer.write("RANK " + myUsername);
            writer.newLine();
            writer.flush();
            String jfile = reader.readLine();
            JSONArray jlist = null;
            ArrayList<String> list = new ArrayList<>();
            try {
                jlist = (JSONArray) parser.parse(jfile);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            for(int i = 0; i< jlist.size(); i++) {
                JSONObject usr = (JSONObject) jlist.get(i);
                list.add(usr.get("username") + " Points: " + usr.get("score"));
            }
            return list;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Code addFriend (String friend) {
        try {
            writer.write("ADD " + myUsername + " " + friend);
            writer.newLine();
            writer.flush();
            return Code.valueOf(reader.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Code.FAILED;
    }

    public Code challengePrcs (String friend) {
        try {
            writer.write("CHALLENGE " + myUsername + " " + friend);
            writer.newLine();
            writer.flush();
            return Code.valueOf(reader.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Code.FAILED;
    }

    public Code logoutPrcs() {
        try {
            writer.write("LOGOUT " + myUsername);
            writer.newLine();
            writer.flush();
            if (notify.isAlive())
                UDPsock.close();
            return Code.valueOf(reader.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Code.FAILED;
    }

    public static String getMessage (Code cd) {
        switch (cd) {
            case USER_REGISTERED:
                return "Utente registrato correttamente";
            case USER_PASS_INVALID:
                return "Username o password invalido";
            case ALREADY_REGISTERED:
                return "Utente già registrato";
            case USER_ALR_CONNECTED:
                return "Utente già connesso";
            case PASS_NOT_CORRECT:
                return "Password errata";
            case USER_NOT_REGISTERED:
                return "Utente non registrato";
            case USER_ALR_DISCONNECTED:
                return "Utente già disconesso";
            case ADD_FRIEND:
                return "Amico aggiunto";
            case ALREADY_FRIEND:
                return "Già tuo amico";
            case FRIEND_NOT_ONLINE:
                return "Amico non online";
            case SEND_REQUEST:
                return "Invio richiesta...";
            case NOT_FRIEND:
                return "Utente non amico";
            case OP_INVALID:
                return "Operazione non valida";
            case FAILED:
                return "Operazione fallita";
            default:
                return "Codice non riconosciuto";
        }
    }

}
