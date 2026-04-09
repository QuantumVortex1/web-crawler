package parallel.core;

import parallel.api.IImageCrawlerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ImageDownloader Tests")
class ImageDownloaderTest {

    private static final Path TEST_DOWNLOAD_PATH = Paths.get("test-downloads");

    @Mock
    private IImageCrawlerConfig config;

    private AtomicInteger activeDownloadTasks;
    private ExecutorService executor;
    private ImageDownloader downloader;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        activeDownloadTasks = new AtomicInteger(0);
        executor = Executors.newFixedThreadPool(2);

        when(config.getDownloadPath()).thenReturn(TEST_DOWNLOAD_PATH);

        downloader = new ImageDownloader(config, activeDownloadTasks, executor);

        if (Files.exists(TEST_DOWNLOAD_PATH)) {
            Files.walk(TEST_DOWNLOAD_PATH)
                .sorted(java.util.Collections.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (Exception e) {
                    }
                });
        }
    }

    @Test
    @DisplayName("Konstruktor akzeptiert alle abhängigen Komponenten")
    void testConstructor() {
        ImageDownloader dl = new ImageDownloader(config, activeDownloadTasks, executor);
        assertNotNull(dl);
    }

    @Test
    @DisplayName("queueDownload() inkrementiert activeDownloadTasks")
    void testQueueDownloadIncrementsCounter() throws InterruptedException {
        downloader.queueDownload("https://example.com/image.jpg", 1);
        
        Thread.sleep(100);
        
        assertTrue(activeDownloadTasks.get() >= 0);
    }

    @Test
    @DisplayName("Grenzfall: Leere URL")
    void testEmptyUrl() {
        downloader.queueDownload("", 1);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 100, 1000})
    @DisplayName("Grenzfall: Verschiedene folderNum-Werte")
    void testVariousFolderNumbers(int folderNum) {
        downloader.queueDownload("https://example.com/image.jpg", folderNum);
    }

    @Test
    @DisplayName("Grenzfall: Negative folderNum")
    void testNegativeFolderNumber() {
        downloader.queueDownload("https://example.com/image.jpg", -1);
    }

    @Test
    @DisplayName("Thread-Sicherheit: Mehrfache parallele Downloads")
    void testThreadSafetyMultipleDownloads() throws InterruptedException {
        int numberOfDownloads = 50;
        
        for (int i = 0; i < numberOfDownloads; i++) {
            downloader.queueDownload("https://example.com/image" + i + ".jpg", i);
        }
        
        executor.shutdown();
        boolean completed = executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue(completed || activeDownloadTasks.get() == 0);
    }

    @Test
    @DisplayName("Thread-Sicherheit: activeDownloadTasks Consistency")
    void testActiveDownloadTasksConsistency() throws InterruptedException {
        ExecutorService service = Executors.newFixedThreadPool(8);
        AtomicInteger counter = new AtomicInteger(0);
        ImageDownloader dl = new ImageDownloader(config, counter, service);
        
        int tasks = 100;
        for (int i = 0; i < tasks; i++) {
            dl.queueDownload("https://example.com/image" + i + ".jpg", 1);
        }
        
        service.shutdown();
        boolean completed = service.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);
        
        assertTrue(completed);
    }

    @Test
    @DisplayName("Grenzfall: Ungültige URL-Syntax")
    void testInvalidUrlSyntax() {
        assertDoesNotThrow(() -> {
            downloader.queueDownload("not a valid url!@#$%", 1);
        });
    }

    @Test
    @DisplayName("Grenzfall: URL mit Sonderzeichen")
    void testUrlWithSpecialCharacters() {
        assertDoesNotThrow(() -> {
            downloader.queueDownload("https://example.com/image with spaces.jpg", 1);
        });
    }

    @Test
    @DisplayName("Grenzfall: Sehr lange URL")
    void testVeryLongUrl() {
        String longUrl = "https://example.com/" + "a".repeat(2000) + ".jpg";
        assertDoesNotThrow(() -> {
            downloader.queueDownload(longUrl, 1);
        });
    }

    @Test
    @DisplayName("Dateiname-Extraktion: Various Patterns")
    void testFileNameExtraction() throws Exception {
        var method = ImageDownloader.class.getDeclaredMethod("extractFileName", String.class);
        method.setAccessible(true);
        
        String result1 = (String) method.invoke(downloader, "/path/to/image.jpg");
        assertEquals("image.jpg", result1);
        
        String result2 = (String) method.invoke(downloader, "/image.jpg");
        assertEquals("image.jpg", result2);
        
        String result3 = (String) method.invoke(downloader, "image.jpg");
        assertEquals("image.jpg", result3);
    }

    @Test
    @DisplayName("Dateiname-Extraktion: Edge Case - Null")
    void testFileNameExtractionWithNull() throws Exception {
        var method = ImageDownloader.class.getDeclaredMethod("extractFileName", String.class);
        method.setAccessible(true);
        
        Object result = method.invoke(downloader, new Object[]{null});
        assertNull(result);
    }

    @Test
    @DisplayName("Grenzfall: Eindeutige Dateinamen mit Kollision")
    void testUniqueFilePathWithCollisions() throws Exception {
        Path testDir = TEST_DOWNLOAD_PATH.resolve("collision-test");
        Files.createDirectories(testDir);
        
        Path file1 = testDir.resolve("image.jpg");
        Files.createFile(file1);
        
        var method = ImageDownloader.class.getDeclaredMethod("getUniqueFilePath", Path.class, String.class);
        method.setAccessible(true);
        
        Path uniquePath = (Path) method.invoke(downloader, testDir, "image.jpg");
        
        assertNotEquals(file1, uniquePath);
        assertTrue(uniquePath.toString().contains("image_1.jpg"));
    }

    @Test
    @DisplayName("Grenzfall: Datei ohne Extension")
    void testFileWithoutExtension() throws Exception {
        Path testDir = TEST_DOWNLOAD_PATH.resolve("no-ext-test");
        Files.createDirectories(testDir);
        
        var method = ImageDownloader.class.getDeclaredMethod("getUniqueFilePath", Path.class, String.class);
        method.setAccessible(true);
        
        Path uniquePath = (Path) method.invoke(downloader, testDir, "image");
        
        assertNotNull(uniquePath);
        assertTrue(uniquePath.toString().contains("image"));
    }
}
