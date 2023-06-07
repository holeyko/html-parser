package com.holeyko.downloader;

import java.io.IOException;
import java.nio.file.Path;

public interface Downloader {
    void download(String downloadFrom, Path downloadTo, String name) throws IOException;
}
