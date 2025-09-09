package me.sfclog.oregen4.hook;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;

public class AdvancedEnchantmentsHook {
    private static boolean enabled = false;
    private static Plugin plugin = null;

    public static void init() {
        plugin = Bukkit.getPluginManager().getPlugin("AdvancedEnchantments");
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
     * Thêm enchant cho ore dựa trên level của người chơi
     * 
     * @param block  Block ore được tạo ra
     * @param player Người chơi
     * @param level  Level của người chơi
     */
    public static void addOreEnchants(Block block, Player player, String level) {
        if (!enabled || block == null || player == null)
            return;

        ItemStack item = new ItemStack(block.getType());

        // Tỷ lệ enchant dựa theo level
        double chance;
        switch (level) {
            case "vip":
                chance = 0.3; // 30% cho VIP
                break;
            case "level3":
                chance = 0.2; // 20% cho level 3
                break;
            case "level2":
                chance = 0.1; // 10% cho level 2
                break;
            default:
                chance = 0.05; // 5% cho level còn lại
                break;
        }

        if (Math.random() <= chance) {
            // Thêm enchant đặc biệt cho ore
            try {
                // Gọi API của AdvancedEnchantments để thêm enchant
                // Ví dụ: addCustomEnchant(item, "Fortune", level)
                block.setType(item.getType());
            } catch (Exception e) {
                Bukkit.getLogger().warning("Failed to add enchant to ore: " + e.getMessage());
            }
        }
    }
}
