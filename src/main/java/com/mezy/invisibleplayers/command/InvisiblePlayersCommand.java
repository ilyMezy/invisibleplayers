package com.mezy.invisibleplayers.command;

import com.mezy.invisibleplayers.InvisiblePlayersPlugin;
import com.mezy.invisibleplayers.compat.DiscordSrvVanishSync;
import com.mezy.invisibleplayers.config.PluginConfig;
import com.mezy.invisibleplayers.service.InvisibilityService;
import com.mezy.invisibleplayers.service.TabListTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Locale;

public final class InvisiblePlayersCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("help", "reload", "status");

    private final InvisiblePlayersPlugin plugin;
    private final TabListTracker tabListTracker;
    private final InvisibilityService invisibilityService;

    public InvisiblePlayersCommand(InvisiblePlayersPlugin plugin, TabListTracker tabListTracker,
                                    InvisibilityService invisibilityService) {
        this.plugin = plugin;
        this.tabListTracker = tabListTracker;
        this.invisibilityService = invisibilityService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "help";

        switch (sub) {
            case "reload" -> handleReload(sender);
            case "status" -> handleStatus(sender);
            case "help" -> sendHelp(sender);
            default -> {
                sender.sendMessage(Component.text("Unknown subcommand: " + sub, NamedTextColor.RED));
                sendHelp(sender);
            }
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("invisibleplayers.reload")) {
            sender.sendMessage(Component.text("You do not have permission to do that.", NamedTextColor.RED));
            return;
        }
        try {
            plugin.reload();
            sender.sendMessage(Component.text("InvisiblePlayers configuration reloaded.", NamedTextColor.GREEN));
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reload InvisiblePlayers config: " + e.getMessage());
            sender.sendMessage(Component.text("Reload failed; see console for details.", NamedTextColor.RED));
        }
    }

    private void handleStatus(CommandSender sender) {
        if (!sender.hasPermission("invisibleplayers.status")) {
            sender.sendMessage(Component.text("You do not have permission to do that.", NamedTextColor.RED));
            return;
        }

        PluginConfig config = plugin.pluginConfig();
        long invisibleCount = Bukkit.getOnlinePlayers().stream()
                .filter(invisibilityService::isInvisible)
                .count();

        sender.sendMessage(Component.text("InvisiblePlayers v" + plugin.getPluginMeta().getVersion(), NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Tab-list hiding: " + (config.tabListEnabled() ? "enabled" : "disabled"),
                config.tabListEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
        sender.sendMessage(Component.text("Death-message redaction: " + (config.deathRedactionEnabled() ? "enabled" : "disabled"),
                config.deathRedactionEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
        sender.sendMessage(Component.text("Invisible players online: " + invisibleCount, NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Tracked hidden players: " + tabListTracker.trackedHiddenCount(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Reconciliation interval: " + config.reconciliationIntervalSeconds() + "s", NamedTextColor.YELLOW));

        DiscordSrvVanishSync discordSync = plugin.discordSrvVanishSync();
        if (discordSync != null) {
            sender.sendMessage(Component.text(
                    "DiscordSRV compatibility: active (" + discordSync.trackedVanishedCount() + " synced)",
                    NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("DiscordSRV compatibility: not detected", NamedTextColor.GRAY));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("InvisiblePlayers commands:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/invisibleplayers help - show this message", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/invisibleplayers reload - reload configuration", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/invisibleplayers status - show plugin status", NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        return List.of();
    }
}
