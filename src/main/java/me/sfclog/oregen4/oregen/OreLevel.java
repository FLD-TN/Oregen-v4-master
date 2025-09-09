package me.sfclog.oregen4.oregen;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import java.util.HashMap;
import java.util.Map;

public class OreLevel {
    private final String permission;
    private final Map<Material, Integer> ores;
    private final Map<Integer, Material> weightedOres;
    private int totalWeight;

    public OreLevel(String permission, Map<Material, Integer> ores) {
        this.permission = permission;
        this.ores = ores;
        this.weightedOres = new HashMap<>();
        this.calculateWeights();
    }

    private void calculateWeights() {
        totalWeight = 0;
        for (Map.Entry<Material, Integer> entry : ores.entrySet()) {
            int chance = entry.getValue();
            for (int i = 0; i < chance; i++) {
                weightedOres.put(totalWeight++, entry.getKey());
            }
        }
    }

    public String getPermission() {
        return permission;
    }

    public Material getRandomOre() {
        if (totalWeight == 0)
            return Material.STONE;
        int random = (int) (Math.random() * totalWeight);
        for (Map.Entry<Material, Integer> entry : ores.entrySet()) {
            random -= entry.getValue();
            if (random < 0) {
                return entry.getKey();
            }
        }
        return Material.STONE;
    }

    public static OreLevel fromConfig(String levelName, ConfigurationSection config) {
        if (config == null)
            return null;

        String permission = config.getString("permission");
        Map<Material, Integer> ores = new HashMap<>();

        ConfigurationSection oresSection = config.getConfigurationSection("ores");
        if (oresSection != null) {
            for (String oreName : oresSection.getKeys(false)) {
                try {
                    Material material = Material.valueOf(oreName);
                    int chance = oresSection.getInt(oreName);
                    ores.put(material, chance);
                } catch (IllegalArgumentException e) {
                    System.out.println("[OreGen4] Invalid material name in config: " + oreName);
                }
            }
        }

        return new OreLevel(permission, ores);
    }
}
