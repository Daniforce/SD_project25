package search;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Random;
import java.util.Set;

public class RobotWorker implements Runnable {
    private final Index index;
    private final String id;
    private volatile boolean running;
    private volatile boolean isRunning;
    
    private static final Random random = new Random();
    private static final Set<String> EXCLUDED_URLS = Set.of(
        "facebook.com", "twitter.com", "instagram.com", "linkedin.com",
        "youtube.com", "tiktok.com", "reddit.com", "whatsapp.com");
    
    public RobotWorker(Index index, String id) {
        this.index = index;
        this.id = id;
        this.running = true;
        this.isRunning = true;
    }
    
    public String getId() {
        return id;
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public void stop() {
        running = false;
        isRunning = false;
    }
    
    private String getDomain(String url) {
        try {
            String host = Jsoup.connect(url).request().url().getHost().toLowerCase();
            if (host == null || host.isEmpty()) return "unknown";
            if (host.startsWith("www.")) host = host.substring(4);
            return host;
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private boolean isIndexableURL(String url) {
        try {
            String domain = Jsoup.connect(url).request().url().getHost().toLowerCase();
            return EXCLUDED_URLS.stream().noneMatch(domain::contains);
        } catch (Exception e) {
            return false;
        }
    }
    
    private void respectfulDelay(String domain) {
        try {
            int delay = index.getRecommendedDelay(domain);
            Thread.sleep(delay + random.nextInt(3000));
        } catch (Exception e) {
            try {
                Thread.sleep(2000 + random.nextInt(3000));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    @Override
    public void run() {
        System.out.println(id + " started");
        
        try {
            while (running) {
                try {
                    String url = index.takeNext();
                    System.out.println(id + " processing: " + url);
                    
                    if (!isIndexableURL(url)) {
                        System.out.println(id + " skipped undesirable URL: " + url);
                        continue;
                    }
                    
                    String domain = getDomain(url);
                    Document doc;
                    
                    try {
                        doc = Jsoup.connect(url)
                            .timeout(10000)
                            .userAgent("Mozilla/5.0 (compatible; AcademicBot/1.0;)")
                            .followRedirects(true)
                            .ignoreHttpErrors(true)
                            .ignoreContentType(true)
                            .get();
                        
                        respectfulDelay(domain);
                        index.reportSuccess(domain);
                    } catch (org.jsoup.HttpStatusException e) {
                        if (e.getStatusCode() == 429) {
                            System.out.println(id + " rate limited for domain: " + domain);
                            index.reportRateLimit(domain);
                            Thread.sleep(30000);
                        }
                        continue;
                    } catch (IOException e) {
                        System.err.println(id + " IO error: " + e.getMessage());
                        continue;
                    }
                    
                    // Index content
                    String text = doc.text();
                    String[] words = text.split("[ ,!\n;.()?:]");
                    for (String word : words) {
                        if (!word.trim().isEmpty()) {
                            index.addToIndex(word, url);
                        }
                    }
                    
                    // Discover new URLs
                    Elements urls = doc.select("a[href]");
                    for (Element link : urls) {
                        String foundURL = link.attr("abs:href");
                        if (foundURL.startsWith("http")) {
                            index.putNew(foundURL);
                        }
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (running) {
                        System.err.println(id + " error: " + e.getMessage());
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        } finally {
            isRunning = false;
            System.out.println(id + " stopped");
        }
    }
}