package com.mezy.invisibleplayers.compat;

import com.mezy.invisibleplayers.service.InvisibilityService;
import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Detects an installed, enabled DiscordSRV and constructs the vanish-sync
 * compat layer if present. This is the only class in the codebase that
 * imports a DiscordSRV type -- the actual sync mechanism
 * ({@link DiscordSrvVanishSync}, {@link DiscordSrvVanishListener}) never
 * references DiscordSRV at all, since the "vanished" Bukkit metadata key it
 * writes is DiscordSRV's own documented, plugin-agnostic extension point.
 */
public final class DiscordSrvIntegration {

    private DiscordSrvIntegration() {
    }

    public static Optional<DiscordSrvVanishSync> tryEnable(Plugin owningPlugin,
                                                             InvisibilityService invisibilityService,
                                                             Logger logger) {
        Plugin found = Bukkit.getPluginManager().getPlugin("DiscordSRV");
        if (!(found instanceof DiscordSRV discordSrv) || !discordSrv.isEnabled()) {
            return Optional.empty();
        }

        logger.info("DiscordSRV " + discordSrv.getPluginMeta().getVersion()
                + " detected -- enabling DiscordSRV compatibility (syncing the \"vanished\" Bukkit metadata key).");
        return Optional.of(new DiscordSrvVanishSync(owningPlugin, invisibilityService));
    }
}
