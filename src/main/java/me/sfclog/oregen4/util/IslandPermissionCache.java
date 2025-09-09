package me.sfclog.oregen4.util;

import me.sfclog.oregen4.Main;
import me.sfclog.oregen4.config.OreLevel;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache quyền theo đảo - chỉ kiểm tra quyền của chủ đảo và áp dụng cho tất cả
 * thành viên
 */
public class IslandPermissionCache {
    // Cache chính: IslandID -> Environment -> CachedIslandData
    private static final Map<String, Map<World.Environment, CachedIslandData>> islandCache = new ConcurrentHashMap<>();

    // Task dọn dẹp cache
    private static BukkitTask cleanupTask;

    // Thời gian cache cho mỗi đảo (ms)
    private static final long CACHE_DURATION = 1800000; // 30 phút
    private static final long CLEANUP_INTERVAL = 600000; // 10 phút

    /**
     * Khởi tạo hệ thống cache đảo
     */
    public static void initialize() {
        // Khởi tạo task dọn dẹp cache định kỳ
        cleanupTask = Main.pl.getServer().getScheduler().runTaskTimerAsynchronously(
                Main.pl,
                IslandPermissionCache::cleanupExpiredCache,
                CLEANUP_INTERVAL / 50,
                CLEANUP_INTERVAL / 50);

        Main.sendlog("§a[OreGen4] Đã khởi tạo IslandPermissionCache!");
    }

    /**
     * Dọn dẹp các mục cache đã hết hạn
     */
    private static void cleanupExpiredCache() {
        long now = System.currentTimeMillis();
        int cleaned = 0;

        for (Map.Entry<String, Map<World.Environment, CachedIslandData>> entry : islandCache.entrySet()) {
            for (Map.Entry<World.Environment, CachedIslandData> envEntry : entry.getValue().entrySet()) {
                if (envEntry.getValue().isExpired(now)) {
                    entry.getValue().remove(envEntry.getKey());
                    cleaned++;
                }
            }

            if (entry.getValue().isEmpty()) {
                islandCache.remove(entry.getKey());
            }
        }

        if (cleaned > 0 && Main.getInstance().getConfig().getBoolean("debug", false)) {
            Main.sendlog("§e[OreGen4] Đã dọn dẹp " + cleaned + " mục cache đảo hết hạn");
        }
    }

    /**
     * Lấy hoặc tạo ID của đảo từ vị trí
     */
    public static String getIslandId(Location location) {
        // Đối với BentoBox
        if (Main.hookbentobox != null) {
            return Main.hookbentobox.getIslandIdAt(location);
        }
        // Đối với SuperiorSkyblock
        else if (Main.hooksuper != null) {
            return Main.hooksuper.getIslandID(location);
        }
        // Fallback cho Vanilla - dùng chunk coordinates
        else {
            return location.getWorld().getName() + "_" +
                    (location.getBlockX() >> 4) + "_" +
                    (location.getBlockZ() >> 4);
        }
    }

    /**
     * Lấy cấp độ OreGen cho một vị trí dựa trên quyền của chủ đảo
     */
    public static OreLevel getIslandOreLevel(Location location, World.Environment env) {
        String islandId = getIslandId(location);
        if (islandId == null)
            return null;

        // Kiểm tra cache trước
        CachedIslandData cachedData = getCachedData(islandId, env);
        if (cachedData != null) {
            return cachedData.getOreLevel();
        }

        // Nếu không có trong cache, tìm chủ đảo và kiểm tra quyền
        UUID ownerUUID = GetUUID.getOwner(location);
        if (ownerUUID == null)
            return null;

        // Lấy OreLevel từ cache của chủ đảo (không gọi LuckPerms API)
        OreLevel ownerLevel = null;

        // 1. Kiểm tra EnhancedPermissionCache trước
        ownerLevel = EnhancedPermissionCache.getOreLevelFromCache(ownerUUID, env);

        // 2. Nếu không có trong EnhancedCache, kiểm tra PermissionUpdater (cache cũ)
        if (ownerLevel == null) {
            String permLevel = me.sfclog.oregen4.updater.PermissionUpdater.getCurrentPermission(ownerUUID, env);
            if (permLevel != null) {
                ownerLevel = me.sfclog.oregen4.config.ConfigManager.getLevel(env, permLevel);
            }
        }

        // Lưu vào cache đảo
        if (ownerLevel != null) {
            cacheIslandOreLevel(islandId, env, ownerUUID, ownerLevel);
        }

        return ownerLevel;
    }

    /**
     * Lấy dữ liệu từ cache
     */
    private static CachedIslandData getCachedData(String islandId, World.Environment env) {
        Map<World.Environment, CachedIslandData> envCache = islandCache.get(islandId);
        if (envCache == null)
            return null;

        CachedIslandData cachedData = envCache.get(env);
        if (cachedData == null || cachedData.isExpired(System.currentTimeMillis())) {
            return null;
        }

        return cachedData;
    }

    /**
     * Lưu cấp độ OreGen của đảo vào cache
     */
    public static void cacheIslandOreLevel(String islandId, World.Environment env, UUID ownerUUID, OreLevel oreLevel) {
        Map<World.Environment, CachedIslandData> envCache = islandCache.computeIfAbsent(
                islandId, k -> new ConcurrentHashMap<>());

        envCache.put(env, new CachedIslandData(ownerUUID, oreLevel));

        if (Main.getInstance().getConfig().getBoolean("debug", false)) {
            Main.sendlog("§e[OreGen4] Đã cache quyền đảo " + islandId + " với cấp độ " +
                    (oreLevel != null ? oreLevel.getPermission() : "null"));
        }
    }

    /**
     * Xóa cache của một đảo
     */
    public static void invalidateIslandCache(String islandId) {
        islandCache.remove(islandId);
    }

    /**
     * Xóa cache của một đảo từ vị trí
     */
    public static void invalidateIslandCache(Location location) {
        String islandId = getIslandId(location);
        if (islandId != null) {
            invalidateIslandCache(islandId);
        }
    }

    /**
     * Xóa tất cả cache
     */
    public static void clearAllCache() {
        islandCache.clear();
        Main.sendlog("§e[OreGen4] Đã xóa tất cả cache quyền đảo");
    }

    /**
     * Xóa cache khi plugin tắt
     */
    public static void shutdown() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        clearAllCache();
    }

    /**
     * Lớp lưu trữ dữ liệu đảo được cache
     */
    private static class CachedIslandData {
        private final UUID ownerUUID;
        private final OreLevel oreLevel;
        private final long timestamp;
        private long expiryTime;

        public CachedIslandData(UUID ownerUUID, OreLevel oreLevel) {
            this.ownerUUID = ownerUUID;
            this.oreLevel = oreLevel;
            this.timestamp = System.currentTimeMillis();
            this.expiryTime = timestamp + CACHE_DURATION;
        }

        public UUID getOwnerUUID() {
            return ownerUUID;
        }

        public OreLevel getOreLevel() {
            return oreLevel;
        }

        public boolean isExpired(long currentTime) {
            return currentTime > expiryTime;
        }
    }
}
