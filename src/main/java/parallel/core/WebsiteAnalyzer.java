package parallel.core;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.net.URI;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Analysiert Website-HTML und extrahiert Bild-URIs aus img-Tags.
 * Delegiert Downloads an ImageDownloader.
 */
public class WebsiteAnalyzer {

    private static final Logger LOGGER = Logger.getLogger(WebsiteAnalyzer.class.getName());
    private static final int TIMEOUT_MS = 10000;
    private final ImageDownloader imageDownloader;

    /**
     * Erstellt einen neuen WebsiteAnalyzer.
     *
     * @param imageDownloader der ImageDownloader für delegierte Downloads
     */
    public WebsiteAnalyzer(ImageDownloader imageDownloader) {
        this.imageDownloader = imageDownloader;
    }

    /**
     * Analysiert die Website unter der angegebenen URI und extrahiert Bild-Links.
     *
     * @param uri die zu analysierende Website-URI
     * @param folderNum die Ordnernummer für die Downloads dieser URL
     */
    public void analyze(URI uri, int folderNum) {
        try {
            String baseUrl = uri.toString();
            Document doc = Jsoup.connect(baseUrl)
                .timeout(TIMEOUT_MS)
                .get();

            Elements imageElements = doc.select("img[src]");
            
            for (Element imgTag : imageElements) {
                String srcAttribute = imgTag.attr("src");
                if (srcAttribute != null && !srcAttribute.trim().isEmpty()) {
                    String absoluteUrl = resolveUrl(srcAttribute, baseUrl);
                    if (absoluteUrl != null && !absoluteUrl.isEmpty()) {
                        imageDownloader.queueDownload(absoluteUrl, folderNum);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Fehler beim Analysieren von " + uri + ": " + e.getMessage());
        }
    }

    /**
     * Konvertiert relative URLs zu absoluten URLs.
     *
     * @param url die möglicherweise relative URL
     * @param baseUrl die Basis-URL
     * @return die absolute URL
     */
    private String resolveUrl(String url, String baseUrl) {
        try {
            if (url == null || url.trim().isEmpty()) {
                return null;
            }

            if (url.startsWith("http://") || url.startsWith("https://")) {
                return url;
            }

            URL base = new URL(baseUrl);
            if (url.startsWith("//")) {
                return base.getProtocol() + ":" + url;
            }
            URL resolved = new URL(base, url);
            return resolved.toString();
        } catch (Exception e) {
            LOGGER.warning("Fehler beim Konvertieren der URL '" + url + "' mit Basis '" + baseUrl + "': " + e.getMessage());
            return null;
        }
    }
}
