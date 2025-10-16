import java.rmi.registry.*;
import java.util.*;

public class client {
    public static void main(String[] args) throws Exception {
        Index index = (Index) LocateRegistry.getRegistry(8183).lookup("index");
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("Pesquisar: ");
            String word = sc.nextLine();
            List<String> results = index.searchWord(word);
            System.out.println("Resultados:");
            for (String url : results) {
                System.out.println(" - " + url);
            }
        }
    }
}