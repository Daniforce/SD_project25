package search;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;


public class Client {

    private Index index;
    private Scanner scanner;

    public Client(){
        this.scanner = new Scanner(System.in);
    }

    public boolean tryConnect(String host,int port){
        try {
            Registry registry = LocateRegistry.getRegistry(host,port);
            index = (Index) registry.lookup("index");
            System.out.println("Connected to server at " + host + ":" + port);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            return false;
        }
    }


    public void start(){
        if(index == null){
            System.out.println("Server not connected yet. Use \"connect\" method or something on it didn't go well");
            return;
        }

        System.out.println("Index Client Started. Available commands:");
        System.out.println("  add <url>     - Add URL for indexing");
        System.out.println("  search <word> - Search for word in index");
        System.out.println("  status        - Show server status");
        System.out.println("  quit          - Exit client");

        while(true){
            System.out.print("\nclient>");
            String input = scanner.nextLine().trim();

            if(input.isEmpty()) continue;

            String[] parts = input.split(" ",2);
            String command = parts[0].toLowerCase();

            try {
                switch (command) {
                    case "add":
                        if(parts.length > 1 && parts[1].startsWith("http")) index.putNew(parts[1]);
                        break;
                    case "search":
                        if(parts.length > 1){
                            List<String> results = index.searchWord(parts[1]);
                            System.out.println("Found " + results.size() + " results for '" + parts[1] + "':");
                            for (String url : results) System.out.println("  - " + url);
                        }
                        else System.out.println("Usage: search <word>");
                        break;
                    case "status":
                        System.out.println("Client connected to server."); // Not what's gonna be final but for now let it be
                        
                        break;
                    case "quit":
                    case "exit":
                        System.out.println("Goodbye!");
                        return;
                    default:
                        System.out.println("Unknown command: " + command);
                        System.out.println("Available commands: add, search, status, quit");

                        break;
                }
            } catch (Exception e) {
                System.err.println("Error executing command: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        
        Client client = new Client();

        String host = "localhost";
        int port = 8184;

        if(args.length >= 1) host = args[0];
        if(args.length >= 2) port = Integer.parseInt(args[1]);

        if(client.tryConnect(host, port)) client.start();

        client.scanner.close();

    }
    
}
