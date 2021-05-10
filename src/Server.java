import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server {
    public static void main(String[] args) {
        Database db = new Database();
        //creazione registry e pubblicazione stub
        try {
            RegInt stub = (RegInt) UnicastRemoteObject.exportObject(db, 0);
            LocateRegistry.createRegistry(8989);
            Registry r = LocateRegistry.getRegistry(8989);
            r.rebind("REG-SERVER", stub);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        //thread per ogi utente loggato
        LinkedBlockingQueue<Runnable> myQueue = new LinkedBlockingQueue<>();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(50, 100, 320000, TimeUnit.MILLISECONDS, myQueue);
        // creazione socket tcp
        ServerSocket ssock;
        try {
            ssock = new ServerSocket(6767);
            while(true){
                //aspetto l'arrivo di un utente
                Socket sock = ssock.accept();
                // esecuzione thread
                Task t = new Task(db, sock);
                executor.execute(t);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}