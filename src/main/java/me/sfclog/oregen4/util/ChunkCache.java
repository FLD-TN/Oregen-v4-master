package me.sfclog.oregen4.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache lưu trữ kết quả kiểm tra chunk để tránh quét lại nhiều lần
 */
public class ChunkCache {
    // Sử dụng ConcurrentHashMap để thread-safe
    private static final Map<String, CachedValue> cache = new ConcurrentHashMap<>();

    // Tần suất dọn dẹp cache (ms)
    private static final long CLEANUP_INTERVAL = 60000; // 1 phút

    // Thời gian cache mặc định
    private static final long DEFAULT_TTL = 60000; // 1 phút

    // Class lưu trữ giá trị và thời gian hết hạn
    private static class CachedValue {
        private final Object value;
        private final long expiryTime;

        public CachedValue(Object value, long ttl) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + ttl;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }

        public Object getValue() {
            return value;
        }
    }

    static {
        // Lên lịch task dọn dẹp cache
        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(CLEANUP_INTERVAL);
                    cleanup();
                }
            } catch (InterruptedException e) {
                // Ignore
            }
        }, "ChunkCache-Cleanup").start();
    }

    /**
     * Dọn dẹp các mục hết hạn trong cache
     */
    private static void cleanup() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Lưu giá trị vào cache
     * 
     * @param key   Khóa
     * @param value Giá trị
     * @param ttl   Thời gian sống (ms)
     */
    public static void put(String key, Object value, long ttl) {
        cache.put(key, new CachedValue(value, ttl));
    }

    /**
     * Lưu giá trị vào cache với thời gian sống mặc định
     * 
     * @param key   Khóa
     * @param value Giá trị
     */
    public static void put(String key, Object value) {
        put(key, value, DEFAULT_TTL);
    }

    /**
     * Lấy giá trị từ cache
     * 
     * @param key Khóa
     * @return Giá trị hoặc null nếu không tìm thấy hoặc hết hạn
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        CachedValue cachedValue = cache.get(key);
        if (cachedValue == null || cachedValue.isExpired()) {
            if (cachedValue != null && cachedValue.isExpired()) {
                cache.remove(key);
            }
            return null;
        }
        return (T) cachedValue.getValue();
    }

    /**
     * Xóa một mục khỏi cache
     * 
     * @param key Khóa
     */
    public static void remove(String key) {
        cache.remove(key);
    }

    /**
     * Xóa toàn bộ cache
     */
    public static void clear() {
        cache.clear();
    }

    /**
     * Lấy số lượng mục trong cache
     * 
     * @return Số lượng mục
     */
    public static int size() {
        return cache.size();
    }
}
