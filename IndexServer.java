import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;
import java.util.concurrent.*;

public class IndexServer extends UnicastRemoteObject implements Index 
{
    private final Queue<String> urlsToIndex;
    private final Map<String, Set<String>> invertedIndex;
    private final Set<String> visitedUrls;

    public IndexServer() throws RemoteException 
    {
        urlsToIndex = new ConcurrentLinkedQueue<>();
        invertedIndex = new ConcurrentHashMap<>();
        visitedUrls = ConcurrentHashMap.newKeySet();
    }

    public static void main(String[] args) throws Exception 
    {
        IndexServer server = new IndexServer();
        String nome = args.length > 0 ? args[0] : "barrel1";
        Registry registry;
        try {
        registry = LocateRegistry.createRegistry(8183);
        System.out.println("Registry criado na porta 8183");
        } catch (ExportException e) {
            registry = LocateRegistry.getRegistry(8183);
            System.out.println("Registry ja existe, a conectar...");
        }
        registry.rebind(nome, server);
        System.out.println("IndexServer " + nome + " pronto");
        System.out.println("IndexServer pronto");

        try (Scanner sc = new Scanner(System.in)) 
        {
            while (true) 
            {
                System.out.println("1 - Add URL | 2 - Search");
                String op = sc.nextLine();
                if (op.equals("1")) 
                {
                    System.out.print("URL: ");
                    server.putNew(sc.nextLine());
                } 
                else if (op.equals("2")) 
                {
                    System.out.print("Word: ");
                    System.out.println(server.searchWord(sc.nextLine()));
                }
            }
        }
    }

    @Override
    public String takeNext() throws RemoteException {
        return urlsToIndex.poll();
    }

    @Override
    public void putNew(String url) throws RemoteException {
        if (visitedUrls.add(url)) { 
            urlsToIndex.add(url);
        }
    }

    @Override
    public void addToIndex(String word, String url) throws RemoteException {
        invertedIndex.computeIfAbsent(word, k -> ConcurrentHashMap.newKeySet()).add(url);
    }

    @Override
    public List<String> searchWord(String word) throws RemoteException {
        Set<String> urls = invertedIndex.getOrDefault(word, Collections.emptySet());
        return new ArrayList<>(urls);
    }
}