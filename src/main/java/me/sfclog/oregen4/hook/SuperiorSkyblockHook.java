package me.sfclog.oregen4.hook;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;

import me.sfclog.oregen4.Main;

public class SuperiorSkyblockHook {
    // Lưu trữ thời gian tính toán cuối cùng cho mỗi đảo để giảm số lần gọi
    // calcIslandWorth
    private static final Map<String, Long> lastCalcTimeMap = new HashMap<>();
    private static final long MIN_CALC_INTERVAL = 30000; // 30 giây giữa các lần tính toán

    public UUID getIslandOwner(Location location) {
        Island is = SuperiorSkyblockAPI.getGrid().getIslandAt(location);
        if (is != null && is.getOwner() != null) {
            return is.getOwner().getUniqueId();
        }
        return null;
    }

    public String getIslandID(Location location) {
        Island is = SuperiorSkyblockAPI.getGrid().getIslandAt(location);
        if (is != null) {
            // Sử dụng UUID của đảo làm ID
            String islandId = is.getUniqueId().toString();
            
            // Debug log
            if (Main.isDebugEnabled()) {
                Main.sendlog("§e[OreGen4] §bSuperiorSkyblock: Đã tìm thấy đảo với ID §a" + islandId + 
                         "§b tại vị trí §a" + location.getWorld().getName() + " - " + 
                         location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
            }
            
            return islandId;
        }
        
        if (Main.isDebugEnabled()) {
            Main.sendlog("§c[OreGen4] SuperiorSkyblock: Không tìm thấy đảo tại vị trí " + 
                      location.getWorld().getName() + " - " + 
                      location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
        }
        return null;
    }

    /**
     * Lấy UUID của chủ đảo từ location (static method)
     * 
     * @param location Vị trí cần kiểm tra
     * @return UUID của chủ đảo hoặc null nếu không tìm thấy
     */
    public static UUID getIslandOwnerUUID(Location location) {
        try {
            Island is = SuperiorSkyblockAPI.getGrid().getIslandAt(location);
            if (is != null && is.getOwner() != null) {
                return is.getOwner().getUniqueId();
            }
        } catch (Exception e) {
            // Xử lý lỗi nếu có
        }
        return null;
    }

    /**
     * Cập nhật block một cách an toàn trong SuperiorSkyblock2
     * Phương pháp mới nhất để đảm bảo level đảo không bị giảm
     * 
     * @param block    Block cần thay đổi
     * @param material Material mới
     */
    public void updateBlockSafely(Block block, Material material) {
        try {
            // Lưu thông tin block cũ trước khi thay đổi
            Location location = block.getLocation();
            Material oldMaterial = block.getType();

            // Đặt block ngay để người chơi thấy hiệu ứng
            block.setType(material);

            // Kiểm tra xem block có nằm trên đảo không
            Island island = SuperiorSkyblockAPI.getGrid().getIslandAt(location);
            if (island == null) {
                // Không nằm trên đảo, không cần xử lý thêm
                return;
            }

            // Nếu block cũ là chất lỏng, chỉ đặt block không cần xử lý giá trị đảo
            if (oldMaterial == Material.WATER || oldMaterial == Material.LAVA ||
                    oldMaterial.name().contains("WATER") || oldMaterial.name().contains("LAVA")) {
                return;
            }

            if (Main.isDebugEnabled()) {
                Main.sendlog("§a[OreGen4] Block trên đảo đã được thay đổi: " + oldMaterial + " -> " + material);
            }

            // Giới hạn tần suất tính toán lại giá trị đảo
            String islandId = island.getUniqueId().toString();
            long currentTime = System.currentTimeMillis();
            Long lastCalcTime = lastCalcTimeMap.get(islandId);

            // Chỉ tính toán lại nếu đã qua một khoảng thời gian
            if (lastCalcTime == null || (currentTime - lastCalcTime) > MIN_CALC_INTERVAL) {
                lastCalcTimeMap.put(islandId, currentTime);

                // Thông báo trong debug mode
                if (Main.isDebugEnabled()) {
                    Main.sendlog("§a[OreGen4] Đang tính toán lại giá trị đảo: " + islandId);
                }

                // PHƯƠNG PHÁP 1: Thử sử dụng reflection để thêm block vào đếm
                try {
                    // Thử sử dụng API không trực tiếp để tránh lỗi compilation
                    // Nếu có lỗi sẽ bỏ qua và thử phương pháp khác
                    try {
                        // Phương pháp này chỉ cho phiên bản mới nhất (dùng reflection để tránh lỗi
                        // compile)
                        Class<?> blockCountsClass = Class
                                .forName("com.bgsoftware.superiorskyblock.api.island.IslandBlockCounts");
                        java.lang.reflect.Method getBlockCountsMethod = island.getClass().getMethod("getBlockCounts");
                        Object blockCounts = getBlockCountsMethod.invoke(island);

                        java.lang.reflect.Method addBlockMethod = blockCountsClass.getMethod("addBlock", Material.class,
                                int.class);
                        addBlockMethod.invoke(blockCounts, material, 1);

                        if (Main.isDebugEnabled()) {
                            Main.sendlog(
                                    "§a[OreGen4] Đã thêm block " + material.name() + " vào đảo thông qua reflection");
                        }
                        return;
                    } catch (Exception e) {
                        // Bỏ qua và thử phương pháp khác
                        if (Main.isDebugEnabled()) {
                            Main.sendlog("§e[OreGen4] Không thể sử dụng reflection, thử phương pháp khác...");
                        }
                    }
                } catch (Throwable t) {
                    // Bỏ qua và thử phương pháp khác
                    if (Main.isDebugEnabled()) {
                        Main.sendlog("§e[OreGen4] Không thể sử dụng BlockCounts API, thử phương pháp khác...");
                    }
                }

                // PHƯƠNG PHÁP 2: Gọi trực tiếp tính toán đảo (cách đơn giản nhất)
                try {
                    // Phương thức này có trong hầu hết các phiên bản SuperiorSkyblock2
                    island.calcIslandWorth(null);

                    if (Main.isDebugEnabled()) {
                        Main.sendlog("§a[OreGen4] Đã tính toán lại giá trị đảo " + islandId);
                    }
                    return;
                } catch (Throwable t) {
                    // Bỏ qua và thử phương pháp khác
                    if (Main.isDebugEnabled()) {
                        Main.sendlog("§e[OreGen4] Không thể calcIslandWorth, thử phương pháp cuối cùng...");
                    }
                }

                // PHƯƠNG PHÁP 3: Thử một phương pháp phổ biến khác với reflection
                try {
                    // Thử sử dụng reflection để gọi API khác
                    try {
                        java.lang.reflect.Method handleBlockMethod = island.getClass().getMethod("handlePlacedBlock",
                                Material.class);
                        handleBlockMethod.invoke(island, material);

                        if (Main.isDebugEnabled()) {
                            Main.sendlog("§a[OreGen4] Đã xử lý block thông qua reflection handlePlacedBlock");
                        }
                        return;
                    } catch (Exception e) {
                        // Tiếp tục thử các phương pháp khác nếu không được
                        if (Main.isDebugEnabled()) {
                            Main.sendlog("§e[OreGen4] Phương pháp reflection handlePlacedBlock không thành công");
                        }
                    }

                    // Thử tìm và gọi API quản lý đảo
                    try {
                        Class<?> islandRegistryClass = Class
                                .forName("com.bgsoftware.superiorskyblock.api.handlers.IslandRegistry");
                        java.lang.reflect.Method getIslandRegistryMethod = SuperiorSkyblockAPI.class
                                .getMethod("getIslandRegistry");
                        Object islandRegistry = getIslandRegistryMethod.invoke(null);

                        java.lang.reflect.Method handleBlockPlaceMethod = islandRegistryClass
                                .getMethod("handleBlockPlace", Island.class, Block.class);
                        handleBlockPlaceMethod.invoke(islandRegistry, island, block);

                        if (Main.isDebugEnabled()) {
                            Main.sendlog("§a[OreGen4] Đã xử lý block thông qua reflection IslandRegistry");
                        }
                        return;
                    } catch (Exception e) {
                        if (Main.isDebugEnabled()) {
                            Main.sendlog("§e[OreGen4] Phương pháp reflection IslandRegistry không thành công");
                        }
                    }

                    // Nếu tất cả phương pháp reflection đều không thành công, thông báo
                    if (Main.isDebugEnabled()) {
                        Main.sendlog("§c[OreGen4] Đã thử tất cả phương pháp, nhưng không có API nào hoạt động");
                    }
                } catch (Throwable t) {
                    // Bỏ qua nếu phương pháp này cũng không hoạt động
                    if (Main.isDebugEnabled()) {
                        Main.sendlog("§c[OreGen4] Lỗi khi thử phương pháp phản chiếu: " + t.getMessage());
                    }
                }
            } else if (Main.isDebugEnabled()) {
                Main.sendlog("§e[OreGen4] Đã bỏ qua tính toán lại đảo do mới được tính " +
                        ((currentTime - lastCalcTime) / 1000) + " giây trước");
            }

        } catch (Exception e) {
            // Fallback an toàn nếu có lỗi
            if (Main.isDebugEnabled()) {
                Main.sendlog("§c[OreGen4] Lỗi khi cập nhật block an toàn: " + e.getMessage());
            }

            // Đảm bảo block vẫn được đặt
            block.setType(material);
        }
    }
}
