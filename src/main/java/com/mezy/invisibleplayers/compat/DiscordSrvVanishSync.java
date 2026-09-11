package com.mezy.invisibleplayers.compat;

import com.mezy.invisibleplayers.service.InvisibilityService;
import com.mezy.invisibleplayers.util.TabListDiff;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps DiscordSRV's "vanished" Bukkit metadata key (the mechanism DiscordSRV
 * itself checks to exclude players from its Discord-facing online-player
 * list; see {@code github.scarsz.discordsrv.util.PlayerUtil#isVanished}) in
 * sync with {@link InvisibilityService}, tracked by UUID.
 *
 * <p>Only ever writes/removes the metadata entry owned by this plugin
 * instance, so it coexists with any other plugin that also sets the same
 * key. Mirrors {@code TabListTracker}'s reconciliation shape, minus
 * per-viewer bookkeeping, since DiscordSRV's list is global, not per-viewer.</p>
 */
public final class DiscordSrvVanishSync {

    static final String VANISHED_METADATA_KEY = "vanished";

    private final Plugin owningPlugin;
    private final InvisibilityService invisibilityService;
    private final Set<UUID> markedVanished = new HashSet<>();

    public DiscordSrvVanishSync(Plugin owningPlugin, InvisibilityService invisibilityService) {
        this.owningPlugin = owningPlugin;
        this.invisibilityService = invisibilityService;
    }

    public int trackedVanishedCount() {
        return markedVanished.size();
    }

    /** Safety-net reconciliation pass: corrects any drift for every online player. */
    public void reconcileAll(Collection<? extends Player> online) {
        Set<UUID> invisibleNow = new HashSet<>();
        for (Player player : online) {
            if (invisibilityService.isInvisible(player)) {
                invisibleNow.add(player.getUniqueId());
            }
        }

        TabListDiff diff = TabListDiff.compute(markedVanished, invisibleNow);

        for (UUID uuid : diff.toHide()) {
            Player target = findOnline(online, uuid);
            if (target != null) {
                markVanished(target);
            }
        }
        for (UUID uuid : diff.toShow()) {
            Player target = findOnline(online, uuid);
            if (target != null) {
                unmarkVanished(target);
            } else {
                markedVanished.remove(uuid);
            }
        }
    }

    /** Immediate, event-driven reconciliation for a single player. */
    public void reconcileOne(Player player) {
        boolean invisible = invisibilityService.isInvisible(player);
        boolean tracked = markedVanished.contains(player.getUniqueId());

        if (invisible && !tracked) {
            markVanished(player);
        } else if (!invisible && tracked) {
            unmarkVanished(player);
        }
    }

    /** Removes our metadata entry and bookkeeping for a departing player. */
    public void forgetPlayer(Player player) {
        if (markedVanished.remove(player.getUniqueId())) {
            player.removeMetadata(VANISHED_METADATA_KEY, owningPlugin);
        }
    }

    /** Restores (un-vanishes) everything this plugin marked, regardless of any enabled flag. */
    public void restoreAll(Collection<? extends Player> online) {
        for (UUID uuid : new HashSet<>(markedVanished)) {
            Player target = findOnline(online, uuid);
            if (target != null) {
                target.removeMetadata(VANISHED_METADATA_KEY, owningPlugin);
            }
        }
        markedVanished.clear();
    }

    // FixedMetadataValue is deprecated on this Paper API in favor of PersistentDataContainer,
    // but DiscordSRV's PlayerUtil#isVanished reads exactly this Bukkit Metadatable system --
    // it does not read PersistentDataContainer -- so this is the only way to reach DiscordSRV's
    // documented extension point. Unavoidable interop constraint, not a shortcut.
    @SuppressWarnings("deprecation")
    private void markVanished(Player player) {
        player.setMetadata(VANISHED_METADATA_KEY, new FixedMetadataValue(owningPlugin, true));
        markedVanished.add(player.getUniqueId());
    }

    private void unmarkVanished(Player player) {
        player.removeMetadata(VANISHED_METADATA_KEY, owningPlugin);
        markedVanished.remove(player.getUniqueId());
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
