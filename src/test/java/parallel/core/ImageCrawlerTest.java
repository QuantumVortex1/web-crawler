package parallel.core;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("ImageCrawler Tests")
class ImageCrawlerTest {

    @TempDir
    Path tempDownloadPath;

    private ImageCrawler crawler;
    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if ("/page1.html".equals(path)) {
                    String html = "<html><body><img src='/img/first.jpg'><img src='/img/second.jpg'></body></html>";
                    return new MockResponse().setResponseCode(200).setBody(html);
                }
                if ("/page2.html".equals(path)) {
                    String html = "<html><body><img src='/img/third.jpg'></body></html>";
                    return new MockResponse().setResponseCode(200).setBody(html);
                }
                if ("/slow-page.html".equals(path)) {
                    String html = "<html><body><img src='/img/slow.jpg'></body></html>";
                    return new MockResponse()
                        .setResponseCode(200)
                        .setBody(html)
                        .setBodyDelay(350, TimeUnit.MILLISECONDS);
                }
                if (path != null && path.startsWith("/img/")) {
                    return new MockResponse().setResponseCode(200).setBody("img-bytes");
                }
                return new MockResponse().setResponseCode(404);
            }
        });

        crawler = new ImageCrawler(new ImageCrawlerConfig(1, 4, tempDownloadPath));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (crawler != null) {
            crawler.shutdown();
        }
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    @DisplayName("Konstruktor initialisiert Crawler korrekt")
    void testConstructor() {
        assertNotNull(crawler);
        assertTrue(crawler.isIdle());
    }

    @Test
    @DisplayName("crawl() laedt Bilder in nummerierte Unterordner")
    void testCrawlDownloadsImagesIntoNumberedFolders() throws Exception {
        URI page1 = URI.create(server.url("/page1.html").toString());
        URI page2 = URI.create(server.url("/page2.html").toString());

        crawler.crawl(page1);
        crawler.crawl(page2);

        assertTrue(waitFor(crawler::isIdle, 10000));

        assertTrue(Files.exists(tempDownloadPath.resolve("1").resolve("first.jpg")));
        assertTrue(Files.exists(tempDownloadPath.resolve("1").resolve("second.jpg")));
        assertTrue(Files.exists(tempDownloadPath.resolve("2").resolve("third.jpg")));
    }

    @Test
    @DisplayName("isIdle() meldet busy waehrend Verarbeitung und danach idle")
    void testIsIdleTransition() throws Exception {
        URI slowPage = URI.create(server.url("/slow-page.html").toString());

        crawler.crawl(slowPage);

        assertTrue(waitFor(() -> !crawler.isIdle(), 3000));
        assertTrue(waitFor(crawler::isIdle, 10000));
    }

    @Test
    @DisplayName("Thread-Sicherheit: viele parallele crawl() Aufrufe")
    void testParallelCrawlCalls() throws Exception {
        URI page1 = URI.create(server.url("/page1.html").toString());

        int callCount = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(callCount);
        AtomicReference<Throwable> threadFailure = new AtomicReference<>();

        for (int i = 0; i < callCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    crawler.crawl(page1);
                } catch (Exception e) {
                    threadFailure.compareAndSet(null, e);
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(endLatch.await(5, TimeUnit.SECONDS));
        if (threadFailure.get() != null) {
            fail("Paralleler crawl-Aufruf war nicht erfolgreich: " + threadFailure.get().getMessage());
        }
        assertTrue(waitFor(crawler::isIdle, 15000));

        try (Stream<Path> dirs = Files.list(tempDownloadPath)) {
            long folderCount = dirs.filter(Files::isDirectory).count();
            assertEquals(callCount, folderCount);
        }
    }

    @Test
    @DisplayName("crawl() nach shutdown haelt Idle-State konsistent")
    void testCrawlAfterShutdownKeepsIdleStateConsistent() {
        URI page1 = URI.create(server.url("/page1.html").toString());

        crawler.shutdown();
        crawler.crawl(page1);

        assertTrue(crawler.isIdle());
    }

    private boolean waitFor(BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(25);
        }
        return condition.getAsBoolean();
    }
}
