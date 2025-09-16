package me.sfclog.oregen4.command;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import me.sfclog.oregen4.Main;
import me.sfclog.oregen4.config.OreLevel;
import me.sfclog.oregen4.config.OreLevelUtils;
import me.sfclog.oregen4.island.IslandOreManager;
import me.sfclog.oregen4.util.Color;

/**
 * Lệnh nâng cấp quyền sinh ore cho đảo
 */
public class UpgradeOreCommand implements CommandExecutor {

    private final Plugin plugin;

    public UpgradeOreCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Kiểm tra quyền
        if (!sender.hasPermission("oregen.admin")) {
            sender.sendMessage(Color.tran("&cBạn không có quyền thực hiện lệnh này!"));
            return true;
        }

        // Kiểm tra số tham số
        if (args.length < 2) {
            sender.sendMessage(Color.tran("&cSử dụng: /upgradeore <tên người chơi> <cấp độ> [world]"));
            sender.sendMessage(Color.tran("&cVí dụ: /upgradeore Notch 5"));
            sender.sendMessage(Color.tran("&cVí dụ: /upgradeore Notch cap5"));
            sender.sendMessage(Color.tran("&cCác cấp độ: 0-7 hoặc cap1-cap7 (0 là mặc định)"));
            return true;
        }

        // Lấy tên người chơi và cấp độ
        final String playerName = args[0];
        final int level;
        
        // Hỗ trợ cả định dạng số (1-7) và "cap1", "cap2", ...
        String levelArg = args[1].toLowerCase();
        if (levelArg.startsWith("cap")) {
            try {
                // Trích xuất số từ "cap{số}"
                level = Integer.parseInt(levelArg.substring(3));
                if (level < 0 || level > 7) {
                    sender.sendMessage(Color.tran("&cCấp độ không hợp lệ! Các cấp độ từ 0-7"));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(Color.tran("&cĐịnh dạng cấp độ không hợp lệ! Sử dụng số 0-7 hoặc cap1, cap2, ..., cap7"));
                return true;
            }
        } else {
            try {
                level = Integer.parseInt(levelArg);
                if (level < 0 || level > 7) {
                    sender.sendMessage(Color.tran("&cCấp độ không hợp lệ! Các cấp độ từ 0-7"));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(Color.tran("&cCấp độ phải là số từ 0-7 hoặc định dạng cap1, cap2, ..., cap7!"));
                return true;
            }
        }

        // Xác định môi trường (world)
        World.Environment environment = World.Environment.NORMAL; // Mặc định là thế giới thường
        if (args.length >= 3) {
            try {
                environment = World.Environment.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(Color.tran("&cThế giới không hợp lệ! Sử dụng: NORMAL, NETHER, THE_END"));
                return true;
            }
        }

        // Tìm người chơi
        final Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage(Color.tran("&cNgười chơi &e" + playerName + " &ckhông online!"));
            sender.sendMessage(Color.tran("&cNgười chơi phải online để xác định đảo của họ."));
            return true;
        }

        // Xác định ID của đảo
        String islandId = getIslandIdFromPlayer(targetPlayer);
        if (islandId == null) {
            sender.sendMessage(Color.tran("&cKhông thể xác định đảo của người chơi &e" + playerName + "&c!"));
            sender.sendMessage(Color.tran("&cNgười chơi phải đứng trên đảo của họ."));
            return true;
        }

        if (level > 0) {
            // Lấy OreLevel tương ứng với cấp độ
            String permission;
            OreLevel oreLevel;
            
            // Thử lấy theo định dạng "oregen.level.{số}"
            permission = OreLevelUtils.getPermissionFromLevelId(level);
            oreLevel = OreLevelUtils.getOreLevel(permission);
            
            // Nếu không tìm thấy, thử lấy theo định dạng "oregen.cap{số}"
            if (oreLevel == null) {
                permission = "oregen.cap" + level;
                oreLevel = OreLevelUtils.getOreLevel(permission);
            }
            
            if (oreLevel == null) {
                sender.sendMessage(Color.tran("&cKhông tìm thấy cấu hình cho cấp độ &e" + level + "&c!"));
                return true;
            }
        }

        // Thông báo đang xử lý
        sender.sendMessage(Color.tran("&eDang nâng cấp ore cho đảo của &b" + playerName + 
                " &elên cấp độ &b" + level + "&e trong thế giới &b" + environment.name() + "&e..."));

        // Đặt cấp độ ore cho đảo
        if (level > 0) {
            // Thử lấy theo định dạng "oregen.level.{số}"
            String permission = OreLevelUtils.getPermissionFromLevelId(level);
            OreLevel oreLevel = OreLevelUtils.getOreLevel(permission);
            
            // Nếu không tìm thấy, thử lấy theo định dạng "oregen.cap{số}"
            if (oreLevel == null) {
                permission = "oregen.cap" + level;
                oreLevel = OreLevelUtils.getOreLevel(permission);
            }
            
            if (oreLevel != null) {
                // Debug log trước khi đặt cấp độ ore
                if (Main.isDebugEnabled()) {
                    Main.sendlog("§e[UpgradeOreCmd] §bĐặt cấp độ ore §a" + permission + 
                            "§b cho đảo §a" + islandId + "§b trong môi trường §a" + environment);
                }
                
                IslandOreManager.setIslandOreLevel(islandId, environment, oreLevel);
                
                // Kiểm tra lại xem đã được lưu vào cache chưa
                if (Main.isDebugEnabled()) {
                    OreLevel checkLevel = IslandOreManager.getIslandOreLevel(islandId, environment);
                    if (checkLevel != null) {
                        Main.sendlog("§e[UpgradeOreCmd] §aXác nhận: Cấp độ ore đã được đặt thành §b" + 
                                checkLevel.getPermission() + "§a cho đảo §b" + islandId);
                    } else {
                        Main.sendlog("§c[UpgradeOreCmd] LỖI: Không thể tìm thấy cấp độ ore sau khi đặt cho đảo §b" + islandId);
                    }
                }
            } else {
                sender.sendMessage(Color.tran("&cKhông tìm thấy cấu hình cho cấp độ &e" + level + "&c!"));
                return true;
            }
        } else {
            // Nếu level là 0, xóa ore level (reset về mặc định)
            Map<World.Environment, OreLevel> envMap = IslandOreManager.getIslandOreLevels(islandId);
            if (envMap != null) {
                envMap.remove(environment);
                if (envMap.isEmpty()) {
                    IslandOreManager.removeIsland(islandId);
                }
            }
        }

        // Thông báo thành công
        sender.sendMessage(Color.tran("&aNâng cấp ore cho đảo của &b" + playerName + 
                " &alên cấp độ &b" + level + " &athành công!"));
        
        // Thông báo cho người chơi
        targetPlayer.sendMessage(Color.tran("&aOre của đảo bạn đã được nâng cấp lên cấp độ &b" + level + "&a!"));

        return true;
    }

    /**
     * Lấy ID của đảo từ người chơi
     */
    private String getIslandIdFromPlayer(Player player) {
        String islandId = null;
        
        // Thử lấy từ BentoBox hook
        if (Main.hookbentobox != null) {
            islandId = Main.hookbentobox.getIslandIdAt(player.getLocation());
            if (islandId != null) {
                if (Main.isDebugEnabled()) {
                    Main.sendlog("§e[OreGen4] §aBentoBox: Đã tìm thấy đảo ID §b" + islandId + "§a cho người chơi §b" + player.getName());
                }
                return islandId;
            } else if (Main.isDebugEnabled()) {
                Main.sendlog("§c[OreGen4] BentoBox: Không tìm thấy đảo cho người chơi " + player.getName());
            }
        }
        
        // Thử lấy từ SuperiorSkyblock hook
        if (Main.hooksuper != null) {
            islandId = Main.hooksuper.getIslandID(player.getLocation());
            if (islandId != null) {
                if (Main.isDebugEnabled()) {
                    Main.sendlog("§e[OreGen4] §aSuperiorSkyblock: Đã tìm thấy đảo ID §b" + islandId + "§a cho người chơi §b" + player.getName());
                }
                return islandId;
            } else if (Main.isDebugEnabled()) {
                Main.sendlog("§c[OreGen4] SuperiorSkyblock: Không tìm thấy đảo cho người chơi " + player.getName());
            }
        }
        
        // Nếu không tìm thấy đảo
        if (Main.isDebugEnabled()) {
            Main.sendlog("§c[OreGen4] Không tìm thấy đảo cho người chơi " + player.getName());
            Main.sendlog("§c[OreGen4] World: " + player.getWorld().getName());
            Main.sendlog("§c[OreGen4] Location: " + player.getLocation().getX() + ", " + 
                      player.getLocation().getY() + ", " + player.getLocation().getZ());
        }
        
        return null;
    }
}
