package parallel.core;

import parallel.api.IImageCrawlerConfig;
import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Verantwortlich für das Herunterladen und Speichern von Bildern.
 * Behandelt Dateiname-Kollisionen durch Suffix-Anhang.
 */
public class ImageDownloader {

    private static final Logger LOGGER = Logger.getLogger(ImageDownloader.class.getName());
    private static final int TIMEOUT_MS = 10000;
    private final IImageCrawlerConfig config;
    private final AtomicInteger activeDownloadTasks;
    private final ExecutorService downloadExecutor;

    /**
     * Erstellt einen neuen ImageDownloader mit externem ExecutorService.
     *
     * @param config die Crawl-Konfiguration mit Download-Pfad
     * @param activeDownloadTasks AtomicInteger zum Tracking der aktiven Download-Tasks
     * @param downloadExecutor der ExecutorService für Download-Tasks
     */
    public ImageDownloader(IImageCrawlerConfig config, AtomicInteger activeDownloadTasks, 
                          ExecutorService downloadExecutor) {
        this.config = config;
        this.activeDownloadTasks = activeDownloadTasks;
        this.downloadExecutor = downloadExecutor;
    }

    /**
     * Reiht einen Image-Download in die Warteschlange ein.
     *
     * @param imageUrl die herunterladende Image-URL
     * @param folderNum die Ordnernummer für das Ziel-Verzeichnis
     */
    public void queueDownload(String imageUrl, int folderNum) {
        activeDownloadTasks.incrementAndGet();
        try {
            downloadExecutor.submit(() -> {
                try {
                    download(imageUrl, folderNum);
                } finally {
                    activeDownloadTasks.decrementAndGet();
                }
            });
        } catch (RejectedExecutionException e) {
            activeDownloadTasks.decrementAndGet();
            LOGGER.warning("Download-Task konnte nicht eingeplant werden (Executor beendet): " + imageUrl);
        }
    }

    /**
     * Lädt ein Bild herunter und speichert es mit Kollisionsprüfung.
     *
     * @param imageUrl die herunterladende Image-URL
     * @param folderNum die Ordnernummer
     */
    private void download(String imageUrl, int folderNum) {
        try {
            URI imageUri = new URI(imageUrl);
            Path targetDir = config.getDownloadPath().resolve(String.valueOf(folderNum));
            Files.createDirectories(targetDir);

            String fileName = extractFileName(imageUri.getPath());
            if (fileName == null || fileName.isEmpty()) {
                fileName = "image_" + System.currentTimeMillis();
            }

            downloadWithUniqueName(imageUri, targetDir, fileName);

        } catch (Exception e) {
            LOGGER.warning("Fehler beim Download von " + imageUrl + ": " + e.getMessage());
        }
    }

    /**
     * Extrahiert den Dateinamen aus einem URL-Pfad.
     *
     * @param path der URL-Pfad
     * @return der Dateiname oder null
     */
    private String extractFileName(String path) {
        if (path == null || path.isEmpty()) return null;
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    /**
     * Findet einen eindeutigen Dateipfad, indem bei Bedarf ein Suffix angehängt wird.
     *
     * @param imageUri die Bild-URI
     * @param directory das Zielverzeichnis
     * @param fileName der gewuenschte Dateiname
     * @throws IOException bei I/O-Fehlern
     */
    private void downloadWithUniqueName(URI imageUri, Path directory, String fileName)
        throws IOException {
        String nameWithoutExt = fileName;
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            nameWithoutExt = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }

        int counter = 1;
        while (true) {
            String candidateName = counter == 1
                ? fileName
                : nameWithoutExt + "_" + counter + extension;
            Path targetPath = directory.resolve(candidateName);
            try {
                downloadFile(imageUri, targetPath);
                LOGGER.info("Image gespeichert: " + targetPath);
                return;
            } catch (FileAlreadyExistsException ignored) {
                // Another thread wrote this name first; try the next suffix.
                counter++;
            }
        }
    }

    /**
     * Laedt eine Datei von der Bild-URI herunter und speichert sie lokal.
     *
     * @param imageUri die Bild-URI
     * @param targetPath der Dateipfad zum Speichern
     * @throws IOException bei I/O-Fehlern
     */
    private void downloadFile(URI imageUri, Path targetPath) throws IOException {
        URLConnection connection = imageUri.toURL().openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        );

        try (var inputStream = connection.getInputStream()) {
            Files.copy(inputStream, targetPath);
        }
    }
}
