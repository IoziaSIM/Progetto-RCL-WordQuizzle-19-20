import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Database extends RemoteServer implements RegInt {

    private static HashMap<String, User> db;
    private Gson gsonw;
    private static final String jpath = "./json/database.json";

    public Database() {
        db = new HashMap<>();
        //writer per serializzare
        gsonw = new GsonBuilder().setPrettyPrinting().create();
        // se esiste il database, lo deserializzo
        if(Files.exists(Paths.get(jpath))) {
            try {
                FileReader reader = new FileReader(jpath);
                Gson gson = new Gson();
                db = gson.fromJson(reader, new TypeToken<HashMap<String, User>>(){}.getType());
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            System.out.println("Database vuoto!");
        }

    }

    //registrazione utente
    public synchronized Code userRegistration (String name, String psw) throws NullPointerException {
        if (name == null || psw == null) throw new NullPointerException("Username o password invalido!");
        if (name.equals("") || psw.equals("")) return Code.USER_PASS_INVALID;
        if (db.containsKey(name)) return Code.ALREADY_REGISTERED;

        db.put (name, new User(name, psw));

        //serializzazione
        try {
            FileWriter writer = new FileWriter(jpath);
            gsonw.toJson(db, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Code.USER_REGISTERED;
    }

    //login di un utente
    public synchronized Code userLogin (String name, String psw, String address, int port) throws NullPointerException {
        if (name == null || psw == null) throw new NullPointerException("Username o password invalido!");

        if (db.containsKey(name)) {
            if (db.get(name).password.equals(psw)) {
                if (!db.get(name).getOnline()) {
                    db.get(name).setOnline(true);
                    db.get(name).setTCPAddress(address);
                    db.get(name).setUDPPort(port);
                    return Code.SUCCESS;
                } else
                    return Code.USER_ALR_CONNECTED;
            } else
                return Code.PASS_NOT_CORRECT;
        } else
            return Code.USER_NOT_REGISTERED;
    }

    //logout di un utente
    public synchronized Code userLogout(String name) throws NullPointerException {
        if (name == null) throw new NullPointerException("Username invalido!");

        if (db.containsKey(name)) {
            if (db.get(name).getOnline()) {
                db.get(name).setOnline(false);
                return Code.SUCCESS;
            }
            else
                return Code.USER_ALR_DISCONNECTED;
        } else
            return Code.USER_NOT_REGISTERED;
    }

    //utente aggiunge amico
    public synchronized Code userAddFriend (String name, String friend) throws NullPointerException {
        if (name == null) throw new NullPointerException("Username invalido!");
        if (friend == null) throw new NullPointerException("Username amico invalido!");

        if (db.containsKey(name)) {
            if(db.get(name).addFriend(friend) == 1 && db.get(friend).addFriend(name) == 1) {
                try {
                    FileWriter wrt = new FileWriter(jpath);
                    gsonw.toJson(db, wrt);
                    wrt.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return Code.ADD_FRIEND;
            } else
                return Code.ALREADY_FRIEND;
        } else
            return Code.USER_NOT_REGISTERED;
    }

    //mostra lista amici
    @SuppressWarnings("unchecked")
    public synchronized JSONArray userFriendList (String name) throws NullPointerException {
        if (name == null) throw new NullPointerException("Username invalido!");

        if (db.containsKey(name)) {
            Iterator<String> itr = db.get(name).getFriendList().iterator();
            JSONArray list = new JSONArray();
            while(itr.hasNext()) {
                list.add(itr.next());
            }
            return list;
        } else
            return null;
    }

    //mostra punteggio
    public int userScore (String name) {
        if (name == null) throw new NullPointerException("Username invalido!");
        if (db.containsKey(name)) {
            return db.get(name).getScore();
        } else
            return -1;
    }

    //mostra classifica amici
    @SuppressWarnings("unchecked")
    public synchronized JSONArray showRanking (String name) throws NullPointerException {
        if (name == null) throw new NullPointerException("Username invalido!");

        if (db.containsKey(name)) {
            Iterator<String> itr = db.get(name).getFriendList().iterator();
            ArrayList<User> ranking = new ArrayList<>();
            while (itr.hasNext()) {
                ranking.add(db.get(itr.next()));
            }
            ranking.add(db.get(name));
            // ordina la classifica con compareTo
            Collections.sort(ranking);
            Iterator<User> iter = ranking.iterator();
            JSONArray rank = new JSONArray();
            while(iter.hasNext()) {
                JSONObject user = new JSONObject();
                User u = iter.next();
                user.put("username", u.username);
                user.put("score", u.score);
                rank.add(user);
            }
            return rank;
        } else
            return null;
    }

    //invio della challenge all'amico
    public Code sendChallenge(String name, String friend, DatagramSocket dsock, int port) throws NullPointerException {
        if (name == null) throw new NullPointerException("Username invalido!");
        if (friend == null) throw new NullPointerException("Username amico invalido!");

        if (db.containsKey(name)) {
            if (db.get(name).getFriendList().contains(friend)) {
                if (db.get(friend).getOnline()) {
                    String message = "CHALLENGE " + name + " " + port;
                    byte[] buffer = message.getBytes();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, db.get(friend).getAddress(), db.get(friend).getPort());
                    try {
                        dsock.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return Code.SEND_REQUEST;
                } else
                    return Code.FRIEND_NOT_ONLINE;
            } else
                return Code.NOT_FRIEND;
        } else
            return Code.USER_NOT_REGISTERED;
    }

    //invio risposta alla richiesta di sfida
    public void acceptChallenge (String name, DatagramSocket dsock, int port) {
        String message = "ACCEPTED " + port;
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, db.get(name).getAddress(), db.get(name).getPort());
        try {
            dsock.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void declineChallenge (String name, DatagramSocket dsock) {
        String message = "DECLINED";
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, db.get(name).getAddress(), db.get(name).getPort());
        try {
            dsock.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void timeoutChallenge (String name, String friend, DatagramSocket dsock) {
        String message = "TIMEOUT " + name;
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, db.get(friend).getAddress(), db.get(friend).getPort());
        try {
            dsock.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public User getUser (String name) throws NullPointerException {
        if (name == null) throw new NullPointerException("Username invalido!");
        return db.get(name);
    }

    public void setUpdate() {
        try {
            FileWriter writer = new FileWriter(jpath);
            gsonw.toJson(db, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}