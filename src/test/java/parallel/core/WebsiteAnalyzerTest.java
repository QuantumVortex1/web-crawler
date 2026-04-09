package parallel.core;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("WebsiteAnalyzer Tests")
class WebsiteAnalyzerTest {

    @Mock
    private ImageDownloader imageDownloader;

    private WebsiteAnalyzer analyzer;
    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        analyzer = new WebsiteAnalyzer(imageDownloader);

        server = new MockWebServer();
        server.start();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if ("/page.html".equals(request.getPath())) {
                    String html = "<html><body>"
                        + "<img src='/img/root.jpg'>"
                        + "<img src='img/relative.jpg'>"
                        + "<img src='https://images.example/absolute.jpg'>"
                        + "<img src=''>"
                        + "</body></html>";
                    return new MockResponse().setResponseCode(200).setBody(html);
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
    }

    @Test
    @DisplayName("analyze() extrahiert Bild-URLs und delegiert an Downloader")
    void testAnalyzeDelegatesResolvedImageUrls() {
        URI pageUri = URI.create(server.url("/page.html").toString());

        analyzer.analyze(pageUri, 7);

        verify(imageDownloader).queueDownload(eq(server.url("/img/root.jpg").toString()), eq(7));
        verify(imageDownloader).queueDownload(eq(server.url("/img/relative.jpg").toString()), eq(7));
        verify(imageDownloader).queueDownload(eq("https://images.example/absolute.jpg"), eq(7));
        verify(imageDownloader, never()).queueDownload(eq(""), eq(7));
    }

    @Test
    @DisplayName("resolveUrl(): Root-relative URL behaelt Host und Port")
    void testResolveUrlKeepsPortForRootRelativePath() throws Exception {
        var method = WebsiteAnalyzer.class.getDeclaredMethod("resolveUrl", String.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(analyzer, "/img/picture.jpg", "http://localhost:8087/page.html");
        assertEquals("http://localhost:8087/img/picture.jpg", result);
    }

    @Test
    @DisplayName("resolveUrl(): null oder leer ergibt null")
    void testResolveUrlWithNullAndEmpty() throws Exception {
        var method = WebsiteAnalyzer.class.getDeclaredMethod("resolveUrl", String.class, String.class);
        method.setAccessible(true);

        Object nullResult = method.invoke(analyzer, null, "https://example.com");
        Object emptyResult = method.invoke(analyzer, " ", "https://example.com");

        assertNull(nullResult);
        assertNull(emptyResult);
    }
}
