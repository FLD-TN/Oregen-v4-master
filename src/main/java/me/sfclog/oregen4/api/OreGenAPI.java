package me.sfclog.oregen4.api;

import me.sfclog.oregen4.util.Color;
import me.sfclog.oregen4.util.PermissionCache;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

/**
 * Public API for OreGen4
 * Other plugins can use this to interact with OreGen4
 */
public class OreGenAPI {

    private static OreGenAPI instance;
    private static LuckPerms luckPermsApi;

    /**
     * Constructor initializes the API and LuckPerms
     */
    public OreGenAPI() {
        instance = this;

        // Set up LuckPerms
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager()
                    .getRegistration(LuckPerms.class);
            if (provider != null) {
                luckPermsApi = provider.getProvider();
            }
        }
    }

    /**
     * Get the singleton instance of the API
     * 
     * @return The API instance
     */
    public static OreGenAPI getInstance() {
        return instance;
    }

    /**
     * Update a player's ore generation level permission
     * 
     * @param player The player to update
     * @param level  The level to set (e.g., "lv1", "lv2", "default")
     * @return True if successful, false otherwise
     */
    public boolean updatePlayerPermission(Player player, String level) {
        if (player == null || level == null) {
            return false;
        }

        if (luckPermsApi == null) {
            Bukkit.getLogger().warning(Color.tran("&cLuckPerms API not available for permission update!"));
            return false;
        }

        try {
            UUID uuid = player.getUniqueId();
            User user = luckPermsApi.getUserManager().getUser(uuid);

            if (user == null) {
                return false;
            }

            // Remove any existing oregen permissions
            user.data().clear(node -> node.getKey().startsWith("oregen."));

            // Add the new permission
            String permission = "oregen." + level.toLowerCase();
            Node node = Node.builder(permission).build();
            user.data().add(node);

            // Save the user
            luckPermsApi.getUserManager().saveUser(user);

            // Clear the permission cache for this player
            clearPlayerCache(player.getName());

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Clear a player's permission cache to force a refresh
     * 
     * @param playerName The name of the player
     */
    public void clearPlayerCache(String playerName) {
        PermissionCache.clearPlayerCache(playerName);
    }
}
