package me.sfclog.oregen4;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import me.sfclog.oregen4.config.ConfigManager;
import me.sfclog.oregen4.hook.BentoBoxHook;
import me.sfclog.oregen4.hook.SuperiorSkyblockHook;
import me.sfclog.oregen4.oregen.EventOreGen;
import net.luckperms.api.LuckPerms;

public class Main extends JavaPlugin {

	public static Plugin pl;
	public static BentoBoxHook hookbentobox = null;

	public static SuperiorSkyblockHook hooksuper = null;

	public static LuckPerms luckapi;

	// Debug flag check
	public static boolean isDebugEnabled() {
		return pl.getConfig().getBoolean("debug", false);
	}

	// Singleton access
	public static Main getInstance() {
		return (Main) pl;
	}

	// LuckPerms API access
	public LuckPerms getLuckPermsApi() {
		return luckapi;
	}

	@Override
	public void onEnable() {
		pl = this;

		// Lưu file config.yml mặc định nếu chưa tồn tại
		saveDefaultConfig();
		sendlog("§a[OreGen4] §2Đã tạo file config.yml mặc định!");

		// Sử dụng ConfigManager thay vì các phương thức đã deprecated
		ConfigManager.loadConfig();
		sendlog("§a[OreGen4] §2Đã tải cấu hình từ config.yml!");

		// Kết nối với LuckPerms API
		RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
		if (provider != null) {
			luckapi = provider.getProvider();
			sendlog("§2[OreGen4] §aKết nối thành công với LuckPerms API!");
		} else {
			sendlog("§c[OreGen4] §4Không thể kết nối với LuckPerms API. §cMột số chức năng sẽ bị vô hiệu hóa!");
		}

		// Khởi tạo hooks với các plugin SkyBlock
		if (Bukkit.getPluginManager().getPlugin("BentoBox") != null) {
			hookbentobox = new BentoBoxHook();
			sendlog("§2[OreGen4] §aHook BentoBox thành công!");
		} else if (Bukkit.getPluginManager().getPlugin("SuperiorSkyblock2") != null) {
			sendlog("§2[OreGen4] §aHook SuperiorSkyblock2 thành công!");
			hooksuper = new SuperiorSkyblockHook();
		} else {
			sendlog("§2[OreGen4] §aEnable Vanilla Mode!");
		}

		// Đăng ký sự kiện sinh quặng
		Bukkit.getPluginManager().registerEvents(new EventOreGen(), this);

		// Đăng ký các lệnh
		getCommand("oregen").setExecutor(new me.sfclog.oregen4.command.OreGenCommand(this));
		getCommand("upgradeore").setExecutor(new me.sfclog.oregen4.command.UpgradeOreCommand(this));
		getCommand("permission").setExecutor(new me.sfclog.oregen4.updater.PermissionCommand(this));
		getCommand("orestats").setExecutor(new me.sfclog.oregen4.command.StatsCommand(this));
		sendlog("§a[OreGen4] §2Đã đăng ký tất cả các lệnh!");

		// Khởi tạo hệ thống cache nâng cao nếu LuckPerms có sẵn
		if (luckapi != null) {
			me.sfclog.oregen4.util.EnhancedPermissionCache.initialize();

			// Đăng ký các listener liên quan đến quyền
			Bukkit.getPluginManager().registerEvents(
					new me.sfclog.oregen4.listener.PlayerPermissionLoader(luckapi), this);
			Bukkit.getPluginManager().registerEvents(
					new me.sfclog.oregen4.listener.IslandPermissionTracker(luckapi), this);
			sendlog("§a[OreGen4] §2Đã đăng ký các listener quyền để tối ưu hóa hiệu suất!");
		}

		// Khởi tạo hệ thống cache vị trí
		me.sfclog.oregen4.util.LocationOwnerCache.initialize();

		// Khởi tạo hệ thống cache theo đảo
		me.sfclog.oregen4.util.IslandPermissionCache.initialize();
		sendlog("§a[OreGen4] §2Đã khởi tạo hệ thống cache theo đảo!");
		
		// Khởi tạo hệ thống quản lý ore theo đảo
		me.sfclog.oregen4.island.IslandOreManager.init();
		sendlog("§a[OreGen4] §2Đã khởi tạo hệ thống quản lý ore theo đảo!");
		
		// Đăng ký listener cho sự kiện xóa đảo
		me.sfclog.oregen4.listener.IslandDisbandListener islandListener = new me.sfclog.oregen4.listener.IslandDisbandListener();
		Bukkit.getPluginManager().registerEvents(islandListener, this);
		
		// Log thông tin về việc đăng ký listener
		sendlog("§a[OreGen4] §2Đã đăng ký listener xử lý sự kiện xóa đảo!");
		sendlog("§a[OreGen4] §2Đã đăng ký listener xử lý sự kiện xóa đảo!");
		
		// Khởi tạo API
		me.sfclog.oregen4.api.OreGenAPI api = new me.sfclog.oregen4.api.OreGenAPI();
		sendlog("§a[OreGen4] §2Đã khởi tạo OreGenAPI!");
		
		// Đăng ký placeholder nếu PlaceholderAPI có sẵn
		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			new me.sfclog.oregen4.placeholders.OreGenPlaceholders(this).register();
			sendlog("§a[OreGen4] §2Đã đăng ký placeholders cho PlaceholderAPI!");
		}
	}

	public static UUID get_hook(Location location) {
		if (hookbentobox != null) {
			return hookbentobox.getIslandOwner(location);

		} else if (hooksuper != null) {
			return hooksuper.getIslandOwner(location);
		}
		return null;
	}

	@Override
	public void onDisable() {
		// Đóng tất cả các hệ thống cache
		me.sfclog.oregen4.util.EnhancedPermissionCache.shutdown();
		me.sfclog.oregen4.util.IslandPermissionCache.shutdown();
		me.sfclog.oregen4.util.LocationOwnerCache.shutdown();
		
		// Đóng hệ thống quản lý ore theo đảo
		me.sfclog.oregen4.island.IslandOreManager.shutdown();

		sendlog("§a[OreGen4] §2Đã tắt tất cả các hệ thống cache và lưu dữ liệu!");
	}

	public static void sendlog(String string) {
		Bukkit.getConsoleSender().sendMessage(string);
	}
	

}
