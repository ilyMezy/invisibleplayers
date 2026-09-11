package com.mezy.invisibleplayers.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigValidatorTest {

    @Test
    void validValuesPassThroughUnchangedWithNoWarnings() {
        List<String> warnings = new ArrayList<>();

        PluginConfig config = ConfigValidator.validate(false, false, "X", 5, warnings::add);

        assertEquals(new PluginConfig(false, false, "X", 5), config);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void nonBooleanTabListEnabledFallsBackToDefault() {
        List<String> warnings = new ArrayList<>();

        PluginConfig config = ConfigValidator.validate("not-a-boolean", true, "?", 1, warnings::add);

        assertEquals(PluginConfig.DEFAULT_TAB_LIST_ENABLED, config.tabListEnabled());
        assertEquals(1, warnings.size());
    }

    @Test
    void emptyReplacementFallsBackToDefault() {
        List<String> warnings = new ArrayList<>();

        PluginConfig config = ConfigValidator.validate(true, true, "", 1, warnings::add);

        assertEquals(PluginConfig.DEFAULT_REPLACEMENT, config.replacement());
        assertEquals(1, warnings.size());
    }

    @Test
    void nonStringReplacementFallsBackToDefault() {
        List<String> warnings = new ArrayList<>();

        PluginConfig config = ConfigValidator.validate(true, true, 42, 1, warnings::add);

        assertEquals(PluginConfig.DEFAULT_REPLACEMENT, config.replacement());
    }

    @Test
    void zeroIntervalFallsBackToDefault() {
        List<String> warnings = new ArrayList<>();

        PluginConfig config = ConfigValidator.validate(true, true, "?", 0, warnings::add);

        assertEquals(PluginConfig.DEFAULT_RECONCILIATION_INTERVAL_SECONDS, config.reconciliationIntervalSeconds());
        assertEquals(1, warnings.size());
    }

    @Test
    void negativeIntervalFallsBackToDefault() {
        List<String> warnings = new ArrayList<>();

        PluginConfig config = ConfigValidator.validate(true, true, "?", -5, warnings::add);

        assertEquals(PluginConfig.DEFAULT_RECONCILIATION_INTERVAL_SECONDS, config.reconciliationIntervalSeconds());
    }

    @Test
    void nonNumericIntervalFallsBackToDefault() {
        List<String> warnings = new ArrayList<>();

        PluginConfig config = ConfigValidator.validate(true, true, "?", "five", warnings::add);

        assertEquals(PluginConfig.DEFAULT_RECONCILIATION_INTERVAL_SECONDS, config.reconciliationIntervalSeconds());
    }

    @Test
    void missingValuesFallBackToAllDefaults() {
        List<String> warnings = new ArrayList<>();

        PluginConfig config = ConfigValidator.validate(null, null, null, null, warnings::add);

        assertEquals(new PluginConfig(
                PluginConfig.DEFAULT_TAB_LIST_ENABLED,
                PluginConfig.DEFAULT_DEATH_REDACTION_ENABLED,
                PluginConfig.DEFAULT_REPLACEMENT,
                PluginConfig.DEFAULT_RECONCILIATION_INTERVAL_SECONDS), config);
        assertEquals(4, warnings.size());
    }
}
