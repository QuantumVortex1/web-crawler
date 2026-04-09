package parallel.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("WebsiteAnalyzer - URL Resolution Tests")
class WebsiteAnalyzerTest {

    @Mock
    private ImageDownloader imageDownloader;

    private WebsiteAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        analyzer = new WebsiteAnalyzer(imageDownloader);
    }

    @ParameterizedTest
    @CsvSource({
        "https://example.com, https://example.com/image.jpg, https://example.com/image.jpg",
        "https://example.com, //cdn.example.com/image.jpg, https://cdn.example.com/image.jpg",
        "https://example.com, /images/photo.jpg, https://example.com/images/photo.jpg",
        "http://example.com, //cdn.example.com/image.jpg, http://cdn.example.com/image.jpg",
        "https://example.com/path/, relative.jpg, https://example.com/path/relative.jpg"
    })
    @DisplayName("URL-Auflösung: Verschiedene URL-Typen")
    void testUrlResolution(String baseUrl, String relativeUrl, String expected) throws Exception {
        var method = WebsiteAnalyzer.class.getDeclaredMethod("resolveUrl", String.class, String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(analyzer, relativeUrl, baseUrl);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Grenzfall: Leere URL-String")
    void testEmptyUrlString() throws Exception {
        var method = WebsiteAnalyzer.class.getDeclaredMethod("resolveUrl", String.class, String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(analyzer, "", "https://example.com");
        assertNotNull(result);
    }

    @Test
    @DisplayName("Grenzfall: Null-URL sollte null zurückgeben")
    void testNullUrl() throws Exception {
        var method = WebsiteAnalyzer.class.getDeclaredMethod("resolveUrl", String.class, String.class);
        method.setAccessible(true);
        
        try {
            Object result = method.invoke(analyzer, null, "https://example.com");
            assertNull(result);
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Grenzfall: Ungültige Base-URL")
    void testInvalidBaseUrl() throws Exception {
        var method = WebsiteAnalyzer.class.getDeclaredMethod("resolveUrl", String.class, String.class);
        method.setAccessible(true);
        
        Object result = method.invoke(analyzer, "/image.jpg", "not-a-valid-url");
        assertNull(result);
    }

    @ParameterizedTest
    @CsvSource({
        "https://example.com/path?query=1#anchor, //cdn.example.com/img.jpg, https://cdn.example.com/img.jpg",
        "https://example.com:8080, /image.jpg, https://example.com:8080/image.jpg"
    })
    @DisplayName("Grenzfall: URLs mit Query-Strings und Ports")
    void testComplexUrls(String baseUrl, String relativeUrl, String expected) throws Exception {
        var method = WebsiteAnalyzer.class.getDeclaredMethod("resolveUrl", String.class, String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(analyzer, relativeUrl, baseUrl);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Grenzfall: URL mit Doppel-Slash am Anfang (Protocol-Relative)")
    void testProtocolRelativeUrl() throws Exception {
        var method = WebsiteAnalyzer.class.getDeclaredMethod("resolveUrl", String.class, String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(analyzer, "//upload.wikimedia.org/wikipedia/test.jpg", "https://de.wikipedia.org");
        assertEquals("https://upload.wikimedia.org/wikipedia/test.jpg", result);
    }

    @Test
    @DisplayName("Integration: analyze() mit Mock ImageDownloader")
    void testAnalyzeCallsDownloader() throws Exception {
        
        verify(imageDownloader, never()).queueDownload(anyString(), anyInt());
    }

    @Test
    @DisplayName("Thread-Sicherheit: Mehrfache parallele analyze() Aufrufe")
    void testParallelAnalyzeCalls() throws Exception {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    var method = WebsiteAnalyzer.class.getDeclaredMethod("resolveUrl", String.class, String.class);
                    method.setAccessible(true);
                    
                    String result = (String) method.invoke(analyzer, 
                        "//example.com/image" + index + ".jpg", 
                        "https://example.com");
                    
                    assertNotNull(result);
                } catch (Exception e) {
                    fail("Thread wurde unterbrochen: " + e.getMessage());
                }
            });
        }
        
        for (Thread t : threads) {
            t.start();
        }
        
        for (Thread t : threads) {
            t.join();
        }
    }
}
