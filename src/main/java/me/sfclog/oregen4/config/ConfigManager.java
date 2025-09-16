package me.sfclog.oregen4.config;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import me.sfclog.oregen4.Main;
import me.sfclog.oregen4.island.IslandOreManager;
import me.sfclog.oregen4.util.EnhancedPermissionCache;
import me.sfclog.oregen4.util.IslandPermissionCache;
import me.sfclog.oregen4.util.LocationOwnerCache;
import me.sfclog.oregen4.util.PermissionCache;

public class ConfigManager {
    private static final Map<World.Environment, Map<String, OreLevel>> levels = new HashMap<>();
    private static final Map<World.Environment, OreLevel> defaultLevels = new HashMap<>();

    /**
     * Phương thức cải tiến để tải lại cấu hình
     * Đảm bảo xóa tất cả các cache và tải lại cấu hình từ đĩa
     */
    public static void reloadConfig() {
        // Đọc lại file config.yml từ ổ đĩa
        Main.pl.reloadConfig();
        
        if (Main.isDebugEnabled()) {
            Main.sendlog("§a[OreGen4] §2Đã đọc lại file config.yml từ ổ đĩa");
        }
        
        // Xóa tất cả các cache hiện có
        clearAllCaches();
        
        // Tải lại cấu hình
        loadConfig();
        
        if (Main.isDebugEnabled()) {
            Main.sendlog("§a[OreGen4] §2Đã tải lại cấu hình thành công!");
        }
    }
    
    /**
     * Xóa tất cả các cache được sử dụng trong plugin
     */
    public static void clearAllCaches() {
        // Bảo vệ dữ liệu đảo hiện tại bằng cách tạo bản sao trước
        Map<String, Map<World.Environment, OreLevel>> islandDataBackup = null;
        
        try {
            // Lưu trữ dữ liệu đảo hiện tại
            islandDataBackup = IslandOreManager.getIslandOreDataCopy();
            IslandOreManager.saveData();
            Main.sendlog("§a[OreGen4] §2Đã lưu dữ liệu đảo trước khi xóa cache");
        } catch (Exception e) {
            Main.sendlog("§c[OreGen4] §4Lỗi khi lưu dữ liệu đảo: " + e.getMessage());
        }
        
        // Xóa cache trong ConfigManager
        levels.clear();
        defaultLevels.clear();
        
        // Xóa cache quyền người chơi
        EnhancedPermissionCache.clearAllCache();
        PermissionCache.clearAllCache();
        
        // Xóa cache đảo
        IslandPermissionCache.clearAllCache();
        
        // Xóa cache vị trí
        LocationOwnerCache.clearAllCache();
        
        // Tải lại IslandOreManager (nếu cần)
        IslandOreManager.loadData();
        
        // Kiểm tra nếu không có dữ liệu nào được tải, khôi phục từ bản sao
        if (IslandOreManager.getIslandCount() == 0 && islandDataBackup != null && !islandDataBackup.isEmpty()) {
            Main.sendlog("§e[OreGen4] §6Không tìm thấy dữ liệu đảo từ file, đang khôi phục từ bản sao...");
            IslandOreManager.restoreFromBackup(islandDataBackup);
            IslandOreManager.saveData(); // Lưu lại dữ liệu đã khôi phục
        }
        
        if (Main.isDebugEnabled()) {
            Main.sendlog("§a[OreGen4] §2Đã xóa tất cả cache!");
        }
    }

    public static void loadConfig() {
        // Clear existing data
        levels.clear();
        defaultLevels.clear();

        // Initialize maps for each environment
        for (World.Environment env : World.Environment.values()) {
            levels.put(env, new HashMap<>());
        }

        // Load levels from hooks.levels (current config structure)
        loadEnvironmentLevels(World.Environment.NORMAL, "hooks.levels");

        // Sử dụng cùng một cấu hình levels cho tất cả các thế giới
        // Điều này làm cho Nether và End cũng sử dụng cùng cấu hình với thế giới thường
        Map<String, OreLevel> normalLevels = levels.get(World.Environment.NORMAL);
        if (normalLevels != null && !normalLevels.isEmpty()) {
            levels.put(World.Environment.NETHER, new HashMap<>(normalLevels));
            levels.put(World.Environment.THE_END, new HashMap<>(normalLevels));

            // Xử lý môi trường CUSTOM nếu có
            try {
                World.Environment customEnv = World.Environment.valueOf("CUSTOM");
                levels.put(customEnv, new HashMap<>(normalLevels));
            } catch (IllegalArgumentException e) {
                // CUSTOM environment có thể không tồn tại trong một số phiên bản Minecraft
                Main.sendlog("§e[OreGen4] §cMôi trường CUSTOM không được hỗ trợ trong phiên bản này");
            }

            Main.sendlog("§e[OreGen4] §aSao chép cấu hình levels từ NORMAL sang các môi trường khác");
        }

        // Load default levels từ hooks.default (current config structure)
        loadDefaultLevel(World.Environment.NORMAL, "hooks.default");

        // Sử dụng cùng default level cho Nether và End
        OreLevel defaultLevel = defaultLevels.get(World.Environment.NORMAL);
        if (defaultLevel != null) {
            defaultLevels.put(World.Environment.NETHER, defaultLevel);
            defaultLevels.put(World.Environment.THE_END, defaultLevel);

            // Xử lý môi trường CUSTOM nếu có
            try {
                World.Environment customEnv = World.Environment.valueOf("CUSTOM");
                defaultLevels.put(customEnv, defaultLevel);
            } catch (IllegalArgumentException e) {
                // CUSTOM environment có thể không tồn tại trong một số phiên bản Minecraft
            }

            Main.sendlog("§e[OreGen4] §aSao chép cấu hình default level từ NORMAL sang các môi trường khác");
        }

        // In tất cả thông tin đã tải để debug
        for (World.Environment env : World.Environment.values()) {
            if (levels.containsKey(env)) {
                Main.sendlog("§e[OreGen4] §2== Môi trường §a" + env.name() + "§2 có §a" +
                        levels.get(env).size() + "§2 level ==");
                for (Map.Entry<String, OreLevel> entry : levels.get(env).entrySet()) {
                    Main.sendlog("§e[OreGen4] §2   - Key: §a" + entry.getKey() +
                            "§2, Permission: §a" + entry.getValue().getPermission());
                }
            }
        }
    }

    private static void loadEnvironmentLevels(World.Environment env, String path) {
        ConfigurationSection section = Main.pl.getConfig().getConfigurationSection(path);
        if (section == null) {
            Main.sendlog("§c[OreGen4] §eKhông tìm thấy mục cấu hình tại đường dẫn: " + path);
            // Thử tải từ đường dẫn root nếu không tìm thấy từ đường dẫn hooks.levels
            if (path.equals("hooks.levels")) {
                ConfigurationSection alternativeSection = Main.pl.getConfig().getConfigurationSection("hooks.levels");
                if (alternativeSection != null) {
                    section = alternativeSection;
                    Main.sendlog("§e[OreGen4] §aTìm thấy cấu hình thay thế tại đường dẫn: hooks.levels");
                }
            }

            if (section == null) {
                return;
            }
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection levelSection = section.getConfigurationSection(key);
            if (levelSection != null) {
                OreLevel level = OreLevel.fromConfig(levelSection);
                if (level != null) {
                    levels.get(env).put(key, level);
                    Main.sendlog("§e[OreGen4] §2Loaded " + env.name() + " level: " + key + " with permission: "
                            + level.getPermission());
                }
            }
        }
    }

    private static void loadDefaultLevel(World.Environment env, String path) {
        ConfigurationSection section = Main.pl.getConfig().getConfigurationSection(path);
        if (section != null) {
            OreLevel level = OreLevel.fromConfig(section);
            if (level != null) {
                defaultLevels.put(env, level);
                Main.sendlog("§e[OreGen4] §2Loaded default level for " + env.name());
            }
        }
    }

    public static OreLevel getLevel(World.Environment env, String permission) {
        // Chỉ log khi debug mode được bật và permission không phải cái đã xử lý gần đây
        boolean shouldLog = Main.isDebugEnabled();
        
        // Xử lý tên quyền để khớp với cấu trúc trong config
        // Ví dụ: "oregen.cap5" sẽ thành "cap5"
        String permKey = permission;
        if (permission.startsWith("oregen.")) {
            permKey = permission.substring("oregen.".length());
        }
        
        if (shouldLog) {
            Main.sendlog("§e[OreGen4] §bTìm level cho permission §a" + permission + "§b trong môi trường §a" + env);
            Main.sendlog("§e[OreGen4] §bĐã chuyển đổi permission §a" + permission + "§b thành key §a" + permKey);
        }

        Map<String, OreLevel> envLevels = levels.get(env);
        if (envLevels != null) {
            // Chỉ log khi debug mode được bật
            if (shouldLog && false) { // Tắt log này, quá nhiều
                Main.sendlog("§e[OreGen4] §bCó §a" + envLevels.size() + "§b level đã được tải trong môi trường §a" + env);

                // Liệt kê tất cả các level đã tải - tắt để giảm spam
                /*
                Main.sendlog("§e[OreGen4] §bDanh sách các level có sẵn:");
                for (Map.Entry<String, OreLevel> entry : envLevels.entrySet()) {
                    Main.sendlog("§e[OreGen4] §b - Level §a" + entry.getKey() + "§b với permission §a"
                            + entry.getValue().getPermission());
                }
                */
            }

            // Phương pháp 1: Tìm kiếm theo key trực tiếp (cách nhanh nhất)
            OreLevel directLevel = envLevels.get(permKey);
            if (directLevel != null) {
                if (shouldLog) Main.sendlog("§e[OreGen4] §aTìm thấy level trực tiếp với key §2" + permKey +
                        "§a trong môi trường §2" + env);
                return directLevel;
            } else {
                if (shouldLog) Main.sendlog("§e[OreGen4] §cKhông tìm thấy level trực tiếp với key §a" + permKey);
            }

            // Phương pháp 2: Tìm kiếm theo permission đầy đủ
            for (OreLevel level : envLevels.values()) {
                if (level.getPermission().equals(permission)) {
                    if (shouldLog) Main.sendlog("§e[OreGen4] §aTìm thấy level theo permission đầy đủ §2" + permission +
                            "§a trong môi trường §2" + env);
                    return level;
                }
            }

            // Phương pháp 3: So sánh phần sau của permission
            for (OreLevel level : envLevels.values()) {
                String levelPerm = level.getPermission();
                String levelKey = levelPerm;
                if (levelPerm.startsWith("oregen.")) {
                    levelKey = levelPerm.substring("oregen.".length());
                }

                // Quá nhiều log, vô hiệu hóa kể cả khi debug mode bật
                // if (shouldLog) Main.sendlog("§e[OreGen4] §bSo sánh key §a" + permKey + "§b với §a" + levelKey);

                if (permKey.equals(levelKey)) {
                    if (shouldLog) Main.sendlog("§a[OreGen4] §aTìm thấy level theo key rút gọn §2" + permKey +
                            "§a trong môi trường §2" + env);
                    return level;
                }
            }

            // Phương pháp 4: Khớp một phần của key (cap5 trong cap5, v.v.)
            for (Map.Entry<String, OreLevel> entry : envLevels.entrySet()) {
                String configKey = entry.getKey();
                if (configKey.equals(permKey) || permKey.equals(configKey)) {
                    if (shouldLog) Main.sendlog("§a[OreGen4] §aTìm thấy level thông qua khớp phần: §2" + configKey +
                            "§a trong môi trường §2" + env);
                    return entry.getValue();
                }
            }
        } else {
            if (shouldLog) Main.sendlog("§c[OreGen4] §4CẢNH BÁO: Không có level nào được tải cho môi trường §a" + env);
        }

        if (shouldLog) Main.sendlog("§c[OreGen4] §4Không tìm thấy level nào cho permission §a" + permission +
                "§4 trong môi trường §a" + env + "§4 sau khi thử tất cả các phương pháp");
        return null;
    }

    public static OreLevel getDefaultLevel(World.Environment env) {
        return defaultLevels.get(env);
    }
}
