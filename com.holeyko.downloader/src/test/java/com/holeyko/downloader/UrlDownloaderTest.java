package com.holeyko.downloader;

import com.holeyko.downloader.impl.UrlDownloader;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

class UrlDownloaderTest {
    record DownloadResource(String url, String relativeDirPath, String name) {
    }

    private static final String TMP_DIRECTORY = "tmp";
    private static final String PATH_TO_RESOURCES = "src/test/resources";

    // Resource's size is less 1 MB
    private static final List<DownloadResource> smallResources = List.of(
            new DownloadResource("https://en.wikipedia.org/wiki/Lion", "small", "wiki-lion.html"),
            new DownloadResource("https://en.wikipedia.org/wiki/Moscow", "small", "wiki-moscow.html"),
            new DownloadResource("https://raw.githubusercontent.com/holeyko/itmo-projects/main/java-adv/java-adv.iml", "small", "java-adv.iml"),
            new DownloadResource("https://raw.githubusercontent.com/kamranahmedse/developer-roadmap/master/public/pdfs/best-practices/aws.pdf", "small", "aws.pdf")
    );

    // Resource's size is between 1 MB and 100 MB
    private static final List<DownloadResource> mediumResources = List.of(
            new DownloadResource("https://en.wikipedia.org/wiki/Moscow#/media/File:Saint_Basil's_Cathedral_and_the_Red_Square.jpg", "medium", "img-moscow.jpg"),
            new DownloadResource("https://cdn-icons-png.flaticon.com/512/5968/5968282.png", "medium", "java-icon.png"),
            new DownloadResource("https://yes-pdf.com/electronic-book/4937", "medium", "comedy-comedy-comedy.pdf"),
            new DownloadResource("https://yes-pdf.com/electronic-book/4928", "medium", "falls-boys.pdf")
    );

    private Downloader downloader = new UrlDownloader();

    @Test
    @DisplayName("Test small resources")
    void testDownloadSmallResources() {
        smallResources.forEach(this::testDownloadResource);
    }

    @Test
    @DisplayName("Test medium resources")
    void testDownloadMediumResources() {
        mediumResources.forEach(this::testDownloadResource);
    }

    private void testDownloadResource(DownloadResource resource) {
        final Path toPath = Path.of(TMP_DIRECTORY, resource.relativeDirPath());

        Assertions.assertDoesNotThrow(() -> downloader.download(resource.url(), toPath, resource.name()));
        try {
            final Path expectedResource = Path.of(PATH_TO_RESOURCES, resource.relativeDirPath(), resource.name());
            final Path downloadedResource = Path.of(TMP_DIRECTORY, resource.relativeDirPath(), resource.name());

            Assertions.assertEquals(-1, Files.mismatch(downloadedResource, expectedResource));
        } catch (IOException e) {
            throw new RuntimeException("Comparing files was failed [url=%s]"
                    .formatted(resource.url()), e
            );
        }
    }

    @BeforeAll
    static void createTmpDirectory() throws IOException {
        Files.createDirectories(Path.of(TMP_DIRECTORY));
    }

    @AfterAll
    static void removeTmpDirectory() throws IOException {
        Files.walkFileTree(Path.of(TMP_DIRECTORY), new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}