package me.sfclog.oregen4.placeholders;

import org.bukkit.entity.Player;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.sfclog.oregen4.Main;
import me.sfclog.oregen4.api.OreGenAPI;

/**
 * Placeholder API cho OreGen4 
 * Hỗ trợ các placeholder để hiển thị trong GUI và các plugin khác
 */
public class OreGenPlaceholders extends PlaceholderExpansion {

    private final Main plugin;
    private final OreGenAPI api;

    public OreGenPlaceholders(Main plugin) {
        this.plugin = plugin;
        this.api = OreGenAPI.getInstance();
    }

    @Override
    public String getIdentifier() {
        return "oregen";
    }

    @Override
    public String getAuthor() {
        return "sfclog";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        // %oregen_island_level% - Lấy cấp độ ore của đảo hiện tại
        if (identifier.equals("island_level")) {
            return String.valueOf(api.getOreLevel(player.getLocation()));
        }

        return null;
    }
}