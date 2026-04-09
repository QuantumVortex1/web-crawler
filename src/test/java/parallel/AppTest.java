package parallel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import parallel.api.IImageCrawler;
import parallel.core.ImageCrawlerConfig;

import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("App Integration Tests")
public class AppTest {

    @Test
    @DisplayName("getDefaultUrls() liefert defensive Kopie")
    void testGetDefaultUrlsReturnsDefensiveCopy() {
        String[] first = App.getDefaultUrls();
        String firstEntry = first[0];

        first[0] = "https://changed.example";
        String[] second = App.getDefaultUrls();

        assertNotSame(first, second);
        assertEquals(firstEntry, second[0]);
    }

    @Test
    @DisplayName("crawlUrls() verarbeitet nur gueltige URIs")
    void testCrawlUrlsQueuesOnlyValidUris() {
        RecordingCrawler crawler = new RecordingCrawler(0);
        String[] urls = {
            "https://example.com/a",
            "not a valid uri",
            "https://example.com/b"
        };

        App.crawlUrls(crawler, urls);

        assertEquals(2, crawler.getCrawledUris().size());
        assertEquals(URI.create("https://example.com/a"), crawler.getCrawledUris().get(0));
        assertEquals(URI.create("https://example.com/b"), crawler.getCrawledUris().get(1));
    }

    @Test
    @DisplayName("waitUntilIdle() pollt bis der Crawler idle ist")
    void testWaitUntilIdlePollsUntilIdle() {
        RecordingCrawler crawler = new RecordingCrawler(2);

        App.waitUntilIdle(crawler, 1);

        assertTrue(crawler.getIdleChecks() >= 3);
    }

    @Test
    @DisplayName("execute() startet Crawl und wartet auf idle")
    void testExecuteRunsCrawlAndWaitCycle() {
        RecordingCrawler crawler = new RecordingCrawler(1);
        ImageCrawlerConfig config = new ImageCrawlerConfig(1, 1, Paths.get("downloads"));

        App.execute(crawler, config, new String[] {"https://example.com/image-page"}, 1);

        assertEquals(1, crawler.getCrawledUris().size());
        assertTrue(crawler.getIdleChecks() >= 2);
    }

    private static final class RecordingCrawler implements IImageCrawler {

        private final List<URI> crawledUris = new ArrayList<>();
        private int idleChecks;
        private final int nonIdleChecksBeforeIdle;

        private RecordingCrawler(int nonIdleChecksBeforeIdle) {
            this.nonIdleChecksBeforeIdle = nonIdleChecksBeforeIdle;
        }

        @Override
        public synchronized void crawl(final URI uri) {
            crawledUris.add(uri);
        }

        @Override
        public synchronized boolean isIdle() {
            idleChecks++;
            return idleChecks > nonIdleChecksBeforeIdle;
        }

        private synchronized List<URI> getCrawledUris() {
            return new ArrayList<>(crawledUris);
        }

        private synchronized int getIdleChecks() {
            return idleChecks;
        }
    }
}
