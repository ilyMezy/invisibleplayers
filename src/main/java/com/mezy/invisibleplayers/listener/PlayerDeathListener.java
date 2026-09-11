package com.mezy.invisibleplayers.listener;

import com.mezy.invisibleplayers.service.DeathAttributionTracker;
import com.mezy.invisibleplayers.service.DeathMessageService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class PlayerDeathListener implements Listener {

    private final DeathMessageService deathMessageService;
    private final DeathAttributionTracker attributionTracker;

    public PlayerDeathListener(DeathMessageService deathMessageService, DeathAttributionTracker attributionTracker) {
        this.deathMessageService = deathMessageService;
        this.attributionTracker = attributionTracker;
    }

    // HIGH so most other plugins' message edits have already applied, while
    // still leaving room for MONITOR-priority listeners to observe the result.
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        try {
            deathMessageService.handleDeath(event);
        } finally {
            attributionTracker.forget(event.getEntity().getUniqueId());
        }
    }
}
