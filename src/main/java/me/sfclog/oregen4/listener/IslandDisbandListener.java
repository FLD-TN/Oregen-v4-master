package me.sfclog.oregen4.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import me.sfclog.oregen4.Main;
import me.sfclog.oregen4.island.IslandOreManager;

/**
 * Listener xử lý sự kiện xóa đảo
 * Khi đảo bị xóa, cần xóa dữ liệu ore level của đảo đó
 */
public class IslandDisbandListener implements Listener {
    
    public IslandDisbandListener() {
        // Khởi tạo và in log phiên bản BentoBox nếu có
        Plugin bentoBox = Bukkit.getPluginManager().getPlugin("BentoBox");
        if (bentoBox != null) {
            Main.sendlog("§e[OreGen4] §aBentoBox được phát hiện, phiên bản: §b" + bentoBox.getDescription().getVersion());
            
            // Ghi log để kiểm tra các event class có tồn tại không
            try {
                Class.forName("world.bentobox.bentobox.api.events.island.IslandDeletedEvent");
                Main.sendlog("§e[OreGen4] §aEvent IslandDeletedEvent được tìm thấy");
            } catch (ClassNotFoundException e) {
                Main.sendlog("§e[OreGen4] §cEvent IslandDeletedEvent không tìm thấy");
            }
            
            try {
                Class.forName("world.bentobox.bentobox.api.events.island.IslandDeleteEvent");
                Main.sendlog("§e[OreGen4] §aEvent IslandDeleteEvent được tìm thấy");
            } catch (ClassNotFoundException e) {
                Main.sendlog("§e[OreGen4] §cEvent IslandDeleteEvent không tìm thấy");
            }
        }
    }

    /**
     * Xử lý sự kiện đảo bị xóa trong SuperiorSkyblock
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSuperiorIslandDelete(com.bgsoftware.superiorskyblock.api.events.IslandDisbandEvent event) {
        if (event.isCancelled()) {
            return;
        }

        String islandId = event.getIsland().getUniqueId().toString();
        
        // Xóa dữ liệu ore level của đảo
        IslandOreManager.removeIsland(islandId);
        
        if (Main.isDebugEnabled()) {
            Main.sendlog("§e[OreGen4] §aĐã xóa dữ liệu ore level của đảo SuperiorSkyblock ID: §b" + islandId);
        }
    }
}