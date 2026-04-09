package parallel.api;

import java.net.URI;

public interface IImageCrawler {
    void crawl(final URI uri);
    boolean isIdle();
}
