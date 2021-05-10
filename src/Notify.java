import javafx.application.Platform;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Notify extends Thread {

    private MainController mainController;
    private Client client;
    private DatagramSocket UDPsock;
    private InetAddress destAddr;
    private int destPort;

    public Notify(Client client, DatagramSocket UDPsock) {
        this.client = client;
        this.UDPsock = UDPsock;
    }

    public void run() {
        try {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            while (true) {
                // attesa delle notifiche
                UDPsock.receive(packet);
                String datagram = new String(packet.getData(), 0, packet.getLength());
                final String[] token = datagram.split(" ");

                //ricezione della notifica
                if (token[0].equals("CHALLENGE") && token.length == 3) {
                    destAddr = packet.getAddress();
                    destPort = packet.getPort();
                    //serve per aggiornare la gui nei thread javafx altrimenti non è possibile aggiornare la GUI da altri thread
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            //aggiungo la notifica alla coda
                            mainController.addNotification(token[1], destAddr, Integer.parseInt(token[2]), destPort);
                        }
                    });
                }

                // se lo sfidato non risponde in tempo
                if (token[0].equals("TIMEOUT")) {
                     Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                     //rimuovo la notifica dalla coda
                        mainController.removeNotification(token[1]);
                            }
                     });
                }

                //se lo sfidato accetta
                if (token[0].equals("ACCEPTED")) {
                    client.challPort = Integer.parseInt(token[1]);
                    Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                        client.gotoGame();
                            }
                    });
                }

                //se lo sfidato rifiuta (o scade il timer)
                if (token[0].equals("DECLINED")) {
                    Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                        //rendo nuovamente cliccabile il tasto di challenge
                        mainController.challerror.setText("Sfida rifiutata");
                        mainController.challbtn.setDisable(false);
                            }
                    });
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void setController(MainController contr) {
        this.mainController = contr;
    }

    //messaggi per lo sfidante
    public void accepted(InetAddress IA, int port) {
        String line = "ACCEPT";
        byte[] buffer = line.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, IA, port);
        try {
            UDPsock.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mainController.setNotifyInvisible();
    }

    public void declined(InetAddress IA, int port) {
        String line = "DECLINE";
        byte[] buffer = line.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, IA, port);
        try {
            UDPsock.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mainController.setNotifyInvisible();
    }
}
