package me.sfclog.oregen4.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import me.sfclog.oregen4.Main;

/**
 * Quản lý cache cho vị trí và chủ sở hữu của máy sinh quặng
 * Hỗ trợ xác định chủ sở hữu ngay cả khi không có ai gần đó
 */
public class LocationOwnerCache {

    // Cache chính: ChunkKey -> UUID chủ sở hữu
    private static final Map<String, CachedOwnerInfo> ownerCache = new ConcurrentHashMap<>();

    // Thời gian cache cho mỗi mục (ms)
    private static final long CACHE_DURATION = 86400000; // 24 giờ

    // Chu kỳ dọn dẹp cache (ms)
    private static final long CLEANUP_INTERVAL = 3600000; // 1 giờ

    // Task dọn dẹp cache
    private static BukkitTask cleanupTask;

    /**
     * Lớp lưu trữ thông tin về chủ sở hữu được cache
     */
    public static class CachedOwnerInfo {
        private UUID ownerUUID;
        private long timestamp;
        private int accessCount;

        public CachedOwnerInfo(UUID ownerUUID) {
            this.ownerUUID = ownerUUID;
            this.timestamp = System.currentTimeMillis();
            this.accessCount = 1;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION;
        }

        public void updateTimestamp() {
            this.timestamp = System.currentTimeMillis();
            this.accessCount++;
        }

        public UUID getOwnerUUID() {
            return ownerUUID;
        }

        public void setOwnerUUID(UUID ownerUUID) {
            this.ownerUUID = ownerUUID;
            this.timestamp = System.currentTimeMillis();
        }

        public int getAccessCount() {
            return accessCount;
        }
    }

    /**
     * Khởi tạo hệ thống cache và lên lịch cho task dọn dẹp
     */
    public static void initialize() {
        // Hủy task cũ nếu có
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }

        // Lên lịch task dọn dẹp cache định kỳ
        cleanupTask = Main.pl.getServer().getScheduler().runTaskTimerAsynchronously(Main.pl,
                () -> cleanupExpiredCache(),
                CLEANUP_INTERVAL / 50, // Ticks đầu tiên
                CLEANUP_INTERVAL / 50 // Ticks lặp lại
        );

        Main.sendlog("§a[OreGen4] Khởi tạo LocationOwnerCache thành công");
    }

    /**
     * Tạo key duy nhất cho chunk từ location
     */
    private static String getChunkKey(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return null;
        }
        return loc.getWorld().getName() + ":" + loc.getBlockX() / 16 + ":" + loc.getBlockZ() / 16;
    }

    /**
     * Tạo key chính xác hơn cho block
     */
    private static String getBlockKey(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return null;
        }
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    /**
     * Lưu chủ sở hữu vào cache
     * 
     * @param loc       Vị trí
     * @param ownerUUID UUID của chủ sở hữu
     */
    public static void cacheOwner(Location loc, UUID ownerUUID) {
        if (loc == null || ownerUUID == null) {
            return;
        }

        String chunkKey = getChunkKey(loc);
        if (chunkKey == null) {
            return;
        }

        CachedOwnerInfo existingInfo = ownerCache.get(chunkKey);
        if (existingInfo != null) {
            // Cập nhật nếu đã tồn tại
            existingInfo.setOwnerUUID(ownerUUID);
        } else {
            // Thêm mới nếu chưa có
            ownerCache.put(chunkKey, new CachedOwnerInfo(ownerUUID));
        }
    }

    /**
     * Lấy chủ sở hữu từ cache
     * 
     * @param loc Vị trí
     * @return UUID của chủ sở hữu hoặc null nếu không có trong cache
     */
    public static UUID getCachedOwner(Location loc) {
        if (loc == null) {
            return null;
        }

        String chunkKey = getChunkKey(loc);
        if (chunkKey == null) {
            return null;
        }

        CachedOwnerInfo info = ownerCache.get(chunkKey);
        if (info != null && !info.isExpired()) {
            info.updateTimestamp();
            return info.getOwnerUUID();
        }

        return null;
    }

    /**
     * Xóa cache cho một chunk cụ thể
     * 
     * @param loc Vị trí trong chunk
     */
    public static void clearChunkCache(Location loc) {
        if (loc == null) {
            return;
        }

        String chunkKey = getChunkKey(loc);
        if (chunkKey != null) {
            ownerCache.remove(chunkKey);
        }
    }

    /**
     * Dọn dẹp các cache đã hết hạn
     */
    public static void cleanupExpiredCache() {
        try {
            int cleaned = 0;

            for (Iterator<Map.Entry<String, CachedOwnerInfo>> iterator = ownerCache.entrySet().iterator(); iterator
                    .hasNext();) {
                Map.Entry<String, CachedOwnerInfo> entry = iterator.next();
                if (entry.getValue().isExpired()) {
                    iterator.remove();
                    cleaned++;
                }
            }

            // Hiển thị thống kê cache nếu đang debug
            if (cleaned > 0 && Main.pl.getConfig().getBoolean("debug", false)) {
                Main.sendlog("§a[OreGen4] Đã dọn dẹp " + cleaned + " cache vị trí, còn lại " + ownerCache.size());
            }
        } catch (Exception e) {
            Main.sendlog("§c[OreGen4] Lỗi khi dọn dẹp cache vị trí: " + e.getMessage());
        }
    }

    /**
     * Truyền đảo (chuyển chủ) giữa hai người chơi
     * Cập nhật tất cả các vị trí thuộc về chủ cũ sang chủ mới
     * 
     * @param oldOwnerUUID UUID của chủ cũ
     * @param newOwnerUUID UUID của chủ mới
     * @return Số lượng vị trí đã được cập nhật
     */
    public static int transferIsland(UUID oldOwnerUUID, UUID newOwnerUUID) {
        if (oldOwnerUUID == null || newOwnerUUID == null) {
            return 0;
        }

        int updated = 0;

        for (Map.Entry<String, CachedOwnerInfo> entry : ownerCache.entrySet()) {
            CachedOwnerInfo info = entry.getValue();
            if (oldOwnerUUID.equals(info.getOwnerUUID())) {
                info.setOwnerUUID(newOwnerUUID);
                updated++;
            }
        }

        if (updated > 0) {
            Main.sendlog("§a[OreGen4] Đã cập nhật " + updated + " vị trí từ chủ " +
                    oldOwnerUUID + " sang chủ " + newOwnerUUID);
        }

        return updated;
    }

    /**
     * Xóa tất cả cache
     */
    public static void clearAllCache() {
        ownerCache.clear();
    }

    /**
     * Lấy số lượng vị trí trong cache
     * 
     * @return Số lượng vị trí trong cache
     */
    public static int getCachedLocationsCount() {
        return ownerCache.size();
    }

    /**
     * Xử lý khi tắt plugin
     */
    public static void shutdown() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        clearAllCache();
    }
}
