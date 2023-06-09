package com.holeyko.downloader;

import com.holeyko.downloader.impl.UrlDownloader;
import org.junit.jupiter.api.*;
import org.opentest4j.AssertionFailedError;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

class UrlDownloaderTest {
    record DownloadResource(String url, String relativeDirPath, String name) {
    }

    private static final String TMP_DIRECTORY = "tmp";
    private static final String PATH_TO_RESOURCES = "src/test/resources";

    // Resource's size is less 1 MB
    private static final List<DownloadResource> smallResources = List.of(
            new DownloadResource("https://raw.githubusercontent.com/holeyko/itmo-projects/main/java-adv/java-adv.iml", "small", "java-adv.iml"),
            new DownloadResource("https://nsrassociation.ru/themes/default/images/fon2.jpg", "small", "nsra-logo.jpg"),
            new DownloadResource("http://web.archive.org/web/20131021165347im_/http://ia.media-imdb.com/images/M/MV5BODE4OTc5MDUyN15BMl5BanBnXkFtZTgwMDk5Mzc0MDE@._V1._SY200_CR50,0,200,200_.jpg", "small", "mr-banks.jpg")
    );

    // Resource's size is between 1 MB and 100 MB
    private static final List<DownloadResource> mediumResources = List.of(
            new DownloadResource("https://codeload.github.com/facebook/react/zip/refs/tags/v18.2.0", "medium", "react-18.2.0.zip"),
            new DownloadResource("https://codeload.github.com/spring-projects/spring-framework/zip/refs/tags/v6.0.9", "medium", "spring-framework-6.0.9.zip")
    );

    private Downloader downloader = new UrlDownloader();

    @Test
    @DisplayName("Test small resources")
    void testDownloadSmallResources() {
        smallResources.forEach(this::testDownloadResource);
    }

//    @Test
//    @DisplayName("Test medium resources")
//    void testDownloadMediumResources() {
//        mediumResources.forEach(this::testDownloadResource);
//    }

    private void testDownloadResource(DownloadResource resource) {
        final Path toPath = Path.of(TMP_DIRECTORY, resource.relativeDirPath());

        Assertions.assertDoesNotThrow(() -> downloader.download(resource.url(), toPath, resource.name()), "URL: %s".formatted(resource.url()));
        try {
            final Path expectedResource = Path.of(PATH_TO_RESOURCES, resource.relativeDirPath(), resource.name());
            final Path downloadedResource = Path.of(TMP_DIRECTORY, resource.relativeDirPath(), resource.name());

            Assertions.assertEquals(-1, Files.mismatch(downloadedResource, expectedResource), "URL: %s".formatted(resource.url()));
        } catch (IOException e) {
            throw new AssertionFailedError("Comparing files was failed [url=%s]"
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