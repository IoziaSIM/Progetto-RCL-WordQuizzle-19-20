import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

//dati di un utente
public class User implements Comparable<User> {

    public String username;
    public String password;
    public int score;
    private transient boolean online;
    private ArrayList<String> friendList;
    private transient InetAddress addr;
    private transient int port;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.score = 0;
        this.online = false;
        this.friendList = new ArrayList<>();
    }

    public void setOnline (boolean status) {
        online = status;
    }

    public void setScore (int points) {
        score = score + points;
    }

    public void setTCPAddress(String ind) {
        try {
            addr = InetAddress.getByName(ind);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void setUDPPort(int p) {
        port = p;
    }

    public String getPassword () {
        return password;
    }

    public boolean getOnline () {
        return online;
    }

    public InetAddress getAddress (){
        return addr;
    }

    public int  getPort() {
        return port;
    }

    public ArrayList<String> getFriendList() {
        return friendList;
    }

    public int getScore () {
        return score;
    }

    public int addFriend (String friend) {
        if(!friendList.contains(friend)) {
            friendList.add(friend);
            return 1;
        } else
            return 0;
    }

    @Override
    public int compareTo(User user) {
        return user.score - this.score;
    }

}