package mx.network.supershop.gui;

import mx.network.supershop.SuperShop;
import mx.network.supershop.economy.EconomyManager;
import mx.network.supershop.shop.ShopCategory;
import mx.network.supershop.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class MainMenuGUI {

    private final SuperShop plugin;

    public MainMenuGUI(SuperShop plugin) {
        this.plugin = plugin;
    }

    public Inventory build(Player player) {
        String title = ItemBuilder.color(plugin.getConfig().getString("gui.main-menu.title", "&8&lUltimate Shop"));
        MainMenuHolder holder = new MainMenuHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        Material borderGlassMat = safeMaterial(
                plugin.getConfig().getString("gui.main-menu.border-glass", "BLACK_STAINED_GLASS_PANE"));
        ItemStack borderGlass = new ItemBuilder(borderGlassMat).name(" ").build();

        // Rellenamos todo el fondo con vidrio de borde primero.
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, borderGlass);
        }

        // Colocamos cada categoría en su slot configurado, con su propio ícono.
        for (ShopCategory category : plugin.getShopManager().getCategories().values()) {
            ItemStack icon = new ItemBuilder(category.getIcon())
                    .name(category.getDisplayName())
                    .lore(
                            "&7" + category.getItems().size() + " productos disponibles",
                            "",
                            "&eClic para entrar »"
                    )
                    .build();
            if (category.getMainMenuSlot() >= 0 && category.getMainMenuSlot() < 54) {
                inv.setItem(category.getMainMenuSlot(), icon);
            }
        }

        // Cabeza con el saldo del jugador, al centro abajo.
        int balanceSlot = plugin.getConfig().getInt("gui.main-menu.balance-head-slot", 49);
        inv.setItem(balanceSlot, buildBalanceHead(player));

        return inv;
    }

    /** Cabeza del jugador con su saldo actual en el lore. Se puede llamar de nuevo para refrescarla. */
    public ItemStack buildBalanceHead(Player player) {
        String balance = formatBalance(player);

        String nameTemplate = plugin.getConfig().getString("balance-head.name", "&6&lTu Saldo");
        List<String> loreTemplate = plugin.getConfig().getStringList("balance-head.lore");

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer((OfflinePlayer) player);
            meta.setDisplayName(ItemBuilder.color(nameTemplate));
            meta.setLore(loreTemplate.stream()
                    .map(line -> ItemBuilder.color(line.replace("{balance}", balance)))
                    .toList());
            head.setItemMeta(meta);
        }
        return head;
    }

    private String formatBalance(Player player) {
        if (EconomyManager.get() == null) return "0.00";
        return String.format("%,.2f", EconomyManager.get().getBalance(player));
    }

    private Material safeMaterial(String name) {
        try {
            return Material.valueOf(name);
        } catch (Exception e) {
            return Material.BLACK_STAINED_GLASS_PANE;
        }
    }
}
