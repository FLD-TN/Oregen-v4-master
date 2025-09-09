package me.sfclog.oregen4.oregen;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.cryptomorin.xseries.XBlock;
import me.sfclog.oregen4.config.GeneratorSettings;
import me.sfclog.oregen4.Main;
import me.sfclog.oregen4.config.ConfigManager;
import me.sfclog.oregen4.config.OreLevel;
import me.sfclog.oregen4.updater.PermissionUpdater;

import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;

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
                            Main.sendlog("§a[OreGen4] Fence+Water - Hàng rào được tìm thấy: " + blockMaterial);
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

            // Sử dụng IslandPermissionCache để lấy cấp độ OreGen dựa trên đảo, không phải
            // từng người chơi
            OreLevel islandLevel = me.sfclog.oregen4.util.IslandPermissionCache.getIslandOreLevel(block.getLocation(),
                    env);

            if (islandLevel != null) {
                Material oreMaterial = islandLevel.getRandomOre();
                if (Main.isDebugEnabled()) {
                    Main.sendlog("§e[OreGen4] Sinh quặng theo cấp độ đảo: " + islandLevel.getPermission());
                }

                // Sử dụng API của SuperiorSkyblock2 nếu có thể để cập nhật block
                if (Main.hooksuper != null) {
                    try {
                        // Gọi API để cập nhật block một cách an toàn
                        Main.hooksuper.updateBlockSafely(block, oreMaterial);
                    } catch (Exception e) {
                        if (Main.isDebugEnabled()) {
                            Main.sendlog("§c[OreGen4] Lỗi khi sử dụng SuperiorSkyblock API: " + e.getMessage());
                        }
                        // Fallback nếu có lỗi
                        block.setType(oreMaterial);
                    }
                } else {
                    block.setType(oreMaterial);
                }
                return;
            }

            // Fallback - kiểm tra theo chủ đảo
            UUID ownerUUID = me.sfclog.oregen4.util.GetUUID.getOwner(block.getLocation());
            if (ownerUUID == null) {
                // Nếu không có chủ đảo -> sử dụng level mặc định
                OreLevel defaultLevel = ConfigManager.getDefaultLevel(env);
                if (defaultLevel != null) {
                    Material oreMaterial = defaultLevel.getRandomOre();
                    if (Main.isDebugEnabled()) {
                        Main.sendlog("§e[OreGen4] Không có chủ đảo, sử dụng cấp độ mặc định: "
                                + defaultLevel.getPermission());
                    }

                    // Sử dụng API của SuperiorSkyblock2 nếu có thể để cập nhật block
                    if (Main.hooksuper != null) {
                        try {
                            Main.hooksuper.updateBlockSafely(block, oreMaterial);
                        } catch (Exception e) {
                            if (Main.isDebugEnabled()) {
                                Main.sendlog("§c[OreGen4] Lỗi khi sử dụng SuperiorSkyblock API: " + e.getMessage());
                            }
                            block.setType(oreMaterial);
                        }
                    } else {
                        block.setType(oreMaterial);
                    }
                } else {
                    // Nếu không có level mặc định -> cobblestone
                    if (Main.isDebugEnabled()) {
                        Main.sendlog("§e[OreGen4] Không có chủ đảo và không có cấp độ mặc định, sử dụng COBBLESTONE");
                    }
                    block.setType(Material.COBBLESTONE);
                }
                return;
            }

            if (Main.isDebugEnabled()) {
                Main.sendlog("§e[OreGen4] Sinh quặng trong môi trường " + env.name() + " cho player UUID " + ownerUUID);
                String playerName = me.sfclog.oregen4.util.GetUUID.getName(ownerUUID);
                Main.sendlog("§e[OreGen4] Sinh quặng cho người chơi: " + playerName + " (UUID: " + ownerUUID + ")");
            }

            // Lấy Island ID cho cache
            String islandId = null;
            try {
                // Sử dụng getIslandId từ IslandPermissionCache để lấy island ID
                islandId = me.sfclog.oregen4.util.IslandPermissionCache.getIslandId(block.getLocation());
            } catch (Exception e) {
                if (Main.isDebugEnabled()) {
                    Main.sendlog("§c[OreGen4] Lỗi khi lấy Island ID: " + e.getMessage());
                }
            }

            // Sử dụng EnhancedPermissionCache - hệ thống cache mới
            OreLevel enhancedCachedLevel = me.sfclog.oregen4.util.EnhancedPermissionCache
                    .getOreLevelFromCache(ownerUUID, env);

            if (enhancedCachedLevel != null) {
                // Lấy quyền từ cache
                String cachedPermission = me.sfclog.oregen4.util.EnhancedPermissionCache
                        .getCachedPermissionLevel(ownerUUID, env);

                if (Main.isDebugEnabled()) {
                    Main.sendlog("§e[OreGen4] Trạng thái cache: Có");
                    Main.sendlog("§e[OreGen4] Quyền trong cache: " + cachedPermission);
                }

                Material oreMaterial = enhancedCachedLevel.getRandomOre();

                if (Main.isDebugEnabled()) {
                    Main.sendlog(
                            "§e[OreGen4] Sử dụng EnhancedPermissionCache để sinh quặng với quyền " + cachedPermission);

                    // Debug thông tin về các loại vật liệu hỗ trợ
                    if (Material.getMaterial("COPPER_ORE") != null) {
                        Main.sendlog("§e[OreGen4] Phiên bản Minecraft của bạn hỗ trợ COPPER_ORE");
                    }

                    // Debug về level được dùng
                    Main.sendlog("§e[OreGen4] Kiểm tra level " + cachedPermission + ":");
                    if (enhancedCachedLevel.containsOre(Material.getMaterial("COPPER_ORE"))) {
                        Main.sendlog("§e[OreGen4]  - Có COPPER_ORE: Có");
                    } else {
                        Main.sendlog("§e[OreGen4]  - Có COPPER_ORE: Không");
                    }
                }

                // Lưu vào IslandPermissionCache để sử dụng sau này cho các thành viên đảo
                // Điều này giúp khi một người khác trên đảo tạo ra quặng, sẽ không cần gọi
                // LuckPerms API nữa
                if (islandId != null) {
                    me.sfclog.oregen4.util.IslandPermissionCache.cacheIslandOreLevel(islandId, env, ownerUUID,
                            enhancedCachedLevel);
                    if (Main.isDebugEnabled()) {
                        Main.sendlog("§e[OreGen4] Đã cập nhật IslandPermissionCache cho đảo " + islandId);
                    }
                }

                block.setType(oreMaterial);
                return;
            } else {
                if (Main.isDebugEnabled()) {
                    Main.sendlog("§e[OreGen4] Trạng thái cache: Không");
                }
            }

            // Kiểm tra cache trong PermissionUpdater (hệ thống cache cũ)
            String permUpdaterPermission = PermissionUpdater.getCurrentPermission(ownerUUID, env);
            if (permUpdaterPermission != null) {
                if (Main.isDebugEnabled()) {
                    Main.sendlog("§e[OreGen4] Tìm thấy quyền trong PermissionUpdater: " + permUpdaterPermission);
                }

                OreLevel cachedLevel = ConfigManager.getLevel(env, permUpdaterPermission);
                if (cachedLevel != null) {
                    Material oreMaterial = cachedLevel.getRandomOre();

                    if (Main.isDebugEnabled()) {
                        Main.sendlog("§e[OreGen4] Sử dụng PermissionUpdater cache để sinh quặng với quyền "
                                + permUpdaterPermission);
                    }

                    // Lấy Island ID cho cache
                    try {
                        String locationIslandId = me.sfclog.oregen4.util.IslandPermissionCache
                                .getIslandId(block.getLocation());
                        // Lưu vào IslandPermissionCache để sử dụng sau này cho các thành viên đảo
                        if (locationIslandId != null) {
                            me.sfclog.oregen4.util.IslandPermissionCache.cacheIslandOreLevel(locationIslandId, env,
                                    ownerUUID, cachedLevel);
                            if (Main.isDebugEnabled()) {
                                Main.sendlog("§e[OreGen4] Đã cập nhật IslandPermissionCache cho đảo " + locationIslandId
                                        + " từ PermissionUpdater");
                            }
                        }
                    } catch (Exception e) {
                        if (Main.isDebugEnabled()) {
                            Main.sendlog("§c[OreGen4] Lỗi khi lấy hoặc cập nhật Island ID: " + e.getMessage());
                        }
                    }

                    // Sử dụng API của SuperiorSkyblock2 nếu có thể để cập nhật block
                    if (Main.hooksuper != null) {
                        try {
                            Main.hooksuper.updateBlockSafely(block, oreMaterial);
                        } catch (Exception e) {
                            if (Main.isDebugEnabled()) {
                                Main.sendlog("§c[OreGen4] Lỗi khi sử dụng SuperiorSkyblock API: " + e.getMessage());
                            }
                            block.setType(oreMaterial);
                        }
                    } else {
                        block.setType(oreMaterial);
                    }

                    // Cập nhật vào cache mới cho lần sau
                    me.sfclog.oregen4.util.EnhancedPermissionCache.cachePermission(ownerUUID, env,
                            permUpdaterPermission, cachedLevel);

                    return;
                } else if (Main.isDebugEnabled()) {
                    Main.sendlog("§e[OreGen4] Không tìm thấy level cho quyền " + permUpdaterPermission);
                }
            } else if (Main.isDebugEnabled()) {
                Main.sendlog("§e[OreGen4] Không tìm thấy quyền trong PermissionUpdater");
            }

            // Sử dụng LuckPerms qua plugin chính
            LuckPerms luckPermsApi = null;
            try {
                Plugin mainPlugin = Bukkit.getPluginManager().getPlugin("OreGen4");
                if (mainPlugin instanceof Main) {
                    Main mainClass = (Main) mainPlugin;
                    luckPermsApi = mainClass.getLuckPermsApi();
                }
            } catch (Exception ignored) {
                // Ignore errors if we can't get the LuckPerms API
            }

            if (luckPermsApi != null) {
                // Lấy level mặc định trước khi bắt đầu async
                final OreLevel defaultLevel = ConfigManager.getDefaultLevel(env);

                // Đặt block trước với mặc định để người chơi không phải đợi
                if (defaultLevel != null) {
                    // Sử dụng API của SuperiorSkyblock2 nếu có thể để cập nhật block
                    if (Main.hooksuper != null) {
                        try {
                            Main.hooksuper.updateBlockSafely(block, defaultLevel.getRandomOre());
                        } catch (Exception e) {
                            if (Main.isDebugEnabled()) {
                                Main.sendlog("§c[OreGen4] Lỗi khi sử dụng SuperiorSkyblock API: " + e.getMessage());
                            }
                            block.setType(defaultLevel.getRandomOre());
                        }
                    } else {
                        block.setType(defaultLevel.getRandomOre());
                    }
                } else {
                    // Sử dụng API của SuperiorSkyblock2 nếu có thể để cập nhật block cobblestone
                    if (Main.hooksuper != null) {
                        try {
                            Main.hooksuper.updateBlockSafely(block, Material.COBBLESTONE);
                        } catch (Exception e) {
                            if (Main.isDebugEnabled()) {
                                Main.sendlog("§c[OreGen4] Lỗi khi sử dụng SuperiorSkyblock API: " + e.getMessage());
                            }
                            block.setType(Material.COBBLESTONE);
                        }
                    } else {
                        block.setType(Material.COBBLESTONE);
                    }
                }

                // Lưu LuckPerms reference cho lambda
                final LuckPerms finalLuckPermsApi = luckPermsApi;

                // Thực hiện async để không block main thread
                CompletableFuture.runAsync(() -> {
                    net.luckperms.api.model.user.User user = null;
                    OreLevel selectedLevel = null;

                    try {
                        // Tải user từ LuckPerms với timeout
                        user = finalLuckPermsApi.getUserManager().loadUser(ownerUUID).get(5, TimeUnit.SECONDS);

                        if (user != null) {
                            // Kiểm tra permissions từ cao đến thấp - cập nhật để phù hợp với cấu hình mới
                            String[] levels = { "cap7", "cap6", "cap5", "cap4", "cap3", "cap2", "cap1", "vip", "level3",
                                    "level2", "level1" };

                            // Debug log để giúp theo dõi quá trình kiểm tra
                            if (Main.isDebugEnabled()) {
                                Main.sendlog("§e[OreGen4] -- Môi trường: " + env.name());
                            }

                            for (String level : levels) {
                                String permission = "oregen." + level;
                                try {
                                    Main.sendlog("§e[OreGen4]  - Kiểm tra quyền: " + permission);
                                    boolean hasPerm = user.getCachedData().getPermissionData()
                                            .checkPermission(permission).asBoolean();

                                    Main.sendlog("§e[OreGen4]  - Kết quả: " + (hasPerm ? "Có" : "Không"));

                                    if (hasPerm) {
                                        selectedLevel = ConfigManager.getLevel(env, permission);
                                        if (selectedLevel != null) {
                                            // Đăng ký với cả hai hệ thống cache
                                            PermissionUpdater.registerPermission(ownerUUID, env, permission);
                                            me.sfclog.oregen4.util.EnhancedPermissionCache.cachePermission(ownerUUID,
                                                    env, permission, selectedLevel, true);

                                            // Lưu vào IslandPermissionCache luôn để các thành viên khác trên đảo sử
                                            // dụng
                                            try {
                                                String locationIslandId = me.sfclog.oregen4.util.IslandPermissionCache
                                                        .getIslandId(block.getLocation());
                                                if (locationIslandId != null) {
                                                    me.sfclog.oregen4.util.IslandPermissionCache.cacheIslandOreLevel(
                                                            locationIslandId, env, ownerUUID, selectedLevel);
                                                    if (Main.isDebugEnabled()) {
                                                        Main.sendlog(
                                                                "§e[OreGen4] Đã cập nhật IslandPermissionCache cho đảo "
                                                                        + locationIslandId + " từ LuckPerms check");
                                                    }
                                                }
                                            } catch (Exception e) {
                                                if (Main.isDebugEnabled()) {
                                                    Main.sendlog(
                                                            "§c[OreGen4] Lỗi khi cập nhật IslandPermissionCache từ LuckPerms: "
                                                                    + e.getMessage());
                                                }
                                            }

                                            Main.sendlog("§e[OreGen4] Preloaded permission for " + user.getUsername() +
                                                    " (" + ownerUUID + "): " + permission + " in " + env.name());

                                            break;
                                        }
                                    }
                                } catch (Exception e) {
                                    // Ignore permission check errors
                                    Main.sendlog("§c[OreGen4] Lỗi kiểm tra quyền: " + e.getMessage());
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Ignore user loading errors
                        Main.sendlog("§c[OreGen4] Lỗi tải user: " + e.getMessage());
                    }

                    // Fall back to default level nếu không tìm thấy level phù hợp
                    if (selectedLevel == null) {
                        selectedLevel = defaultLevel;
                        if (selectedLevel != null) {
                            // Đăng ký default permission với cả hai hệ thống cache
                            PermissionUpdater.registerPermission(ownerUUID, env, selectedLevel.getPermission());
                            me.sfclog.oregen4.util.EnhancedPermissionCache.cachePermission(ownerUUID, env,
                                    selectedLevel.getPermission(), selectedLevel, false);
                            Main.sendlog("§e[OreGen4] §6Async: Không tìm thấy quyền, sử dụng mức mặc định cho UUID §a" +
                                    ownerUUID + "§6 trong môi trường §a" + env.name());
                        }
                    }

                    // Đặt block trong main thread chỉ khi level khác với default level
                    final OreLevel finalLevel = selectedLevel;

                    if (finalLevel != null && (defaultLevel == null || !finalLevel.equals(defaultLevel))) {
                        Plugin plugin = Bukkit.getPluginManager().getPlugin("OreGen4");
                        if (plugin != null) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                // Kiểm tra block vẫn còn ở trạng thái mặc định
                                if (block.getType() == Material.COBBLESTONE ||
                                        (defaultLevel != null && defaultLevel.containsOre(block.getType()))) {

                                    // Sử dụng API của SuperiorSkyblock2 nếu có thể để cập nhật block
                                    if (Main.hooksuper != null) {
                                        try {
                                            Main.hooksuper.updateBlockSafely(block, finalLevel.getRandomOre());
                                        } catch (Exception e) {
                                            if (Main.isDebugEnabled()) {
                                                Main.sendlog("§c[OreGen4] Lỗi khi sử dụng SuperiorSkyblock API: "
                                                        + e.getMessage());
                                            }
                                            block.setType(finalLevel.getRandomOre());
                                        }
                                    } else {
                                        block.setType(finalLevel.getRandomOre());
                                    }
                                }
                            });
                        }
                    }
                });
                return;
            }

            // Nếu không có LuckPerms hoặc có lỗi, dùng default
            OreLevel defaultLevel = ConfigManager.getDefaultLevel(env);
            if (defaultLevel != null) {
                // Sử dụng API của SuperiorSkyblock2 nếu có thể để cập nhật block
                if (Main.hooksuper != null) {
                    try {
                        Main.hooksuper.updateBlockSafely(block, defaultLevel.getRandomOre());
                    } catch (Exception e) {
                        if (Main.isDebugEnabled()) {
                            Main.sendlog("§c[OreGen4] Lỗi khi sử dụng SuperiorSkyblock API: " + e.getMessage());
                        }
                        block.setType(defaultLevel.getRandomOre());
                    }
                } else {
                    block.setType(defaultLevel.getRandomOre());
                }
            } else {
                // Sử dụng API của SuperiorSkyblock2 nếu có thể để cập nhật block cobblestone
                if (Main.hooksuper != null) {
                    try {
                        Main.hooksuper.updateBlockSafely(block, Material.COBBLESTONE);
                    } catch (Exception e) {
                        if (Main.isDebugEnabled()) {
                            Main.sendlog("§c[OreGen4] Lỗi khi sử dụng SuperiorSkyblock API: " + e.getMessage());
                        }
                        block.setType(Material.COBBLESTONE);
                    }
                } else {
                    block.setType(Material.COBBLESTONE);
                }
            }
        } catch (Exception e) {
            Main.sendlog("§c[OreGen4] Error generating ore: " + e.getMessage());

            // Sử dụng API của SuperiorSkyblock2 nếu có thể để cập nhật block cobblestone
            if (Main.hooksuper != null) {
                try {
                    Main.hooksuper.updateBlockSafely(block, Material.COBBLESTONE);
                } catch (Exception ex) {
                    block.setType(Material.COBBLESTONE);
                }
            } else {
                block.setType(Material.COBBLESTONE);
            }
        }
    }

    private boolean generateCobble(Material material, Block b) {
        Material mirMat = material == Material.WATER ? Material.LAVA : Material.WATER;

        for (BlockFace face : FACES) {
            Block check = b.getRelative(face, 1);
            if (check.getType() == mirMat) {
                return true;
            }
        }
        return false;
    }

    private boolean canGenerateBlock(final Block src, final Block to) {
        final Material material = src.getType();
        for (final BlockFace face : FACES) {
            final Block check = to.getRelative(face);
            if (isBlock(check)
                    && (isWater(material))) {
                return true;
            } else if (isBlock(check)
                    && (isLava(material))) {
                return true;
            }
        }
        return false;
    }

    /*
     * Checks for Water + Lava, block will use another method to prevent confusion
     */
    private boolean canGenerate(final Material material, final Block b) {
        final boolean check = isWater(material);
        for (final BlockFace face : FACES) {
            final Material type = b.getRelative(face).getType();
            if (((check && isLava(type)) || (!check && isWater(type)))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Phương thức này đã không còn được sử dụng trong phiên bản tối giản hóa
     * Giữ comment để tham khảo trong tương lai nếu cần
     * 
     * @deprecated Đã được thay thế bằng kiểm tra đơn giản hơn trực tiếp trong
     *             onOre()
     */
    /*
     * Phương thức kiểm tra hàng rào đã bị loại bỏ để tối ưu hóa hiệu suất
     * private boolean checkForFenceInChunk(Block block) {
     * // Lấy chunk của block
     * org.bukkit.Chunk chunk = block.getChunk();
     * int chunkX = chunk.getX();
     * int chunkZ = chunk.getZ();
     * 
     * // Tạo cache key dựa trên vị trí chunk
     * String cacheKey = "chunk_fence_check_" + chunkX + "_" + chunkZ;
     * 
     * // Kiểm tra cache trước
     * Boolean cachedResult = me.sfclog.oregen4.util.ChunkCache.get(cacheKey);
     * if (cachedResult != null) {
     * return cachedResult;
     * }
     * 
     * // Kiểm tra tất cả các block trong chunk
     * World world = block.getWorld();
     * int minX = chunkX << 4;
     * int minZ = chunkZ << 4;
     * int minY = Math.max(0, block.getY() - 5);
     * int maxY = Math.min(world.getMaxHeight(), block.getY() + 5);
     * 
     * for (int x = 0; x < 16; x++) {
     * for (int z = 0; z < 16; z++) {
     * for (int y = minY; y <= maxY; y++) {
     * Block checkBlock = world.getBlockAt(minX + x, y, minZ + z);
     * if (isFence(checkBlock.getType())) {
     * // Lưu kết quả vào cache
     * me.sfclog.oregen4.util.ChunkCache.put(cacheKey, true, 30000); // 30 giây
     * return true;
     * }
     * }
     * }
     * }
     * 
     * // Lưu kết quả vào cache
     * me.sfclog.oregen4.util.ChunkCache.put(cacheKey, false, 30000); // 30 giây
     * return false;
     * }
     */

    public static boolean isFence(final Material type) {
        if (type == null)
            return false;

        String name = type.name();
        boolean result = name.contains("FENCE") ||
                name.contains("WALL") ||
                name.contains("GATE") ||
                type == Material.IRON_BARS ||
                type == Material.CHAIN;

        if (Main.isDebugEnabled() && result) {
            Main.sendlog("§a[OreGen4] Fence check - Detected fence material: " + type.name());
        }

        return result;
    }

    public static boolean isBlock(final Block b) {
        if (b == null)
            return false;
        return isFence(b.getType());
    }

    public static boolean isWater(final Material material) {
        try {
            return XBlock.isWater(material);
        } catch (Exception e) {
            return material == Material.WATER;
        }
    }

    public static boolean isLava(final Material material) {
        try {
            return XBlock.isLava(material);
        } catch (Exception e) {
            return material == Material.LAVA;
        }
    }

    public static boolean isSurroundedByWater(final Location loc) {
        if (loc == null || loc.getWorld() == null)
            return false;

        final World world = loc.getWorld();
        final int x = loc.getBlockX();
        final int y = loc.getBlockY();
        final int z = loc.getBlockZ();

        Block[] surroundingBlocks = {
                world.getBlockAt(x + 1, y, z),
                world.getBlockAt(x - 1, y, z),
                world.getBlockAt(x, y, z + 1),
                world.getBlockAt(x, y, z - 1)
        };

        for (Block block : surroundingBlocks) {
            if (isWater(block.getType())) {
                return true;
            }
        }
        return false;
    }

}
