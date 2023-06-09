package com.holeyko.cli;

import com.holeyko.downloader.Downloader;
import com.holeyko.downloader.impl.UrlDownloader;
import com.holeyko.parser.exception.ParseException;
import com.holeyko.parser.impl.HTMLParser;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class Application {
    public static void main(String[] args) {
        try (var input = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8)
        )) {
            String choice;
            while (true) {
                System.out.println("""
                        Choose an action' number:
                            1. Parse HTML file
                            2. Download a file rom URL
                        Enter exist to get out
                        """);
                choice = input.readLine().toLowerCase().trim();
                if ("1".equals(choice)) {
                    parseHtml(input);
                } else if ("2".equals(choice)) {
                    downloadFile(input);
                } else if ("exit".equals(choice)) {
                    return;
                } else {
                    System.out.println("There is no your choice");
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void parseHtml(BufferedReader input) throws IOException {
        System.out.println("""
                Choose a number of a HTML's source:
                    1. URL
                    2. Local file
                Enter exist to get out""");
        final String choice = input.readLine().toLowerCase().trim();

        try {
            final InputStream htmlInputStream;
            if ("1".equals(choice)) {
                System.out.println("Enter the URL:");
                htmlInputStream = new URL(input.readLine()).openStream();
            } else if ("2".equals(choice)) {
                System.out.println("Enter path to html:");
                htmlInputStream = new FileInputStream(input.readLine());
            } else if ("exist".equals(choice)) {
                return;
            } else {
                System.out.println("There is no your choice");
                return;
            }

            try (var parser = new HTMLParser(htmlInputStream)) {
                System.out.println(parser.parse());
            } catch (ParseException e) {
                System.err.println(e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Can't handle source of html. %s"
                    .formatted(e.getMessage()));
        }
    }

    private static void downloadFile(BufferedReader input) throws IOException {
        final Downloader downloader = new UrlDownloader();

        System.out.println("Enter the URL:");
        final String url = input.readLine().trim();
        System.out.println("Enter the directory for download:");
        final Path dir;
        try {
            dir = Path.of(input.readLine());
        } catch (InvalidPathException e) {
            System.err.println("Invalid path");
            return;
        }

        System.out.println("Enter the name of file:");
        final String name = input.readLine();

        try {
            Files.createDirectories(dir);
            downloader.download(url, dir, name);
            System.out.println("The file was downloaded");
        } catch (IOException e) {
            System.err.println("Cant' download the file. %s"
                    .formatted(e.getMessage()));
        }
    }
}
