package me.sfclog.oregen4.oregen;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import me.sfclog.oregen4.Main;
import me.sfclog.oregen4.config.ConfigManager;
import me.sfclog.oregen4.config.OreLevel;
import me.sfclog.oregen4.util.PermissionCache;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

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
			UUID uuid = Main.get_hook(to.getLocation());

			// Debug logs để kiểm tra
			Main.sendlog("§e[OreGen4] §bSinh quặng trong môi trường §a" + env.name() + "§b cho player UUID §a" + uuid);

			if (uuid == null) {
				// Nếu không có chủ đảo -> cobblestone
				Main.sendlog("§e[OreGen4] §cKhông tìm thấy UUID người chơi cho vị trí này, đặt COBBLESTONE");
				to.setType(Material.COBBLESTONE);
				return;
			}

			// Log thông tin người chơi
			String playerName = me.sfclog.oregen4.util.GetUUID.getName(uuid);
			Main.sendlog("§e[OreGen4] §bSinh quặng cho người chơi: §a" +
					(playerName != null ? playerName : "Unknown") + " §b(UUID: §a" + uuid + "§b)");

			// Kiểm tra xem có cache permission không
			boolean hasCachedPerm = me.sfclog.oregen4.util.EnhancedPermissionCache.hasCachedPermission(uuid, env);
			Main.sendlog("§e[OreGen4] §bTrạng thái cache: " + (hasCachedPerm ? "§aCó" : "§cKhông"));

			if (hasCachedPerm) {
				String cachedPerm = me.sfclog.oregen4.util.EnhancedPermissionCache.getCachedPermissionLevel(uuid, env);
				Main.sendlog("§e[OreGen4] §bQuyền trong cache: §a" + cachedPerm);
			}

			// Kiểm tra enhanced cache trước (cache được preload khi player join)
			OreLevel cachedOreLevel = me.sfclog.oregen4.util.EnhancedPermissionCache.getOreLevelFromCache(uuid, env);
			if (cachedOreLevel != null) {
				// Sử dụng cache cho hiệu suất tối ưu - không cần LuckPerms lookup
				Material oreMaterial = cachedOreLevel.getRandomOre();
				Main.sendlog("§a[OreGen4] Sử dụng OreLevel từ EnhancedCache cho môi trường " +
						env.name() + ", sinh ra: " + oreMaterial.name());
				to.setType(oreMaterial);
				return;
			} else {
				Main.sendlog("§c[OreGen4] Không tìm thấy OreLevel trong EnhancedCache");
			}

			// Kiểm tra cache cũ trước khi chuyển sang mới
			OreLevel oldCachedOreLevel = PermissionCache.getCachedLevel(uuid, env);
			if (oldCachedOreLevel != null) {
				// Nếu có trong cache cũ -> sử dụng và cập nhật vào cache mới
				Material oreMaterial = oldCachedOreLevel.getRandomOre();
				to.setType(oreMaterial);
				Main.sendlog("§a[OreGen4] Sử dụng OreLevel từ LegacyCache cho môi trường " +
						env.name() + ", sinh ra: " + oreMaterial.name());

				// Cập nhật vào cache mới cho lần sau
				me.sfclog.oregen4.util.EnhancedPermissionCache.cachePermission(uuid, env, "oregen.imported",
						oldCachedOreLevel);
				return;
			} else {
				Main.sendlog("§c[OreGen4] Không tìm thấy OreLevel trong LegacyCache");
			}

			// Không có trong cache - sử dụng cách tiếp cận mới ít blocking hơn
			// Lấy level mặc định
			final OreLevel defaultLevel = ConfigManager.getDefaultLevel(env);

			if (defaultLevel != null) {
				Material oreMaterial = defaultLevel.getRandomOre();
				Main.sendlog("§e[OreGen4] §aSử dụng OreLevel mặc định cho môi trường " +
						env.name() + ", sinh ra: " + oreMaterial.name());
				to.setType(oreMaterial);
			} else {
				Main.sendlog("§c[OreGen4] Không tìm thấy OreLevel mặc định, sử dụng COBBLESTONE");
				to.setType(Material.COBBLESTONE);
			}

			// Chỉ tải permission nếu LuckPerms khả dụng
			if (Main.getInstance() != null && Main.getInstance() instanceof Main &&
					((Main) Main.getInstance()).getLuckPermsApi() != null) {

				// Lên lịch async task để cập nhật cache cho lần sau
				// Nhưng KHÔNG chờ đợi kết quả ngay bây giờ
				Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
					// Tìm và cache permission của player này cho lần sau
					// Chỉ thực hiện nếu chưa có trong cache (tránh gọi nhiều lần liên tiếp)
					if (!me.sfclog.oregen4.util.EnhancedPermissionCache.hasCachedPermission(uuid, env)) {
						String pName = me.sfclog.oregen4.util.GetUUID.getName(uuid);
						if (pName != null && Bukkit.getPlayerExact(pName) != null) {
							// Chỉ cache nếu player online
							Main.sendlog("§e[OreGen4] §bBắt đầu preload quyền cho người chơi §a" + pName);
							new me.sfclog.oregen4.listener.PlayerPermissionLoader(
									((Main) Main.getInstance()).getLuckPermsApi()).preloadPlayerPermissions(uuid);
						} else {
							Main.sendlog("§e[OreGen4] §cKhông thể preload quyền: người chơi không online");
						}
					} else {
						Main.sendlog("§e[OreGen4] §bKhông cần preload quyền: đã có trong cache");
					}
				});
			} else {
				Main.sendlog("§c[OreGen4] LuckPerms API không khả dụng, không thể preload quyền");
			}

		} catch (Exception e) {
			Main.sendlog("§c[OreGen4] Error generating ore: " + e.getMessage());
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
