package me.sfclog.oregen4.hook;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class AdvancedEnchantmentHook {
    private static boolean enabled = false;
    private static Plugin plugin = null;

    public static void init() {
        plugin = Bukkit.getPluginManager().getPlugin("AdvancedEnchantment");
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
}
