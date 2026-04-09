package parallel.core;

import parallel.api.IImageCrawler;
import parallel.api.IImageCrawlerConfig;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Koordiniert den parallelen Web-Crawling-Prozess mit separaten Thread-Pools
 * für Website-Scans und Image-Downloads.
 */
public class ImageCrawler implements IImageCrawler {

    private static final Logger LOGGER = Logger.getLogger(ImageCrawler.class.getName());
    private final ExecutorService websiteScanExecutor;
    private final ExecutorService imageDownloadExecutor;
    private final AtomicInteger folderCounter;
    private final AtomicInteger activeWebsiteTasks;
    private final AtomicInteger activeDownloadTasks;
    private final WebsiteAnalyzer websiteAnalyzer;
    private final ImageDownloader imageDownloader;

    /**
     * Erstellt einen neuen ImageCrawler mit separaten ExecutorServices für
     * Website-Scans und Image-Downloads.
     *
     * @param config die Konfiguration mit Pool-Größen und Download-Pfad
     */
    public ImageCrawler(IImageCrawlerConfig config) {
        this.folderCounter = new AtomicInteger(0);
        this.activeWebsiteTasks = new AtomicInteger(0);
        this.activeDownloadTasks = new AtomicInteger(0);

        this.websiteScanExecutor = Executors.newFixedThreadPool(
            config.getNumberOfAllowedParallelWebsiteScans()
        );
        this.imageDownloadExecutor = Executors.newFixedThreadPool(
            config.getNumberOfAllowedParallelImageDownloads()
        );

        this.imageDownloader = new ImageDownloader(config, activeDownloadTasks, 
                                                   imageDownloadExecutor);
        this.websiteAnalyzer = new WebsiteAnalyzer(imageDownloader);
    }

    @Override
    public void crawl(final URI uri) {
        activeWebsiteTasks.incrementAndGet();
        websiteScanExecutor.submit(() -> {
            try {
                int folderNum = folderCounter.incrementAndGet();
                websiteAnalyzer.analyze(uri, folderNum);
            } catch (Exception e) {
                LOGGER.warning("Fehler beim Crawling von " + uri + ": " + e.getMessage());
            } finally {
                activeWebsiteTasks.decrementAndGet();
            }
        });
    }

    @Override
    public boolean isIdle() {
        return activeWebsiteTasks.get() == 0 && activeDownloadTasks.get() == 0;
    }

    /**
     * Beendet beide ExecutorServices.
     */
    public void shutdown() {
        websiteScanExecutor.shutdown();
        imageDownloadExecutor.shutdown();
    }
}
