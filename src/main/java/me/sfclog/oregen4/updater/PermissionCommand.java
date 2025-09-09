package me.sfclog.oregen4.updater;

import me.sfclog.oregen4.util.Color;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/**
 * Lệnh quản lý và xem thông tin quyền
 */
public class PermissionCommand implements CommandExecutor {

    private final Plugin plugin;

    public PermissionCommand(Plugin plugin) {
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

        // Xử lý các lệnh con
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "status":
                showStatus(sender);
                break;
            case "update":
                runUpdate(sender);
                break;
            case "reload":
                reloadUpdater(sender);
                break;
            case "clear":
                if (args.length >= 2) {
                    clearCache(sender, args[1]);
                } else {
                    sender.sendMessage(Color.tran("&cSử dụng: /permission clear <player>"));
                }
                break;
            default:
                showHelp(sender);
                break;
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(Color.tran("&a==== Hệ thống cập nhật quyền OreGen4 ===="));
        sender.sendMessage(Color.tran("&a/permission status &7- Xem trạng thái cập nhật quyền"));
        sender.sendMessage(Color.tran("&a/permission update &7- Chạy cập nhật quyền ngay lập tức"));
        sender.sendMessage(Color.tran("&a/permission reload &7- Tải lại hệ thống cập nhật quyền"));
        sender.sendMessage(Color.tran("&a/permission clear <player> &7- Xóa cache quyền của người chơi"));
    }

    private void showStatus(CommandSender sender) {
        int cachedPlayers = PermissionUpdater.getCachedPlayerCount();
        boolean isEnabled = PermissionUpdater.isEnabled();
        long interval = PermissionUpdater.getUpdateInterval();

        sender.sendMessage(Color.tran("&a==== Trạng thái hệ thống cập nhật quyền ===="));
        sender.sendMessage(Color.tran("&aTrạng thái: " + (isEnabled ? "&aĐang hoạt động" : "&cĐang tắt")));
        sender.sendMessage(Color.tran("&aKhoảng thời gian cập nhật: &f" + (interval / 1000) + " giây"));
        sender.sendMessage(Color.tran("&aSố người chơi đang cache: &f" + cachedPlayers));
    }

    private void runUpdate(CommandSender sender) {
        sender.sendMessage(Color.tran("&aĐang chạy cập nhật quyền..."));

        // Chạy cập nhật bất đồng bộ để không block main thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PermissionUpdater.updateAllPermissions();
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> sender.sendMessage(Color.tran("&aCập nhật quyền hoàn tất!")));
            } catch (Exception e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(Color.tran("&cLỗi khi cập nhật quyền: " + e.getMessage()));
                    e.printStackTrace();
                });
            }
        });
    }

    private void reloadUpdater(CommandSender sender) {
        sender.sendMessage(Color.tran("&aĐang tải lại hệ thống cập nhật quyền..."));

        try {
            PermissionUpdater.reload();
            sender.sendMessage(Color.tran("&aĐã tải lại hệ thống cập nhật quyền thành công!"));
        } catch (Exception e) {
            sender.sendMessage(Color.tran("&cLỗi khi tải lại: " + e.getMessage()));
        }
    }

    private void clearCache(CommandSender sender, String playerName) {
        try {
            PermissionUpdater.clearPlayerInfo(playerName);
            sender.sendMessage(Color.tran("&aĐã xóa cache quyền cho người chơi &f" + playerName));
        } catch (Exception e) {
            sender.sendMessage(Color.tran("&cLỗi khi xóa cache: " + e.getMessage()));
        }
    }
}
