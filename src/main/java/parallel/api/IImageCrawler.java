package parallel.api;

import java.net.URI;

/**
 * Definiert die grundlegenden Operationen eines Image-Crawlers.
 */
public interface IImageCrawler {

    /**
     * Fuegt eine weitere Website zur Crawl-Warteschlange hinzu.
     *
     * @param uri die zu analysierende Website-URI
     */
    void crawl(final URI uri);

    /**
     * Prueft, ob aktuell weder Website-Analysen noch Bild-Downloads laufen.
     *
     * @return true, wenn der Crawler aktuell keine aktive Arbeit ausfuehrt
     */
    boolean isIdle();
}
