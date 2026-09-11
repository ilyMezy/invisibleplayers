package com.mezy.invisibleplayers;

import com.mezy.invisibleplayers.command.InvisiblePlayersCommand;
import com.mezy.invisibleplayers.compat.DiscordSrvIntegration;
import com.mezy.invisibleplayers.compat.DiscordSrvVanishListener;
import com.mezy.invisibleplayers.compat.DiscordSrvVanishSync;
import com.mezy.invisibleplayers.config.ConfigLoader;
import com.mezy.invisibleplayers.config.PluginConfig;
import com.mezy.invisibleplayers.listener.DamageAttributionListener;
import com.mezy.invisibleplayers.listener.PlayerDeathListener;
import com.mezy.invisibleplayers.listener.PlayerJoinQuitListener;
import com.mezy.invisibleplayers.listener.PotionEffectListener;
import com.mezy.invisibleplayers.service.DeathAttributionTracker;
import com.mezy.invisibleplayers.service.DeathMessageService;
import com.mezy.invisibleplayers.service.InvisibilityService;
import com.mezy.invisibleplayers.service.ReconciliationTask;
import com.mezy.invisibleplayers.service.TabListTracker;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Optional;

/**
 * Bootstrap only: loads config, constructs services, registers
 * listeners/commands, and starts/stops the reconciliation task. No feature
 * logic lives here -- see the {@code service}, {@code listener}, and
 * {@code command} packages.
 */
public final class InvisiblePlayersPlugin extends JavaPlugin {

    private PluginConfig config;
    private InvisibilityService invisibilityService;
    private TabListTracker tabListTracker;
    private DeathAttributionTracker attributionTracker;
    private DeathMessageService deathMessageService;
    private DiscordSrvVanishSync discordSrvVanishSync;
    private BukkitTask reconciliationTask;

    @Override
    public void onEnable() {
        config = ConfigLoader.load(this);

        invisibilityService = new InvisibilityService();
        tabListTracker = new TabListTracker(invisibilityService);
        tabListTracker.setEnabled(config.tabListEnabled());
        attributionTracker = new DeathAttributionTracker();
        deathMessageService = new DeathMessageService(invisibilityService, attributionTracker, config);

        getServer().getPluginManager().registerEvents(new PotionEffectListener(this, tabListTracker), this);
        getServer().getPluginManager().registerEvents(
                new PlayerJoinQuitListener(tabListTracker, attributionTracker), this);
        getServer().getPluginManager().registerEvents(
                new PlayerDeathListener(deathMessageService, attributionTracker), this);
        getServer().getPluginManager().registerEvents(
                new DamageAttributionListener(invisibilityService, attributionTracker), this);

        InvisiblePlayersCommand commandExecutor = new InvisiblePlayersCommand(this, tabListTracker, invisibilityService);
        PluginCommand pluginCommand = getCommand("invisibleplayers");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(commandExecutor);
            pluginCommand.setTabCompleter(commandExecutor);
        }

        Optional<DiscordSrvVanishSync> discordSrv = DiscordSrvIntegration.tryEnable(this, invisibilityService, getLogger());
        discordSrvVanishSync = discordSrv.orElse(null);
        if (discordSrvVanishSync != null) {
            getServer().getPluginManager().registerEvents(new DiscordSrvVanishListener(this, discordSrvVanishSync), this);
            discordSrvVanishSync.reconcileAll(Bukkit.getOnlinePlayers());
        }

        if (reconciliationTaskShouldRun(config)) {
            scheduleReconciliationTask(config.reconciliationIntervalSeconds());
        }
        tabListTracker.reconcileAll(Bukkit.getOnlinePlayers());

        getLogger().info("InvisiblePlayers enabled (v" + getPluginMeta().getVersion() + ")");
    }

    @Override
    public void onDisable() {
        cancelReconciliationTask();
        if (tabListTracker != null) {
            tabListTracker.restoreAll(Bukkit.getOnlinePlayers());
        }
        if (discordSrvVanishSync != null) {
            discordSrvVanishSync.restoreAll(Bukkit.getOnlinePlayers());
        }
        getLogger().info("InvisiblePlayers disabled");
    }

    /** Reloads config, applies it live, and restarts the reconciliation task only if needed. */
    public void reload() {
        PluginConfig previous = config;
        PluginConfig updated = ConfigLoader.load(this);
        config = updated;
        deathMessageService.updateConfig(updated);

        tabListTracker.setEnabled(updated.tabListEnabled());
        if (!updated.tabListEnabled()) {
            tabListTracker.restoreAll(Bukkit.getOnlinePlayers());
        } else {
            tabListTracker.reconcileAll(Bukkit.getOnlinePlayers());
        }

        if (discordSrvVanishSync != null) {
            discordSrvVanishSync.reconcileAll(Bukkit.getOnlinePlayers());
        }

        boolean enabledChanged = reconciliationTaskShouldRun(previous) != reconciliationTaskShouldRun(updated);
        boolean intervalChanged = previous.reconciliationIntervalSeconds() != updated.reconciliationIntervalSeconds();
        if (enabledChanged || intervalChanged) {
            cancelReconciliationTask();
            if (reconciliationTaskShouldRun(updated)) {
                scheduleReconciliationTask(updated.reconciliationIntervalSeconds());
            }
        }
    }

    public PluginConfig pluginConfig() {
        return config;
    }

    public TabListTracker tabListTracker() {
        return tabListTracker;
    }

    public InvisibilityService invisibilityService() {
        return invisibilityService;
    }

    /** Nullable: null means DiscordSRV was not detected/enabled at startup. */
    public DiscordSrvVanishSync discordSrvVanishSync() {
        return discordSrvVanishSync;
    }

    /** The single reconciliation task must run if either tab-list hiding or DiscordSRV compat needs it. */
    private boolean reconciliationTaskShouldRun(PluginConfig cfg) {
        return cfg.tabListEnabled() || discordSrvVanishSync != null;
    }

    private void scheduleReconciliationTask(int intervalSeconds) {
        long ticks = 20L * intervalSeconds;
        ReconciliationTask tabListReconciliation = new ReconciliationTask(tabListTracker);
        Runnable combined = () -> {
            tabListReconciliation.run();
            if (discordSrvVanishSync != null) {
                discordSrvVanishSync.reconcileAll(Bukkit.getOnlinePlayers());
            }
        };
        reconciliationTask = getServer().getScheduler().runTaskTimer(this, combined, ticks, ticks);
    }

    private void cancelReconciliationTask() {
        if (reconciliationTask != null) {
            reconciliationTask.cancel();
            reconciliationTask = null;
        }
    }
}
