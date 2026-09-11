package com.mezy.invisibleplayers.service;

import com.mezy.invisibleplayers.config.PluginConfig;
import com.mezy.invisibleplayers.util.ComponentRedactor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Owns death-message concealment: if the responsible player killer was
 * invisible at the moment the lethal damage was dealt, replaces only the
 * killer's identity in the existing death message with the configured
 * placeholder. Never originates a new message.
 */
public final class DeathMessageService {

    private final InvisibilityService invisibilityService;
    private final DeathAttributionTracker attributionTracker;
    private PluginConfig config;

    public DeathMessageService(InvisibilityService invisibilityService,
                                DeathAttributionTracker attributionTracker,
                                PluginConfig config) {
        this.invisibilityService = invisibilityService;
        this.attributionTracker = attributionTracker;
        this.config = config;
    }

    public void updateConfig(PluginConfig config) {
        this.config = config;
    }

    public void handleDeath(PlayerDeathEvent event) {
        if (!config.deathRedactionEnabled()) {
            return;
        }

        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) {
            // No responsible player killer (mob kill, environmental death, or
            // an unattributed projectile) -- leave the message untouched.
            return;
        }

        Component message = event.deathMessage();
        if (message == null) {
            // Message already suppressed/blanked by another plugin; never originate one.
            return;
        }

        if (!wasKillerInvisibleAtHit(victim, killer)) {
            return;
        }

        Set<String> names = new HashSet<>();
        names.add(killer.getName());
        names.add(PlainTextComponentSerializer.plainText().serialize(killer.name()));

        Component replacement = Component.text(config.replacement());
        Component redacted = ComponentRedactor.redactNames(message, names, replacement);
        event.deathMessage(redacted);
    }

    private boolean wasKillerInvisibleAtHit(Player victim, Player killer) {
        Optional<DeathAttributionTracker.HitRecord> record = attributionTracker.get(victim.getUniqueId());
        if (record.isPresent() && record.get().attackerUuid().equals(killer.getUniqueId())) {
            return record.get().attackerWasInvisible();
        }
        // No hit record (e.g. attribution expired) -- fall back to the killer's
        // current invisibility state as the best available approximation.
        return invisibilityService.isInvisible(killer);
    }
}
