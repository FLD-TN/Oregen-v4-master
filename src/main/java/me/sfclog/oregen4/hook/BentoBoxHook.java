package me.sfclog.oregen4.hook;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;

import me.sfclog.oregen4.Main;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;

public class BentoBoxHook {

    public IslandsManager manager;

    public BentoBoxHook() {
        manager = BentoBox.getInstance().getIslands();
    }

    public UUID getIslandOwner(final Location loc) {
        final Optional<Island> island = manager.getIslandAt(loc);
        return island.map(Island::getOwner).orElse(null);
    }

    /**
     * Lấy ID duy nhất của đảo từ vị trí
     * 
     * @param loc Vị trí cần kiểm tra
     * @return ID của đảo hoặc null nếu không tìm thấy
     */
    public String getIslandIdAt(final Location loc) {
        final Optional<Island> island = manager.getIslandAt(loc);
        if (island.isPresent()) {
            // Sử dụng uniqueId của đảo làm ID
            String islandId = island.get().getUniqueId();
            
            // Debug log
            if (Main.isDebugEnabled()) {
                Main.sendlog("§e[OreGen4] §bBentoBox: Đã tìm thấy đảo với ID §a" + islandId + 
                         "§b tại vị trí §a" + loc.getWorld().getName() + " - " + 
                         loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
            }
            
            return islandId;
        }
        
        if (Main.isDebugEnabled()) {
            Main.sendlog("§c[OreGen4] BentoBox: Không tìm thấy đảo tại vị trí " + 
                      loc.getWorld().getName() + " - " + 
                      loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        }
        return null;
    }

    /**
     * Lấy UUID của chủ đảo từ location (static method)
     * 
     * @param loc Vị trí cần kiểm tra
     * @return UUID của chủ đảo hoặc null nếu không tìm thấy
     */
    public static UUID getIslandOwnerUUID(final Location loc) {
        try {
            IslandsManager manager = BentoBox.getInstance().getIslands();
            final Optional<Island> island = manager.getIslandAt(loc);
            return island.map(Island::getOwner).orElse(null);
        } catch (Exception e) {
            // Xử lý lỗi nếu có
        }
        return null;
    }
}
