package com.holeyko.parser.impl;

import com.holeyko.parser.exception.ParseException;
import com.holeyko.parser.model.HTMLElement;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

public class HTMLParser extends AbstractParser<HTMLElement> {
    private enum TagEnvironment {
        HTML, SCRIPT, STYLE
    }

    private record TagContext(HTMLElement parent, TagEnvironment environment) {
    }

    private static final String MAIN_TAG = "html";
    private static final String BEGIN_OPEN_TAG = "<";
    private static final String END_OPEN_TAG = ">";
    private static final String FINAL_END_OPEN_TAG = "/>";
    private static final String BEGIN_CLOSE_TAG = "</";
    private static final String END_CLOSE_TAG = ">";
    private static final String BEGIN_COMMENT = "<!--";
    private static final String END_COMMENT = "-->";
    private static final String ASSIGN = "=";
    private static final String DOCTYPE_HTML = "<!DOCTYPE html>";
    private static final List<String> VOID_TAGS = List.of(
            "area", "base", "br", "col", "command", "embed",
            "hr", "img", "input", "keygen", "link", "meta", "param", "source", "track", "wbr"
    );

    public HTMLParser(InputStream inputStream) {
        super(inputStream);
    }

    public HTMLParser(InputStream inputStream, Charset charset) {
        super(inputStream, charset);
    }

    @Override
    public HTMLElement parse() throws ParseException {
        HTMLElement root = new HTMLElement();
        root.setTag(MAIN_TAG);
        parseTag(new TagContext(root, TagEnvironment.HTML));

        if (canRead()) {
            throw new ParseException("HTML must have ended at %d position"
                    .formatted(countSkipChars));
        }

        final List<HTMLElement> rootChildren = root.getChildren();
        if (rootChildren.size() == 1 && MAIN_TAG.equals(rootChildren.get(0).getTag())) {
            root = rootChildren.get(0);
        }
        return root;
    }

    private void parseTag(TagContext context) throws ParseException {
        final String closeTag = makeCloseTag(context.parent().getTag());
        skipUnnecessary();
        while (canRead() && !checkString(closeTag, false)) {
            if (checkString(BEGIN_CLOSE_TAG)) {
                require(closeTag);
            }

            final String plainText = parsePlainText(context);
            if (!plainText.isBlank()) {
                context.parent().addChild(
                        HTMLElement.builder().value(plainText).build()
                );
            }

            skipUnnecessary();
            final HTMLElement htmlElement = parseOpenTag();
            if (htmlElement != null) {
                if (!htmlElement.isSingle() && !htmlElement.isVoid()) {
                    parseTag(new TagContext(htmlElement, makeTagEnvironment(htmlElement)));
                    require(makeCloseTag(htmlElement.getTag()), false);
                }
                context.parent().addChild(htmlElement);
            }
            skipUnnecessary();
        }
    }

    private String parsePlainText(TagContext context) throws ParseException {
        StringBuilder result = new StringBuilder();
        switch (context.environment()) {
            case STYLE, SCRIPT -> {
                String closeTag = makeCloseTag(context.parent().getTag());
                while (canRead() && !checkString(closeTag, false)) {
                    result.append(next());
                }
            }
            case HTML -> {
                skipComments();
                while (canRead() && !checkString(BEGIN_OPEN_TAG, false)) {
                    result.append(next());
                    skipComments();
                }
            }
        }

        return result.toString().trim();
    }

    private HTMLElement parseOpenTag() throws ParseException {
        skipUnnecessary();
        if (!canRead() || checkString(BEGIN_CLOSE_TAG) || !checkStringAndSkip(BEGIN_OPEN_TAG)) {
            return null;
        }

        HTMLElement htmlElement = new HTMLElement();
        skipUnnecessary();
        htmlElement.setTag(parseTagName());
        skipUnnecessary();
        parseAttributes(htmlElement);
        skipUnnecessary();

        if (checkStringAndSkip(FINAL_END_OPEN_TAG)) {
            htmlElement.setSingle(true);
        } else {
            require(END_OPEN_TAG);
        }

        if (VOID_TAGS.contains(htmlElement.getTag())) {
            htmlElement.setVoid(true);
        }

        return htmlElement;
    }

    private String parseTagName() throws ParseException {
        return parseUntilExclude(
                () -> !Character.isWhitespace(lookup()) &&
                        !checkListString(List.of(FINAL_END_OPEN_TAG, END_OPEN_TAG)),
                () -> checkString(BEGIN_OPEN_TAG)
        ).toLowerCase();
    }

    private void parseAttributes(HTMLElement htmlElement) throws ParseException {
        String attributeName;
        do {
            skipUnnecessary();
            attributeName = parseUntilExclude(
                    () -> !Character.isWhitespace(lookup()) &&
                            !checkListString(List.of(ASSIGN, FINAL_END_OPEN_TAG, END_OPEN_TAG)),
                    () -> checkString(BEGIN_OPEN_TAG)
            );

            if (!attributeName.isEmpty()) {
                if (Character.isWhitespace(lookup()) || checkString(FINAL_END_OPEN_TAG) || checkString(END_OPEN_TAG)) {
                    htmlElement.addAttributeWithoutArgs(attributeName);
                } else {
                    checkStringAndSkip(ASSIGN);
                    String quote = Character.toString(next());
                    String attributeValue = parseUntilExclude(
                            () -> !checkString(quote),
                            () -> checkString(BEGIN_OPEN_TAG)
                    );
                    require(quote);
                    htmlElement.addAttribute(attributeName, attributeValue);
                }
            } else {
                if (checkString(ASSIGN)) {
                    throw new ParseException("Incorrect attribute declaration at %d position");
                }
            }
        } while (!checkString(FINAL_END_OPEN_TAG) && !checkString(END_OPEN_TAG));
    }

    private void skipComments() throws ParseException {
        if (checkStringAndSkip(BEGIN_COMMENT)) {
            parseUntilString(List.of(END_COMMENT));
            require(END_COMMENT);
        }
    }

    private void skipDoctype() throws ParseException {
        checkStringAndSkip(DOCTYPE_HTML, false);
    }

    private void skipUnnecessary() throws ParseException {
        int startPos;
        do {
            startPos = countSkipChars;
            skipWhitespace();
            skipComments();
            skipDoctype();
        } while (startPos < countSkipChars);
    }

    private String makeCloseTag(String tagName) {
        return BEGIN_CLOSE_TAG + tagName + END_CLOSE_TAG;
    }

    private TagEnvironment makeTagEnvironment(HTMLElement htmlElement) {
        return switch (htmlElement.getTag().toLowerCase()) {
            case "script" -> TagEnvironment.SCRIPT;
            case "style" -> TagEnvironment.STYLE;
            default -> TagEnvironment.HTML;
        };
    }
}
