import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;

public class GatewayServer extends UnicastRemoteObject implements GatewayInterface {
    private final List<Index> barrels;
    private int currentBarrel = 0; // round-robin

    public GatewayServer(List<Index> barrels) throws RemoteException {
        this.barrels = barrels;
    }

    @Override
    public List<String> searchWord(String word) throws RemoteException {
        for (int i = 0; i < barrels.size(); i++) {
            Index barrel = barrels.get(currentBarrel);
            try {
                List<String> result = barrel.searchWord(word);
                currentBarrel = (currentBarrel + 1) % barrels.size();
                return result;
            } catch (RemoteException e) {
                System.out.println("barrel falhou, a tentar outro...");
                currentBarrel = (currentBarrel + 1) % barrels.size();
            }
        }
        throw new RemoteException("Nenhum Barrel disponivel");
    }

    @Override
    public void putNew(String url) throws RemoteException {
        for (int i = 0; i < barrels.size(); i++) {
            Index barrel = barrels.get(currentBarrel);
            try {
                barrel.putNew(url);
                currentBarrel = (currentBarrel + 1) % barrels.size();
                return;
            } catch (RemoteException e) {
                System.out.println(" Barrel falhou, a tentar outro...");
                currentBarrel = (currentBarrel + 1) % barrels.size();
            }
        }
        throw new RemoteException("nenhum Barrel disponivel");
    }

    public static void main(String[] args) throws Exception {
        // Conectar aos Barrels registados no RMI Registry
        Registry registry = LocateRegistry.getRegistry(8183);
        Index barrel1 = (Index) registry.lookup("barrel1");
        Index barrel2 = (Index) registry.lookup("barrel2");

        List<Index> barrels = Arrays.asList(barrel1, barrel2);
        GatewayServer gateway = new GatewayServer(barrels);

        Registry gatewayRegistry = LocateRegistry.createRegistry(8184);
        gatewayRegistry.rebind("gateway", gateway);
        System.out.println("Gateway pronta no port 8184");
    }
}
