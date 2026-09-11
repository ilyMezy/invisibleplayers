package com.mezy.invisibleplayers.config;

import java.util.function.Consumer;

/**
 * Pure validation logic for raw config values, kept independent of Bukkit's
 * {@code FileConfiguration} so it can be unit tested directly.
 */
public final class ConfigValidator {

    private ConfigValidator() {
    }

    public static PluginConfig validate(Object tabListEnabledRaw,
                                         Object deathRedactionEnabledRaw,
                                         Object replacementRaw,
                                         Object intervalRaw,
                                         Consumer<String> warn) {
        boolean tabListEnabled = validateBoolean(tabListEnabledRaw, "tab-list.enabled",
                PluginConfig.DEFAULT_TAB_LIST_ENABLED, warn);
        boolean deathRedactionEnabled = validateBoolean(deathRedactionEnabledRaw, "death-redaction.enabled",
                PluginConfig.DEFAULT_DEATH_REDACTION_ENABLED, warn);
        String replacement = validateReplacement(replacementRaw, warn);
        int interval = validateInterval(intervalRaw, warn);
        return new PluginConfig(tabListEnabled, deathRedactionEnabled, replacement, interval);
    }

    private static boolean validateBoolean(Object raw, String path, boolean fallback, Consumer<String> warn) {
        if (raw instanceof Boolean bool) {
            return bool;
        }
        warn.accept("Invalid value for '" + path + "' (expected true/false); using default " + fallback);
        return fallback;
    }

    private static String validateReplacement(Object raw, Consumer<String> warn) {
        if (raw instanceof String str && !str.isEmpty()) {
            return str;
        }
        warn.accept("Invalid value for 'death-redaction.replacement' (expected a non-empty string); using default \""
                + PluginConfig.DEFAULT_REPLACEMENT + "\"");
        return PluginConfig.DEFAULT_REPLACEMENT;
    }

    private static int validateInterval(Object raw, Consumer<String> warn) {
        if (raw instanceof Number number) {
            int value = number.intValue();
            if (value > 0) {
                return value;
            }
        }
        warn.accept("Invalid value for 'settings.reconciliation-interval' (expected a positive integer, in seconds); "
                + "using default " + PluginConfig.DEFAULT_RECONCILIATION_INTERVAL_SECONDS);
        return PluginConfig.DEFAULT_RECONCILIATION_INTERVAL_SECONDS;
    }
}
