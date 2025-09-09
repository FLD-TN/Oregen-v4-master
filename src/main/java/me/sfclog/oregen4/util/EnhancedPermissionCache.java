package me.sfclog.oregen4.util;

import me.sfclog.oregen4.Main;
import me.sfclog.oregen4.config.ConfigManager;
import me.sfclog.oregen4.config.OreLevel;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache thông tin quyền và OreLevel cho người chơi
 * Hệ thống cache được tối ưu hóa để giảm tải truy vấn LuckPerms
 */
public class EnhancedPermissionCache {
    // Cache chính: UUID -> Environment -> Thông tin cache
    private static final Map<UUID, Map<World.Environment, CachedPermissionData>> cache = new ConcurrentHashMap<>();

    // Thời gian cache cho mỗi mục (ms)
    private static final long CACHE_DURATION = 600000; // 10 phút
    private static final long PRIORITY_CACHE_DURATION = 1800000; // 30 phút cho cache ưu tiên

    // Chu kỳ dọn dẹp cache (ms)
    private static final long CLEANUP_INTERVAL = 300000; // 5 phút

    // Thời gian giảm TTL khi người chơi đăng xuất (ms)
    private static final long REDUCED_TTL = 180000; // 3 phút

    // Task dọn dẹp cache
    private static BukkitTask cleanupTask;

    // Lưu trữ số lần truy cập cache để thống kê
    private static long cacheHits = 0;
    private static long cacheMisses = 0;

    /**
     * Lớp lưu trữ thông tin về quyền và OreLevel được cache
     */
    public static class CachedPermissionData {
        private String permission;
        private OreLevel oreLevel;
        private long timestamp;
        private int accessCount;
        private boolean isPriority;
        private long expiryTime;

        public CachedPermissionData(String permission, OreLevel oreLevel) {
            this(permission, oreLevel, false);
        }

        public CachedPermissionData(String permission, OreLevel oreLevel, boolean isPriority) {
            this.permission = permission;
            this.oreLevel = oreLevel;
            this.timestamp = System.currentTimeMillis();
            this.accessCount = 1;
            this.isPriority = isPriority;
            this.expiryTime = isPriority ? PRIORITY_CACHE_DURATION : CACHE_DURATION;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > expiryTime;
        }

        public void updateTimestamp() {
            this.timestamp = System.currentTimeMillis();
            this.accessCount++;
        }

        public void reduceTTL() {
            // Giảm TTL khi người chơi đăng xuất, nhưng không ít hơn REDUCED_TTL
            this.expiryTime = Math.min(this.expiryTime, REDUCED_TTL);
        }

        public String getPermission() {
            return permission;
        }

        public OreLevel getOreLevel() {
            return oreLevel;
        }

        public int getAccessCount() {
            return accessCount;
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

        // Lên lịch task dọn dẹp cache
        cleanupTask = Main.pl.getServer().getScheduler().runTaskTimerAsynchronously(
                Main.pl, EnhancedPermissionCache::cleanupCache, CLEANUP_INTERVAL / 50, CLEANUP_INTERVAL / 50);

        Main.sendlog("§a[OreGen4] Khởi tạo EnhancedPermissionCache thành công");
    }

    /**
     * Dọn dẹp các cache đã hết hạn
     */
    private static void cleanupCache() {
        try {
            int cleaned = 0;
            int remaining = 0;

            for (Iterator<Map.Entry<UUID, Map<World.Environment, CachedPermissionData>>> iterator = cache.entrySet()
                    .iterator(); iterator.hasNext();) {

                Map.Entry<UUID, Map<World.Environment, CachedPermissionData>> entry = iterator.next();
                Map<World.Environment, CachedPermissionData> playerCache = entry.getValue();

                // Dọn dẹp các cache environment đã hết hạn
                int before = playerCache.size();
                playerCache.entrySet().removeIf(envEntry -> envEntry.getValue().isExpired());
                int after = playerCache.size();

                cleaned += (before - after);

                // Nếu không còn cache environment nào, xóa player cache
                if (playerCache.isEmpty()) {
                    iterator.remove();
                } else {
                    remaining += playerCache.size();
                }
            }

            // Hiển thị thống kê cache
            boolean debug = Main.pl.getConfig().getBoolean("debug", false);
            if ((cleaned > 0 || debug) && Main.pl != null) {
                Main.sendlog("§a[OreGen4] Cache Stats: Cleaned=" + cleaned +
                        ", Remaining=" + remaining +
                        ", Hit Rate=" + getCacheHitRate() + "%");

                // Reset thống kê
                if (cacheHits + cacheMisses > 10000) {
                    cacheHits = 0;
                    cacheMisses = 0;
                }
            }
        } catch (Exception e) {
            Main.sendlog("§c[OreGen4] Lỗi khi dọn dẹp cache: " + e.getMessage());
        }
    }

    /**
     * Lưu thông tin quyền và OreLevel vào cache
     * 
     * @param uuid       UUID của người chơi
     * @param env        Environment của thế giới
     * @param permission Quyền của người chơi
     * @param oreLevel   OreLevel tương ứng với quyền
     */
    public static void cachePermission(UUID uuid, World.Environment env, String permission, OreLevel oreLevel) {
        cachePermission(uuid, env, permission, oreLevel, false);
    }

    /**
     * Lưu thông tin quyền và OreLevel vào cache với tùy chọn ưu tiên
     * 
     * @param uuid       UUID của người chơi
     * @param env        Environment của thế giới
     * @param permission Quyền của người chơi
     * @param oreLevel   OreLevel tương ứng với quyền
     * @param isPriority Có phải là cache ưu tiên (TTL dài hơn)
     */
    public static void cachePermission(UUID uuid, World.Environment env, String permission, OreLevel oreLevel,
            boolean isPriority) {
        try {
            Map<World.Environment, CachedPermissionData> playerCache = cache.computeIfAbsent(uuid,
                    k -> new ConcurrentHashMap<>());

            CachedPermissionData existingData = playerCache.get(env);
            if (existingData != null && permission.equals(existingData.getPermission())) {
                // Cập nhật timestamp nếu đã có trong cache
                existingData.updateTimestamp();
            } else {
                // Thêm mới vào cache
                playerCache.put(env, new CachedPermissionData(permission, oreLevel, isPriority));
            }
        } catch (Exception e) {
            Main.sendlog("§c[OreGen4] Lỗi khi cache quyền: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra xem một player đã có cache quyền cho environment cụ thể chưa
     * 
     * @param uuid UUID của người chơi
     * @param env  Environment của thế giới
     * @return true nếu đã có cache, false nếu chưa
     */
    public static boolean hasCachedPermission(UUID uuid, World.Environment env) {
        Map<World.Environment, CachedPermissionData> playerCache = cache.get(uuid);
        return playerCache != null && playerCache.containsKey(env);
    }

    /**
     * Lấy OreLevel từ cache cho một người chơi và environment
     * 
     * @param uuid UUID của người chơi
     * @param env  Environment của thế giới
     * @return OreLevel từ cache hoặc null nếu không có
     */
    public static OreLevel getOreLevelFromCache(UUID uuid, World.Environment env) {
        cacheMisses++;

        if (uuid == null)
            return null;

        Map<World.Environment, CachedPermissionData> playerCache = cache.get(uuid);
        if (playerCache == null)
            return null;

        CachedPermissionData data = playerCache.get(env);
        if (data == null || data.isExpired()) {
            if (data != null && data.isExpired()) {
                // Xóa cache hết hạn
                playerCache.remove(env);
            }
            return null;
        }

        // Tăng hit count và cập nhật timestamp
        cacheHits++;
        cacheMisses--; // Điều chỉnh vì đã tăng ở đầu phương thức
        data.updateTimestamp();

        return data.getOreLevel();
    }

    /**
     * Lấy permission level của một người chơi từ OreLevel cached
     * 
     * @param uuid UUID của người chơi
     * @param env  Environment của thế giới
     * @return Permission level hoặc null nếu không tìm thấy trong cache
     */
    public static String getCachedPermissionLevel(UUID uuid, World.Environment env) {
        Map<World.Environment, CachedPermissionData> playerCache = cache.get(uuid);
        if (playerCache == null)
            return null;

        CachedPermissionData data = playerCache.get(env);
        if (data == null || data.isExpired()) {
            if (data != null && data.isExpired()) {
                // Xóa cache hết hạn
                playerCache.remove(env);
            }
            return null;
        }

        // Cập nhật timestamp và trả về permission
        data.updateTimestamp();
        return data.getPermission();
    }

    /**
     * Xóa cache cho một người chơi dựa vào UUID
     * 
     * @param uuid UUID của người chơi
     */
    public static void clearPlayerCache(UUID uuid) {
        cache.remove(uuid);
    }

    /**
     * Xóa cache cho một người chơi dựa vào tên
     * 
     * @param playerName Tên người chơi
     */
    public static void clearPlayerCache(String playerName) {
        UUID uuid = GetUUID.getUUID(playerName);
        if (uuid != null) {
            clearPlayerCache(uuid);
        }
    }

    /**
     * Xóa tất cả cache
     */
    public static void clearAllCache() {
        cache.clear();
    }

    /**
     * Giảm thời gian tồn tại (TTL) của cache cho một người chơi
     * Sử dụng khi người chơi logout để giữ cache nhưng giảm thời gian tồn tại
     * 
     * @param uuid UUID của người chơi
     */
    public static void reduceTTL(UUID uuid) {
        Map<World.Environment, CachedPermissionData> playerCache = cache.get(uuid);
        if (playerCache != null) {
            for (CachedPermissionData data : playerCache.values()) {
                data.reduceTTL();
            }
        }
    }

    /**
     * Lấy số lượng người chơi trong cache
     * 
     * @return Số người chơi trong cache
     */
    public static int getCachedPlayerCount() {
        return cache.size();
    }

    /**
     * Lấy tỷ lệ cache hit
     * 
     * @return Tỷ lệ phần trăm cache hit
     */
    public static int getCacheHitRate() {
        long total = cacheHits + cacheMisses;
        if (total == 0) {
            return 0;
        }
        return (int) ((cacheHits * 100) / total);
    }

    /**
     * Xóa cache cho một người chơi trong môi trường cụ thể
     *
     * @param uuid        UUID của người chơi
     * @param environment Môi trường cụ thể
     */
    public static void removeCachedPermissions(UUID uuid, World.Environment environment) {
        Map<World.Environment, CachedPermissionData> playerCache = cache.get(uuid);
        if (playerCache != null) {
            playerCache.remove(environment);
            if (Main.getInstance().getConfig().getBoolean("debug", false)) {
                Main.sendlog("§e[OreGen4] Đã xóa cache quyền cho " + uuid + " trong môi trường " + environment.name());
            }
        }
    }

    /**
     * Xóa tất cả cache cho một người chơi
     *
     * @param uuid UUID của người chơi
     */
    public static void removeCachedPermissions(UUID uuid) {
        cache.remove(uuid);
        if (Main.getInstance().getConfig().getBoolean("debug", false)) {
            Main.sendlog("§e[OreGen4] Đã xóa tất cả cache quyền cho " + uuid);
        }
    }

    /**
     * Xử lý khi tắt plugin
     */
    public static void shutdown() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        clearAllCache();
    }
}
