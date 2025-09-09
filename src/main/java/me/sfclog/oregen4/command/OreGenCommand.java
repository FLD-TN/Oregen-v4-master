package me.sfclog.oregen4.command;

import me.sfclog.oregen4.Main;
import me.sfclog.oregen4.config.ConfigManager;
import me.sfclog.oregen4.util.Color;
import me.sfclog.oregen4.util.EnhancedPermissionCache;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/**
 * Lệnh chính của plugin OreGen4
 */
public class OreGenCommand implements CommandExecutor {

    private final Plugin plugin;

    public OreGenCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Kiểm tra quyền
        if (!sender.hasPermission("oregen.admin")) {
            sender.sendMessage(Color.tran("&cBạn không có quyền thực hiện lệnh này!"));
            return true;
        }

        // Không có tham số
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        // Xử lý các lệnh phụ
        switch (args[0].toLowerCase()) {
            case "reload":
                // Tải lại cấu hình
                ConfigManager.loadConfig();
                sender.sendMessage(Color.tran("&aĐã tải lại cấu hình từ file!"));
                return true;

            case "clearcache":
                // Xóa cache
                EnhancedPermissionCache.clearAllCache();
                me.sfclog.oregen4.util.IslandPermissionCache.clearAllCache();
                sender.sendMessage(Color.tran("&aĐã xóa tất cả cache quyền và cache đảo!"));
                return true;

            case "islandcache":
                // Quản lý cache đảo
                if (args.length >= 2 && args[1].equalsIgnoreCase("clear")) {
                    me.sfclog.oregen4.util.IslandPermissionCache.clearAllCache();
                    sender.sendMessage(Color.tran("&aĐã xóa tất cả cache đảo!"));
                } else {
                    sender.sendMessage(Color.tran("&eSử dụng: /oregen islandcache clear &7- Xóa tất cả cache đảo"));
                }
                return true;

            case "debug":
                // Bật/tắt chế độ debug
                boolean currentDebug = plugin.getConfig().getBoolean("debug", false);
                plugin.getConfig().set("debug", !currentDebug);
                plugin.saveConfig();

                if (!currentDebug) {
                    sender.sendMessage(Color.tran("&aĐã bật chế độ debug!"));
                    Main.sendlog("§aĐã bật chế độ debug từ lệnh của " + sender.getName());
                } else {
                    sender.sendMessage(Color.tran("&aĐã tắt chế độ debug!"));
                    Main.sendlog("§aĐã tắt chế độ debug từ lệnh của " + sender.getName());
                }
                return true;

            case "version":
                // Hiển thị thông tin phiên bản
                sender.sendMessage(Color.tran("&8-----------------[ &bOreGen4 &8]-----------------"));
                sender.sendMessage(Color.tran("&7Phiên bản: &b" + plugin.getDescription().getVersion()));
                sender.sendMessage(Color.tran("&7Tác giả: &bSFC_Log"));
                sender.sendMessage(Color.tran("&7Phiên bản API: &b" + plugin.getDescription().getAPIVersion()));
                sender.sendMessage(Color.tran("&8------------------------------------------"));
                return true;

            default:
                showHelp(sender);
                return true;
        }
    }

    /**
     * Hiển thị hướng dẫn sử dụng lệnh
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(Color.tran("&8-----------------[ &bOreGen4 &8]-----------------"));
        sender.sendMessage(Color.tran("&e/oregen reload &7- Tải lại cấu hình"));
        sender.sendMessage(Color.tran("&e/oregen clearcache &7- Xóa tất cả cache"));
        sender.sendMessage(Color.tran("&e/oregen islandcache clear &7- Xóa cache đảo"));
        sender.sendMessage(Color.tran("&e/oregen debug &7- Bật/tắt chế độ debug"));
        sender.sendMessage(Color.tran("&e/oregen version &7- Hiển thị thông tin phiên bản"));
        sender.sendMessage(Color.tran("&e/upgradeore <player> <level> &7- Nâng cấp quyền"));
        sender.sendMessage(Color.tran("&e/permission status &7- Xem trạng thái cập nhật quyền"));
        sender.sendMessage(Color.tran("&e/orestats &7- Xem thông tin hiệu suất"));
        sender.sendMessage(Color.tran("&8------------------------------------------"));
    }
}
