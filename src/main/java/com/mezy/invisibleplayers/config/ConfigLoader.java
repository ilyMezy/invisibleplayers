package com.mezy.invisibleplayers.config;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads and validates {@code config.yml} against the live {@link JavaPlugin} config file.
 */
public final class ConfigLoader {

    private ConfigLoader() {
    }

    public static PluginConfig load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        var cfg = plugin.getConfig();

        return ConfigValidator.validate(
                cfg.get("tab-list.enabled"),
                cfg.get("death-redaction.enabled"),
                cfg.get("death-redaction.replacement"),
                cfg.get("settings.reconciliation-interval"),
                message -> plugin.getLogger().warning(message)
        );
    }
}
