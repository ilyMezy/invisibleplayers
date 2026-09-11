package com.mezy.invisibleplayers.util;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabListDiffTest {

    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();
    private static final UUID C = UUID.randomUUID();

    @Test
    void newlyInvisiblePlayerIsMarkedToHide() {
        TabListDiff diff = TabListDiff.compute(Set.of(), Set.of(A));

        assertEquals(Set.of(A), diff.toHide());
        assertTrue(diff.toShow().isEmpty());
    }

    @Test
    void noLongerInvisiblePlayerIsMarkedToShow() {
        TabListDiff diff = TabListDiff.compute(Set.of(A), Set.of());

        assertTrue(diff.toHide().isEmpty());
        assertEquals(Set.of(A), diff.toShow());
    }

    @Test
    void stillInvisiblePlayerIsUntouched() {
        TabListDiff diff = TabListDiff.compute(Set.of(A), Set.of(A));

        assertTrue(diff.toHide().isEmpty());
        assertTrue(diff.toShow().isEmpty());
    }

    @Test
    void multiplePlayersAreIndependentlyDiffed() {
        // A stays hidden, B becomes visible again, C newly goes invisible.
        TabListDiff diff = TabListDiff.compute(Set.of(A, B), Set.of(A, C));

        assertEquals(Set.of(C), diff.toHide());
        assertEquals(Set.of(B), diff.toShow());
    }

    @Test
    void emptyInputsProduceEmptyDiff() {
        TabListDiff diff = TabListDiff.compute(Set.of(), Set.of());

        assertTrue(diff.toHide().isEmpty());
        assertTrue(diff.toShow().isEmpty());
    }
}
