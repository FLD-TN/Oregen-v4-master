package me.sfclog.oregen4.config;

import me.sfclog.oregen4.Main;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

public class OreLevel {
    private static final Random RANDOM = new Random();
    private final String permission;
    private final Map<Material, Integer> ores;
    private final int totalWeight;
    private final Material[] weightedMaterials;

    public OreLevel(String permission, Map<Material, Integer> ores) {
        if (permission == null)
            throw new IllegalArgumentException("Permission cannot be null");
        if (ores == null)
            throw new IllegalArgumentException("Ores map cannot be null");

        this.permission = permission;
        this.ores = new HashMap<>(ores); // Create defensive copy
        this.totalWeight = calculateTotalWeight();
        this.weightedMaterials = createWeightedArray();
    }

    private int calculateTotalWeight() {
        return ores.values().stream().mapToInt(Integer::intValue).sum();
    }

    private Material[] createWeightedArray() {
        if (totalWeight == 0)
            return new Material[0];

        Material[] materials = new Material[totalWeight];
        int index = 0;

        if (Main.isDebugEnabled()) {
            Main.sendlog("§e[OreGen4] §bTạo mảng trọng số cho permission §a" + permission);
            Main.sendlog("§e[OreGen4] §bTổng trọng số: §a" + totalWeight);
        }

        for (Map.Entry<Material, Integer> entry : ores.entrySet()) {
            Material material = entry.getKey();
            int weight = entry.getValue();

            if (Main.isDebugEnabled()) {
                Main.sendlog("§e[OreGen4] §b - Thêm §a" + weight + "§b lần vật liệu §a" + material.name());
            }

            for (int i = 0; i < weight; i++) {
                materials[index++] = material;
            }
        }

        if (Main.isDebugEnabled()) {
            Main.sendlog("§e[OreGen4] §bMảng trọng số tạo thành công với §a" + materials.length + "§b phần tử");

            // Kiểm tra lại mảng
            Map<Material, Integer> counts = new HashMap<>();
            for (Material m : materials) {
                counts.put(m, counts.getOrDefault(m, 0) + 1);
            }

            for (Map.Entry<Material, Integer> entry : counts.entrySet()) {
                Main.sendlog("§e[OreGen4] §b - §a" + entry.getKey().name() + "§b xuất hiện §a" +
                        entry.getValue() + "§b lần trong mảng");
            }
        }

        return materials;
    }

    public String getPermission() {
        return permission;
    }

    public Material getRandomOre() {
        if (totalWeight == 0 || weightedMaterials.length == 0) {
            if (Main.isDebugEnabled()) {
                Main.sendlog("§c[OreGen4] CẢNH BÁO: Mảng quặng trọng số trống, trả về STONE mặc định");
            }
            return Material.STONE;
        }

        if (Main.isDebugEnabled()) {
            // Kiểm tra danh sách tất cả vật liệu có sẵn
            Main.sendlog("§e[OreGen4] §bDanh sách tất cả vật liệu có sẵn:");
            Set<String> allMaterials = new HashSet<>();
            for (Material m : Material.values()) {
                allMaterials.add(m.name());
            }
            if (allMaterials.contains("COPPER_ORE")) {
                Main.sendlog("§e[OreGen4] §aPhiên bản Minecraft của bạn hỗ trợ COPPER_ORE");
            } else {
                Main.sendlog("§c[OreGen4] CẢNH BÁO: Phiên bản Minecraft của bạn KHÔNG hỗ trợ COPPER_ORE");
            }

            // Kiểm tra xem COPPER_ORE có nằm trong danh sách quặng không
            boolean hasCopper = false;
            int copperCount = 0;
            for (Material m : weightedMaterials) {
                if (m.name().equals("COPPER_ORE")) {
                    hasCopper = true;
                    copperCount++;
                }
            }

            Main.sendlog("§e[OreGen4] §bKiểm tra level " + permission + ":");
            Main.sendlog("§e[OreGen4] §b - Có COPPER_ORE: " + (hasCopper ? "§aCó" : "§cKhông"));
        }
        if (Main.isDebugEnabled()) {
            // Kiểm tra nếu có COPPER_ORE trong mảng
            boolean hasCopper = false;
            int copperCount = 0;
            for (Material m : weightedMaterials) {
                if (m.name().equals("COPPER_ORE")) {
                    hasCopper = true;
                    copperCount++;
                }
            }

            if (hasCopper) {
                Main.sendlog("§e[OreGen4] §b - Số lượng COPPER_ORE trong mảng trọng số: §a" + copperCount + "/"
                        + weightedMaterials.length);
                double percentage = (double) copperCount / weightedMaterials.length * 100;
                Main.sendlog("§e[OreGen4] §b - Tỉ lệ thực tế: §a" + String.format("%.2f", percentage) + "%");
            }

            // Kiểm tra mảng weightedMaterials
            Map<Material, Integer> counts = new HashMap<>();
            for (Material m : weightedMaterials) {
                counts.put(m, counts.getOrDefault(m, 0) + 1);
            }

            Main.sendlog("§e[OreGen4] §bPhân tích mảng trọng số:");
            for (Map.Entry<Material, Integer> entry : counts.entrySet()) {
                double percent = (double) entry.getValue() / weightedMaterials.length * 100;
                Main.sendlog("§e[OreGen4] §b - " + entry.getKey().name() + ": §a" +
                        entry.getValue() + "/" + weightedMaterials.length +
                        " (" + String.format("%.2f", percent) + "%)");
            }
        }

        // Sinh ngẫu nhiên chính xác
        int randomIndex = RANDOM.nextInt(totalWeight);
        Material selectedOre = weightedMaterials[randomIndex];

        if (Main.isDebugEnabled()) {
            Main.sendlog("§e[OreGen4] §aChỉ số ngẫu nhiên: §f" + randomIndex + "/" + totalWeight +
                    "§a, quặng được chọn: §b" + selectedOre.name() +
                    "§a cho level với permission: §6" + permission);
        }

        // Hiển thị tỉ lệ các loại quặng trong level này nếu debug mode được bật
        if (Main.isDebugEnabled()) {
            Main.sendlog("§e[OreGen4] §aTỉ lệ quặng trong level §6" + permission + "§a:");
            for (Map.Entry<Material, Integer> entry : ores.entrySet()) {
                Main.sendlog("§e[OreGen4]   §b" + entry.getKey().name() + "§a: §f" +
                        entry.getValue() + " §a(" +
                        String.format("%.1f", (entry.getValue() * 100.0 / totalWeight)) + "%)");
            }
        }

        return selectedOre;
    }

    /**
     * Kiểm tra xem một Material có nằm trong danh sách ore của level không
     * 
     * @param material Material cần kiểm tra
     * @return true nếu material nằm trong danh sách ore, false nếu không
     */
    public boolean containsOre(Material material) {
        return ores.containsKey(material);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OreLevel oreLevel = (OreLevel) o;
        return Objects.equals(permission, oreLevel.permission) &&
                Objects.equals(ores, oreLevel.ores);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permission, ores);
    }

    public static OreLevel fromConfig(ConfigurationSection config) {
        if (config == null)
            return null;

        String permission = config.getString("permission");
        Map<Material, Integer> ores = new LinkedHashMap<>();

        // Kiểm tra cấu trúc cấu hình
        // Hỗ trợ cả hai định dạng: "ores" và "blocks" (định dạng mới)
        ConfigurationSection oresSection = config.getConfigurationSection("ores");
        if (oresSection == null) {
            // Thử định dạng mới "blocks"
            oresSection = config.getConfigurationSection("blocks");
        }

        if (oresSection != null) {
            if (Main.isDebugEnabled()) {
                Main.sendlog("§e[OreGen4] §bĐang đọc cấu hình ore cho permission §a" + permission);
            }

            for (String oreName : oresSection.getKeys(false)) {
                try {
                    if (Main.isDebugEnabled()) {
                        Main.sendlog("§e[OreGen4] §b  - Đang thử đọc vật liệu: §f" + oreName);
                    }

                    Material material = Material.valueOf(oreName);
                    int chance = oresSection.getInt(oreName);
                    ores.put(material, chance);

                    if (Main.isDebugEnabled()) {
                        Main.sendlog("§e[OreGen4] §a  - Thành công: §f" + material + " §avới tỉ lệ §f" + chance);
                    }
                } catch (IllegalArgumentException e) {
                    Main.sendlog("§c[OreGen4] LỖI: Không thể đọc vật liệu từ config: §f" + oreName + " §c- "
                            + e.getMessage());
                    System.out.println("[OreGen4] Invalid material name in config: " + oreName);
                }
            }
        } else {
            Main.sendlog(
                    "§c[OreGen4] LỖI: Không tìm thấy mục cấu hình 'ores' hoặc 'blocks' cho permission " + permission);
        }

        return new OreLevel(permission, ores);
    }
}
