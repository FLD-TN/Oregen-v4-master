package me.sfclog.oregen4.listener;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import me.sfclog.oregen4.Main;
import me.sfclog.oregen4.config.ConfigManager;
import me.sfclog.oregen4.config.OreLevel;
import me.sfclog.oregen4.util.EnhancedPermissionCache;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;

/**
 * Listener để xử lý việc tải và cache quyền người chơi khi tham gia server
 */
public class PlayerPermissionLoader implements Listener {

    private final LuckPerms luckPermsApi;

    public PlayerPermissionLoader(LuckPerms luckPermsApi) {
        this.luckPermsApi = luckPermsApi;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Lên lịch tải quyền của người chơi sau 1 tick để đảm bảo tất cả plugin đã sẵn
        // sàng
        new BukkitRunnable() {
            @Override
            public void run() {
                preloadPlayerPermissions(event.getPlayer().getUniqueId());
            }
        }.runTaskLater(Main.pl, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Người chơi logout sau một thời gian có thể xóa cache để tiết kiệm bộ nhớ
        // (Tùy chọn: bạn có thể giữ cache nếu muốn người chơi quay lại nhanh chóng)
        // Tại đây chỉ giảm TTL (time-to-live) của cache thay vì xóa hoàn toàn
        EnhancedPermissionCache.reduceTTL(event.getPlayer().getUniqueId());
    }

    /**
     * Tải trước quyền của người chơi vào cache
     * 
     * @param uuid UUID của người chơi
     */
    public void preloadPlayerPermissions(UUID uuid) {
        // Đảm bảo chúng ta có LuckPerms API
        if (luckPermsApi == null) {
            Main.sendlog("§c[OreGen4] Không thể preload quyền: LuckPerms API không khả dụng");
            return;
        }

        // Tải người dùng từ LuckPerms theo cách async
        CompletableFuture<User> userFuture = luckPermsApi.getUserManager().loadUser(uuid);

        userFuture.thenAcceptAsync(user -> {
            if (user == null) {
                Main.sendlog("§c[OreGen4] Không thể preload quyền: User không tồn tại trong LuckPerms");
                return;
            }

            String playerName = user.getUsername();
            
            // Chỉ hiển thị log debug khi thực sự cần thiết
            if (Main.isDebugEnabled()) {
                Main.sendlog("§e[OreGen4] §2Đang preload quyền cho người chơi: §a" + playerName);
            }

            // Tải quyền cho tất cả các môi trường
            for (World.Environment env : World.Environment.values()) {
                try {
                    // Bỏ qua môi trường CUSTOM nếu không hỗ trợ
                    if (env.name().equals("CUSTOM")) {
                        try {
                            World.Environment.valueOf("CUSTOM");
                        } catch (IllegalArgumentException e) {
                            // Bỏ qua môi trường này nếu không được hỗ trợ
                            continue;
                        }
                    }

                    if (Main.isDebugEnabled()) {
                        Main.sendlog("§e[OreGen4] §2-- Môi trường: §a" + env.name());
                    }

                    // Kiểm tra quyền từ cao đến thấp - cập nhật để phù hợp với cấu hình mới
                    String[] levels = { "cap7", "cap6", "cap5", "cap4", "cap3", "cap2", "cap1", "vip", "level3",
                            "level2", "level1" };

                    // Tìm quyền cao nhất mà người chơi có
                    boolean foundPermission = false;
                    for (String level : levels) {
                        String permission = "oregen." + level;
                        try {
                            boolean hasPermission = user.getCachedData().getPermissionData().checkPermission(permission)
                                    .asBoolean();
                            
                            // Chỉ log khi debug mode được bật
                            if (Main.isDebugEnabled()) {
                                Main.sendlog("§e[OreGen4]  - Kiểm tra quyền: " + permission);
                                Main.sendlog("§e[OreGen4]  - Kết quả: " + (hasPermission ? "Có" : "Không"));
                            }

                            if (hasPermission) {
                                OreLevel oreLevel = ConfigManager.getLevel(env, permission);

                                if (oreLevel != null) {
                                    // Cache level ore với ưu tiên cao (TTL dài hơn)
                                    EnhancedPermissionCache.cachePermission(uuid, env, permission, oreLevel, true);

                                    // Chỉ log khi debug mode được bật
                                    if (Main.isDebugEnabled()) {
                                        Main.sendlog("§a[OreGen4] Preloaded permission for " + user.getUsername() +
                                            " (" + uuid + "): " + permission + " in " + env);
                                    }

                                    // Đã tìm thấy quyền cao nhất, không cần kiểm tra tiếp
                                    foundPermission = true;
                                    break;
                                } else {
                                    // Chỉ log lỗi khi debug mode được bật
                                    if (Main.isDebugEnabled()) {
                                        Main.sendlog(
                                            "§c[OreGen4] Không tìm thấy cấu hình OreLevel cho quyền: " + permission);
                                    }
                                    
                                    // Thử kiểm tra lại với key rút gọn
                                    String shortKey = level;
                                    oreLevel = ConfigManager.getLevel(env, shortKey);
                                    if (oreLevel != null) {
                                        // Cache level ore với ưu tiên cao (TTL dài hơn)
                                        EnhancedPermissionCache.cachePermission(uuid, env, permission, oreLevel, true);

                                        // Chỉ log khi debug mode được bật
                                        if (Main.isDebugEnabled()) {
                                            Main.sendlog("§a[OreGen4] §2Đã cache thành công permission §a" + permission +
                                                "§2 (key=§a" + shortKey + "§2) cho người chơi §a" + playerName +
                                                "§2 trong môi trường §a" + env.name());
                                        }

                                        // Đã tìm thấy quyền cao nhất, không cần kiểm tra tiếp
                                        foundPermission = true;
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Main.sendlog("§c[OreGen4] Error checking permission during preload: " + e.getMessage());
                        }
                    }

                    if (!foundPermission) {
                        // Chỉ log khi debug mode được bật
                        if (Main.isDebugEnabled()) {
                            Main.sendlog("§e[OreGen4] §cKhông tìm thấy quyền OreGen nào cho người chơi §a" + playerName +
                                "§c trong môi trường §a" + env.name() + "§c - sẽ sử dụng mức mặc định");
                        }

                        // Cache mức mặc định
                        OreLevel defaultLevel = ConfigManager.getDefaultLevel(env);
                        if (defaultLevel != null) {
                            EnhancedPermissionCache.cachePermission(uuid, env, "oregen.default", defaultLevel, false);
                            
                            // Chỉ log khi debug mode được bật
                            if (Main.isDebugEnabled()) {
                                Main.sendlog("§e[OreGen4] §aĐã cache mức mặc định cho người chơi §6" + playerName);
                            }
                        }
                    }
                } catch (Exception e) {
                    Main.sendlog("§c[OreGen4] Error during environment processing: " + e.getMessage());
                }
            }
        });
    }
}
