package me.sfclog.oregen4.util;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Bộ lọc log để tránh spam console
 * Sử dụng hệ thống rate limiting để giảm số lượng log khi có quá nhiều sự kiện
 * giống nhau
 */
public class LogFilter {

    // Thời gian tối thiểu giữa các log giống nhau (ms)
    private static final long MIN_LOG_INTERVAL = 5000; // 5 giây

    // Số lượng log tối đa cho mỗi loại trong khoảng thời gian
    private static final int MAX_LOGS_PER_TYPE = 3;

    // Cache lưu trữ thời gian log cuối cùng cho mỗi loại
    private static final ConcurrentHashMap<String, LogInfo> logCache = new ConcurrentHashMap<>();

    // Set lưu trữ các loại log đang bị giới hạn
    private static final Set<String> throttledTypes = new HashSet<>();

    /**
     * Thông tin về log
     */
    private static class LogInfo {
        private long lastLogTime;
        private int countSinceLastInterval;

        public LogInfo() {
            this.lastLogTime = System.currentTimeMillis();
            this.countSinceLastInterval = 1;
        }

        public void incrementCount() {
            this.countSinceLastInterval++;
        }

        public void reset() {
            this.lastLogTime = System.currentTimeMillis();
            this.countSinceLastInterval = 0;
        }

        public boolean shouldThrottle() {
            return countSinceLastInterval > MAX_LOGS_PER_TYPE;
        }

        public boolean intervalExpired() {
            return System.currentTimeMillis() - lastLogTime > MIN_LOG_INTERVAL;
        }
    }

    /**
     * Kiểm tra xem log có nên được hiển thị hay không
     * 
     * @param type Loại log (ví dụ: "fence_water_debug", "block_event")
     * @return true nếu log nên được hiển thị, false nếu nên được lọc bỏ
     */
    public static boolean shouldLog(String type) {
        // Nếu loại này đang bị giới hạn, kiểm tra xem interval đã hết hạn chưa
        if (throttledTypes.contains(type)) {
            LogInfo info = logCache.get(type);
            if (info != null && info.intervalExpired()) {
                // Interval đã hết hạn, reset và cho phép log
                info.reset();
                throttledTypes.remove(type);
                return true;
            }
            return false;
        }

        // Kiểm tra và cập nhật cache
        LogInfo info = logCache.computeIfAbsent(type, k -> new LogInfo());

        // Nếu đã quá số lượng tối đa, bắt đầu giới hạn
        if (info.shouldThrottle()) {
            throttledTypes.add(type);
            return false;
        }

        // Nếu interval đã hết hạn, reset counter
        if (info.intervalExpired()) {
            info.reset();
            return true;
        }

        // Tăng counter và cho phép log
        info.incrementCount();
        return true;
    }

    /**
     * Tạo key log dựa trên vị trí
     * 
     * @param type Loại log
     * @param loc  Vị trí
     * @return Key duy nhất cho log
     */
    public static String createLogKey(String type, Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return type;
        }

        return type + ":" + loc.getWorld().getName() + ":" +
                (loc.getBlockX() / 16) + ":" + (loc.getBlockZ() / 16);
    }

    /**
     * Tạo key log dựa trên vật liệu và face
     * 
     * @param type     Loại log
     * @param material Vật liệu
     * @param face     Face
     * @return Key duy nhất cho log
     */
    public static String createLogKey(String type, Material material, String face) {
        return type + ":" + material.name() + ":" + face;
    }

    /**
     * Dọn dẹp cache log cũ
     */
    public static void cleanup() {
        logCache.clear();
        throttledTypes.clear();
    }
}
