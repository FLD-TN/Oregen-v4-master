package me.sfclog.oregen4.island;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import me.sfclog.oregen4.Main;
import me.sfclog.oregen4.config.OreLevel;
import me.sfclog.oregen4.config.OreLevelUtils;

/**
 * Quản lý cấp độ ore cho từng đảo
 * Mỗi đảo sẽ có một cấp độ ore riêng
 * Cấp độ ore của đảo sẽ không thay đổi khi chuyển chủ hoặc thành viên rời đi
 * Chỉ khi đảo bị xóa (disband) thì cấp độ ore mới bị xóa
 */
public class IslandOreManager {

    private static final Map<String, Map<World.Environment, OreLevel>> islandOreCache = new ConcurrentHashMap<>();
    private static File dataFile;
    private static FileConfiguration dataConfig;
    private static final JavaPlugin plugin = Main.getInstance();
    
    /**
     * Khởi tạo manager
     */
    public static void init() {
        dataFile = new File(plugin.getDataFolder(), "island_ore_data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Không thể tạo file dữ liệu island ore", e);
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadData();
        
        // Đăng ký task tự động lưu dữ liệu mỗi 5 phút
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, IslandOreManager::saveData, 6000L, 6000L);
    }
    
    /**
     * Tải dữ liệu từ file
     */
    public static void loadData() {
        // Tạo bản sao của cache hiện tại để phòng trường hợp lỗi
        Map<String, Map<World.Environment, OreLevel>> cacheCopy = null;
        if (!islandOreCache.isEmpty()) {
            cacheCopy = getIslandOreDataCopy();
        }
        
        // Xóa cache hiện tại
        islandOreCache.clear();
        
        // Kiểm tra file tồn tại
        if (dataFile == null) {
            plugin.getLogger().log(Level.SEVERE, "dataFile là null! Không thể tải dữ liệu.");
            if (cacheCopy != null) {
                restoreFromBackup(cacheCopy);
                return;
            }
        }
        
        // Đọc lại file từ đĩa trước khi tải dữ liệu
        if (dataFile != null && dataFile.exists()) {
            try {
                dataConfig = YamlConfiguration.loadConfiguration(dataFile);
                plugin.getLogger().log(Level.INFO, "Đã đọc lại file island_ore_data.yml");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Lỗi khi đọc file island_ore_data.yml: {0}", e.getMessage());
                if (cacheCopy != null) {
                    restoreFromBackup(cacheCopy);
                    return;
                }
            }
        } else {
            plugin.getLogger().log(Level.WARNING, "File island_ore_data.yml không tồn tại hoặc không thể truy cập!");
            if (dataFile != null) {
                try {
                    dataFile.createNewFile();
                    dataConfig = YamlConfiguration.loadConfiguration(dataFile);
                    plugin.getLogger().log(Level.INFO, "Đã tạo file island_ore_data.yml mới.");
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Không thể tạo file island_ore_data.yml: {0}", e.getMessage());
                }
            }
        }
        
        int islandCount = 0;
        
        // Kiểm tra nếu có dữ liệu islands trong file
        if (dataConfig != null && dataConfig.contains("islands")) {
            ConfigurationSection islandsSection = dataConfig.getConfigurationSection("islands");
            if (islandsSection != null) {
                for (String islandId : islandsSection.getKeys(false)) {
                    Map<World.Environment, OreLevel> envMap = new ConcurrentHashMap<>();
                    
                    ConfigurationSection islandSection = islandsSection.getConfigurationSection(islandId);
                    if (islandSection != null) {
                        for (String env : islandSection.getKeys(false)) {
                            World.Environment environment;
                            try {
                                environment = World.Environment.valueOf(env);
                            } catch (IllegalArgumentException e) {
                                continue;
                            }
                            
                            int levelId = dataConfig.getInt("islands." + islandId + "." + env);
                            OreLevel oreLevel = OreLevelUtils.getOreLevelById(levelId);
                            if (oreLevel != null) {
                                envMap.put(environment, oreLevel);
                            }
                        }
                    }
                    
                    if (!envMap.isEmpty()) {
                        islandOreCache.put(islandId, envMap);
                        islandCount++;
                    }
                }
            }
        }
        
        // Nếu không tải được đảo nào và có bản sao, khôi phục từ bản sao
        if (islandCount == 0 && cacheCopy != null && !cacheCopy.isEmpty()) {
            plugin.getLogger().log(Level.WARNING, "Không thể tải dữ liệu từ file, khôi phục từ bản sao...");
            restoreFromBackup(cacheCopy);
        } else {
            plugin.getLogger().log(Level.INFO, "Đã tải {0} đảo với dữ liệu ore level.", islandOreCache.size());
        }
    }
    
    /**
     * Lưu dữ liệu vào file
     */
    public static void saveData() {
        // Kiểm tra nếu không có dữ liệu đảo, không xóa dữ liệu cũ
        if (islandOreCache.isEmpty()) {
            plugin.getLogger().log(Level.WARNING, "Không có dữ liệu đảo trong cache để lưu. Bỏ qua xóa dữ liệu cũ.");
            return;
        }
        
        // Lưu số lượng đảo trước khi xóa phần islands
        int islandCount = islandOreCache.size();
        plugin.getLogger().log(Level.INFO, "Đang lưu dữ liệu ore cho {0} đảo...", islandCount);
        
        // Xóa dữ liệu cũ
        dataConfig.set("islands", null);
        
        // Đếm số đảo đã lưu
        int savedCount = 0;
        
        for (Map.Entry<String, Map<World.Environment, OreLevel>> entry : islandOreCache.entrySet()) {
            String islandId = entry.getKey();
            Map<World.Environment, OreLevel> envMap = entry.getValue();
            
            boolean islandSaved = false;
            
            for (Map.Entry<World.Environment, OreLevel> envEntry : envMap.entrySet()) {
                World.Environment env = envEntry.getKey();
                OreLevel level = envEntry.getValue();
                
                if (level == null) {
                    continue;
                }
                
                // Lưu level ID từ permission
                String permission = level.getPermission();
                int levelId = OreLevelUtils.getLevelIdFromPermission(permission);
                
                // Debug log
                if (Main.isDebugEnabled()) {
                    Main.sendlog("§e[OreGen4] §aĐang lưu đảo §b" + islandId + "§a, môi trường §b" + env.name() + 
                             "§a, permission §b" + permission + "§a, level ID §b" + levelId);
                }
                
                // Nếu không tìm thấy level ID hợp lệ, ghi log cảnh báo
                if (levelId == 0 && !permission.endsWith("default")) {
                    plugin.getLogger().log(Level.WARNING, "Permission không hợp lệ cho đảo {0}: {1}", new Object[]{islandId, permission});
                }
                
                dataConfig.set("islands." + islandId + "." + env.name(), levelId);
                islandSaved = true;
            }
            
            if (islandSaved) {
                savedCount++;
            }
        }
        
        try {
            dataConfig.save(dataFile);
            plugin.getLogger().log(Level.INFO, "Đã lưu dữ liệu {0}/{1} đảo vào island_ore_data.yml thành công!", new Object[]{savedCount, islandCount});
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Không thể lưu dữ liệu island ore", e);
        }
    }
    
    /**
     * Lấy cấp độ ore của đảo trong environment cụ thể
     *
     * @param islandId ID của đảo
     * @param env Environment (thế giới)
     * @return OreLevel của đảo, null nếu đảo chưa được nâng cấp
     */
    public static OreLevel getIslandOreLevel(String islandId, World.Environment env) {
        if (Main.isDebugEnabled()) {
            Main.sendlog("§e[OreGen4] §bTìm cấp độ ore cho đảo §a" + islandId + "§b trong môi trường §a" + env);
            Main.sendlog("§e[OreGen4] §bCó §a" + islandOreCache.size() + "§b đảo trong cache");
            
            // In ra tất cả các đảo trong cache để kiểm tra
            for (String id : islandOreCache.keySet()) {
                Main.sendlog("§e[OreGen4] §b - ID đảo trong cache: §a" + id);
            }
        }
        
        if (islandOreCache.containsKey(islandId)) {
            Map<World.Environment, OreLevel> envMap = islandOreCache.get(islandId);
            OreLevel level = envMap.get(env);
            
            if (Main.isDebugEnabled()) {
                if (level != null) {
                    Main.sendlog("§e[OreGen4] §aTìm thấy cấp độ ore §b" + level.getPermission() + 
                            "§a cho đảo §b" + islandId + "§a trong môi trường §b" + env);
                } else {
                    Main.sendlog("§c[OreGen4] Đảo §b" + islandId + 
                            "§c có trong cache nhưng không có cấp độ ore cho môi trường §b" + env);
                }
            }
            
            return level;
        }
        
        if (Main.isDebugEnabled()) {
            Main.sendlog("§c[OreGen4] Không tìm thấy đảo §b" + islandId + "§c trong cache");
        }
        return null;
    }
    
    /**
     * Lấy tất cả cấp độ ore của đảo
     *
     * @param islandId ID của đảo
     * @return Map chứa cấp độ ore cho mỗi environment, hoặc null nếu đảo chưa được nâng cấp
     */
    public static Map<World.Environment, OreLevel> getIslandOreLevels(String islandId) {
        return islandOreCache.get(islandId);
    }
    
    /**
     * Đặt cấp độ ore cho đảo
     *
     * @param islandId ID của đảo
     * @param env Environment (thế giới)
     * @param level Cấp độ ore
     */
    public static void setIslandOreLevel(String islandId, World.Environment env, OreLevel level) {
        if (islandId == null) {
            plugin.getLogger().warning("Không thể đặt cấp độ ore: Island ID là null");
            return;
        }
        
        if (level == null) {
            plugin.getLogger().log(Level.WARNING, "Không thể đặt cấp độ ore cho đảo {0}: OreLevel là null", islandId);
            return;
        }
        
        // Debug log
        if (Main.isDebugEnabled()) {
            Main.sendlog("§e[OreGen4] §aĐặt cấp độ ore cho đảo §b" + islandId + 
                     "§a, môi trường §b" + env.name() + 
                     "§a, permission §b" + level.getPermission());
        }
        
        // Lưu vào cache
        Map<World.Environment, OreLevel> envMap = islandOreCache.computeIfAbsent(islandId, k -> new ConcurrentHashMap<>());
        envMap.put(env, level);
        
        // Thông báo số lượng đảo trong cache
        if (Main.isDebugEnabled()) {
            Main.sendlog("§e[OreGen4] §aHiện có §b" + islandOreCache.size() + "§a đảo trong cache");
        }
        
        // Lưu ngay lập tức để tránh mất dữ liệu nếu server crash
        Bukkit.getScheduler().runTaskAsynchronously(plugin, IslandOreManager::saveData);
    }
    
    /**
     * Xóa dữ liệu ore level của đảo khi đảo bị xóa
     *
     * @param islandId ID của đảo
     */
    public static void removeIsland(String islandId) {
        islandOreCache.remove(islandId);
        
        // Lưu ngay lập tức để cập nhật file dữ liệu
        Bukkit.getScheduler().runTaskAsynchronously(plugin, IslandOreManager::saveData);
    }
    
    /**
     * Xóa toàn bộ cache và dữ liệu khi plugin tắt
     */
    public static void shutdown() {
        saveData();
        islandOreCache.clear();
        plugin.getLogger().info("Đã lưu và dọn dẹp dữ liệu island ore.");
    }
    
    /**
     * Lấy số lượng đảo có trong cache
     * @return Số đảo có trong cache
     */
    public static int getIslandCount() {
        return islandOreCache.size();
    }
    
    /**
     * Tạo một bản sao của dữ liệu đảo hiện tại
     * @return Bản sao của dữ liệu đảo
     */
    public static Map<String, Map<World.Environment, OreLevel>> getIslandOreDataCopy() {
        Map<String, Map<World.Environment, OreLevel>> dataCopy = new HashMap<>();
        
        for (Map.Entry<String, Map<World.Environment, OreLevel>> entry : islandOreCache.entrySet()) {
            String islandId = entry.getKey();
            Map<World.Environment, OreLevel> envMap = entry.getValue();
            
            Map<World.Environment, OreLevel> envCopy = new HashMap<>();
            for (Map.Entry<World.Environment, OreLevel> envEntry : envMap.entrySet()) {
                envCopy.put(envEntry.getKey(), envEntry.getValue());
            }
            
            dataCopy.put(islandId, envCopy);
        }
        
        return dataCopy;
    }
    
    /**
     * Khôi phục dữ liệu từ bản sao
     * @param backup Bản sao dữ liệu đảo
     */
    public static void restoreFromBackup(Map<String, Map<World.Environment, OreLevel>> backup) {
        if (backup == null || backup.isEmpty()) {
            plugin.getLogger().warning("Không thể khôi phục dữ liệu từ bản sao: Dữ liệu trống");
            return;
        }
        
        islandOreCache.clear();
        islandOreCache.putAll(backup);
        plugin.getLogger().log(Level.INFO, "Đã khôi phục {0} đảo từ bản sao", islandOreCache.size());
    }
}