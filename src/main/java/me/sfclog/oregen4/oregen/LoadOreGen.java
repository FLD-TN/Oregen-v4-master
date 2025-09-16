package me.sfclog.oregen4.oregen;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import me.sfclog.oregen4.Main;
import me.sfclog.oregen4.config.ConfigManager;
import me.sfclog.oregen4.config.OreLevel;
import me.sfclog.oregen4.island.IslandOreManager;

public class LoadOreGen {
	/**
	 * Phương thức sinh block với hỗ trợ cache tối ưu
	 * Sử dụng EnhancedPermissionCache và cache preloading để giảm tải LuckPerms
	 * 
	 * @param to Block cần đặt
	 */
	public static void genblock(Block to) {
		try {
			World.Environment env = to.getWorld().getEnvironment();
			
			// Lấy ID của đảo từ vị trí block
			String islandId = null;
			if (Main.hookbentobox != null) {
				islandId = Main.hookbentobox.getIslandIdAt(to.getLocation());
			} else if (Main.hooksuper != null) {
				islandId = Main.hooksuper.getIslandID(to.getLocation());
			}

			// Debug logs để kiểm tra
			if (Main.isDebugEnabled()) Main.sendlog("§e[OreGen4] §bSinh quặng trong môi trường §a" + env.name() + 
				"§b cho đảo ID §a" + (islandId != null ? islandId : "null"));

			if (islandId == null) {
				// Nếu không có đảo -> cobblestone
				if (Main.isDebugEnabled()) Main.sendlog("§e[OreGen4] §cKhông tìm thấy ID đảo cho vị trí này, đặt COBBLESTONE");
				to.setType(Material.COBBLESTONE);
				return;
			}

			// Lấy cấp độ ore từ IslandOreManager
			OreLevel islandLevel = IslandOreManager.getIslandOreLevel(islandId, env);
			
			if (islandLevel != null) {
				Material oreMaterial = islandLevel.getRandomOre();
				if (Main.isDebugEnabled()) Main.sendlog("§a[OreGen4] §2Sử dụng OreLevel của đảo §a" + islandId +
						"§2, permission: §a" + islandLevel.getPermission() +
						"§2, sinh ra: §a" + oreMaterial.name());
						
				// Sử dụng API của SuperiorSkyblock2 nếu có thể để cập nhật block an toàn
				if (Main.hooksuper != null) {
					try {
						Main.hooksuper.updateBlockSafely(to, oreMaterial);
					} catch (Exception e) {
						to.setType(oreMaterial);
					}
				} else {
					to.setType(oreMaterial);
				}
				return;
			}

			// Không tìm thấy cấp độ ore cho đảo - sử dụng cấp độ mặc định
			final OreLevel defaultLevel = ConfigManager.getDefaultLevel(env);

			if (defaultLevel != null) {
				Material oreMaterial = defaultLevel.getRandomOre();
				if (Main.isDebugEnabled()) Main.sendlog("§e[OreGen4] §aKhông tìm thấy cấp độ ore cho đảo §2" + islandId +
						"§a, sử dụng cấp độ mặc định: §2" + defaultLevel.getPermission() +
						"§a, sinh ra: §2" + oreMaterial.name());
						
				// Sử dụng API của SuperiorSkyblock2 nếu có thể để cập nhật block an toàn
				if (Main.hooksuper != null) {
					try {
						Main.hooksuper.updateBlockSafely(to, oreMaterial);
					} catch (Exception e) {
						to.setType(oreMaterial);
					}
				} else {
					to.setType(oreMaterial);
				}
			} else {
				if (Main.isDebugEnabled()) Main.sendlog("§c[OreGen4] §4Không tìm thấy OreLevel mặc định, sử dụng COBBLESTONE");
				to.setType(Material.COBBLESTONE);
			}

		} catch (Exception e) {
			Main.sendlog("§c[OreGen4] §4Error generating ore: §c" + e.getMessage());
			e.printStackTrace();
			to.setType(Material.COBBLESTONE);
		}
	}

	/**
	 * Các phương thức dưới đây chỉ để tương thích với code cũ
	 * DEPRECATED: Vui lòng sử dụng ConfigManager thay thế
	 */

	/**
	 * @deprecated Sử dụng
	 *             {@link me.sfclog.oregen4.config.ConfigManager#loadConfig()} thay
	 *             thế
	 */
	@Deprecated
	public static void load_config_and_rate() {
		// Chức năng đã được chuyển sang ConfigManager
		ConfigManager.loadConfig();
	}

	/**
	 * @deprecated Sử dụng
	 *             {@link me.sfclog.oregen4.config.ConfigManager#loadConfig()} thay
	 *             thế
	 */
	@Deprecated
	public static void load_config_and_rate_nether() {
		// Chức năng đã được chuyển sang ConfigManager
		ConfigManager.loadConfig();
	}

	/**
	 * @deprecated Sử dụng
	 *             {@link me.sfclog.oregen4.config.ConfigManager#loadConfig()} thay
	 *             thế
	 */
	@Deprecated
	public static void load_config_and_rate_theend() {
		// Chức năng đã được chuyển sang ConfigManager
		ConfigManager.loadConfig();
	}

	/**
	 * @deprecated Không cần thiết với hệ thống mới
	 */
	@Deprecated
	public static void remap() {
		// Không cần thiết với hệ thống mới
	}
}
