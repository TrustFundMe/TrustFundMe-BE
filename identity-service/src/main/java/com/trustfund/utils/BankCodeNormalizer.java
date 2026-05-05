package com.trustfund.utils;

import java.util.Map;

/**
 * Normalizes bank codes to match the codes used by payment providers (e.g.,
 * Casso).
 * VietQR API returns "MB" for MB Bank, but Casso uses "MBB".
 * This utility ensures consistent bank code usage across the system.
 */
public final class BankCodeNormalizer {

    private static final Map<String, String> CODE_MAP = Map.of(
            "MB", "MBB");

    private BankCodeNormalizer() {
    }

    /**
     * Normalize a bank code to the system-standard code.
     * E.g., "MB" → "MBB".
     * Returns the original code if no mapping exists.
     */
    public static String normalize(String bankCode) {
        if (bankCode == null)
            return null;
        return CODE_MAP.getOrDefault(bankCode.trim(), bankCode.trim());
    }
}
