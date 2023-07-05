package com.holeyko.parser.impl;

import com.holeyko.parser.Parser;
import com.holeyko.parser.exception.ParseException;
import com.holeyko.parser.model.HTMLElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;

class HTMLParserTest {
    private record HTMLFileParsed(String path, HTMLElement result) {
    }

    private static final String PATH_TO_RESOURCES = "src/test/resources";
    private static final HTMLFileParsed BLANK_HTML = new HTMLFileParsed(
            PATH_TO_RESOURCES + "/correct/blank.html",
            HTMLElement.builder().tag("html").build()
    );
    private static final HTMLFileParsed NO_HTML_TAG_HTML = new HTMLFileParsed(
            PATH_TO_RESOURCES + "/correct/no-html-tag.html",
            HTMLElement.builder()
                    .tag("html")
                    .addChild(HTMLElement.builder()
                            .tag("head")
                            .addChild(HTMLElement.builder()
                                    .tag("title")
                                    .addChild(HTMLElement.builder().value("No HTML Tag").build())
                                    .build()
                            )
                            .build()
                    )
                    .build()
    );
    private static final HTMLFileParsed WITH_COMMENTS_HTML = new HTMLFileParsed(
            PATH_TO_RESOURCES + "/correct/with-comments.html",
            HTMLElement.builder()
                    .tag("html")
                    .addAttribute("lang", "en")
                    .addChild(HTMLElement.builder()
                            .tag("head")
                            .addChild(HTMLElement.builder()
                                    .tag("meta")
                                    .isVoid(true)
                                    .addAttribute("charset", "UTF-8")
                                    .build()
                            )
                            .build()
                    )
                    .addChild(HTMLElement.builder()
                            .tag("body")
                            .addChild(HTMLElement.builder()
                                    .value("Hello, world!!!")
                                    .build()
                            )
                            .build()
                    )
                    .build()
    );
    private static final HTMLFileParsed SIMPLE_HTML = new HTMLFileParsed(
            PATH_TO_RESOURCES + "/correct/simple.html",
            HTMLElement.builder()
                    .tag("html")
                    .addAttribute("lang", "en")
                    .addChild(HTMLElement.builder()
                            .tag("head")
                            .addChild(HTMLElement.builder()
                                    .tag("meta")
                                    .isVoid(true)
                                    .addAttribute("charset", "UTF-8")
                                    .build()
                            )
                            .addChild(HTMLElement.builder()
                                    .tag("title")
                                    .addChild(HTMLElement.builder()
                                            .value("Simple HTML")
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .addChild(HTMLElement.builder()
                            .tag("body")
                            .addChild(HTMLElement.builder()
                                    .value("Hello, world!!!")
                                    .build()
                            )
                            .build()
                    )
                    .build()
    );
    private static final HTMLFileParsed MEDIUM_HTML = new HTMLFileParsed(
            PATH_TO_RESOURCES + "/correct/medium.html",
            HTMLElement.builder()
                    .tag("html")
                    .addAttribute("lang", "en")
                    .addChild(HTMLElement.builder()
                            .tag("head")
                            .addChild(HTMLElement.builder()
                                    .tag("script")
                                    .addChild(HTMLElement.builder()
                                            .value("console.log(\"Hello, world\");")
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .addChild(HTMLElement.builder()
                            .tag("body")
                            .addChild(HTMLElement.builder()
                                    .tag("header")
                                    .addChild(HTMLElement.builder()
                                            .tag("h1")
                                            .addAttribute("class", "test test2 asdf")
                                            .addAttribute("hidden", null)
                                            .addChild(HTMLElement.builder()
                                                    .value("The best site ever")
                                                    .build()
                                            )
                                            .build()
                                    )
                                    .addChild(HTMLElement.builder()
                                            .value(", maybe")
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
    );
    private static final List<String> URLS = List.of(
            "https://www.virustotal.com/gui/home/upload",
            "https://www.kgeorgiy.info/courses/java-advanced/homeworks.html",
            "https://lichess.org/",
            "https://www.overleaf.com/",
            "https://vk.com/feed",
            "https://habr.com/ru/all/",
            "https://www.youtube.com/"
    );

    @Test
    @DisplayName("Blank HTML")
    void testBlankHtml() throws IOException {
        testPreparedParsing(BLANK_HTML);
    }

    @Test
    @DisplayName("HTML without html tag")
    void testNoHtmlTag() throws IOException {
        testPreparedParsing(NO_HTML_TAG_HTML);
    }

    @Test
    @DisplayName("HTML with comments")
    void testHtmlWithComments() throws IOException {
        testPreparedParsing(WITH_COMMENTS_HTML);
    }

    @Test
    @DisplayName("Simple HTML")
    void testSimpleHtml() throws IOException {
        testPreparedParsing(SIMPLE_HTML);
    }

    @Test
    @DisplayName("Medium HTML")
    void testMediumHtml() throws IOException {
        testPreparedParsing(MEDIUM_HTML);
    }

    @Test
    @DisplayName("Online HTML")
    void testOnlineHtml() throws IOException {
        for (String url : URLS) {
            try (var parser = new HTMLParser(new URL(url).openStream())) {
                Assertions.assertDoesNotThrow(parser::parse, "URL: %s"
                        .formatted(url));
            }
        }
    }

    @Test
    @DisplayName("No close tag")
    void testNoCloseTag() throws IOException {
        testIncorrectHtml(PATH_TO_RESOURCES + "/incorrect/no-close-tag.html");
    }

    @Test
    @DisplayName("No close comment")
    void testNoCloseComment() throws IOException {
        testIncorrectHtml(PATH_TO_RESOURCES + "/incorrect/no-close-comment.html");
    }

    private void testPreparedParsing(HTMLFileParsed prepared) throws IOException {
        try (Parser<HTMLElement> parser = new HTMLParser(
                new FileInputStream(prepared.path())
        )) {
            HTMLElement result = parser.parse();
            Assertions.assertEquals(prepared.result(), result, "File: %s\nExpected:\n%s \nActual:\n%s"
                    .formatted(prepared.path(), prepared.result(), result));
        } catch (ParseException e) {
            throw new AssertionFailedError();
        }
    }

    private void testIncorrectHtml(String path) throws IOException {
        try (Parser<HTMLElement> parser = new HTMLParser(
                new FileInputStream(path)
        )) {
            Assertions.assertThrows(ParseException.class, parser::parse, "File: %s"
                    .formatted(path));
        }
    }
}