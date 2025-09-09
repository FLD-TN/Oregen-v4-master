package me.sfclog.oregen4.oregen;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.sfclog.oregen4.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.sfclog.oregen4.util.Color;

/**
 * Utility class cho file oregen.yml cũ
 * 
 * @deprecated Hệ thống config mới đã được chuyển sang
 *             {@link me.sfclog.oregen4.config.ConfigManager}
 */
@Deprecated
public class OreGenLoad {

	public static File locate = new File("plugins/OreGen4/", "oregen.yml");
	public static FileConfiguration DataFile = (FileConfiguration) YamlConfiguration.loadConfiguration(locate);

	public static void load() {
		try {
			DataFile.save(locate);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void save() {
		try {
			DataFile.save(locate);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static int getint(String s) {
		if (DataFile.contains(s)) {
			return DataFile.getInt(s);
		}
		return 0;
	}

	public static double getdoubl(String s) {
		if (DataFile.contains(s)) {
			return DataFile.getDouble(s);
		}
		return 0;
	}

	public static boolean getb(String s) {
		if (DataFile.contains(s)) {
			return DataFile.getBoolean(s);
		}
		return false;
	}

	public static String getlang(String s) {
		if (DataFile.contains(s)) {
			return Color.tran(DataFile.getString(s));
		}
		return s;
	}

	public static String getlang_nocolor(String s) {
		if (DataFile.contains(s)) {
			return DataFile.getString(s);
		}
		return null;
	}

	public static void addlang(String s, double s2) {
		if (!DataFile.contains(s)) {
			DataFile.set(s, s2);
		}
	}

	public static void addlang(String s, Boolean s2) {
		if (!DataFile.contains(s)) {
			DataFile.set(s, s2);
		}
	}

	public static void addlang(String s, List<String> s2) {
		if (!DataFile.contains(s)) {
			DataFile.set(s, s2);
		}
	}

	public static void setforcelang(String s, String s2) {
		DataFile.set(s, s2);
	}

	public static void setforcelang(String s, double x) {
		DataFile.set(s, x);
	}

	public static void addlang(String s, String s2) {
		if (!DataFile.contains(s)) {
			DataFile.set(s, s2);
		}
	}

	public static void addlang(String s, int s2) {
		if (!DataFile.contains(s)) {
			DataFile.set(s, s2);
		}
	}

}
