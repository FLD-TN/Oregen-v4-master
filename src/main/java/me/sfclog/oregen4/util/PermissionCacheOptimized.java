package me.sfclog.oregen4.util;

import me.sfclog.oregen4.Main;
import me.sfclog.oregen4.config.ConfigManager;
import me.sfclog.oregen4.config.OreLevel;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cache permission-based OreLevel cho người chơi
 * Cải tiến từ PermissionCache gốc với cơ chế tự động cập nhật quyền
 */
public class PermissionCacheOptimized {
    // Cache chính lưu trữ OreLevel theo UUID và Environment
    private static final Map<UUID, Map<World.Environment, CachedOreLevel>> cache = new ConcurrentHashMap<>();

    // Plugin instance
    private static Plugin plugin;

    // LuckPerms API
    private static LuckPerms luckPermsApi;

    // Logger
    private static final Logger logger = Logger.getLogger(PermissionCacheOptimized.class.getName());

    // Thời gian cache mặc định: 5 phút (ms)
    private static final long DEFAULT_CACHE_DURATION = 300000;

    // Thời gian cache có thể được thay đổi từ cấu hình
    private static long CACHE_DURATION = DEFAULT_CACHE_DURATION;

    // Task dọn dẹp cache
    private static BukkitTask cleanupTask;

    // Task cập nhật quyền tự động
    private static BukkitTask updateTask;

    // Khoảng thời gian dọn dẹp cache (ms)
    private static final long CLEANUP_INTERVAL = 300000; // 5 phút

    // Khoảng thời gian cập nhật quyền (ms)
    private static long UPDATE_INTERVAL = 60000; // 1 phút

    // Trạng thái cập nhật tự động
    private static boolean autoUpdateEnabled = true;

    // Debug mode
    private static boolean debug = false;

    /**
     * Lớp lưu trữ thông tin OreLevel trong cache
     */
    public static class CachedOreLevel {
        private OreLevel oreLevel;
        private long timestamp;

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

        public void updateOreLevel(OreLevel newOreLevel) {
            this.oreLevel = newOreLevel;
            this.timestamp = System.currentTimeMillis();
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Khởi tạo hệ thống cache và lên lịch cho các task
     * 
     * @param mainPlugin     Plugin instance
     * @param lpApi          LuckPerms API instance
     * @param cacheDuration  Thời gian cache (ms)
     * @param updateInterval Khoảng thời gian cập nhật quyền (ms)
     * @param autoUpdate     Bật/tắt cập nhật tự động
     */
    public static void initialize(Plugin mainPlugin, LuckPerms lpApi, long cacheDuration, long updateInterval,
            boolean autoUpdate, boolean debugMode) {
        plugin = mainPlugin;
        luckPermsApi = lpApi;
        logger.setLevel(Level.ALL);
        debug = debugMode;

        clearAllCache();

        // Cập nhật các thông số cấu hình
        CACHE_DURATION = cacheDuration > 0 ? cacheDuration : DEFAULT_CACHE_DURATION;
        UPDATE_INTERVAL = updateInterval > 0 ? updateInterval : 60000;
        autoUpdateEnabled = autoUpdate;

        // Hủy các task cũ nếu có
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }

        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
        }

        // Lên lịch task dọn dẹp cache định kỳ
        cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                () -> cleanupExpiredCache(),
                CLEANUP_INTERVAL / 50, // Ticks đầu tiên (chuyển từ ms sang tick)
                CLEANUP_INTERVAL / 50 // Ticks lặp lại
        );

        // Chỉ lên lịch task cập nhật quyền nếu được bật
        if (autoUpdateEnabled) {
            startAutoUpdate();
        }

        log("§a[OreGen4] PermissionCacheOptimized đã khởi tạo:");
        log("§a[OreGen4] - Thời gian cache: " + (CACHE_DURATION / 1000) + " giây");
        log("§a[OreGen4] - Cập nhật tự động: " + (autoUpdateEnabled ? "§aĐang bật" : "§cĐang tắt"));
        if (autoUpdateEnabled) {
            log("§a[OreGen4] - Khoảng thời gian cập nhật: " + (UPDATE_INTERVAL / 1000) + " giây");
        }
    }

    /**
     * Helper method để log messages
     */
    private static void log(String message) {
        if (plugin != null) {
            logger.info(Color.tran(message));
        }
    }

    /**
     * Helper method để log debug messages
     */
    private static void debugLog(String message) {
        if (debug && plugin != null) {
            logger.info(Color.tran(message));
        }
    }

    /**
     * Khởi tạo với các giá trị mặc định
     */
    public static void initialize(Plugin mainPlugin, LuckPerms lpApi) {
        boolean debugMode = mainPlugin.getConfig().getBoolean("debug", false);
        initialize(mainPlugin, lpApi, DEFAULT_CACHE_DURATION, 60000, true, debugMode);
    }

    /**
     * Bật chế độ tự động cập nhật quyền
     */
    public static void enableAutoUpdate() {
        if (!autoUpdateEnabled) {
            autoUpdateEnabled = true;
            startAutoUpdate();
            log("§a[OreGen4] Đã bật chế độ tự động cập nhật quyền");
        }
    }

    /**
     * Tắt chế độ tự động cập nhật quyền
     */
    public static void disableAutoUpdate() {
        if (autoUpdateEnabled) {
            autoUpdateEnabled = false;
            if (updateTask != null && !updateTask.isCancelled()) {
                updateTask.cancel();
                updateTask = null;
            }
            log("§a[OreGen4] Đã tắt chế độ tự động cập nhật quyền");
        }
    }

    /**
     * Bắt đầu task cập nhật tự động
     */
    private static void startAutoUpdate() {
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
        }

        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                () -> updateAllPermissions(),
                20, // Bắt đầu sau 1 giây
                UPDATE_INTERVAL / 50 // Chuyển đổi ms sang ticks
        );

        log("§a[OreGen4] Task cập nhật quyền tự động đã bắt đầu (mỗi " + (UPDATE_INTERVAL / 1000) + " giây)");
    }

    /**
     * Cập nhật quyền cho tất cả người chơi trong cache
     */
    public static void updateAllPermissions() {
        if (luckPermsApi == null) {
            log("§c[OreGen4] Không thể cập nhật quyền: LuckPerms API không khả dụng");
            return;
        }

        int updatedPlayers = 0;
        int checkedPlayers = 0;

        try {
            for (Map.Entry<UUID, Map<World.Environment, CachedOreLevel>> entry : cache.entrySet()) {
                UUID uuid = entry.getKey();
                Map<World.Environment, CachedOreLevel> environmentMap = entry.getValue();
                boolean playerUpdated = false;
                checkedPlayers++;

                if (environmentMap.isEmpty()) {
                    continue;
                }

                // Chỉ tải user từ LuckPerms một lần cho mỗi player
                try {
                    net.luckperms.api.model.user.User user = luckPermsApi.getUserManager().loadUser(uuid).get(3, TimeUnit.SECONDS);

                    if (user == null) {
                        continue;
                    }

                    for (Map.Entry<World.Environment, CachedOreLevel> envEntry : environmentMap.entrySet()) {
                        World.Environment env = envEntry.getKey();
                        CachedOreLevel cachedLevel = envEntry.getValue();

                        // Kiểm tra tất cả các level từ cao xuống thấp
                        String[] levels = { "vip", "lv3", "lv2", "lv1" };
                        OreLevel highestLevel = null;
                        String highestPermission = null;

                        for (String level : levels) {
                            String permission = "oregen." + level;
                            try {
                                boolean hasPerm = user.getCachedData().getPermissionData()
                                        .checkPermission(permission).asBoolean();

                                if (hasPerm) {
                                    OreLevel oreLevel = ConfigManager.getLevel(env, permission);
                                    if (oreLevel != null) {
                                        highestLevel = oreLevel;
                                        highestPermission = permission;
                                        break;
                                    }
                                }
                            } catch (Exception e) {
                                debugLog("§c[OreGen4] Lỗi kiểm tra quyền " + permission + ": " + e.getMessage());
                            }
                        }

                        // Nếu không tìm thấy quyền nào, sử dụng mặc định
                        if (highestLevel == null) {
                            highestLevel = ConfigManager.getDefaultLevel(env);
                        }

                        // Nếu tìm thấy level mới khác với level cũ trong cache
                        if (highestLevel != null &&
                                !highestLevel.getPermission().equals(cachedLevel.getOreLevel().getPermission())) {
                            String oldPermission = cachedLevel.getOreLevel().getPermission();
                            cachedLevel.updateOreLevel(highestLevel);

                            debugLog("§e[OreGen4] §bCập nhật quyền cho người chơi §a" + uuid +
                                    "§b trong môi trường §a" + env +
                                    "§b từ §c" + oldPermission +
                                    "§b thành §a" + highestPermission);

                            playerUpdated = true;
                        }
                    }
                } catch (TimeoutException | ExecutionException | InterruptedException e) {
                    logger.log(Level.SEVERE, "Error occurred", e);
                    continue;
                }

                if (playerUpdated) {
                    updatedPlayers++;
                }
            }

            if (debug || updatedPlayers > 0) {
                log("§a[OreGen4] Đã kiểm tra " + checkedPlayers + " người chơi và cập nhật quyền cho " +
                        updatedPlayers + " người chơi");
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during permission update", e);
        }
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

            if (cleaned > 0) {
                debugLog("§a[OreGen4] Đã dọn dẹp " + cleaned + " cache quyền đã hết hạn");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during cache cleanup", e);
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
            debugLog("§c[OreGen4] Không tìm thấy cache cho người chơi " + uuid);
            return null;
        }

        CachedOreLevel cachedLevel = playerCache.get(env);
        if (cachedLevel == null) {
            debugLog("§c[OreGen4] Không tìm thấy cache cho người chơi " + uuid + " trong môi trường " + env);
            return null;
        }

        if (cachedLevel.isExpired()) {
            debugLog("§c[OreGen4] Cache đã hết hạn cho người chơi " + uuid + " trong môi trường " + env);
            return null;
        }

        debugLog("§a[OreGen4] Sử dụng cache level cho người chơi " + uuid +
                " trong môi trường " + env + ": " + cachedLevel.getOreLevel().getPermission());
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
            if (oreLevel != null && oldPermission != null && oreLevel.getPermission() != null && !oldPermission.equals(oreLevel.getPermission())) {
                log("§e[OreGen4] §bCập nhật cache quyền cho người chơi §a" + uuid +
                        "§b trong môi trường §a" + env +
                        "§b từ §c" + oldPermission +
                        "§b thành §a" + oreLevel.getPermission());
            }
        } else {
            debugLog("§a[OreGen4] Đã tạo cache quyền cho người chơi " + uuid +
                    " trong môi trường " + env + " với quyền " + oreLevel.getPermission());
        }
    }

    /**
     * Xóa cache của một player
     * 
     * @param uuid UUID của player
     */
    public static void clearCache(UUID uuid) {
        cache.remove(uuid);
        debugLog("§a[OreGen4] Đã xóa cache cho người chơi " + uuid);
    }

    /**
     * Xóa cache theo tên người chơi
     * 
     * @param playerName Tên người chơi
     */
    public static void clearPlayerCache(String playerName) {
        UUID uuid = GetUUID.getUUID(playerName);
        if (uuid != null) {
            clearCache(uuid);
        }
    }

    /**
     * Xóa toàn bộ cache
     */
    public static void clearAllCache() {
        cache.clear();
        debugLog("§a[OreGen4] Đã xóa toàn bộ cache");
    }

    /**
     * Lấy thời gian cache (ms)
     */
    public static long getCacheDuration() {
        return CACHE_DURATION;
    }

    /**
     * Cập nhật thời gian cache
     * 
     * @param duration Thời gian mới (ms)
     */
    public static void setCacheDuration(long duration) {
        if (duration > 0) {
            CACHE_DURATION = duration;
            log("§a[OreGen4] Đã cập nhật thời gian cache: " + (duration / 1000) + " giây");
        }
    }

    /**
     * Cập nhật khoảng thời gian tự động cập nhật quyền
     * 
     * @param interval Khoảng thời gian mới (ms)
     */
    public static void setUpdateInterval(long interval) {
        if (interval > 0) {
            UPDATE_INTERVAL = interval;

            // Khởi động lại task nếu đang bật
            if (autoUpdateEnabled) {
                startAutoUpdate();
            }

            log("§a[OreGen4] Đã cập nhật khoảng thời gian cập nhật quyền: " + (interval / 1000) + " giây");
        }
    }

    /**
     * Kiểm tra xem một người chơi có trong cache không
     * 
     * @param uuid UUID người chơi
     * @return true nếu có trong cache
     */
    public static boolean isPlayerCached(UUID uuid) {
        return cache.containsKey(uuid);
    }

    /**
     * Lấy số lượng người chơi được cache
     * 
     * @return Số lượng người chơi trong cache
     */
    public static int getCachedPlayerCount() {
        return cache.size();
    }

    /**
     * Lấy thời điểm cập nhật mới nhất của người chơi
     * 
     * @param uuid UUID người chơi
     * @param env  Môi trường
     * @return Thời điểm cập nhật (timestamp), -1 nếu không có trong cache
     */
    public static long getLastUpdateTime(UUID uuid, World.Environment env) {
        Map<World.Environment, CachedOreLevel> playerCache = cache.get(uuid);
        if (playerCache != null) {
            CachedOreLevel cachedLevel = playerCache.get(env);
            if (cachedLevel != null) {
                return cachedLevel.getTimestamp();
            }
        }
        return -1;
    }

    /**
     * Kiểm tra xem chế độ tự động cập nhật có được bật không
     * 
     * @return true nếu chế độ tự động cập nhật đang bật
     */
    public static boolean isAutoUpdateEnabled() {
        return autoUpdateEnabled;
    }

    /**
     * Kiểm tra tình trạng của cache
     * 
     * @return Chuỗi thông tin về tình trạng cache
     */
    public static String getStatus() {
        StringBuilder status = new StringBuilder();
        status.append("§a===== Tình trạng PermissionCacheOptimized =====\n");
        status.append("§aTổng số người chơi trong cache: §f").append(cache.size()).append("\n");
        status.append("§aThời gian cache: §f").append(CACHE_DURATION / 1000).append(" giây\n");
        status.append("§aTự động cập nhật: §f").append(autoUpdateEnabled ? "§aĐang bật" : "§cĐang tắt").append("\n");

        if (autoUpdateEnabled) {
            status.append("§aKhoảng thời gian cập nhật: §f").append(UPDATE_INTERVAL / 1000).append(" giây\n");
        }

        int totalEnvironments = 0;
        for (Map<World.Environment, CachedOreLevel> environmentMap : cache.values()) {
            totalEnvironments += environmentMap.size();
        }
        status.append("§aTổng số môi trường được cache: §f").append(totalEnvironments).append("\n");

        return status.toString();
    }

    /**
     * Ghi nhận thống kê cache
     */
    public static void logCacheStats() {
        int totalEntries = cache.size();
        int totalSubEntries = cache.values().stream().mapToInt(Map::size).sum();
        log("§a[OreGen4] Cache Stats: Total Entries=" + totalEntries + ", Sub-Entries=" + totalSubEntries);
    }

    /**
     * Hủy tất cả task dọn dẹp và xóa cache khi plugin shutdown
     */
    public static void shutdown() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
            cleanupTask = null;
        }

        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
            updateTask = null;
        }

        clearAllCache();
        log("§a[OreGen4] PermissionCacheOptimized đã tắt và dọn dẹp tất cả cache");
    }

    /**
     * Đặt lại toàn bộ cache (cho tương thích với API cũ)
     */
    public static void resetCache() {
        clearAllCache();
    }
}
