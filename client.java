import java.rmi.registry.*;
import java.util.*;

public class client {
    public static void main(String[] args) throws Exception {
        GatewayInterface gateway = (GatewayInterface) LocateRegistry.getRegistry(8184).lookup("gateway");
        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.print("Pesquisar: ");
                String word = sc.nextLine();
                List<String> results = gateway.searchWord(word);
                System.out.println("Resultados:");
                for (String url : results) {
                    System.out.println(" - " + url);
                }
            }
        }
    }
}
