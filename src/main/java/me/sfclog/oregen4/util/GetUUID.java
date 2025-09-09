package me.sfclog.oregen4.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import me.sfclog.oregen4.hook.BentoBoxHook;
import me.sfclog.oregen4.hook.SuperiorSkyblockHook;

public class GetUUID {

    public static UUID getUUID(String player_name) {
        Player p = Bukkit.getPlayer(player_name);
        if (p != null) {
            return p.getUniqueId();
        }
        return null;
    }

    /**
     * Lấy tên người chơi từ UUID
     * 
     * @param uuid UUID của người chơi
     * @return Tên người chơi hoặc null nếu không tìm thấy
     */
    public static String getName(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            return p.getName();
        }
        return null;
    }

    public static UUID getUUID_PlayerNear(Location loc) {
        List<Player> player = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getWorld() == loc.getWorld() && p.getLocation().distance(loc) < 10)
                .collect(Collectors.toList());
        if (player != null && !player.isEmpty()) {
            Player p = player.get(0);
            if (p != null) {
                return p.getUniqueId();
            }
        }
        return null;
    }

    /**
     * Lấy UUID của chủ đảo từ location
     * 
     * @param loc Vị trí cần kiểm tra
     * @return UUID của chủ đảo hoặc null nếu không tìm thấy
     */
    public static UUID getOwner(Location loc) {
        Logger logger = Bukkit.getLogger();

        // 1. Đầu tiên kiểm tra trong cache vị trí
        UUID cachedOwner = LocationOwnerCache.getCachedOwner(loc);
        if (cachedOwner != null) {
            // Nếu có trong cache, trả về ngay
            return cachedOwner;
        }

        // 2. Kiểm tra SuperiorSkyblock
        try {
            Plugin superiorPlugin = Bukkit.getPluginManager().getPlugin("SuperiorSkyblock2");
            if (superiorPlugin != null && superiorPlugin.isEnabled()) {
                UUID owner = SuperiorSkyblockHook.getIslandOwnerUUID(loc);
                if (owner != null) {
                    // Lưu vào cache trước khi trả về
                    LocationOwnerCache.cacheOwner(loc, owner);
                    return owner;
                }
            }
        } catch (Exception e) {
            logger.warning("[OreGen4] SuperiorSkyblock hook error: " + e.getMessage());
        }

        // 3. Kiểm tra BentoBox
        try {
            Plugin bentoPlugin = Bukkit.getPluginManager().getPlugin("BentoBox");
            if (bentoPlugin != null && bentoPlugin.isEnabled()) {
                UUID owner = BentoBoxHook.getIslandOwnerUUID(loc);
                if (owner != null) {
                    // Lưu vào cache trước khi trả về
                    LocationOwnerCache.cacheOwner(loc, owner);
                    return owner;
                }
            }
        } catch (Exception e) {
            logger.warning("[OreGen4] BentoBox hook error: " + e.getMessage());
        }

        // 4. Nếu không tìm thấy, thử người chơi gần đó
        UUID nearbyPlayerUUID = getUUID_PlayerNear(loc);
        if (nearbyPlayerUUID != null) {
            // Lưu vào cache trước khi trả về
            LocationOwnerCache.cacheOwner(loc, nearbyPlayerUUID);
            return nearbyPlayerUUID;
        }

        // Không tìm thấy chủ sở hữu nào
        return null;
    }
}
