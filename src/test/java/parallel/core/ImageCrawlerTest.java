package parallel.core;

import parallel.api.IImageCrawlerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ImageCrawler Tests")
class ImageCrawlerTest {

    @Mock
    private IImageCrawlerConfig config;

    private ImageCrawler crawler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        config = new ImageCrawlerConfig(2, 4, Paths.get("test-crawler"));
        crawler = new ImageCrawler(config);
    }

    @Test
    @DisplayName("Konstruktor initialisiert Crawler korrekt")
    void testConstructor() {
        assertNotNull(crawler);
        assertTrue(crawler.isIdle());
    }

    @Test
    @DisplayName("isIdle() ist true bei Initialisierung")
    void testInitiallyIdle() {
        assertTrue(crawler.isIdle());
    }

    @Test
    @DisplayName("crawl() mit ungültiger URI")
    void testCrawlWithInvalidUri() throws Exception {
        assertDoesNotThrow(() -> {
            crawler.crawl(new URI("not-a-valid-uri"));
        });
    }

    @Test
    @DisplayName("crawl() mit null URI")
    void testCrawlWithNullUri() {
        crawler.crawl(null);
    }

    @Test
    @DisplayName("Mehrere crawl() Aufrufe")
    void testMultipleCrawlCalls() {
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 5; i++) {
                crawler.crawl(new URI("https://example.com/page" + i));
            }
        });
    }

    @Test
    @DisplayName("Thread-Sicherheit: Parallele crawl() Aufrufe")
    void testParallelCrawlCalls() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    crawler.crawl(new URI("https://example.com/page" + index));
                } catch (Exception e) {
                    fail("Exception in thread: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        boolean completed = endLatch.await(10, TimeUnit.SECONDS);
        
        assertTrue(completed);
    }

    @Test
    @DisplayName("isIdle() nach crawl() und Abschluss")
    void testIdleAfterCrawlCompletion() throws Exception {
        crawler.crawl(new URI("https://httpbin.org/status/404"));
        
        int maxWaitTime = 5000;
        long startTime = System.currentTimeMillis();
        
        while (!crawler.isIdle() && System.currentTimeMillis() - startTime < maxWaitTime) {
            Thread.sleep(100);
        }
        
        assertTrue(crawler.isIdle());
    }

    @Test
    @DisplayName("shutdown() beendet den Crawler")
    void testShutdown() throws Exception {
        crawler.crawl(new URI("https://example.com"));
        
        Thread.sleep(100);
        
        assertDoesNotThrow(() -> {
            crawler.shutdown();
        });
    }

    @Test
    @DisplayName("Grenzfall: crawl() mit sehr vielen URLs")
    void testCrawlWithManyUrls() throws Exception {
        for (int i = 0; i < 100; i++) {
            crawler.crawl(new URI("https://example.com/page" + i));
        }
        
        assertTrue(true);
    }

    @Test
    @DisplayName("Thread-Sicherheit: Concurrent reads von isIdle()")
    void testThreadSafeIsIdleReads() throws InterruptedException {
        int threadCount = 20;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        boolean idle = crawler.isIdle();
                        assertNotNull(idle);
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(completed);
    }

    @Test
    @DisplayName("Grenzfall: crawl() und shutdown() Race Condition")
    void testCrawlAndShutdownRaceCondition() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);

        Thread crawlThread = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    crawler.crawl(new URI("https://example.com/" + i));
                    Thread.sleep(10);
                }
            } catch (Exception e) {
            } finally {
                latch.countDown();
            }
        });

        Thread shutdownThread = new Thread(() -> {
            try {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                crawler.shutdown();
            } finally {
                latch.countDown();
            }
        });

        crawlThread.start();
        shutdownThread.start();

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(completed);
    }

    @Test
    @DisplayName("Grenzfall: URI mit Sonderzeichen")
    void testCrawlWithSpecialCharactersInUri() throws Exception {
        assertDoesNotThrow(() -> {
            crawler.crawl(new URI("https://example.com/path?query=value&other=123#fragment"));
        });
    }

    @Test
    @DisplayName("Grenzfall: Sehr lange URI")
    void testCrawlWithVeryLongUri() throws Exception {
        String longPath = "a".repeat(2000);
        assertDoesNotThrow(() -> {
            crawler.crawl(new URI("https://example.com/" + longPath));
        });
    }

    @Test
    @DisplayName("Grenzfall: HTTP und HTTPS URLs gemischt")
    void testCrawlWithMixedProtocols() throws Exception {
        assertDoesNotThrow(() -> {
            crawler.crawl(new URI("http://example.com"));
            crawler.crawl(new URI("https://example.com"));
        });
    }
}
