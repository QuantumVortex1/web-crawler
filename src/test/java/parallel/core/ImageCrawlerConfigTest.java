package parallel.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ImageCrawlerConfig Tests")
class ImageCrawlerConfigTest {

    private static final Path TEST_DOWNLOAD_PATH = Paths.get("test-downloads");

    @BeforeEach
    void setUp() throws Exception {
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
    @DisplayName("Konstruktor mit benutzerdefinierten Werten")
    void testConstructorWithCustomValues() {
        int websiteScans = 8;
        int imageDownloads = 16;
        
        ImageCrawlerConfig config = new ImageCrawlerConfig(websiteScans, imageDownloads, TEST_DOWNLOAD_PATH);
        
        assertEquals(websiteScans, config.getNumberOfAllowedParallelWebsiteScans());
        assertEquals(imageDownloads, config.getNumberOfAllowedParallelImageDownloads());
        assertEquals(TEST_DOWNLOAD_PATH, config.getDownloadPath());
    }

    @Test
    @DisplayName("Konstruktor mit Default-Werten")
    void testConstructorWithDefaults() {
        ImageCrawlerConfig config = new ImageCrawlerConfig();
        
        assertEquals(4, config.getNumberOfAllowedParallelWebsiteScans());
        assertEquals(8, config.getNumberOfAllowedParallelImageDownloads());
        assertEquals(Paths.get("downloads"), config.getDownloadPath());
    }

    @Test
    @DisplayName("Factory-Methode createDefault() - alternative Erstellung")
    void testDefaultCreation() {
        ImageCrawlerConfig config = new ImageCrawlerConfig();
        
        assertEquals(4, config.getNumberOfAllowedParallelWebsiteScans());
        assertEquals(8, config.getNumberOfAllowedParallelImageDownloads());
        assertEquals(Paths.get("downloads"), config.getDownloadPath());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 10, 32, 100})
    @DisplayName("Thread-Pools mit verschiedenen Größen")
    void testVariousPoolSizes(int poolSize) {
        ImageCrawlerConfig config = new ImageCrawlerConfig(poolSize, poolSize, TEST_DOWNLOAD_PATH);
        
        assertEquals(poolSize, config.getNumberOfAllowedParallelWebsiteScans());
        assertEquals(poolSize, config.getNumberOfAllowedParallelImageDownloads());
    }

    @Test
    @DisplayName("toString() Methode")
    void testToString() {
        ImageCrawlerConfig config = new ImageCrawlerConfig(4, 8, TEST_DOWNLOAD_PATH);
        String str = config.toString();
        
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }

    @Test
    @DisplayName("Grenzfall: Pool-Größe = 0 (sollte akzeptiert werden)")
    void testZeroPoolSize() {
        ImageCrawlerConfig config = new ImageCrawlerConfig(0, 0, TEST_DOWNLOAD_PATH);
        
        assertEquals(0, config.getNumberOfAllowedParallelWebsiteScans());
        assertEquals(0, config.getNumberOfAllowedParallelImageDownloads());
    }

    @Test
    @DisplayName("Grenzfall: Negative Pool-Größe (Edge Case)")
    void testNegativePoolSize() {
        ImageCrawlerConfig config = new ImageCrawlerConfig(-1, -1, TEST_DOWNLOAD_PATH);
        
        assertEquals(-1, config.getNumberOfAllowedParallelWebsiteScans());
        assertEquals(-1, config.getNumberOfAllowedParallelImageDownloads());
    }

    @Test
    @DisplayName("Grenzfall: Null Download-Pfad")
    void testNullDownloadPath() {
        ImageCrawlerConfig config = new ImageCrawlerConfig(4, 8, null);
        
        assertNull(config.getDownloadPath());
    }

    @Test
    @DisplayName("Default-Konstruktor Tests")
    void testDefaultConstructor() {
        ImageCrawlerConfig config = new ImageCrawlerConfig();
        
        assertNotNull(config.getDownloadPath());
        assertEquals(4, config.getNumberOfAllowedParallelWebsiteScans());
        assertEquals(8, config.getNumberOfAllowedParallelImageDownloads());
    }
}
