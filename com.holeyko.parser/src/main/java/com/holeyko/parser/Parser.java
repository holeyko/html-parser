package com.holeyko.parser;

import com.holeyko.parser.exception.ParseException;

import java.io.Closeable;

public interface Parser<T> extends Closeable {
    T parse() throws ParseException;
}
