package com.holeyko.parser.impl;

import com.holeyko.parser.Parser;
import com.holeyko.parser.exception.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractParser<T> implements Parser<T> {
    protected static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final int MIN_BUFFER_SIZE = 1024;
    private static final int EXTEND_MULTIPLIER = 2;
    private static final double LOAD_FACTOR = 0.75;

    private final Reader input;
    private boolean isInputEnd = false;
    private char[] buffer = new char[MIN_BUFFER_SIZE];
    private int curBufferIndex = 0;
    private int readLimit = 0;
    protected int countSkipChars = 0;

    public AbstractParser(InputStream inputStream) {
        this(inputStream, DEFAULT_CHARSET);
    }

    public AbstractParser(InputStream inputStream, Charset charset) {
        input = new InputStreamReader(inputStream, charset);
    }

    protected boolean canRead() throws ParseException {
        if (!isInputEnd && curBufferIndex == readLimit) {
            prepareBuffer();
        }
        return !isInputEnd || curBufferIndex < readLimit;
    }

    protected char lookup() throws ParseException {
        prepareBuffer();
        if (!canRead()) {
            throw new ParseException("InputStream ended");
        }

        return buffer[curBufferIndex];
    }

    private void prepareBuffer() throws ParseException {
        updateSize();
        if (!isInputEnd && curBufferIndex == readLimit) {
            readInput();
        }
    }

    private void updateSize() {
        if (curBufferIndex < readLimit) {
            return;
        }

        if (readLimit == buffer.length) {
            extendBuffer();
        }
    }

    private void readInput() throws ParseException {
        try {
            int read = input.read(buffer, readLimit, buffer.length - readLimit);
            if (read == -1) {
                isInputEnd = true;
                return;
            }
            readLimit += read;
        } catch (IOException e) {
            throw new ParseException("Can't read inputStream", e);
        }
    }

    private void extendBuffer() {
        buffer = Arrays.copyOf(buffer, buffer.length * EXTEND_MULTIPLIER);
    }

    private void reduceBuffer() {
        buffer = Arrays.copyOfRange(buffer, curBufferIndex, buffer.length);
        readLimit -= curBufferIndex;
        curBufferIndex = 0;
    }

    protected char next() throws ParseException {
        char result = lookup();
        ++countSkipChars;
        ++curBufferIndex;

        if (
                Double.compare((double) curBufferIndex / buffer.length, LOAD_FACTOR) >= 0 &&
                        buffer.length - curBufferIndex >= MIN_BUFFER_SIZE
        ) {
            reduceBuffer();
        }

        return result;
    }

    protected void skipWhitespace() throws ParseException {
        while (canRead() && Character.isWhitespace(lookup())) {
            next();
        }
    }

    protected boolean checkString(String s) throws ParseException {
        return checkString(s, true);
    }

    protected boolean checkString(String s, boolean sensitive) throws ParseException {
        int curPos = 0;
        boolean result = true;
        Comparator<Character> characterComparator = sensitive ? Character::compare :
                (l, r) -> Character.compare(Character.toLowerCase(l), Character.toLowerCase(r));

        while (curPos < s.length()) {
            if (!canRead() || characterComparator.compare(lookup(), s.charAt(curPos)) != 0) {
                result = false;
                break;
            }

            ++curBufferIndex;
            ++curPos;
        }

        curBufferIndex -= curPos;
        return result;
    }

    protected boolean checkListString(List<String> list) throws ParseException {
        return checkListString(list, true);
    }

    protected boolean checkListString(List<String> list, boolean sensitive) throws ParseException {
        for (String s : list) {
            if (checkString(s, sensitive)) {
                return true;
            }
        }

        return false;
    }

    protected boolean checkStringAndSkip(String s) throws ParseException {
        return checkStringAndSkip(s, true);
    }

    protected boolean checkStringAndSkip(String s, boolean sensitive) throws ParseException {
        if (checkString(s, sensitive)) {
            for (int i = 0; i < s.length(); ++i) {
                next();
            }
            return true;
        }

        return false;
    }

    protected void require(String s) throws ParseException {
        require(s, true);
    }

    protected void require(String s, boolean sensitive) throws ParseException {
        requireSeveral(List.of(s), sensitive);
    }

    protected void requireSeveral(List<String> requireStrings) throws ParseException {
        requireSeveral(requireStrings, true);
    }

    protected void requireSeveral(List<String> requireStrings, boolean sensitive) throws ParseException {
        for (var required : requireStrings) {
            if (checkStringAndSkip(required, sensitive)) {
                return;
            }
        }

        throw new ParseException("Expected %s at %d position"
                .formatted(requireStrings, countSkipChars));
    }

    protected String parseUntilString(List<String> expectedStrings) throws ParseException {
        return parseUntilString(expectedStrings, true);
    }

    protected String parseUntilString(List<String> expectedStrings, boolean sensitive) throws ParseException {
        return parseUntilStringsExcludeStrings(expectedStrings, Collections.emptyList(), sensitive);
    }

    protected String parseUntilStringsExcludeStrings(List<String> expectedStrings, List<String> excludeStrings) throws ParseException {
        return parseUntilStringsExcludeStrings(expectedStrings, excludeStrings, true);
    }

    protected String parseUntilStringsExcludeStrings(List<String> expectedStrings, List<String> excludeStrings, boolean sensitive) throws ParseException {
        return parseUntilExclude(
                () -> !checkListString(expectedStrings, sensitive),
                () -> checkListString(excludeStrings, sensitive)
        );
    }

    protected String parseUntil(ParseCheck checkContinue) throws ParseException {
        return parseUntilExclude(checkContinue, () -> false);
    }

    protected String parseUntilExclude(ParseCheck checkContinue, ParseCheck checkExclude) throws ParseException {
        StringBuilder result = new StringBuilder();
        while (canRead() && checkContinue.check()) {
            if (checkExclude.check()) {
                throw new ParseException("Unexpected token at %d position"
                        .formatted(countSkipChars));
            }

            result.append(next());
        }

        return result.toString();
    }

    @Override
    public void close() throws IOException {
        input.close();
    }

    @FunctionalInterface
    protected interface ParseCheck {
        boolean check() throws ParseException;
    }
}
