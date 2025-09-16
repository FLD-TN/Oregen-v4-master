package me.sfclog.oregen4.oregen;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;

import me.sfclog.oregen4.Main;
import me.sfclog.oregen4.config.ConfigManager;
import me.sfclog.oregen4.config.GeneratorSettings;
import me.sfclog.oregen4.config.OreLevel;
import me.sfclog.oregen4.island.IslandOreManager;

public class EventOreGen implements Listener {

    public static BlockFace[] FACES = new BlockFace[] {
            BlockFace.SELF,
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
    };

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onOre(final BlockFromToEvent event) {
        final Block source = event.getBlock();
        final Block to = event.getToBlock();
        final Material sourceMaterial = source.getType();
        final Material toMaterial = to.getType();

        if (sourceMaterial == Material.AIR)
            return;

        // Kiểm tra chế độ Lava + Water - đơn giản
        if (GeneratorSettings.isLavaWaterEnabled() && (isWater(sourceMaterial) || isLava(sourceMaterial))) {
            if ((toMaterial == Material.AIR || isWater(toMaterial))
                    && canGenerate(sourceMaterial, to)
                    && event.getFace() != BlockFace.DOWN) {

                if (isLava(sourceMaterial) && !isSurroundedByWater(to.getLocation())) {
                    // Không sinh nếu là dung nham không được bao quanh bởi nước
                    return;
                }

                event.setCancelled(true);
                generateOre(source.getType(), to);
                return;
            }
        }

        // Kiểm tra chế độ Fence + Water - đơn giản
        if (GeneratorSettings.isFenceWaterEnabled() && isWater(sourceMaterial)) {
            if (toMaterial == Material.AIR && event.getFace() != BlockFace.DOWN) {
                // Chỉ kiểm tra các khối liền kề đơn giản
                boolean fenceFound = false;

                // Chỉ kiểm tra các hướng trực tiếp
                for (BlockFace face : FACES) {
                    Block relativeBlock = to.getRelative(face);
                    Material blockMaterial = relativeBlock.getType();
                    String blockName = blockMaterial.name();

                    // Định nghĩa đơn giản của hàng rào
                    boolean isFence = blockName.contains("FENCE") ||
                            blockName.contains("WALL") ||
                            blockName.contains("GATE") ||
                            blockMaterial == Material.IRON_BARS ||
                            blockMaterial == Material.CHAIN;

                    if (isFence) {
                        fenceFound = true;
                        if (Main.isDebugEnabled()) {
                            if (Main.isDebugEnabled()) Main.sendlog("§a[OreGen4] §bFence+Water - Hàng rào được tìm thấy: §a" + blockMaterial);
                        }
                        break;
                    }
                }

                if (fenceFound) {
                    event.setCancelled(true);
                    generateOre(source.getType(), to);
                    return;
                }
            }
        }

        // Phương thức mặc định
        if (canGenerateBlock(source, to)) {
            event.setCancelled(true);
            generateOre(source.getType(), to);
            return;
        }
    }

    private void generateOre(Material sourceMaterial, Block block) {
        try {
            // Nếu là cobblestone generator thông thường
            if (generateCobble(sourceMaterial, block)) {
                block.setType(Material.COBBLESTONE);
                return;
            }

            World.Environment env = block.getWorld().getEnvironment();

            // Lấy ID của đảo
            String islandId = getIslandId(block.getLocation());
            
            // Log thêm để debug
            if (Main.isDebugEnabled()) {
                Main.sendlog("§e[OreGen4] §bĐang sinh ore tại: §a" + 
                        block.getWorld().getName() + " §b- §a" + 
                        block.getX() + "," + block.getY() + "," + block.getZ() + 
                        "§b, islandId: §a" + (islandId != null ? islandId : "null"));
            }
            
            // Nếu không có đảo -> cobblestone
            if (islandId == null) {
                if (Main.isDebugEnabled()) Main.sendlog("§e[OreGen4] §cKhông tìm thấy ID đảo, sử dụng COBBLESTONE");
                block.setType(Material.COBBLESTONE);
                return;
            }
            
            // Kiểm tra cấp độ ore của đảo từ IslandOreManager
            OreLevel islandLevel = IslandOreManager.getIslandOreLevel(islandId, env);
            
            // Log thêm để debug
            if (Main.isDebugEnabled()) {
                if (islandLevel != null) {
                    Main.sendlog("§e[OreGen4] §aTìm thấy cấp độ ore cho đảo §b" + islandId + 
                            "§a: §b" + islandLevel.getPermission());
                } else {
                    Main.sendlog("§c[OreGen4] §4Không tìm thấy cấp độ ore nào cho đảo: §b" + islandId);
                }
            }

            if (islandLevel != null) {
                Material oreMaterial = islandLevel.getRandomOre();
                if (Main.isDebugEnabled()) {
                    Main.sendlog("§e[OreGen4] §aSinh quặng theo cấp độ đảo §b" + islandId + 
                            "§a: §b" + islandLevel.getPermission() + 
                            "§a, quặng được chọn: §b" + oreMaterial.name());
                }

                // Sử dụng API của SuperiorSkyblock2 nếu có thể để cập nhật block
                if (Main.hooksuper != null) {
                    try {
                        // Gọi API để cập nhật block một cách an toàn
                        Main.hooksuper.updateBlockSafely(block, oreMaterial);
                    } catch (Exception e) {
                        // Log lỗi và fallback
                        Main.sendlog("§c[OreGen4] §4Lỗi khi sử dụng SuperiorSkyblock API: §c" + e.getMessage());
                        block.setType(oreMaterial);
                    }
                } else {
                    block.setType(oreMaterial);
                }
                return;
            }

            // Không tìm thấy cấp độ ore cho đảo, sử dụng cấp độ mặc định
            OreLevel defaultLevel = ConfigManager.getDefaultLevel(env);
            if (defaultLevel != null) {
                Material oreMaterial = defaultLevel.getRandomOre();
                if (Main.isDebugEnabled()) {
                    Main.sendlog("§e[OreGen4] §cKhông tìm thấy cấp độ cho đảo §b" + islandId + 
                                "§c, sử dụng cấp độ mặc định: §b" + defaultLevel.getPermission());
                }

                // Sử dụng API của SuperiorSkyblock2 nếu có thể để cập nhật block
                if (Main.hooksuper != null) {
                    try {
                        Main.hooksuper.updateBlockSafely(block, oreMaterial);
                    } catch (Exception e) {
                        Main.sendlog("§c[OreGen4] §4Lỗi khi sử dụng SuperiorSkyblock API: §c" + e.getMessage());
                        block.setType(oreMaterial);
                    }
                } else {
                    block.setType(oreMaterial);
                }
            } else {
                // Nếu không có level mặc định -> cobblestone
                if (Main.isDebugEnabled()) {
                    Main.sendlog("§e[OreGen4] §cKhông tìm thấy cấp độ mặc định, sử dụng COBBLESTONE");
                }
                block.setType(Material.COBBLESTONE);
            }
        } catch (Exception e) {
            // Xử lý ngoại lệ an toàn để không làm crash server
            Main.sendlog("§c[OreGen4] §4Lỗi khi sinh quặng: §c" + e.getMessage());
            
            // Fallback an toàn: sinh cobblestone
            block.setType(Material.COBBLESTONE);
        }
    }

    /**
     * Lấy Island ID từ vị trí
     * 
     * @param loc Vị trí cần lấy Island ID
     * @return Island ID hoặc null nếu không tìm thấy
     */
    private String getIslandId(Location loc) {
        String islandId = null;
        
        // Thử lấy từ BentoBox hook
        if (Main.hookbentobox != null) {
            islandId = Main.hookbentobox.getIslandIdAt(loc);
            if (islandId != null) {
                if (Main.isDebugEnabled()) {
                    Main.sendlog("§e[OreGen4] §aĐã tìm thấy Island ID BentoBox: §b" + islandId);
                }
                return islandId;
            } else if (Main.isDebugEnabled()) {
                Main.sendlog("§c[OreGen4] Không tìm thấy Island ID từ BentoBox tại vị trí: " 
                        + loc.getWorld().getName() + " - " 
                        + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
            }
        }
        
        // Thử lấy từ SuperiorSkyblock hook
        if (Main.hooksuper != null) {
            islandId = Main.hooksuper.getIslandID(loc);
            if (islandId != null) {
                if (Main.isDebugEnabled()) {
                    Main.sendlog("§e[OreGen4] §aĐã tìm thấy Island ID SuperiorSkyblock: §b" + islandId);
                }
                return islandId;
            } else if (Main.isDebugEnabled()) {
                Main.sendlog("§c[OreGen4] Không tìm thấy Island ID từ SuperiorSkyblock tại vị trí: " 
                        + loc.getWorld().getName() + " - " 
                        + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
            }
        }
        
        // Không tìm thấy Island ID
        if (Main.isDebugEnabled()) {
            Main.sendlog("§c[OreGen4] Không tìm thấy Island ID nào cho vị trí: " 
                    + loc.getWorld().getName() + " - " 
                    + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        }
        
        return null;
    }
    
    private boolean generateCobble(Material sourceMaterial, Block block) {
        // Nếu đảo không có cấp độ nâng cấp hoặc plugin không được enable -> cobblestone
        return !GeneratorSettings.isLavaWaterEnabled() && !GeneratorSettings.isFenceWaterEnabled();
    }

    private boolean canGenerateBlock(Block source, Block to) {
        if (to.getType() != Material.AIR)
            return false;

        Material sourceMaterial = source.getType();
        if (!isLava(sourceMaterial) && !isWater(sourceMaterial))
            return false;

        for (BlockFace face : FACES) {
            Block relativeBlock = to.getRelative(face);
            if (isWater(sourceMaterial) && isLava(relativeBlock.getType()))
                return true;
            if (isLava(sourceMaterial) && isWater(relativeBlock.getType()))
                return true;
        }
        return false;
    }

    private boolean isSurroundedByWater(Location loc) {
        Block block = loc.getBlock();
        for (BlockFace face : FACES) {
            Block relativeBlock = block.getRelative(face);
            if (isWater(relativeBlock.getType()))
                return true;
        }
        return false;
    }

    private boolean canGenerate(Material sourceMaterial, Block to) {
        if (isWater(sourceMaterial)) {
            for (BlockFace face : FACES) {
                if (isLava(to.getRelative(face).getType())) {
                    return true;
                }
            }
        } else if (isLava(sourceMaterial)) {
            for (BlockFace face : FACES) {
                if (isWater(to.getRelative(face).getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isWater(Material material) {
        return material == Material.WATER;
    }

    private boolean isLava(Material material) {
        return material == Material.LAVA;
    }
}