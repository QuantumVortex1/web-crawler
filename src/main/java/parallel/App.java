package parallel;

import parallel.api.IImageCrawler;
import parallel.api.IImageCrawlerConfig;
import parallel.core.ImageCrawler;
import parallel.core.ImageCrawlerConfig;
import java.net.URI;

/**
 * Einstiegspunkt fuer den Beispielaufruf des Image-Crawlers.
 */
public class App {

    private static final long DEFAULT_POLL_INTERVAL_MS = 500L;
    private static final String[] DEFAULT_URLS = {
        "https://de.wikipedia.org/wiki/World_Wide_Web",
        "https://www.bbc.com/news",
        "https://www.nationalgeographic.com/animals",
        "https://www.youtube.com/"
    };

    /**
     * Startet den Crawler mit Beispiel-URLs und wartet bis alle Tasks abgeschlossen sind.
     *
     * @param args Kommandozeilenargumente
     */
    public static void main(String[] args) {
        ImageCrawlerConfig config = new ImageCrawlerConfig();

        IImageCrawler crawler = new ImageCrawler(config);

        execute(crawler, config, getDefaultUrls(), DEFAULT_POLL_INTERVAL_MS);

        if (crawler instanceof ImageCrawler imageCrawler) {
            imageCrawler.shutdown();
        }
    }

    /**
     * Fuehrt den Crawler-Ablauf fuer die uebergebenen URLs aus.
     *
     * @param crawler der zu verwendende Crawler
     * @param config die Crawler-Konfiguration
     * @param urls die zu crawlenden URLs
     * @param pollIntervalMs Warteintervall in Millisekunden fuer isIdle()-Polling
     */
    static void execute(
        IImageCrawler crawler,
        IImageCrawlerConfig config,
        String[] urls,
        long pollIntervalMs
    ) {
        System.out.println("Starte Web-Crawler.");
        System.out.println("Konfiguration:");
        System.out.println(" - Parallele Website-Scans: " + config.getNumberOfAllowedParallelWebsiteScans());
        System.out.println(" - Parallele Image-Downloads: " + config.getNumberOfAllowedParallelImageDownloads());
        System.out.println(" - Download-Pfad: " + config.getDownloadPath());

        crawlUrls(crawler, urls);

        System.out.println("Warte auf Abschluss aller Tasks...");
        waitUntilIdle(crawler, pollIntervalMs);
        System.out.println("Bilder wurden in '" + config.getDownloadPath() + "' gespeichert.");
    }

    /**
     * Fuegt alle gueltigen URLs dem Crawler hinzu.
     *
     * @param crawler der zu verwendende Crawler
     * @param urls die zu crawlenden URLs
     */
    static void crawlUrls(IImageCrawler crawler, String[] urls) {
        for (String url : urls) {
            try {
                crawler.crawl(new URI(url));
                System.out.println("Crawle: " + url);
            } catch (Exception e) {
                System.err.println("Fehler beim Crawlen von " + url + ": " + e.getMessage());
            }
        }
    }

    /**
     * Wartet, bis der Crawler keine aktiven Tasks mehr hat.
     *
     * @param crawler der zu ueberwachende Crawler
     * @param pollIntervalMs Intervall in Millisekunden zwischen den Polls
     */
    static void waitUntilIdle(IImageCrawler crawler, long pollIntervalMs) {
        long effectivePollIntervalMs = Math.max(1L, pollIntervalMs);
        while (!crawler.isIdle()) {
            try {
                Thread.sleep(effectivePollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Liefert die standardmaessigen Start-URLs fuer den Beispielaufruf.
     *
     * @return defensive Kopie der Standard-URL-Liste
     */
    static String[] getDefaultUrls() {
        return DEFAULT_URLS.clone();
    }
}
