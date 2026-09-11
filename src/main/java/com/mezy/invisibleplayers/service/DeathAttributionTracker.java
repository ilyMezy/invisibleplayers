package com.mezy.invisibleplayers.service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Short-lived UUID to "was the attacker invisible at the time of the last hit"
 * tracking, so death-message redaction can use invisibility state at the
 * moment the lethal damage was dealt rather than at the moment the death
 * event fires. Deliberately minimal: not a general combat-log system.
 */
public final class DeathAttributionTracker {

    private static final long DEFAULT_TTL_MILLIS = 10_000L;

    private final Map<UUID, HitRecord> lastHits = new ConcurrentHashMap<>();
    private final LongSupplier clock;
    private final long ttlMillis;

    public DeathAttributionTracker() {
        this(System::currentTimeMillis, DEFAULT_TTL_MILLIS);
    }

    public DeathAttributionTracker(LongSupplier clock, long ttlMillis) {
        this.clock = clock;
        this.ttlMillis = ttlMillis;
    }

    public void recordHit(UUID victim, UUID attacker, boolean attackerInvisible) {
        lastHits.put(victim, new HitRecord(attacker, attackerInvisible, clock.getAsLong()));
    }

    public Optional<HitRecord> get(UUID victim) {
        HitRecord record = lastHits.get(victim);
        if (record == null) {
            return Optional.empty();
        }
        if (clock.getAsLong() - record.timestampMillis() > ttlMillis) {
            lastHits.remove(victim);
            return Optional.empty();
        }
        return Optional.of(record);
    }

    public void forget(UUID victim) {
        lastHits.remove(victim);
    }

    public record HitRecord(UUID attackerUuid, boolean attackerWasInvisible, long timestampMillis) {
    }
}
