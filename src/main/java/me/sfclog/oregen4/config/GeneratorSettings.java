package me.sfclog.oregen4.config;

import me.sfclog.oregen4.Main;

public class GeneratorSettings {
    // Generator types mặc định
    private static boolean lavaWaterEnabled = true; // Mặc định true
    private static boolean fenceWaterEnabled = true; // Mặc định true
    private static boolean generationEnabled = false; // Mặc định false

    // Cấu hình mở rộng cho fence+water generator
    private static boolean extendedFenceCheckEnabled = true; // Mặc định true
    private static int extendedFenceRange = 2; // Mặc định 2 blocks

    public static void loadSettings() {
        // Load main generation setting (mặc định false)
        generationEnabled = Main.pl.getConfig().getBoolean("generation.enabled", false);

        // Load generator types (lava+water và fence+water mặc định true)
        lavaWaterEnabled = Main.pl.getConfig().getBoolean("generation.types.lava_water.enabled", true);
        fenceWaterEnabled = Main.pl.getConfig().getBoolean("generation.types.fence_water.enabled", true);

        // Cấu hình mở rộng không còn được sử dụng nhưng vẫn đọc để tương thích ngược
        extendedFenceCheckEnabled = Main.pl.getConfig().getBoolean("generation.types.fence_water.extended_check",
                false);
        extendedFenceRange = Main.pl.getConfig().getInt("generation.types.fence_water.extended_range", 1);
        fenceInChunkCheckEnabled = Main.pl.getConfig().getBoolean("generation.types.fence_water.chunk_check", false);

        // Đặt giá trị mặc định để plugin luôn hoạt động với phiên bản đơn giản
        extendedFenceCheckEnabled = false;
        fenceInChunkCheckEnabled = false;

        // Chỉ cho phép các generator hoạt động khi generation.enabled = true
        lavaWaterEnabled = generationEnabled && lavaWaterEnabled;
        fenceWaterEnabled = generationEnabled && fenceWaterEnabled;
    }

    public static boolean isLavaWaterEnabled() {
        return lavaWaterEnabled;
    }

    public static boolean isFenceWaterEnabled() {
        return fenceWaterEnabled;
    }

    public static boolean isExtendedFenceCheckEnabled() {
        return fenceWaterEnabled && extendedFenceCheckEnabled;
    }

    public static int getExtendedFenceRange() {
        return extendedFenceRange;
    }

    // Cho phép kiểm tra hàng rào trong toàn bộ chunk
    private static boolean fenceInChunkCheckEnabled = true;

    public static boolean isFenceInChunkCheckEnabled() {
        return fenceWaterEnabled && fenceInChunkCheckEnabled;
    }
}
