package mx.network.supershop.commands;

import mx.network.supershop.SuperShop;
import mx.network.supershop.util.ItemBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    private final SuperShop plugin;

    public ShopCommand(SuperShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("supershop.admin")) {
                sender.sendMessage(ItemBuilder.color(plugin.getConfig().getString("messages.no-permission")));
                return true;
            }
            plugin.reloadConfig();
            plugin.getShopManager().load();
            sender.sendMessage(ItemBuilder.color(plugin.getConfig().getString("messages.reload-success")));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ItemBuilder.color(plugin.getConfig().getString("messages.only-players")));
            return true;
        }
        if (!player.hasPermission("supershop.use")) {
            player.sendMessage(ItemBuilder.color(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }

        player.openInventory(plugin.getMainMenuGUI().build(player));
        return true;
    }
}
