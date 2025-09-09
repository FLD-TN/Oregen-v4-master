package me.sfclog.oregen4.util;

import me.sfclog.oregen4.Main;
import me.sfclog.oregen4.config.OreLevel;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache permission-based OreLevel cho người chơi
 * Giải quyết vấn đề khi chủ đảo offline
 * Tối ưu hiệu suất bằng cách giảm số lượng truy vấn database
 */
public class PermissionCache {
    private static final Map<UUID, Map<World.Environment, CachedOreLevel>> cache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 300000; // 5 phút (ms)
    private static BukkitTask cleanupTask;
    private static final long CLEANUP_INTERVAL = 300000; // 5 phút (ms)

    public static class CachedOreLevel {
        private final OreLevel oreLevel;
        private final long timestamp;

        public CachedOreLevel(OreLevel oreLevel) {
            this.oreLevel = oreLevel;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION;
        }

        public OreLevel getOreLevel() {
            return oreLevel;
        }
    }

    /**
     * Khởi tạo hệ thống cache và lên lịch cho task dọn dẹp
     */
    public static void initialize() {
        clearAllCache();

        // Hủy task cũ nếu có
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }

        // Lên lịch task dọn dẹp cache định kỳ
        cleanupTask = Main.pl.getServer().getScheduler().runTaskTimerAsynchronously(Main.pl,
                () -> cleanupExpiredCache(),
                CLEANUP_INTERVAL / 50, // Ticks đầu tiên (chuyển từ ms sang tick)
                CLEANUP_INTERVAL / 50 // Ticks lặp lại
        );
    }

    /**
     * Dọn dẹp các cache đã hết hạn
     */
    public static void cleanupExpiredCache() {
        try {
            int cleaned = 0;

            for (Iterator<Map.Entry<UUID, Map<World.Environment, CachedOreLevel>>> iterator = cache.entrySet()
                    .iterator(); iterator.hasNext();) {

                Map.Entry<UUID, Map<World.Environment, CachedOreLevel>> entry = iterator.next();
                Map<World.Environment, CachedOreLevel> playerCache = entry.getValue();

                // Dọn dẹp các cache environment đã hết hạn
                playerCache.entrySet().removeIf(envEntry -> envEntry.getValue().isExpired());

                // Nếu không còn cache environment nào, xóa player cache
                if (playerCache.isEmpty()) {
                    iterator.remove();
                    cleaned++;
                }
            }

            if (cleaned > 0 && Main.pl.getConfig().getBoolean("debug", false)) {
                Main.sendlog("§a[OreGen4] Cleaned " + cleaned + " expired permission caches");
            }
        } catch (Exception e) {
            Main.sendlog("§c[OreGen4] Error while cleaning permission cache: " + e.getMessage());
        }
    }

    /**
     * Lấy OreLevel đã cache cho player và environment
     * 
     * @param uuid UUID của player
     * @param env  Environment
     * @return OreLevel hoặc null nếu không có trong cache hoặc đã hết hạn
     */
    public static OreLevel getCachedLevel(UUID uuid, World.Environment env) {
        Map<World.Environment, CachedOreLevel> playerCache = cache.get(uuid);
        if (playerCache == null) {
            if (Main.pl.getConfig().getBoolean("debug", false)) {
                Main.sendlog("§c[OreGen4] No cache found for player " + uuid);
            }
            return null;
        }

        CachedOreLevel cachedLevel = playerCache.get(env);
        if (cachedLevel == null) {
            if (Main.pl.getConfig().getBoolean("debug", false)) {
                Main.sendlog("§c[OreGen4] No cache found for player " + uuid + " in environment " + env);
            }
            return null;
        }

        if (cachedLevel.isExpired()) {
            if (Main.pl.getConfig().getBoolean("debug", false)) {
                Main.sendlog("§c[OreGen4] Cache expired for player " + uuid + " in environment " + env);
            }
            return null;
        }

        if (Main.pl.getConfig().getBoolean("debug", false)) {
            Main.sendlog("§a[OreGen4] Using cached level for player " + uuid +
                    " in environment " + env + ": " + cachedLevel.getOreLevel().getPermission());
        }
        return cachedLevel.getOreLevel();
    }

    /**
     * Cache OreLevel cho player và environment
     * 
     * @param uuid     UUID của player
     * @param env      Environment
     * @param oreLevel OreLevel để cache
     */
    public static void cacheOreLevel(UUID uuid, World.Environment env, OreLevel oreLevel) {
        // Kiểm tra xem có cache cũ không
        Map<World.Environment, CachedOreLevel> playerCache = cache.get(uuid);
        boolean isUpdating = false;
        String oldPermission = null;

        if (playerCache != null) {
            CachedOreLevel oldCache = playerCache.get(env);
            if (oldCache != null) {
                isUpdating = true;
                oldPermission = oldCache.getOreLevel().getPermission();
            }
        }

        // Cập nhật hoặc tạo mới cache
        playerCache = cache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        playerCache.put(env, new CachedOreLevel(oreLevel));

        // Log thông tin
        if (isUpdating) {
            Main.sendlog("§e[OreGen4] §bCập nhật cache quyền cho người chơi §a" + uuid +
                    "§b từ §c" + oldPermission + "§b thành §a" + oreLevel.getPermission());
        } else {
            Main.sendlog("§a[OreGen4] Permission cache created for player " + uuid +
                    " in environment " + env + " with permission " + oreLevel.getPermission());
        }
    }

    /**
     * Xóa cache của một player
     * 
     * @param uuid UUID của player
     */
    public static void clearCache(UUID uuid) {
        cache.remove(uuid);
    }

    /**
     * Xóa cache của một player bằng tên
     * 
     * @param playerName Tên của người chơi
     */
    public static void clearPlayerCache(String playerName) {
        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerName);
        if (player != null) {
            clearCache(player.getUniqueId());
            if (Main.getInstance().getConfig().getBoolean("debug", false)) {
                Main.sendlog("§a[OreGen4] Cleared cache for player: " + playerName);
            }
        }
    }

    /**
     * Reset và cập nhật cache cho người chơi
     * 
     * @param playerName Tên của người chơi
     */
    public static void resetPlayerCache(String playerName) {
        clearPlayerCache(playerName);
        // Cache sẽ được tạo lại khi cần
        if (Main.getInstance().getConfig().getBoolean("debug", false)) {
            Main.sendlog("§a[OreGen4] Reset cache for player: " + playerName);
        }
    }

    /**
     * Xóa toàn bộ cache
     */
    public static void clearAllCache() {
        cache.clear();
    }

    /**
     * Lấy thời gian cache (ms)
     */
    public static long getCacheDuration() {
        return CACHE_DURATION;
    }

    /**
     * Hủy tất cả task dọn dẹp và xóa cache khi plugin shutdown
     */
    public static void shutdown() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        clearAllCache();
    }
}
