package com.mezy.invisibleplayers.util;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Pure computation of which players need to be hidden or shown, given the set
 * currently tracked as hidden and the set currently invisible.
 */
public record TabListDiff(Set<UUID> toHide, Set<UUID> toShow) {

    public static TabListDiff compute(Set<UUID> currentlyHidden, Set<UUID> currentlyInvisible) {
        Set<UUID> toHide = new HashSet<>(currentlyInvisible);
        toHide.removeAll(currentlyHidden);

        Set<UUID> toShow = new HashSet<>(currentlyHidden);
        toShow.removeAll(currentlyInvisible);

        return new TabListDiff(toHide, toShow);
    }
}
