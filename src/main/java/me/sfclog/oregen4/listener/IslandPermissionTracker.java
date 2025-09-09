package me.sfclog.oregen4.listener;

import me.sfclog.oregen4.Main;
import me.sfclog.oregen4.util.IslandPermissionCache;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

/**
 * Theo dõi thay đổi quyền và đồng bộ với hệ thống cache đảo
 */
public class IslandPermissionTracker implements Listener {
    private final LuckPerms luckPermsApi;
    private EventSubscription<UserDataRecalculateEvent> subscription;

    public IslandPermissionTracker(LuckPerms luckPermsApi) {
        this.luckPermsApi = luckPermsApi;
        registerLuckPermsEvents();
    }

    /**
     * Đăng ký lắng nghe sự kiện thay đổi quyền từ LuckPerms
     */
    private void registerLuckPermsEvents() {
        if (luckPermsApi != null) {
            try {
                subscription = luckPermsApi.getEventBus().subscribe(UserDataRecalculateEvent.class,
                        this::onLuckPermsUpdate);
                Main.sendlog("§a[OreGen4] Đã đăng ký IslandPermissionTracker với LuckPerms API");
            } catch (Exception e) {
                Main.sendlog("§c[OreGen4] Lỗi khi đăng ký IslandPermissionTracker: " + e.getMessage());
            }
        }
    }

    /**
     * Xử lý khi có thay đổi quyền từ LuckPerms
     */
    private void onLuckPermsUpdate(UserDataRecalculateEvent event) {
        UUID playerUUID = event.getUser().getUniqueId();

        // Kiểm tra xem người chơi này có phải là chủ đảo không
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            checkIslandOwnerAndInvalidateCache(player);
        }
    }

    /**
     * Kiểm tra xem người chơi có phải là chủ đảo không và xóa cache nếu có
     */
    private void checkIslandOwnerAndInvalidateCache(Player player) {
        Location playerLoc = player.getLocation();
        UUID ownerUUID = me.sfclog.oregen4.util.GetUUID.getOwner(playerLoc);

        // Nếu người chơi này là chủ đảo, xóa cache đảo
        if (ownerUUID != null && ownerUUID.equals(player.getUniqueId())) {
            IslandPermissionCache.invalidateIslandCache(playerLoc);

            if (Main.getInstance().getConfig().getBoolean("debug", false)) {
                Main.sendlog("§e[OreGen4] Đã xóa cache đảo vì quyền của chủ đảo " + player.getName() + " đã thay đổi");
            }
        }
    }

    /**
     * Theo dõi khi người chơi tham gia để kiểm tra chủ đảo
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Chạy bất đồng bộ để không làm giảm hiệu suất khi người chơi tham gia
        Bukkit.getScheduler().runTaskLaterAsynchronously(Main.pl, () -> {
            if (event.getPlayer().isOnline()) {
                checkIslandOwnerAndInvalidateCache(event.getPlayer());
            }
        }, 40L); // Delay 2 giây
    }
}
