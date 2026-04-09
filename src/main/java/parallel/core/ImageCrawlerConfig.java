package parallel.core;

import parallel.api.IImageCrawlerConfig;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Konfigurationsklasse für den ImageCrawler.
 * Verwaltet Thread-Pool-Größen und Download-Pfad.
 */
public class ImageCrawlerConfig implements IImageCrawlerConfig {

    private final int numberOfAllowedParallelWebsiteScans;
    private final int numberOfAllowedParallelImageDownloads;
    private final Path downloadPath;

    /**
     * Erstellt eine neue ImageCrawlerConfig mit benutzerdefinierten Werten.
     *
     * @param numberOfAllowedParallelWebsiteScans Anzahl paralleler Website-Scans
     * @param numberOfAllowedParallelImageDownloads Anzahl paralleler Image-Downloads
     * @param downloadPath Pfad für heruntergeladene Bilder
     */
    public ImageCrawlerConfig(int numberOfAllowedParallelWebsiteScans,
                             int numberOfAllowedParallelImageDownloads,
                             Path downloadPath) {
        this.numberOfAllowedParallelWebsiteScans = numberOfAllowedParallelWebsiteScans;
        this.numberOfAllowedParallelImageDownloads = numberOfAllowedParallelImageDownloads;
        this.downloadPath = downloadPath;
    }

    /**
     * Erstellt eine ImageCrawlerConfig mit Default-Werten.
     *
     * @param downloadPath Pfad für heruntergeladene Bilder
     */
    public ImageCrawlerConfig() {
        this(4, 8, Paths.get("downloads"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfAllowedParallelWebsiteScans() {
        return numberOfAllowedParallelWebsiteScans;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfAllowedParallelImageDownloads() {
        return numberOfAllowedParallelImageDownloads;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getDownloadPath() {
        return downloadPath;
    }
}
