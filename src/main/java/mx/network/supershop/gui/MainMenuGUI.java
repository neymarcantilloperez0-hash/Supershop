package mx.network.supershop.gui;

import mx.network.supershop.SuperShop;
import mx.network.supershop.economy.EconomyManager;
import mx.network.supershop.shop.ShopCategory;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class MainMenuGUI {

    private final SuperShop plugin;

    public MainMenuGUI(SuperShop plugin) {
        this.plugin = plugin;
    }

    public Inventory build(Player player) {
        FileConfiguration cfg = plugin.getConfig();
        MainHolder holder = new MainHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, GuiUtil.title(cfg.getString("gui.main-menu.title", "&8&lShop")));
        holder.setInventory(inv);

        Material border = GuiUtil.mat(cfg.getString("gui.main-menu.border-glass"), Material.BLACK_STAINED_GLASS_PANE);
        Material borderAlt = GuiUtil.mat(cfg.getString("gui.main-menu.border-glass-alt"), Material.GRAY_STAINED_GLASS_PANE);
        Material inner = GuiUtil.mat(cfg.getString("gui.main-menu.inner-glass"), Material.GRAY_STAINED_GLASS_PANE);

        // Marco con patrón alterno + relleno interior
        for (int i = 0; i < 54; i++) {
            int row = i / 9, col = i % 9;
            boolean ring = row == 0 || row == 5 || col == 0 || col == 8;
            if (ring) {
                inv.setItem(i, GuiUtil.pane(i % 2 == 0 ? border : borderAlt));
            } else {
                inv.setItem(i, GuiUtil.pane(inner));
            }
        }

        // Título decorativo
        int infoSlot = cfg.getInt("gui.main-menu.info-slot", 4);
        inv.setItem(infoSlot, GuiUtil.glow(GuiUtil.named(Material.NETHER_STAR,
                cfg.getString("gui.main-menu.info-name", "&6&l✦ TIENDA DEL SERVIDOR ✦"),
                "&8━━━━━━━━━━━━━━━━━━━━",
                "&7Elige una categoría para",
                "&7ver todos los ítems disponibles.",
                "",
                "&7Dentro de cada categoría puedes",
                "&7filtrar por tipo (tablones, lana...).",
                "",
                "&7Al elegir un ítem podrás",
                "&7seleccionar la cantidad y ver",
                "&7el precio antes de pagar.")));

        // Categorías
        for (ShopCategory cat : plugin.getShopManager().getCategories().values()) {
            int slot = cat.getMainMenuSlot();
            if (slot < 0 || slot >= 54) continue;
            if (GuiUtil.categoryHidden(plugin, cat)) continue;

            if (!GuiUtil.categoryEnabled(plugin, cat)) {
                // Categoría deshabilitada desde config.yml: se muestra bloqueada
                String lockName = cfg.getString("disabled-category.name", "{name} &8(&c&lDeshabilitada&8)")
                        .replace("{name}", cat.getDisplayName());
                List<String> lockLore = cfg.getStringList("disabled-category.lore");
                Material lockIcon = GuiUtil.mat(cfg.getString("disabled-category.icon"), Material.BARRIER);
                inv.setItem(slot, GuiUtil.named(lockIcon, lockName, lockLore.toArray(new String[0])));
                continue;
            }

            int n = cat.getItems().size();
            int pages = CategoryGUI.pageCount(cat);
            ItemStack icon = GuiUtil.categoryIcon(plugin, cat, cat.getDisplayName(),
                    "&8━━━━━━━━━━━━━━━━━━",
                    "&7Ítems disponibles: &f" + n,
                    "&7Páginas: &f" + pages,
                    "",
                    "&e▶ Clic para abrir");
            inv.setItem(slot, GuiUtil.glow(icon));
        }

        inv.setItem(cfg.getInt("gui.main-menu.balance-head-slot", 49), buildBalanceHead(player));
        inv.setItem(cfg.getInt("gui.main-menu.close-slot", 53),
                GuiUtil.named(Material.BARRIER, "&c✖ Cerrar", "&7Cerrar la tienda"));

        holder.setRing(GuiUtil.RING_54);
        return inv;
    }

    public ItemStack buildBalanceHead(Player player) {
        FileConfiguration cfg = plugin.getConfig();
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof SkullMeta) {
            SkullMeta sm = (SkullMeta) meta;
            sm.setOwningPlayer(player);
            sm.setDisplayName(GuiUtil.c("&r" + cfg.getString("balance-head.name", "&6&lTu Saldo")));
            List<String> lore = new ArrayList<>();
            String bal = formatBalance(player);
            for (String line : cfg.getStringList("balance-head.lore")) {
                lore.add(GuiUtil.c("&r" + line.replace("{balance}", bal)));
            }
            sm.setLore(lore);
            sm.addItemFlags(ItemFlag.values());
            head.setItemMeta(sm);
        }
        return head;
    }

    private String formatBalance(Player player) {
        Economy econ = EconomyManager.get();
        if (econ == null) return "0";
        return GuiUtil.money(econ.getBalance(player));
    }
}
