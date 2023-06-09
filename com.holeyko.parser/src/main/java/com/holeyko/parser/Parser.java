package com.holeyko.parser;

import com.holeyko.parser.exception.ParseException;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public interface Parser<T> extends Closeable {
    T parse() throws ParseException;
}
