package me.sfclog.oregen4.config;

import org.bukkit.World;
import me.sfclog.oregen4.Main;

/**
 * Tiện ích để làm việc với các cấp độ ore
 */
public class OreLevelUtils {

    /**
     * Lấy OreLevel từ permission
     * 
     * @param permission Permission của ore level
     * @return OreLevel tương ứng hoặc null nếu không tìm thấy
     */
    public static OreLevel getOreLevel(String permission) {
        if (Main.isDebugEnabled()) {
            Main.sendlog("§e[OreGen4] §bĐang tìm OreLevel cho permission: §a" + permission);
        }
        
        // Lấy từ normal environment, vì cấu hình giống nhau cho tất cả thế giới
        OreLevel result = ConfigManager.getLevel(World.Environment.NORMAL, permission);
        
        if (Main.isDebugEnabled()) {
            if (result != null) {
                Main.sendlog("§e[OreGen4] §aTìm thấy OreLevel với permission: §b" + permission);
            } else {
                Main.sendlog("§c[OreGen4] Không tìm thấy OreLevel nào cho permission: §b" + permission);
            }
        }
        
        return result;
    }

    /**
     * Lấy OreLevel từ id số
     * 
     * @param levelId ID cấp độ của ore level
     * @return OreLevel tương ứng hoặc null nếu không tìm thấy
     */
    public static OreLevel getOreLevelById(int levelId) {
        // Thử tìm theo định dạng "oregen.cap{số}" trước
        String permissionCap = "oregen.cap" + levelId;
        OreLevel levelCap = getOreLevel(permissionCap);
        
        if (levelCap != null) {
            return levelCap;
        }
        
        // Nếu không tìm thấy, thử tìm theo định dạng "oregen.level.{số}"
        String permissionLevel = "oregen.level." + levelId;
        return getOreLevel(permissionLevel);
    }
    
    /**
     * Lấy permission từ level id
     * 
     * @param levelId ID cấp độ
     * @return Permission string
     */
    public static String getPermissionFromLevelId(int levelId) {
        // Trong config.yml, permission cho cấp độ 1-7 là "oregen.cap1" đến "oregen.cap7"
        return "oregen.cap" + levelId;
    }
    
    /**
     * Lấy level id từ permission
     * 
     * @param permission Permission string
     * @return Level id hoặc 0 nếu không hợp lệ
     */
    public static int getLevelIdFromPermission(String permission) {
        if (permission == null) {
            return 0;
        }
        
        // Hỗ trợ định dạng "oregen.level.X"
        if (permission.startsWith("oregen.level.")) {
            try {
                return Integer.parseInt(permission.substring("oregen.level.".length()));
            } catch (NumberFormatException e) {
                // Bỏ qua và tiếp tục kiểm tra định dạng khác
            }
        }
        
        // Hỗ trợ định dạng "oregen.capX"
        if (permission.startsWith("oregen.cap")) {
            try {
                return Integer.parseInt(permission.substring("oregen.cap".length()));
            } catch (NumberFormatException e) {
                // Bỏ qua
            }
        }
        
        // Nếu không có định dạng nào khớp, trả về 0
        return 0;
    }
}