package com.holeyko.parser.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@EqualsAndHashCode
public class HTMLElement {
    private String tag;
    private String value;
    private Map<String, String> attributes = new HashMap<>();
    private List<HTMLElement> children = new ArrayList<>();
    private boolean isSingle = false;
    private boolean isVoid = false;

    public String getAttribute(String name) {
        return attributes.get(name);
    }

    public void addAttribute(String name, String value) {
        attributes.put(name, value);
    }

    public void addAttributeWithoutArgs(String name) {
        attributes.put(name, null);
    }

    public boolean containsAttribute(String name) {
        return attributes.containsKey(name);
    }

    public void addChild(HTMLElement child) {
        children.add(child);
    }

    @Override
    public String toString() {
        if (value != null) {
            return value;
        }

        String tagInfo = "tag: %s | isSingle: %s | isVoid: %s | attributes: %s"
                .formatted(tag, isSingle, isVoid, attributes);
        StringBuilder result = new StringBuilder("\n");
        children.forEach(child -> result.append(child).append('\n'));
        result.deleteCharAt(result.length() - 1);

        return tagInfo + result.toString().lines().map(line -> '\t' + line)
                .collect(Collectors.joining("\n"));
    }

    public String toHTML() {
        if (value != null) {
            return value;
        }

        StringBuilder result = new StringBuilder();
        result.append("<").append(tag).append(" ");
        for (var attr : attributes.entrySet()) {
            result.append(attr.getKey()).append("=\"")
                    .append(attr.getValue()).append("\"").append(" ");
        }
        result.deleteCharAt(result.length() - 1);

        if (isSingle) {
            result.append("/>");
        } else if (isVoid) {
            result.append(">");
        } else {
            result.append(">");
            StringBuilder childrenHTML = new StringBuilder("\n");
            children.forEach((child -> childrenHTML.append(child.toHTML()).append("\n")));
            childrenHTML.deleteCharAt(childrenHTML.length() - 1);
            result.append(childrenHTML.toString().lines()
                    .map(line -> '\t' + line).collect(Collectors.joining("\n"))
            ).append("\n").append("</").append(tag).append(">");
        }

        return result.toString();
    }

    public static HTMLElementBuilder builder() {
        return new HTMLElementBuilder();
    }

    public static class HTMLElementBuilder {
        private final HTMLElement htmlElement;

        public HTMLElementBuilder() {
            htmlElement = new HTMLElement();
        }

        public HTMLElementBuilder tag(String tag) {
            htmlElement.setTag(tag);
            return this;
        }

        public HTMLElementBuilder isSingle(boolean isSingle) {
            htmlElement.setSingle(isSingle);
            return this;
        }

        public HTMLElementBuilder isVoid(boolean isVoid) {
            htmlElement.setVoid(isVoid);
            return this;
        }

        public HTMLElementBuilder value(String value) {
            htmlElement.setValue(value);
            return this;
        }

        public HTMLElementBuilder addChild(HTMLElement child) {
            htmlElement.addChild(child);
            return this;
        }

        public HTMLElementBuilder addAttribute(String name, String value) {
            htmlElement.addAttribute(name, value);
            return this;
        }

        public HTMLElement build() {
            return htmlElement;
        }
    }
}
