package search;

import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.util.Random;
import java.util.Set;

// import java.util.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

public class Robot {

    private static final Random random = new Random();

    private static final Set<String> EXCLUDED_URLS = Set.of(
        "facebook.com", "twitter.com", "instagram.com", "linkedin.com",
        "youtube.com", "tiktok.com", "reddit.com", "whatsapp.com" );

    

    private String getDomain(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "unknown";
        }
        
        try {
            // Use Jsoup's built-in URL parsing (which is what connect() uses internally)
            String host = Jsoup.connect(url).request().url().getHost().toLowerCase();
            
            if (host == null || host.isEmpty()) {
                return "unknown";
            }
            
            host = host.toLowerCase();
            
            // Remove www. prefix if present
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            
            return host;
            
        } catch (Exception e) {
            // Final fallback
            return "unknown";
        }
    }

    private static Connection establishConnection(String url){
        return Jsoup.connect(url)
        .timeout(10000)
        .userAgent("Mozilla/5.0 (compatible; AcademicBot/1.0;)")
        .followRedirects(true)
        .ignoreHttpErrors(true)
        .ignoreContentType(true);
    }

    private static boolean isIndexableURL(String url){ // Scan for social media and other undesired URLs in the connection to be made next
        try
        {
            String domain = Jsoup.connect(url).request().url().getHost().toLowerCase();
            return EXCLUDED_URLS.stream().noneMatch(domain::contains); // See if it's not a desired URL
        }
        catch(Exception e) {
            return false;
        }
    }

    private void stopWaitAMinute(Index index, String domain){
        try { // Ramdom delay betwenn requests to the websites between 2 - 5 seconds

            Thread.sleep(index.getRecommendedDelay(domain) + random.nextInt(3000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch(RemoteException e){
            System.err.println("Failed to get recommended delay: " + e.getMessage());
            // Fallback delay
            try {
                Thread.sleep(2000 + random.nextInt(3000));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        
    }

    public void main(String[] args) {
        try {
            Index index = (Index) LocateRegistry.getRegistry(8184).lookup("index");
            while (true) {
                String url = index.takeNext();
                System.out.println(url);

                if(!isIndexableURL(url)){
                    System.out.println("Found an undesiable url: " + url + "\nSkipping to the next...\n");
                    continue;
                }

                Document doc;
                String domain = getDomain(url);
                System.out.println(domain);
                if(domain == null)System.out.println("Im a big bitch\n");

                try {
                    doc = establishConnection(url).get();
                    stopWaitAMinute(index,domain); // Respectfull delay to the urls request
                    index.reportSuccess(domain);
                }   
                catch(HttpStatusException e) {

                    int statsCode = e.getStatusCode();
                    
                    if(statsCode == 429){

                        System.out.println("Requesting too quickly, waiting a bit more");
                        index.reportRateLimit(domain);
                        Thread.sleep(30000);
                    }
                    continue;
                }
                catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                String text = doc.text();
                String[] words = text.split("[ ,!\n;.()?:]");

                for(String word: words){
                    index.addToIndex(word, url);
                }

                Elements urls = doc.select("a[href]");

                for(Element link: urls){
                    String foundURL = link.attr("abs:href");
                    index.putNew(foundURL);
                }


                // System.out.println(doc);
                //Todo: Read JSOUP documentation and parse the html to index the keywords. 
                //Then send back to server via index.addToIndex(...)
            }
        } catch (AccessException e) {
            e.printStackTrace();
        } catch (RemoteException e){
            e.printStackTrace();
        } catch (NotBoundException e){
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
