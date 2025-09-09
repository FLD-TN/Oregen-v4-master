package me.sfclog.oregen4.command;

import me.sfclog.oregen4.util.EnhancedPermissionCache;
import me.sfclog.oregen4.updater.PermissionUpdater;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/**
 * Command để hiển thị thống kê về hiệu suất cache
 */
public class StatsCommand implements CommandExecutor {

    private final Plugin plugin;

    public StatsCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("oregen.admin")) {
            sender.sendMessage(ChatColor.RED + "Bạn không có quyền thực hiện lệnh này!");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "==== Thống kê OreGen4 ====");

        // Thống kê cache
        int cachedPlayers = EnhancedPermissionCache.getCachedPlayerCount();
        int hitRate = EnhancedPermissionCache.getCacheHitRate();
        sender.sendMessage(ChatColor.YELLOW + "Cache Players: " + ChatColor.AQUA + cachedPlayers);
        sender.sendMessage(ChatColor.YELLOW + "Cache Hit Rate: " + ChatColor.AQUA + hitRate + "%");

        // Thống kê PermissionUpdater
        int updaterCachedPlayers = PermissionUpdater.getCachedPlayerCount();
        boolean updaterEnabled = PermissionUpdater.isEnabled();
        long updateInterval = PermissionUpdater.getUpdateInterval() / 1000; // Convert to seconds

        sender.sendMessage(ChatColor.YELLOW + "Permission Cache Players: " + ChatColor.AQUA + updaterCachedPlayers);
        sender.sendMessage(ChatColor.YELLOW + "Updater Status: " +
                (updaterEnabled ? ChatColor.GREEN + "Active" : ChatColor.RED + "Inactive"));
        sender.sendMessage(ChatColor.YELLOW + "Update Interval: " + ChatColor.AQUA + updateInterval + "s");

        // Thông tin về cache vị trí
        int cachedLocations = me.sfclog.oregen4.util.LocationOwnerCache.getCachedLocationsCount();
        sender.sendMessage(ChatColor.YELLOW + "Cached Locations: " + ChatColor.AQUA + cachedLocations);

        sender.sendMessage(ChatColor.GREEN + "========================");

        return true;
    }
}
