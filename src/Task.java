import org.json.simple.JSONArray;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Task implements Runnable {

    private final Database db;
    private final Socket TCPsock;
    private DatagramSocket UDPsock;

    public Task(Database db, Socket sock){
        this.db = db;
        this.TCPsock = sock;
    }

    @Override
    public void run() {
        try {

            BufferedReader reader = new BufferedReader(new InputStreamReader(TCPsock.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(TCPsock.getOutputStream()));
            // leggi comandi dal client
            String line = reader.readLine();
            // dividi le singole parole grazie agli spazi
            String[] token = line.split(" ");
            // fin quando la prima parola non è logout
            while (!token[0].equals("LOGOUT")) {

                // se il client si sta loggando
                if (token[0].equals("LOGIN") && token.length == 5) {
                    Code cd_err = db.userLogin(token[1], token[2], token[3], Integer.parseInt(token[4]));
                    if (cd_err == Code.SUCCESS) {
                        UDPsock = new DatagramSocket();
                    }
                    // comunico al client se il login è andato a buon fine
                    writer.write(String.valueOf(cd_err));
                    writer.newLine();
                    writer.flush();
                    //se il login non è corretto devo uscire dal thread
                    if (cd_err != Code.SUCCESS) {
                        return;
                    }
                } // se il client vuole aggiungere un amico
                else if (token[0].equals("ADD") && token.length == 3) {
                    Code cd_err = db.userAddFriend(token[1], token[2]);
                    writer.write(String.valueOf(cd_err));
                    writer.newLine();
                    writer.flush();
                }  //se il client richiede il punteggio
                else if (token[0].equals("SCORE") && token.length == 2) {
                    int score = db.userScore(token[1]);
                    writer.write(Integer.toString(score));
                    writer.newLine();
                    writer.flush();
                }  //se il client richiede la lista amici
                else if (token[0].equals("LIST") && token.length == 2) {
                    JSONArray list = db.userFriendList(token[1]);
                    writer.write(String.valueOf(list));
                    writer.newLine();
                    writer.flush();
                } // se il client richiede la classifica
                else if (token[0].equals("RANK") && token.length == 2) {
                    JSONArray ranking = db.showRanking(token[1]);
                    writer.write(String.valueOf(ranking));
                    writer.newLine();
                    writer.flush();
                }  //se il client vuole sfidato un amico
                else if (token[0].equals("CHALLENGE") && token.length == 3) {
                    // trovo una porta randomica per la challenge
                    int challPort = (int) ((Math.random() * ((65535 - 1024) + 1)) + 1024);
                    // aziono il thread della challenge
                    Challenge chll = new Challenge(db, challPort);
                    chll.start();
                    // avviso l'amico della sfida
                    Code cd_err = db.sendChallenge(token[1], token[2], UDPsock, challPort);
                    if(cd_err != Code.SEND_REQUEST) {
                        if(chll.isAlive())
                            chll.interrupt();
                        writer.write(String.valueOf(cd_err));
                        writer.newLine();
                        writer.flush();
                    } else {
                        writer.write(String.valueOf(cd_err));
                        writer.newLine();
                        writer.flush();
                        byte[] buffer = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        // sfida da accettare in 15 secondi
                        UDPsock.setSoTimeout(15000);
                        try {
                            // aspetto la risposta dell'amico
                            UDPsock.receive(packet);
                        } catch (SocketTimeoutException e) {
                            db.declineChallenge(token[1], UDPsock);
                            db.timeoutChallenge(token[1], token[2], UDPsock);
                            if(chll.isAlive())
                                chll.interrupt();
                        }
                        // comunico il client della decisione dell'amico
                        String datagram = new String(packet.getData(), 0, packet.getLength());
                        if (datagram.equals("ACCEPT")){
                            db.acceptChallenge(token[1], UDPsock, challPort);
                        }
                        if (datagram.equals("DECLINE")) {
                            db.declineChallenge(token[1], UDPsock);
                            if (chll.isAlive())
                                chll.interrupt();
                        }
                    }
                } else {
                    writer.write(String.valueOf(Code.OP_INVALID));
                    writer.newLine();
                    writer.flush();
                }
                line = reader.readLine();
                token = line.split(" ");
            }
            // se il client vuole uscire chiudo le socket
            writer.write(String.valueOf(db.userLogout(token[1])));
            writer.newLine();
            writer.flush();
            UDPsock.close();
            TCPsock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
