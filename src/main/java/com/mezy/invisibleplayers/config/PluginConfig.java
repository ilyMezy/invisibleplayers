package com.mezy.invisibleplayers.config;

/**
 * Immutable, validated snapshot of {@code config.yml}.
 */
public record PluginConfig(
        boolean tabListEnabled,
        boolean deathRedactionEnabled,
        String replacement,
        int reconciliationIntervalSeconds
) {

    public static final boolean DEFAULT_TAB_LIST_ENABLED = true;
    public static final boolean DEFAULT_DEATH_REDACTION_ENABLED = true;
    public static final String DEFAULT_REPLACEMENT = "?";
    public static final int DEFAULT_RECONCILIATION_INTERVAL_SECONDS = 1;
}
