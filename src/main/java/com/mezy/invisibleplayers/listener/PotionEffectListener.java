package com.mezy.invisibleplayers.listener;

import com.mezy.invisibleplayers.service.TabListTracker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

/**
 * Primary hook for invisibility changes: fires for ADD/REMOVE/CLEARED/CHANGED
 * across every cause, including effect expiry, milk, commands, and other
 * plugins/APIs.
 *
 * <p>This event fires before the effect change is actually applied to the
 * entity (a cancelled event results in no change), so the reconciliation is
 * deferred to the next tick rather than reading {@code hasPotionEffect} live
 * inside the handler.</p>
 */
public final class PotionEffectListener implements Listener {

    private final Plugin plugin;
    private final TabListTracker tabListTracker;

    public PotionEffectListener(Plugin plugin, TabListTracker tabListTracker) {
        this.plugin = plugin;
        this.tabListTracker = tabListTracker;
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
                tabListTracker.reconcileOne(player, Bukkit.getOnlinePlayers());
            }
        });
    }
}
