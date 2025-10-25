package search;

// import java.io.InterruptedIOException;
// import java.nio.channels.InterruptedByTimeoutException;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
// import java.util.concurrent.*;
// import java.io.*;
import java.util.*;

public class IndexServer extends UnicastRemoteObject implements Index {
    // private String[] urlsToIndex;
    // private String[][] indexedItems;
    private ArrayDeque<String> urlsToIndex; // Not made for concurrency
    private HashMap<String,ArrayList<String>> indexedItems; // Not made for concurrency

    private final int INITIAL_DELAY = 1000;
    private final int MAX_DELAY = 60000;
    private HashMap<String,Integer> domainSpecificDelays;


    public IndexServer() throws RemoteException {
        super();
        //This structure has a number of problems. The first is that it is fixed size. Can you enumerate the others?
        // urlsToIndex = new String[10];

        urlsToIndex = new ArrayDeque<String>();
        indexedItems = new HashMap<String,ArrayList<String>>();

        domainSpecificDelays = new HashMap<String,Integer>();

    }

    // public static void main(String args[]) {
    //     Scanner entry = new Scanner(System.in);
    //     try {
    //         IndexServer server = new IndexServer();
    //         Registry registry = LocateRegistry.createRegistry(8184);
    //         registry.rebind("index", server);
    //         System.out.println("Server ready. Waiting for input...");

    //         //This approach needs to become interactive. Use a Scanner(System.in) to create a rudimentary user interface to:
    //         //1. Add urls for indexing
    //         //2. search indexed urls


    //         while(true){
    //             System.out.print("Insert an url to submit for indexing (must start with http) or a continuous word to search for in the index\nR:");
    //             String answer = entry.nextLine();

    //             if(answer.startsWith("http")) server.putNew(answer.trim());
    //             else System.out.println("URLs indexed to \"" + answer.trim() +  "\": " + server.searchWord(answer.trim()));
    //         }


    //         // server.putNew("https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal");
    //     } catch (RemoteException e) {
    //         entry.close();
    //         e.printStackTrace();
    //     }
    //     catch (Exception e){
    //         entry.close();
    //     }
    // }

    public static void main(String args[]) {
        try {
            IndexServer server = new IndexServer();
            Registry registry = LocateRegistry.createRegistry(8184);
            registry.rebind("index", server);
            System.out.println("Server ready and listening on port 8184...");
            
            // Server now runs indefinitely without user interaction
            // Keep the main thread alive
            Object keepAlive = new Object();
            synchronized (keepAlive) {
                keepAlive.wait();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // private long counter = 0, timestamp = System.currentTimeMillis();;

    public String takeNext() throws RemoteException {
        //not implemented fully. Prefer structures that return in a push/pop fashion
        // return urlsToIndex[0]; //v1

        // return urlsToIndex.remove(); // v2 - Take an Url from the top of the "Stack" to start indexing

        synchronized(urlsToIndex){ // v3 - Should make it so that now the Robot waits for URLs to be available before trying to scan whatever is inside, empty or not
            while(urlsToIndex.isEmpty()){
                try {
                    urlsToIndex.wait();
                } catch (Exception e) {
                   Thread.currentThread().interrupt();
                   throw new RemoteException("Interrupted Thread while waiting for URLs in the urlsToIndex Deque");
                }
            }
            return urlsToIndex.remove();
        }
    }

    public void putNew(String url) throws java.rmi.RemoteException {
        //Example code. Must be changed to use structures that have primitives such as .add(...)
        // urlsToIndex[0] = url; //v1
        // urlsToIndex.add(url); //v2

        synchronized(urlsToIndex){
            urlsToIndex.add(url);
            urlsToIndex.notifyAll();
        }
    }

    public void addToIndex(String word, String url) throws java.rmi.RemoteException {
        // v1
        // if(word == null) return; //Check if the word is valid

        // if(indexedItems.get(word) == null){ // Check if it's not been indexed yet
        //     indexedItems.put(word,new ArrayList<String>()); // Set up an initial key value pair
        // }
        // indexedItems.get(word).add(url); // Add the url to the value's ArrayList


        // v2    
        if(word == null) return; //Check if the word is valid

        synchronized(indexedItems){
            if(indexedItems.get(word) == null){ // Check if it's not been indexed yet
            indexedItems.put(word,new ArrayList<String>()); // Set up an initial key value pair
            }
            indexedItems.get(word).add(url); // Add the url to the value's ArrayList
        }
    }

    
    public List<String> searchWord(String word) throws java.rmi.RemoteException {
        // v1
        // if(indexedItems.get(word) == null) return new ArrayList<String>();
        // return indexedItems.get(word);

        // v2
        synchronized(indexedItems){
            if(indexedItems.get(word) == null) return new ArrayList<String>();
            return new ArrayList<String>(indexedItems.get(word)); // Return a copy of what's indexed to avoid concurrent modification problems
        }
    }


    public int getRecommendedDelay(String domain) throws RemoteException{
        synchronized(domainSpecificDelays){
            if(domainSpecificDelays.get(domain) == null) domainSpecificDelays.put(domain,INITIAL_DELAY);
            return domainSpecificDelays.getOrDefault(domain, INITIAL_DELAY);
        }
    }

    public void reportRateLimit(String domain) throws RemoteException{
        synchronized(domainSpecificDelays){
            int currentDelay = domainSpecificDelays.getOrDefault(domain, INITIAL_DELAY);
            int newDelay = Math.min(MAX_DELAY,currentDelay*2);
            domainSpecificDelays.put(domain, newDelay);
            System.out.println("Rate limit exceeded for domain: \"" + domain + "\"\nElevating its delay to " + newDelay + "ms");
        }
    }

    public void reportSuccess(String domain) throws RemoteException{
        synchronized(domainSpecificDelays){
            int currentDelay = domainSpecificDelays.get(domain);
            int newDelay = Math.max(INITIAL_DELAY, currentDelay/2);
            domainSpecificDelays.put(domain,newDelay);
            System.out.println("Success detected for domain: \"" + domain + "\"\nDecreasing its delay to " + newDelay + "ms");
        }
    }
}
