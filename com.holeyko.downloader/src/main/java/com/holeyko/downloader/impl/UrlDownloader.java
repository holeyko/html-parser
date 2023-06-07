package com.holeyko.downloader.impl;

import com.holeyko.downloader.Downloader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class UrlDownloader implements Downloader {
    @Override
    public void download(String downloadFrom, Path downloadTo, String name) throws IOException {
        final var url = new URL(downloadFrom);
        final var downloadedFile = Path.of(downloadTo.toString(), name);
        Files.createDirectories(downloadTo);

        try (
                final ReadableByteChannel downloadChannel = Channels.newChannel(url.openStream());
                final FileChannel fileChannel = new FileOutputStream(downloadedFile.toFile()).getChannel();
        ) {
            long countTransferred = 0;
            long curCountRead = 0;
            while ((curCountRead =
                    fileChannel.transferFrom(downloadChannel, countTransferred, Long.MAX_VALUE)
            ) != 0) {
                countTransferred += curCountRead;
            }
        }
    }
}
