import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Challenge extends Thread {

    private Database db;
    private int port;
    public static double N = 10;
    private Selector slct;
    private ServerSocketChannel ssChannel;
    private ServerSocket ssock;
    private ArrayList<String> selectWords;
    private ArrayList<String> translateWords;
    private ArrayList<SelectionKey> selectKey ;
    private JsonArray wordList;
    private volatile AtomicInteger finish;
    public volatile AtomicInteger timeout;
    private String dictPath = "./json/dizionario.json";

    private static int correct = 3;
    private static int wrong = -1;
    private static int bonus = 5;

    public Challenge (Database db, int port){
        this.db = db;
        this.port = port;
        finish = new AtomicInteger(0);
        timeout = new AtomicInteger(0);
        selectWords = new ArrayList<>();
        selectKey = new ArrayList<>();
    }

    @Override
    public void run() {
        // legge il file json delle parole possibili e lo mette in un array
        try {
            FileReader reader = new FileReader(dictPath);
            Gson gson = new Gson();
            wordList = gson.fromJson(reader, new TypeToken<JsonArray>(){}.getType());
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // mi sceglie 10 parole randomicamente
        for(int i=0; i<N; i++) {
            JsonElement word = wordList.get((int) (Math.random() * wordList.size()));
            selectWords.add(String.valueOf(word).replaceAll("\"", ""));
        }

        // creo la socket tcp per la sfida e realizzazione multiplexing
        try {
            // Creo il channel per poterlo mettere non bloccante
            ssChannel = ServerSocketChannel.open();
            ssChannel.configureBlocking(false);
            // reperisco la socket dal canale (active)
            ssock = ssChannel.socket();

            //associo la porta alla socket (IP wildcard, indica tutti gli ip della macchina, 0.0.0.0)
            InetSocketAddress address = new InetSocketAddress(port);
            ssock.bind(address);

            // creo il selettore e lo associo al channel sull'operazione di accept
            slct = Selector.open();
            ssChannel.register(slct, SelectionKey.OP_ACCEPT);

        } catch (IOException e) {
                e.printStackTrace();
        }

        while (finish.get() != 2 && !Thread.currentThread().isInterrupted()) {
            try {
                // aspetto che un canale sia pronto
                slct.select();
            } catch (IOException ex) {
                ex.printStackTrace();
                break;
            }
            // ready sono i canali pronti
            Set<SelectionKey> ready = slct.selectedKeys();
            Iterator<SelectionKey> itr = ready.iterator();
            while (itr.hasNext()) {
                SelectionKey key = itr.next();
                // rimozione dall'insieme esplicita
                itr.remove();
                try {
                    // connessione accettata
                    if (key.isAcceptable()) {
                        //nuovo canale per l'active socket
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        //
                        SocketChannel client = server.accept();
                        client.configureBlocking(false);
                        //mi metto in attesa che il client scriva
                        SelectionKey clientkey = client.register(slct, SelectionKey.OP_WRITE, new Session(null, 0));
                        selectKey.add(clientkey);
                        if (translateWords == null) {
                            translateWords = new ArrayList<>();
                            //mi salvo la traduzione delle 10 parole
                            WQTranslation();
                            // aziona il timer per la sfida
                            new TimerCh(60, this);
                        }
                    }
                    // se il client attende la parola da tradurre
                    else if (key.isWritable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        // prendi la parola scritta dal client
                        Session mySes = (Session) key.attachment();
                        if (mySes.getWord() == null) {
                            //se non sono finite le parole, se uno dei player non ha finito, se il timer non è finito
                            if (mySes.getIndex() < N && finish.get() != 1 && timeout.get()==0) {
                                // preparo la parola da inviare
                                mySes.setWord(selectWords.get(mySes.getIndex()));
                            } else {
                                String mytoken = "";
                                // se è scattato il timer
                                if(timeout.get() == 1)
                                    mytoken = "TIMEOUT";
                                // se uno dei 2 ha finito le parole
                                else
                                    mytoken = "FINISH";

                                Session player1 = (Session) selectKey.get(0).attachment();
                                Session player2 = (Session) selectKey.get(1).attachment();
                                if(player1.getPoints() == player2.getPoints()) {
                                    mySes.setWord(mytoken + " " + mySes.getPoints() + " DRAW!");
                                } else if(player1.getPoints() > player2.getPoints() && mySes.getUsername().equals(player1.getUsername())) {
                                    mySes.setWord(mytoken + " " + mySes.getPoints() + " WIN!");
                                    db.getUser(mySes.getUsername()).setScore(bonus);
                                } else if(player2.getPoints() > player1.getPoints() && mySes.getUsername().equals(player2.getUsername())) {
                                    mySes.setWord(mytoken + " " + mySes.getPoints() + " WIN!");
                                    db.getUser(mySes.getUsername()).setScore(bonus);
                                } else {
                                    mySes.setWord(mytoken + " " + mySes.getPoints() + " LOSE!");
                                }
                                //avverto che uno dei due ha finito
                                finish.incrementAndGet();
                            }
                        }

                        ByteBuffer wrbuf = ByteBuffer.wrap(mySes.getWord().getBytes());
                        //manda la parola italiana o il risultato al client
                        int byteWritten = client.write(wrbuf);
                        if (byteWritten == mySes.getWord().length()) {
                            mySes.setWord(null);
                            key.attach(mySes);
                            key.interestOps(SelectionKey.OP_READ);
                        } else if(byteWritten == -1) {    // se qualcosa è andato storto
                            key.cancel();
                            key.channel().close();
                        } else {     //se non ha scritto tutto
                            wrbuf.flip();
                            //setto la parola restante
                            mySes.setWord(StandardCharsets.UTF_8.decode(wrbuf).toString());
                            key.attach(mySes);
                        }
                    }

                    // dopo che il client ha mandato la parola
                    else if(key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        Session mySes = (Session) key.attachment();
                        String mytoken = "";
                        if (mySes.getWord() != null)
                            mytoken = mySes.getWord();

                        ByteBuffer rdbuf = ByteBuffer.allocate(1024);
                        rdbuf.clear();

                        int byteRed = client.read(rdbuf);
                        //se il buffer è pieno, rileggi di nuovo
                        if(byteRed == 1024) {
                            rdbuf.flip();
                            mytoken = mytoken + StandardCharsets.UTF_8.decode(rdbuf).toString();
                            mySes.setWord(mytoken);
                            key.attach(mySes);
                        } else if(byteRed < 1024) {  //se il server ha letto tutto
                            rdbuf.flip();
                            mytoken = mytoken + StandardCharsets.UTF_8.decode(rdbuf).toString();
                            if(mytoken.equals("EXIT")) {  //se il client esce prima della fine
                                finish.incrementAndGet();
                                key.cancel();
                                key.channel().close();
                            } else {    //controlla la traduzione
                                String[] token = mytoken.split(" ");
                                if(mySes.getUsername() == null)
                                    mySes.setUsername(token[0]);
                                if (finish.get()==0 && timeout.get()==0 )
                                    checkTransl(token[1], translateWords.get(mySes.getIndex()), token[0], mySes); //controlla la correttezza
                                mySes.setWord(null);
                                mySes.incIndex();
                                key.attach(mySes);
                                key.interestOps(SelectionKey.OP_WRITE);
                            }
                        }

                        if (byteRed == -1) {  //se il client chiude la socket/termina
                            key.cancel();
                            key.channel().close();
                        }
                    }

                } catch (IOException e) {
                   key.cancel();
                    try {
                        key.channel().close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
        }

        //aggiorna il databse alla fine della sfida
        if (finish.get() == 2 || timeout.get() == 1)
            db.setUpdate();

    }

    //connessione al sito per la verifica della traduzione
    public void WQTranslation () throws IOException {
        for (int i = 0; i < selectWords.size(); i++) {
            URL url = new URL("https://api.mymemory.translated.net/get?q=" + selectWords.get(i) + "&langpair=it|en");
            HttpsURLConnection cnnss = (HttpsURLConnection) url.openConnection();
            cnnss.setRequestMethod("GET");
            cnnss.setRequestProperty("Accept", "application/json");
            if (cnnss.getResponseCode() != 200)
                throw new RuntimeException("Failed, error: " + cnnss.getResponseCode());

            // se la richiesta va a buon fine
            BufferedReader rdbuf = new BufferedReader(new InputStreamReader((cnnss.getInputStream())));
            String eng = rdbuf.readLine();
            try {
                JSONParser parser = new JSONParser();
                JSONObject obj1 = (JSONObject) parser.parse(eng);
                JSONObject obj2 = (JSONObject) obj1.get("responseData");
                //toLowercase per la traduzione ha delle lettere maiuscole
                translateWords.add(i, obj2.get("translatedText").toString().toLowerCase());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            cnnss.disconnect();
        }
    }

    //assegna punteggi
    public void checkTransl (String word, String eng, String name, Session item) {
        User player = db.getUser(name);
        if(player != null) {
            if(word.equals(eng)) {
                player.setScore(correct);
                item.points = item.points + correct;
            } else {
                player.setScore(wrong);
                item.points = item.points + wrong;
            }
        }
    }
}