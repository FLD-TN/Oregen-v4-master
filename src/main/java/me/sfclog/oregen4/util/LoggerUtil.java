package me.sfclog.oregen4.util;

import me.sfclog.oregen4.Main;
import org.bukkit.Material;

/**
 * Utility class for logging with rate limiting
 */
public class LoggerUtil {

    /**
     * Log a message with rate limiting
     * 
     * @param type    Log type for filtering
     * @param message Message to log
     */
    public static void logFiltered(String type, String message) {
        if (Main.isDebugEnabled()) {
            if (LogFilter.shouldLog(type)) {
                Main.sendlog(message);
            }
        }
    }

    /**
     * Log a message with rate limiting based on material
     * 
     * @param type     Log type for filtering
     * @param material Material to use for the log key
     * @param info     Additional info for the log key
     * @param message  Message to log
     */
    public static void logFiltered(String type, Material material, String info, String message) {
        if (Main.isDebugEnabled()) {
            String logKey = LogFilter.createLogKey(type, material, info);
            if (LogFilter.shouldLog(logKey)) {
                Main.sendlog(message);
            }
        }
    }

    /**
     * Log an error message (will not be filtered)
     * 
     * @param message Error message
     */
    public static void logError(String message) {
        Main.sendlog("§c[OreGen4] " + message);
    }

    /**
     * Log an info message (always shown regardless of debug setting)
     * 
     * @param message Info message
     */
    public static void logInfo(String message) {
        Main.sendlog("§a[OreGen4] " + message);
    }

    /**
     * Log a warning message (will not be filtered)
     * 
     * @param message Warning message
     */
    public static void logWarning(String message) {
        Main.sendlog("§e[OreGen4] " + message);
    }
}
