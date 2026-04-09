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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import parallel.api.IImageCrawlerConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@DisplayName("ImageDownloader Tests")
class ImageDownloaderTest {

    @Mock
    private IImageCrawlerConfig config;

    @TempDir
    Path tempDownloadPath;

    private AtomicInteger activeDownloadTasks;
    private ExecutorService executor;
    private ImageDownloader downloader;
    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        activeDownloadTasks = new AtomicInteger(0);
        executor = Executors.newFixedThreadPool(2);
        when(config.getDownloadPath()).thenReturn(tempDownloadPath);
        downloader = new ImageDownloader(config, activeDownloadTasks, executor);

        server = new MockWebServer();
        server.start();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if ("/images/image.jpg".equals(request.getPath())) {
                    return new MockResponse().setResponseCode(200).setBody("sample-image");
                }
                return new MockResponse().setResponseCode(404);
            }
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            server.shutdown();
        }
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("queueDownload() speichert Datei im Zielordner")
    void testQueueDownloadStoresFileInTargetFolder() throws Exception {
        String imageUrl = server.url("/images/image.jpg").toString();

        downloader.queueDownload(imageUrl, 1);

        assertTrue(waitFor(() -> activeDownloadTasks.get() == 0, 5000));

        Path storedFile = tempDownloadPath.resolve("1").resolve("image.jpg");
        assertTrue(Files.exists(storedFile));
        assertArrayEquals("sample-image".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(storedFile));
    }

    @Test
    @DisplayName("Dateikollision: zweite Datei bekommt Suffix _2")
    void testNameCollisionUsesSuffixTwo() throws Exception {
        Path targetDir = tempDownloadPath.resolve("1");
        Files.createDirectories(targetDir);
        Files.writeString(targetDir.resolve("image.jpg"), "first");

        String imageUrl = server.url("/images/image.jpg").toString();
        downloader.queueDownload(imageUrl, 1);

        assertTrue(waitFor(() -> activeDownloadTasks.get() == 0, 5000));
        assertTrue(Files.exists(targetDir.resolve("image_2.jpg")));
    }

    @Test
    @DisplayName("RejectedExecution behaelt Counter konsistent")
    void testRejectedExecutionKeepsCounterConsistent() {
        executor.shutdown();

        downloader.queueDownload("https://example.invalid/image.jpg", 1);

        assertEquals(0, activeDownloadTasks.get());
    }

    @Test
    @DisplayName("Ungueltige URL hinterlaesst keinen aktiven Download")
    void testInvalidUrlReturnsToIdleState() throws Exception {
        downloader.queueDownload("not a valid url", 1);

        assertTrue(waitFor(() -> activeDownloadTasks.get() == 0, 5000));
        assertEquals(0, countRegularFiles(tempDownloadPath));
    }

    @Test
    @DisplayName("Dateiname-Extraktion funktioniert fuer typische Pfade")
    void testFileNameExtraction() throws Exception {
        var method = ImageDownloader.class.getDeclaredMethod("extractFileName", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(downloader, "/path/to/image.jpg");
        assertEquals("image.jpg", result);
    }

    @Test
    @DisplayName("Mehrfache Kollisionen: Dateiname wird auf naechstes freies Suffix erhoeht")
    void testMultipleCollisionsUseNextFreeSuffix() throws Exception {
        Path targetDir = tempDownloadPath.resolve("1");
        Files.createDirectories(targetDir);
        Files.writeString(targetDir.resolve("image.jpg"), "first");
        Files.writeString(targetDir.resolve("image_2.jpg"), "second");

        String imageUrl = server.url("/images/image.jpg").toString();
        downloader.queueDownload(imageUrl, 1);

        assertTrue(waitFor(() -> activeDownloadTasks.get() == 0, 5000));
        assertTrue(Files.exists(targetDir.resolve("image_3.jpg")));
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

    private long countRegularFiles(Path root) throws IOException {
        if (!Files.exists(root)) {
            return 0;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile).count();
        }
    }
}
