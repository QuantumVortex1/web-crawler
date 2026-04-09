package parallel.api;

import java.nio.file.Path;

/**
 * Liefert alle benoetigten Einstellungen fuer den Image-Crawler.
 */
public interface IImageCrawlerConfig {

    /**
     * Gibt die maximale Anzahl gleichzeitig laufender Website-Analysen zurueck.
     *
     * @return Anzahl paralleler Website-Scans
     */
    int getNumberOfAllowedParallelWebsiteScans();

    /**
     * Gibt die maximale Anzahl gleichzeitig laufender Bild-Downloads zurueck.
     *
     * @return Anzahl paralleler Bild-Downloads
     */
    int getNumberOfAllowedParallelImageDownloads();

    /**
     * Gibt den Zielpfad fuer heruntergeladene Bilder zurueck.
     *
     * @return Download-Verzeichnis
     */
    Path getDownloadPath();
}
