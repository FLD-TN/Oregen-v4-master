package me.sfclog.oregen4.command;

import me.sfclog.oregen4.Main;
import me.sfclog.oregen4.util.Color;
import me.sfclog.oregen4.util.EnhancedPermissionCache;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Lệnh nâng cấp quyền sinh ore cho người chơi
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
        if (args.length != 2) {
            sender.sendMessage(Color.tran("&cSử dụng: /upgradeore <tên người chơi> <cấp độ>"));
            sender.sendMessage(Color.tran("&cVí dụ: /upgradeore Notch cap1"));
            sender.sendMessage(Color.tran("&cCác cấp độ: default, cap1, cap2, cap3, cap4, cap5, cap6, cap7"));
            return true;
        }

        // Lấy tên người chơi và cấp độ
        final String playerName = args[0];
        final String level = args[1].toLowerCase();

        // Kiểm tra cấp độ hợp lệ
        if (!isValidLevel(level)) {
            sender.sendMessage(
                    Color.tran("&cCấp độ không hợp lệ! Các cấp độ: default, cap1, cap2, cap3, cap4, cap5, cap6, cap7"));
            return true;
        }

        // Tìm người chơi
        final UUID targetUUID = findPlayerUUID(playerName);

        // Nếu không tìm thấy người chơi
        if (targetUUID == null) {
            sender.sendMessage(
                    Color.tran("&cNgười chơi &e" + playerName + " &ckhông tồn tại hoặc chưa từng tham gia server!"));
            return true;
        }

        // Lấy API LuckPerms
        LuckPerms luckPermsApi = Main.getInstance().getLuckPermsApi();
        if (luckPermsApi == null) {
            sender.sendMessage(Color.tran("&cKhông thể kết nối với LuckPerms API!"));
            return true;
        }

        // Thông báo đang xử lý
        sender.sendMessage(Color
                .tran("&eDang nâng cấp quyền sinh quặng cho &b" + playerName + " &elên cấp độ &b" + level + "&e..."));

        processPermissionUpdate(sender, playerName, level, targetUUID, luckPermsApi);

        return true;
    }

    /**
     * Tìm UUID của người chơi từ tên
     */
    private UUID findPlayerUUID(String playerName) {
        // Kiểm tra người chơi online trước
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return onlinePlayer.getUniqueId();
        }

        // Nếu không online, tìm trong danh sách người chơi đã từng tham gia
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(playerName)) {
                return offlinePlayer.getUniqueId();
            }
        }

        return null;
    }

    /**
     * Xử lý cập nhật quyền
     */
    private void processPermissionUpdate(CommandSender sender, String playerName, String level, UUID targetUUID,
            LuckPerms luckPermsApi) {
        // Xử lý bất đồng bộ để không chặn thread chính
        CompletableFuture.runAsync(() -> {
            try {
                // Tải user từ LuckPerms
                User user = luckPermsApi.getUserManager().loadUser(targetUUID).join();
                if (user == null) {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> sender.sendMessage(Color.tran("&cKhông thể tải thông tin người chơi từ LuckPerms!")));
                    return;
                }

                // Xóa tất cả quyền ore.* cũ
                for (String oldLevel : new String[] { "default", "cap1", "cap2", "cap3", "cap4", "cap5", "cap6",
                        "cap7" }) {
                    user.data().remove(Node.builder("oregen." + oldLevel).build());
                }

                // Thêm quyền mới nếu không phải default
                // Với default, chỉ cần xóa tất cả quyền khác
                if (!level.equals("default")) {
                    user.data().add(Node.builder("oregen." + level).build());
                }

                // Lưu user
                luckPermsApi.getUserManager().saveUser(user).join();

                // Xóa cache cũ
                for (World.Environment env : World.Environment.values()) {
                    EnhancedPermissionCache.removeCachedPermissions(targetUUID, env);
                }

                // Thông báo thành công
                final String finalLevel = level;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(Color.tran("&aNâng cấp quyền sinh quặng cho &b" + playerName + " &alên cấp độ &b"
                            + finalLevel + " &athành công!"));

                    // Thông báo cho người chơi nếu đang online
                    Player targetPlayer = Bukkit.getPlayer(targetUUID);
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        targetPlayer.sendMessage(Color.tran(
                                "&aQuyền sinh quặng của bạn đã được nâng cấp lên cấp độ &b" + finalLevel + "&a!"));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(plugin,
                        () -> sender.sendMessage(Color.tran("&cLỗi khi nâng cấp quyền: " + e.getMessage())));
            }
        });
    }

    /**
     * Kiểm tra cấp độ có hợp lệ không
     */
    private boolean isValidLevel(String level) {
        return level.matches("cap[1-7]") || level.equals("default");
    }
}
