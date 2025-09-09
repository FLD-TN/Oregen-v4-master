package me.sfclog.oregen4.listener;

import me.sfclog.oregen4.util.Color;
import me.sfclog.oregen4.util.PermissionCache;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for permission changes in LuckPerms
 * Clears permission cache when permissions are changed
 */
public class PermissionUpdateListener implements Listener {

    private final LuckPerms luckPerms;

    public PermissionUpdateListener(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;

        // Register LuckPerms event listeners
        EventBus eventBus = luckPerms.getEventBus();

        // Listen for permission node additions
        eventBus.subscribe(NodeAddEvent.class, this::onNodeAdd);

        // Listen for permission node removals
        eventBus.subscribe(NodeRemoveEvent.class, this::onNodeRemove);

        Bukkit.getLogger().info(Color.tran("&aLuckPerms event listeners registered successfully!"));
    }

    /**
     * Handle LuckPerms node addition events
     * 
     * @param event The node add event
     */
    private void onNodeAdd(NodeAddEvent event) {
        // Check if this is a user
        if (!(event.getTarget() instanceof User)) {
            return;
        }

        // Check if this is an oregen permission
        String permission = event.getNode().getKey();
        if (!permission.startsWith("oregen.")) {
            return;
        }

        // Get the username
        User user = (User) event.getTarget();
        String username = user.getUsername();

        if (username != null) {
            // Clear the permission cache for this player
            PermissionCache.clearPlayerCache(username);
            Bukkit.getLogger().info(
                    Color.tran("&aLuckPerms oregen permission added for " + username + ", cleared permission cache!"));
        }
    }

    /**
     * Handle LuckPerms node removal events
     * 
     * @param event The node remove event
     */
    private void onNodeRemove(NodeRemoveEvent event) {
        // Check if this is a user
        if (!(event.getTarget() instanceof User)) {
            return;
        }

        // Check if this is an oregen permission
        String permission = event.getNode().getKey();
        if (!permission.startsWith("oregen.")) {
            return;
        }

        // Get the username
        User user = (User) event.getTarget();
        String username = user.getUsername();

        if (username != null) {
            // Clear the permission cache for this player
            PermissionCache.clearPlayerCache(username);
            Bukkit.getLogger().info(Color
                    .tran("&aLuckPerms oregen permission removed for " + username + ", cleared permission cache!"));
        }
    }

    // Standard player events for cache management

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Make sure to populate the cache on join
        PermissionCache.resetPlayerCache(event.getPlayer().getName());
        // Cập nhật cache mới
        try {
            me.sfclog.oregen4.util.EnhancedPermissionCache.clearPlayerCache(event.getPlayer().getUniqueId());
        } catch (Exception e) {
            // Ignore errors
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clear cache on quit to save memory
        PermissionCache.clearPlayerCache(event.getPlayer().getName());
        // Cập nhật cache mới
        try {
            me.sfclog.oregen4.util.EnhancedPermissionCache.clearPlayerCache(event.getPlayer().getUniqueId());
        } catch (Exception e) {
            // Ignore errors
        }
    }
}
