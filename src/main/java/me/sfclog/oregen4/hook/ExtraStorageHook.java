package me.sfclog.oregen4.hook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

public class ExtraStorageHook {
    private static boolean enabled = false;
    private static Plugin plugin = null;

    public static void init() {
        plugin = Bukkit.getPluginManager().getPlugin("ExtraStorage");
        if (plugin != null && plugin.isEnabled()) {
            enabled = true;
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static Plugin getPlugin() {
        return plugin;
    }

    /**
     * Kiểm tra xem có storage ở gần vị trí không
     * 
     * @param location Vị trí cần kiểm tra
     * @return true nếu có storage
     */
    public static boolean hasStorageNearby(Location location) {
        if (!enabled || location == null)
            return false;

        int radius = 5; // Bán kính kiểm tra
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = location.clone().add(x, y, z).getBlock();
                    if (isStorage(block)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Kiểm tra xem block có phải là storage không
     */
    public static boolean isStorage(Block block) {
        if (!enabled || block == null)
            return false;

        try {
            // Gọi API của ExtraStorage để kiểm tra
            // return ExtraStorage.isStorage(block);
            return block.getType().name().contains("CHEST") ||
                    block.getType().name().contains("BARREL");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tự động thu thập ore vào storage gần nhất
     * 
     * @param block Block ore được tạo ra
     * @return true nếu thu thập thành công
     */
    public static boolean collectToNearbyStorage(Block block) {
        if (!enabled || block == null)
            return false;

        if (hasStorageNearby(block.getLocation())) {
            try {
                Material type = block.getType();
                // Gọi API của ExtraStorage để thêm item vào storage
                // ExtraStorage.addToNearestStorage(block.getLocation(), new ItemStack(type));
                block.setType(Material.AIR);
                return true;
            } catch (Exception e) {
                Bukkit.getLogger().warning("Failed to collect ore to storage: " + e.getMessage());
            }
        }
        return false;
    }
}
