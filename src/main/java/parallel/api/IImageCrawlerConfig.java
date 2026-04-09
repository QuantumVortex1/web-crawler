package parallel.api;

import java.nio.file.Path;

public interface IImageCrawlerConfig {
    int getNumberOfAllowedParallelWebsiteScans();
    int getNumberOfAllowedParallelImageDownloads();
    Path getDownloadPath();
}
