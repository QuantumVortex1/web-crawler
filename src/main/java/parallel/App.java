package parallel;

import parallel.api.IImageCrawler;
import parallel.core.ImageCrawler;
import parallel.core.ImageCrawlerConfig;
import java.net.URI;

public class App {
    public static void main(String[] args) {
        ImageCrawlerConfig config = new ImageCrawlerConfig();

        IImageCrawler crawler = new ImageCrawler(config);

        String[] urls = {
            "https://de.wikipedia.org/wiki/World_Wide_Web"
        };

        System.out.println("Starte Web-Crawler.");
        System.out.println("Konfiguration:");
        System.out.println(" - Parallele Website-Scans: " + config.getNumberOfAllowedParallelWebsiteScans());
        System.out.println(" - Parallele Image-Downloads: " + config.getNumberOfAllowedParallelImageDownloads());
        System.out.println(" - Download-Pfad: " + config.getDownloadPath());

        for (String url : urls) {
            try {
                crawler.crawl(new URI(url));
                System.out.println("Crawle: " + url);
            } catch (Exception e) {
                System.err.println("Fehler beim Crawlen von " + url + ": " + e.getMessage());
            }
        }

        System.out.println("Warte auf Abschluss aller Tasks...");
        while (!crawler.isIdle()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("Bilder wurden in '" + config.getDownloadPath() + "' gespeichert.");

        if (crawler instanceof ImageCrawler) {
            ((ImageCrawler) crawler).shutdown();
        }
    }
}
