package me.sfclog.oregen4.updater;

import me.sfclog.oregen4.config.ConfigManager;
import me.sfclog.oregen4.config.OreLevel;
import me.sfclog.oregen4.util.Color;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Lớp quản lý và cập nhật quyền cho OreGen4
 * Hỗ trợ tự động cập nhật quyền theo thời gian
 */
public class PermissionUpdater {

    private static final Map<UUID, Map<World.Environment, PermissionInfo>> permCache = new ConcurrentHashMap<>();
    private static Plugin plugin;
    private static LuckPerms luckPermsApi;
    private static Logger logger;
    private static BukkitTask updateTask;
    private static boolean enabled = false;
    private static long updateInterval = 60000; // 1 phút mặc định

    /**
     * Class lưu trữ thông tin quyền
     */
    public static class PermissionInfo {
        private String permission;
        private long lastChecked;

        public PermissionInfo(String permission) {
            this.permission = permission;
            this.lastChecked = System.currentTimeMillis();
        }

        public String getPermission() {
            return permission;
        }

        public void setPermission(String permission) {
            this.permission = permission;
            this.lastChecked = System.currentTimeMillis();
        }

        public long getLastChecked() {
            return lastChecked;
        }
    }

    /**
     * Khởi tạo updater
     * 
     * @param mainPlugin Plugin chính
     * @param lpApi      LuckPerms API
     * @param interval   Thời gian cập nhật (ms)
     */
    public static void initialize(Plugin mainPlugin, LuckPerms lpApi, long interval) {
        plugin = mainPlugin;
        luckPermsApi = lpApi;
        logger = mainPlugin.getLogger();
        updateInterval = interval > 0 ? interval : 60000;

        if (luckPermsApi != null) {
            enabled = true;
            startUpdateTask();
            log("§a[OreGen4] PermissionUpdater đã được khởi tạo với khoảng thời gian cập nhật: "
                    + (updateInterval / 1000) + " giây");
        } else {
            log("§c[OreGen4] PermissionUpdater không thể khởi tạo: LuckPerms API không khả dụng");
        }
    }

    /**
     * Bắt đầu task cập nhật
     */
    private static void startUpdateTask() {
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
        }

        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                PermissionUpdater::updateAllPermissions,
                20, // 1 giây sau khi plugin bắt đầu
                updateInterval / 50 // chuyển từ ms sang ticks
        );

        log("§a[OreGen4] Task cập nhật quyền tự động đã bắt đầu");
    }

    /**
     * Đăng ký thông tin quyền cho player
     * 
     * @param uuid       UUID người chơi
     * @param env        Environment
     * @param permission Permission string
     */
    public static void registerPermission(UUID uuid, World.Environment env, String permission) {
        if (!enabled)
            return;

        Map<World.Environment, PermissionInfo> playerInfo = permCache.computeIfAbsent(uuid,
                k -> new ConcurrentHashMap<>());

        PermissionInfo currentInfo = playerInfo.get(env);
        if (currentInfo == null) {
            // Thêm mới
            playerInfo.put(env, new PermissionInfo(permission));
        } else if (!currentInfo.getPermission().equals(permission)) {
            // Cập nhật nếu khác
            currentInfo.setPermission(permission);
        }
    }

    /**
     * Lấy quyền hiện tại của người chơi
     * 
     * @param uuid UUID người chơi
     * @param env  Environment
     * @return Permission string hoặc null nếu chưa có
     */
    public static String getCurrentPermission(UUID uuid, World.Environment env) {
        if (!enabled)
            return null;

        Map<World.Environment, PermissionInfo> playerInfo = permCache.get(uuid);
        if (playerInfo != null) {
            PermissionInfo info = playerInfo.get(env);
            if (info != null) {
                return info.getPermission();
            }
        }

        return null;
    }

    /**
     * Cập nhật quyền cho tất cả người chơi trong cache
     */
    public static void updateAllPermissions() {
        if (!enabled || luckPermsApi == null)
            return;

        int updatedPlayers = 0;
        int checkedPlayers = 0;

        try {
            for (Map.Entry<UUID, Map<World.Environment, PermissionInfo>> entry : permCache.entrySet()) {
                UUID uuid = entry.getKey();
                Map<World.Environment, PermissionInfo> environmentMap = entry.getValue();
                boolean playerUpdated = false;
                checkedPlayers++;

                if (environmentMap.isEmpty()) {
                    continue;
                }

                // Chỉ tải user từ LuckPerms một lần cho mỗi player
                net.luckperms.api.model.user.User user = null;
                try {
                    user = luckPermsApi.getUserManager().loadUser(uuid).get(3, TimeUnit.SECONDS);
                } catch (Exception e) {
                    debug("§c[OreGen4] Không thể tải user " + uuid + ": " + e.getMessage());
                    continue;
                }

                if (user == null) {
                    continue;
                }

                for (Map.Entry<World.Environment, PermissionInfo> envEntry : environmentMap.entrySet()) {
                    World.Environment env = envEntry.getKey();
                    PermissionInfo permInfo = envEntry.getValue();

                    // Kiểm tra tất cả các level từ cao xuống thấp
                    String[] levels = { "vip", "lv3", "lv2", "lv1" };
                    String highestPermission = null;

                    for (String level : levels) {
                        String permission = "oregen." + level;
                        try {
                            boolean hasPerm = user.getCachedData().getPermissionData()
                                    .checkPermission(permission).asBoolean();

                            if (hasPerm) {
                                OreLevel oreLevel = ConfigManager.getLevel(env, permission);
                                if (oreLevel != null) {
                                    highestPermission = permission;
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            debug("§c[OreGen4] Lỗi kiểm tra quyền " + permission + ": " + e.getMessage());
                        }
                    }

                    // Nếu không tìm thấy quyền nào, sử dụng default
                    if (highestPermission == null) {
                        OreLevel defaultLevel = ConfigManager.getDefaultLevel(env);
                        if (defaultLevel != null) {
                            highestPermission = defaultLevel.getPermission();
                        }
                    }

                    // Nếu tìm thấy permission mới khác với permission cũ
                    if (highestPermission != null &&
                            !highestPermission.equals(permInfo.getPermission())) {
                        String oldPermission = permInfo.getPermission();
                        permInfo.setPermission(highestPermission);

                        log("§e[OreGen4] §bCập nhật quyền cho người chơi §a" + uuid +
                                "§b trong môi trường §a" + env +
                                "§b từ §c" + oldPermission +
                                "§b thành §a" + highestPermission);

                        playerUpdated = true;
                    }
                }

                if (playerUpdated) {
                    updatedPlayers++;
                }
            }

            if (plugin.getConfig().getBoolean("debug", false) || updatedPlayers > 0) {
                log("§a[OreGen4] Đã kiểm tra " + checkedPlayers + " người chơi và cập nhật quyền cho " +
                        updatedPlayers + " người chơi");
            }

        } catch (Exception e) {
            log("§c[OreGen4] Lỗi khi cập nhật quyền: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Xóa thông tin quyền cho một người chơi
     * 
     * @param uuid UUID người chơi
     */
    public static void clearPlayerInfo(UUID uuid) {
        permCache.remove(uuid);
    }

    /**
     * Xóa thông tin quyền cho một người chơi bằng tên
     * 
     * @param playerName Tên người chơi
     */
    public static void clearPlayerInfo(String playerName) {
        // Chuyển đổi tên thành UUID
        org.bukkit.entity.Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            clearPlayerInfo(player.getUniqueId());
        }
    }

    /**
     * Tắt updater và dọn dẹp tài nguyên
     */
    public static void shutdown() {
        enabled = false;

        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
            updateTask = null;
        }

        permCache.clear();
        log("§a[OreGen4] PermissionUpdater đã tắt và dọn dẹp tất cả thông tin quyền");
    }

    /**
     * Tải lại hệ thống cập nhật quyền
     */
    public static void reload() {
        if (plugin == null || luckPermsApi == null) {
            throw new IllegalStateException("PermissionUpdater chưa được khởi tạo");
        }

        // Lấy cấu hình
        long interval = plugin.getConfig().getLong("permission-update-interval", 60000);

        // Tắt task cũ
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
            updateTask = null;
        }

        // Khởi tạo lại
        updateInterval = interval > 0 ? interval : 60000;
        enabled = true;
        startUpdateTask();

        log("§a[OreGen4] PermissionUpdater đã được tải lại với khoảng thời gian cập nhật: " + (updateInterval / 1000)
                + " giây");
    }

    /**
     * Lấy số lượng người chơi trong cache
     * 
     * @return Số người chơi đang được cache
     */
    public static int getCachedPlayerCount() {
        return permCache.size();
    }

    /**
     * Kiểm tra trạng thái hoạt động
     * 
     * @return true nếu updater đang hoạt động
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Lấy khoảng thời gian cập nhật hiện tại
     * 
     * @return Khoảng thời gian cập nhật (ms)
     */
    public static long getUpdateInterval() {
        return updateInterval;
    }

    private static void log(String message) {
        if (logger != null) {
            logger.info(Color.tran(message));
        }
    }

    private static void debug(String message) {
        if (plugin != null && plugin.getConfig().getBoolean("debug", false) && logger != null) {
            logger.info(Color.tran(message));
        }
    }
}
