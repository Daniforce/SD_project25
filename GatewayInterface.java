import java.rmi.*;
import java.util.*;

public interface GatewayInterface extends Remote 
{
    List<String> searchWord(String word) throws RemoteException;
    void putNew(String url) throws RemoteException;
}
