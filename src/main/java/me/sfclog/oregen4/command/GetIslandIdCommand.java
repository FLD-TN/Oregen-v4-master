package me.sfclog.oregen4.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;

import me.sfclog.oregen4.util.Color;

/**
 * Lệnh lấy ID của đảo của người chơi
 */
public class GetIslandIdCommand implements CommandExecutor {

    private final Plugin plugin;

    public GetIslandIdCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Kiểm tra quyền
        if (!sender.hasPermission("oregen.admin")) {
            sender.sendMessage(Color.tran("&cBạn không có quyền thực hiện lệnh này!"));
            return true;
        }

        // Kiểm tra tham số
        if (args.length < 1) {
            sender.sendMessage(Color.tran("&cSử dụng: /oregen getislandid <playername>"));
            return true;
        }

        // Lấy tên người chơi từ tham số
        String playerName = args[0];
        
        // Tìm người chơi
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            // Nếu người chơi không online, thử tìm từ SuperiorSkyblock API
            SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(playerName);
            if (superiorPlayer == null) {
                sender.sendMessage(Color.tran("&cKhông tìm thấy người chơi &e" + playerName + "&c!"));
                return true;
            }
            
            // Lấy đảo của người chơi
            Island island = superiorPlayer.getIsland();
            if (island == null) {
                sender.sendMessage(Color.tran("&cNgười chơi &e" + playerName + "&c không có đảo!"));
                return true;
            }
            
            // Hiển thị thông tin đảo
            String islandId = island.getUniqueId().toString();
            sender.sendMessage(Color.tran("&a&l[OreGen4] &eThông tin đảo của người chơi &b" + playerName + "&e:"));
            
            // Tạo tin nhắn ID đảo với nút copy có thể click được
            if (sender instanceof Player) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                    "tellraw " + sender.getName() + " [\"\",{\"text\":\"§a• §eID đảo: §b" + islandId + " \"},{\"text\":\"§a[Copy]\",\"bold\":true,\"color\":\"green\",\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"" + islandId + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"§aClick để copy ID đảo\"]}}]");
            } else {
                sender.sendMessage(Color.tran("&a• &eID đảo: &b" + islandId));
            }
            
            if (island.getOwner() != null) {
                sender.sendMessage(Color.tran("&a• &eChủ đảo: &b" + island.getOwner().getName()));
            }
            sender.sendMessage(Color.tran("&a• &eTên đảo: &b" + island.getName()));
            return true;
        }
        
        // Nếu người chơi online, lấy thông tin từ SuperiorSkyblock API
        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);
        Island island = superiorPlayer.getIsland();
        
        if (island == null) {
            sender.sendMessage(Color.tran("&cNgười chơi &e" + playerName + "&c không có đảo!"));
            return true;
        }
        
        // Hiển thị thông tin đảo
        String islandId = island.getUniqueId().toString();
        sender.sendMessage(Color.tran("&a&l[OreGen4] &eThông tin đảo của người chơi &b" + playerName + "&e:"));
        
        // Tạo tin nhắn ID đảo với nút copy có thể click được
        if (sender instanceof Player) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                "tellraw " + sender.getName() + " [\"\",{\"text\":\"§a• §eID đảo: §b" + islandId + " \"},{\"text\":\"§a[Copy]\",\"bold\":true,\"color\":\"green\",\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"" + islandId + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"§aClick để copy ID đảo\"]}}]");
        } else {
            sender.sendMessage(Color.tran("&a• &eID đảo: &b" + islandId));
        }
        
        if (island.getOwner() != null) {
            sender.sendMessage(Color.tran("&a• &eChủ đảo: &b" + island.getOwner().getName()));
        }
        sender.sendMessage(Color.tran("&a• &eTên đảo: &b" + island.getName()));
        
        return true;
    }
}
