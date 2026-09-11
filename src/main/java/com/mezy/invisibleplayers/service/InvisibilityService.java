package com.mezy.invisibleplayers.service;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * Owns the single authoritative check for "is this player invisible".
 *
 * <p>Always reflects the live Bukkit/Paper potion effect state; never a cached
 * boolean, permission, name list, or effect history.</p>
 */
public final class InvisibilityService {

    public boolean isInvisible(Player player) {
        return player.hasPotionEffect(PotionEffectType.INVISIBILITY);
    }
}
