package com.mezy.invisibleplayers.listener;

import com.mezy.invisibleplayers.service.DeathAttributionTracker;
import com.mezy.invisibleplayers.service.InvisibilityService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Records, per victim, whether the responsible player attacker was invisible
 * at the moment of the hit -- melee (direct Player damager) and player-owned
 * projectiles (arrows, tridents, and anything else Paper attributes to a
 * shooting player via {@link Projectile#getShooter()}).
 */
public final class DamageAttributionListener implements Listener {

    private final InvisibilityService invisibilityService;
    private final DeathAttributionTracker attributionTracker;

    public DamageAttributionListener(InvisibilityService invisibilityService,
                                      DeathAttributionTracker attributionTracker) {
        this.invisibilityService = invisibilityService;
        this.attributionTracker = attributionTracker;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }

        attributionTracker.recordHit(victim.getUniqueId(), attacker.getUniqueId(),
                invisibilityService.isInvisible(attacker));
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        return null;
    }
}
