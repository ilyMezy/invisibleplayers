package com.mezy.invisibleplayers.service;

import com.mezy.invisibleplayers.util.TabListDiff;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Owns tab-list state changes: which players are currently hidden from which
 * viewers, tracked by UUID (never by long-lived {@link Player} references).
 *
 * <p>Uses Paper's tab-list-scoped {@code Player#unlistPlayer}/{@code listPlayer}
 * API, which only affects tab/player-list presentation and never entity
 * visibility, tracking, combat, targeting, collision, nametags, skins, or
 * display names.</p>
 */
public final class TabListTracker {

    private final InvisibilityService invisibilityService;
    private final Set<UUID> hidden = new HashSet<>();
    private boolean enabled = true;

    public TabListTracker(InvisibilityService invisibilityService) {
        this.invisibilityService = invisibilityService;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int trackedHiddenCount() {
        return hidden.size();
    }

    /** Safety-net reconciliation pass: corrects any drift for every online player. */
    public void reconcileAll(Collection<? extends Player> online) {
        if (!enabled) {
            return;
        }

        Set<UUID> invisibleNow = new HashSet<>();
        for (Player player : online) {
            if (invisibilityService.isInvisible(player)) {
                invisibleNow.add(player.getUniqueId());
            }
        }

        TabListDiff diff = TabListDiff.compute(hidden, invisibleNow);

        for (UUID uuid : diff.toHide()) {
            Player target = findOnline(online, uuid);
            if (target != null) {
                hideFromAll(target, online);
            }
        }
        for (UUID uuid : diff.toShow()) {
            Player target = findOnline(online, uuid);
            if (target != null) {
                showToAll(target, online);
            } else {
                hidden.remove(uuid);
            }
        }
    }

    /** Immediate, event-driven reconciliation for a single player. */
    public void reconcileOne(Player player, Collection<? extends Player> online) {
        if (!enabled) {
            return;
        }

        boolean invisible = invisibilityService.isInvisible(player);
        boolean tracked = hidden.contains(player.getUniqueId());

        if (invisible && !tracked) {
            hideFromAll(player, online);
        } else if (!invisible && tracked) {
            showToAll(player, online);
        }
    }

    /** Ensures a newly-joined viewer never sees already-invisible players (no flash of visibility). */
    public void applyToNewViewer(Player joined, Collection<? extends Player> online) {
        if (!enabled) {
            return;
        }

        for (UUID uuid : hidden) {
            if (uuid.equals(joined.getUniqueId())) {
                continue;
            }
            Player other = findOnline(online, uuid);
            if (other != null) {
                joined.unlistPlayer(other);
            }
        }
    }

    /** Bookkeeping only; a departing player needs no tab-list packets sent. */
    public void forgetPlayer(UUID uuid) {
        hidden.remove(uuid);
    }

    /** Restores full tab-list visibility for everything this plugin has hidden, regardless of the enabled flag. */
    public void restoreAll(Collection<? extends Player> online) {
        for (UUID uuid : new HashSet<>(hidden)) {
            Player target = findOnline(online, uuid);
            if (target != null) {
                for (Player viewer : online) {
                    if (!viewer.getUniqueId().equals(uuid) && viewer.canSee(target)) {
                        viewer.listPlayer(target);
                    }
                }
            }
        }
        hidden.clear();
    }

    private void hideFromAll(Player target, Collection<? extends Player> online) {
        for (Player viewer : online) {
            if (!viewer.getUniqueId().equals(target.getUniqueId())) {
                viewer.unlistPlayer(target);
            }
        }
        hidden.add(target.getUniqueId());
    }

    private void showToAll(Player target, Collection<? extends Player> online) {
        for (Player viewer : online) {
            // listPlayer() throws IllegalStateException if the viewer can't see
            // the target at all (e.g. hidden by an unrelated plugin's hideEntity
            // call); skip those viewers rather than letting one bad viewer abort
            // the whole reconciliation pass.
            if (!viewer.getUniqueId().equals(target.getUniqueId()) && viewer.canSee(target)) {
                viewer.listPlayer(target);
            }
        }
        hidden.remove(target.getUniqueId());
    }

    private Player findOnline(Collection<? extends Player> online, UUID uuid) {
        for (Player player : online) {
            if (player.getUniqueId().equals(uuid)) {
                return player;
            }
        }
        return null;
    }
}
