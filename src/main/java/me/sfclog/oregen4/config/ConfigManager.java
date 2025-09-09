package me.sfclog.oregen4.config;

import me.sfclog.oregen4.Main;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static final Map<World.Environment, Map<String, OreLevel>> levels = new HashMap<>();
    private static final Map<World.Environment, OreLevel> defaultLevels = new HashMap<>();

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
        Main.sendlog("§e[OreGen4] Tìm level cho permission " + permission + " trong môi trường " + env);

        // Xử lý tên quyền để khớp với cấu trúc trong config
        // Ví dụ: "oregen.cap5" sẽ thành "cap5"
        String permKey = permission;
        if (permission.startsWith("oregen.")) {
            permKey = permission.substring("oregen.".length());
        }

        Main.sendlog("§e[OreGen4] Đã chuyển đổi permission " + permission + " thành key " + permKey);

        Map<String, OreLevel> envLevels = levels.get(env);
        if (envLevels != null) {
            Main.sendlog("§e[OreGen4] Có " + envLevels.size() + " level đã được tải trong môi trường " + env);

            // Liệt kê tất cả các level đã tải
            Main.sendlog("§e[OreGen4] Danh sách các level có sẵn:");
            for (Map.Entry<String, OreLevel> entry : envLevels.entrySet()) {
                Main.sendlog("§e[OreGen4]  - Level " + entry.getKey() + " với permission "
                        + entry.getValue().getPermission());
            }

            // Phương pháp 1: Tìm kiếm theo key trực tiếp (cách nhanh nhất)
            OreLevel directLevel = envLevels.get(permKey);
            if (directLevel != null) {
                Main.sendlog("§e[OreGen4] Tìm thấy level trực tiếp với key " + permKey +
                        " trong môi trường " + env);
                return directLevel;
            } else {
                Main.sendlog("§e[OreGen4] Không tìm thấy level trực tiếp với key " + permKey);
            }

            // Phương pháp 2: Tìm kiếm theo permission đầy đủ
            for (OreLevel level : envLevels.values()) {
                if (level.getPermission().equals(permission)) {
                    Main.sendlog("§e[OreGen4] Tìm thấy level theo permission đầy đủ " + permission +
                            " trong môi trường " + env);
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

                Main.sendlog("§e[OreGen4] §bSo sánh key §a" + permKey + "§b với §a" + levelKey);

                if (permKey.equals(levelKey)) {
                    Main.sendlog("§a[OreGen4] Tìm thấy level theo key rút gọn " + permKey +
                            " trong môi trường " + env);
                    return level;
                }
            }

            // Phương pháp 4: Khớp một phần của key (cap5 trong cap5, v.v.)
            for (Map.Entry<String, OreLevel> entry : envLevels.entrySet()) {
                String configKey = entry.getKey();
                if (configKey.equals(permKey) || permKey.equals(configKey)) {
                    Main.sendlog("§a[OreGen4] Tìm thấy level thông qua khớp phần: " + configKey +
                            " trong môi trường " + env);
                    return entry.getValue();
                }
            }
        } else {
            Main.sendlog("§c[OreGen4] CẢNH BÁO: Không có level nào được tải cho môi trường " + env);
        }

        Main.sendlog("§c[OreGen4] Không tìm thấy level nào cho permission " + permission +
                " trong môi trường " + env + " sau khi thử tất cả các phương pháp");
        return null;
    }

    public static OreLevel getDefaultLevel(World.Environment env) {
        return defaultLevels.get(env);
    }
}
