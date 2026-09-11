package com.mezy.invisibleplayers.listener;

import com.mezy.invisibleplayers.service.DeathAttributionTracker;
import com.mezy.invisibleplayers.service.TabListTracker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Join/quit/respawn bookkeeping for the tab-list feature: a viewer who joins
 * after a player has already gone invisible must not see that player from the
 * start, and a player who joins while already invisible must be hidden
 * immediately. Respawn is handled too since potion effects clear on death.
 */
public final class PlayerJoinQuitListener implements Listener {

    private final TabListTracker tabListTracker;
    private final DeathAttributionTracker attributionTracker;

    public PlayerJoinQuitListener(TabListTracker tabListTracker, DeathAttributionTracker attributionTracker) {
        this.tabListTracker = tabListTracker;
        this.attributionTracker = attributionTracker;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();
        var online = Bukkit.getOnlinePlayers();
        tabListTracker.applyToNewViewer(joined, online);
        tabListTracker.reconcileOne(joined, online);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player left = event.getPlayer();
        tabListTracker.forgetPlayer(left.getUniqueId());
        attributionTracker.forget(left.getUniqueId());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        tabListTracker.reconcileOne(player, Bukkit.getOnlinePlayers());
    }
}
