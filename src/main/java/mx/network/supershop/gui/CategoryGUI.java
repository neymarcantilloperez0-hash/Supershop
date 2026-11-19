package mx.network.supershop.gui;

import mx.network.supershop.SuperShop;
import mx.network.supershop.shop.ShopCategory;
import mx.network.supershop.shop.ShopItem;
import mx.network.supershop.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CategoryGUI {

    private final SuperShop plugin;
    // Slots de la grilla de ítems: filas 1-4, columnas 1-7 (7 x 4 = 28 por página)
    private static final int[] ITEM_SLOTS = buildItemSlots();

    public CategoryGUI(SuperShop plugin) {
        this.plugin = plugin;
    }

    private static int[] buildItemSlots() {
        int[] slots = new int[28];
        int idx = 0;
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                slots[idx++] = row * 9 + col;
            }
        }
        return slots;
    }

    public Inventory build(Player player, ShopCategory category, int page) {
        String title = ItemBuilder.color(category.getDisplayName() + " &8- &7Página " + (page + 1));
        CategoryHolder holder = new CategoryHolder(category, page);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        Material fillerGlassMat = safeMaterial(
                plugin.getConfig().getString("gui.category-menu.filler-glass", "BLACK_STAINED_GLASS_PANE"));
        ItemStack fillerGlass = new ItemBuilder(fillerGlassMat).name(" ").build();
        ItemStack categoryGlass = new ItemBuilder(category.getGlass()).name(" ").build();

        // Fila superior completa con el vidrio de color de la categoría.
        for (int i = 0; i < 9; i++) inv.setItem(i, categoryGlass);

        // Columnas laterales (slot 0 y 8 de cada fila) con el mismo vidrio de color.
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9, categoryGlass);
            inv.setItem(row * 9 + 8, categoryGlass);
        }

        // Fila inferior rellena con vidrio neutro; luego ponemos los botones encima.
        for (int i = 45; i < 54; i++) inv.setItem(i, fillerGlass);

        int itemsPerPage = plugin.getConfig().getInt("gui.category-menu.items-per-page", 28);
        List<ShopItem> items = category.getItems();
        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) itemsPerPage));
        int from = page * itemsPerPage;

        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            int itemIndex = from + i;
            if (itemIndex >= items.size()) break;
            inv.setItem(ITEM_SLOTS[i], buildItemIcon(items.get(itemIndex)));
        }

        // Botón: volver al menú principal
        inv.setItem(
                plugin.getConfig().getInt("gui.category-menu.back-to-main-slot", 46),
                new ItemBuilder(safeMaterial(plugin.getConfig().getString("gui.category-menu.back-to-main-item", "NETHER_STAR")))
                        .name("&e« Volver al menú principal")
                        .build()
        );

        // Botón: página anterior (solo si hay más de 1 página)
        if (page > 0) {
            inv.setItem(
                    plugin.getConfig().getInt("gui.category-menu.previous-page-slot", 48),
                    new ItemBuilder(safeMaterial(plugin.getConfig().getString("gui.category-menu.previous-page-item", "FEATHER")))
                            .name("&a« Página anterior")
                            .build()
            );
        }

        // Cabeza con el saldo, al centro.
        inv.setItem(
                plugin.getConfig().getInt("gui.category-menu.balance-head-slot", 49),
                plugin.getMainMenuGUI().buildBalanceHead(player)
        );

        // Botón: vender todo lo que tenga el jugador de esta categoría
        inv.setItem(
                plugin.getConfig().getInt("gui.category-menu.sell-all-slot", 50),
                new ItemBuilder(safeMaterial(plugin.getConfig().getString("gui.category-menu.sell-all-item", "HOPPER")))
                        .name("&c&lVender todo")
                        .lore("&7Vende todos los ítems de esta", "&7categoría que tengas en tu inventario.")
                        .build()
        );

        // Botón: página siguiente (solo si hay más páginas)
        if (page < totalPages - 1) {
            inv.setItem(
                    plugin.getConfig().getInt("gui.category-menu.next-page-slot", 51),
                    new ItemBuilder(safeMaterial(plugin.getConfig().getString("gui.category-menu.next-page-item", "FIREWORK_ROCKET")))
                            .name("&a Página siguiente »")
                            .build()
            );
        }

        // Botón: cerrar
        inv.setItem(
                plugin.getConfig().getInt("gui.category-menu.close-slot", 53),
                new ItemBuilder(safeMaterial(plugin.getConfig().getString("gui.category-menu.close-item", "BARRIER")))
                        .name("&c&lCerrar")
                        .build()
        );

        return inv;
    }

    private ItemStack buildItemIcon(ShopItem shopItem) {
        String buyLine = shopItem.canBuy()
                ? "&7Precio compra: &a$" + formatPrice(shopItem.getBuyPrice())
                : "&7Precio compra: &cNo disponible";
        String sellLine = shopItem.canSell()
                ? "&7Precio venta: &c$" + formatPrice(shopItem.getSellPrice())
                : "&7Precio venta: &cNo disponible";

        return new ItemBuilder(shopItem.getMaterial())
                .name(shopItem.getDisplayName())
                .lore(
                        buyLine,
                        sellLine,
                        "",
                        "&eClic izq: &fcomprar 1",
                        "&eClic der: &fcomprar 64",
                        "&eShift + clic der: &fvender todo"
                )
                .build();
    }

    private String formatPrice(double price) {
        return String.format("%,.2f", price);
    }

    private Material safeMaterial(String name) {
        try {
            return Material.valueOf(name);
        } catch (Exception e) {
            return Material.STONE;
        }
    }
}
