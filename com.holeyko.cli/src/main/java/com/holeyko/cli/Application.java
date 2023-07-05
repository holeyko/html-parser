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
    private static final String EXIT_MESSAGE = "Enter exit to get out";

    public static void main(String[] args) {
        try (var input = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8)
        )) {
            String choice;
            while (true) {
                try {
                    System.out.println("""
                            Choose an action' number:
                                1. Parse HTML file
                                2. Download a file rom URL
                            """ + EXIT_MESSAGE);
                    choice = input.readLine().toLowerCase().trim();
                    testExit(choice);
                    switch (choice) {
                        case "1" -> parseHtml(input);
                        case "2" -> downloadFile(input);
                        default -> throw new IllegalArgumentException();
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("There is no your command");
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
                """ + EXIT_MESSAGE
        );
        String choice = input.readLine().toLowerCase().trim();
        testExit(choice);

        try (final InputStream htmlInputStream = getHtmlInputStream(choice, input)) {
            System.out.println("""
                    Choose a number of a result parsing:
                        1. Console
                        2. File
                    """ + EXIT_MESSAGE
            );
            choice = input.readLine().trim().toLowerCase();
            testExit(choice);
            try {
                try (var parser = new HTMLParser(htmlInputStream)) {
                    String result = parser.parse().toString();
                    switch (choice) {
                        case "1" -> System.out.println(result);
                        case "2" -> {
                            System.out.println("Enter path to file:");
                            choice = input.readLine().trim().toLowerCase();
                            try (BufferedWriter output = new BufferedWriter(
                                    new FileWriter(choice, StandardCharsets.UTF_8)
                            )) {
                                output.write(result + '\n');
                            }
                        }
                        default -> throw new IllegalArgumentException();
                    }
                } catch (ParseException e) {
                    System.err.println(e.getMessage());
                }
            } catch (IOException e) {
                System.err.printf("Can't write to output. %s%n", e.getMessage());
            }
        } catch (IOException e) {
            System.err.printf("Can't handle source of html. %s%n", e.getMessage());
        }
    }

    private static InputStream getHtmlInputStream(String choice, BufferedReader input) throws IOException {
        switch (choice) {
            case "1" -> {
                System.out.println("Enter the URL:");
                return new URL(input.readLine()).openStream();
            }
            case "2" -> {
                System.out.println("Enter path to html:");
                return new FileInputStream(input.readLine());
            }
            default -> throw new IllegalArgumentException();
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
            System.err.printf("Cant' download the file. %s%n", e.getMessage());
        }
    }

    private static void testExit(String choice) {
        if ("exit".equals(choice)) {
            System.exit(0);
        }
    }
}
