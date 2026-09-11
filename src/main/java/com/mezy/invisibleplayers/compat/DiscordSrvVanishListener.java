package com.mezy.invisibleplayers.compat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

/**
 * Event-driven triggers for {@link DiscordSrvVanishSync}, mirroring the core
 * plugin's {@code PotionEffectListener}/{@code PlayerJoinQuitListener}
 * reasoning: {@code EntityPotionEffectEvent} fires before the effect change
 * lands, so reconciliation is deferred one tick.
 */
public final class DiscordSrvVanishListener implements Listener {

    private final Plugin plugin;
    private final DiscordSrvVanishSync sync;

    public DiscordSrvVanishListener(Plugin plugin, DiscordSrvVanishSync sync) {
        this.plugin = plugin;
        this.sync = sync;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityPotionEffect(EntityPotionEffectEvent event) {
        if (event.getModifiedType() != PotionEffectType.INVISIBILITY) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                sync.reconcileOne(player);
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        sync.reconcileOne(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sync.forgetPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        sync.reconcileOne(event.getPlayer());
    }
}
