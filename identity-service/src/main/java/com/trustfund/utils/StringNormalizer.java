package com.trustfund.utils;

public final class StringNormalizer {

    private StringNormalizer() {}

    public static String normalize(String input) {
        return input == null
                ? null
                : input
                .trim()
                .replaceAll("\\s+", " ");
    }

    public static String normalizeUrl(String input) {
        if (input == null) return null;

        String normalized = input.trim();

        if (normalized.contains(" ")) {
            throw new IllegalArgumentException("URL must not contain spaces");
        }

        return normalized;
    }
}
