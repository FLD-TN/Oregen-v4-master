package me.sfclog.oregen4.listener;

import me.sfclog.oregen4.util.LocationOwnerCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listener cho các sự kiện chuyển chủ đảo từ các plugin đảo phổ biến
 * 
 * Phiên bản này sử dụng command preprocess để theo dõi lệnh chuyển chủ
 * thay vì các event từ plugin đảo để tránh phụ thuộc trực tiếp
 */
public class IslandTransferListener implements Listener {

    // Pattern cho lệnh chuyển chủ của các plugin đảo phổ biến
    private static final Pattern BENTOBOX_TRANSFER_PATTERN = Pattern
            .compile("(?i)/(is|island)\\s+transfer\\s+([\\w-]+)");
    private static final Pattern SUPERIOR_TRANSFER_PATTERN = Pattern
            .compile("(?i)/(is|island)\\s+transfer\\s+([\\w-]+)");
    private static final Pattern ASKYBLOCK_TRANSFER_PATTERN = Pattern
            .compile("(?i)/(is|island)\\s+transfer\\s+([\\w-]+)");

    /**
     * Theo dõi lệnh chuyển chủ đảo
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandTransferCommand(PlayerCommandPreprocessEvent event) {
        try {
            String command = event.getMessage();
            Player sender = event.getPlayer();

            Matcher bentoMatcher = BENTOBOX_TRANSFER_PATTERN.matcher(command);
            Matcher superiorMatcher = SUPERIOR_TRANSFER_PATTERN.matcher(command);
            Matcher askyblockMatcher = ASKYBLOCK_TRANSFER_PATTERN.matcher(command);

            String targetName = null;
            if (bentoMatcher.matches()) {
                targetName = bentoMatcher.group(2);
            } else if (superiorMatcher.matches()) {
                targetName = superiorMatcher.group(2);
            } else if (askyblockMatcher.matches()) {
                targetName = askyblockMatcher.group(2);
            }

            if (targetName != null) {
                // Lưu lại thông tin người nhận để theo dõi sau khi lệnh thực hiện
                final String finalTargetName = targetName;

                // Chờ 1 tick sau khi lệnh được thực hiện để cập nhật cache
                Bukkit.getScheduler().runTaskLater(me.sfclog.oregen4.Main.pl, () -> {
                    Player targetPlayer = Bukkit.getPlayer(finalTargetName);
                    if (targetPlayer != null) {
                        UUID oldOwnerUUID = sender.getUniqueId();
                        UUID newOwnerUUID = targetPlayer.getUniqueId();

                        // Cập nhật cache
                        LocationOwnerCache.transferIsland(oldOwnerUUID, newOwnerUUID);
                        clearPermissionCache(oldOwnerUUID, newOwnerUUID);

                        Bukkit.getLogger().info("§a[OreGen4] Phát hiện đảo được chuyển từ " +
                                sender.getName() + " sang " + targetPlayer.getName());
                    }
                }, 20L); // Chờ 1 second
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("§c[OreGen4] Lỗi khi theo dõi lệnh chuyển chủ: " + e.getMessage());
        }
    }

    /**
     * Xóa cache quyền cho cả chủ cũ và chủ mới
     */
    private void clearPermissionCache(UUID oldOwnerUUID, UUID newOwnerUUID) {
        try {
            // Xóa cache cho chủ cũ
            Player oldOwner = Bukkit.getPlayer(oldOwnerUUID);
            if (oldOwner != null) {
                me.sfclog.oregen4.util.PermissionCache.clearPlayerCache(oldOwner.getName());
                me.sfclog.oregen4.util.EnhancedPermissionCache.clearPlayerCache(oldOwnerUUID);
                me.sfclog.oregen4.updater.PermissionUpdater.clearPlayerInfo(oldOwner.getName());
            }

            // Xóa cache cho chủ mới
            Player newOwner = Bukkit.getPlayer(newOwnerUUID);
            if (newOwner != null) {
                me.sfclog.oregen4.util.PermissionCache.clearPlayerCache(newOwner.getName());
                me.sfclog.oregen4.util.EnhancedPermissionCache.clearPlayerCache(newOwnerUUID);
                me.sfclog.oregen4.updater.PermissionUpdater.clearPlayerInfo(newOwner.getName());
            }
        } catch (Exception e) {
            // Bỏ qua lỗi khi xóa cache
            Bukkit.getLogger().warning("§c[OreGen4] Lỗi khi xóa cache quyền: " + e.getMessage());
        }
    }
}
