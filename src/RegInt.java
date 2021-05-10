import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RegInt extends Remote {
    public Code userRegistration (String name, String psw) throws RemoteException, NullPointerException;
}
