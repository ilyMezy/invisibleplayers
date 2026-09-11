package com.mezy.invisibleplayers.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeathAttributionTrackerTest {

    private final AtomicLong now = new AtomicLong(0L);
    private final DeathAttributionTracker tracker = new DeathAttributionTracker(now::get, 10_000L);

    @Test
    void recordedHitIsRetrievableWithinTtl() {
        UUID victim = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();

        tracker.recordHit(victim, attacker, true);
        now.set(5_000L);

        var record = tracker.get(victim);
        assertTrue(record.isPresent());
        assertEquals(attacker, record.get().attackerUuid());
        assertTrue(record.get().attackerWasInvisible());
    }

    @Test
    void hitExpiresAfterTtl() {
        UUID victim = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();

        tracker.recordHit(victim, attacker, true);
        now.set(10_001L);

        assertTrue(tracker.get(victim).isEmpty());
    }

    @Test
    void unknownVictimHasNoRecord() {
        assertFalse(tracker.get(UUID.randomUUID()).isPresent());
    }

    @Test
    void laterHitOverwritesEarlierOne() {
        UUID victim = UUID.randomUUID();
        UUID firstAttacker = UUID.randomUUID();
        UUID secondAttacker = UUID.randomUUID();

        tracker.recordHit(victim, firstAttacker, true);
        now.set(1_000L);
        tracker.recordHit(victim, secondAttacker, false);

        var record = tracker.get(victim);
        assertTrue(record.isPresent());
        assertEquals(secondAttacker, record.get().attackerUuid());
        assertFalse(record.get().attackerWasInvisible());
    }

    @Test
    void forgetRemovesRecord() {
        UUID victim = UUID.randomUUID();
        tracker.recordHit(victim, UUID.randomUUID(), true);

        tracker.forget(victim);

        assertTrue(tracker.get(victim).isEmpty());
    }
}
