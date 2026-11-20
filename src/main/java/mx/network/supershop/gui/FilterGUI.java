package mx.network.supershop.gui;

import mx.network.supershop.SuperShop;
import mx.network.supershop.shop.ShopCategory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Menú de 36 slots para elegir el filtro de una categoría (hasta 14 filtros). */
public class FilterGUI {

    public static final int[] FILTER_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    public static final int SLOT_INFO = 4;
    public static final int SLOT_BACK = 31;

    private final SuperShop plugin;

    public FilterGUI(SuperShop plugin) {
        this.plugin = plugin;
    }

    public static int indexOfSlot(int slot) {
        for (int i = 0; i < FILTER_SLOTS.length; i++) {
            if (FILTER_SLOTS[i] == slot) return i;
        }
        return -1;
    }

    public Inventory build(Player player, BrowseHolder from) {
        ShopCategory cat = from.getCategory();
        List<ShopFilter> filters = from.getFilters();
        FilterHolder holder = new FilterHolder(cat, filters, from.getFilterIndex(), from.getPage());
        Inventory inv = Bukkit.createInventory(holder, 36, GuiUtil.title("&8Filtros » " + GuiUtil.plain(cat.getDisplayName())));
        holder.setInventory(inv);

        Material black = Material.BLACK_STAINED_GLASS_PANE;
        for (int i = 0; i < 36; i++) inv.setItem(i, GuiUtil.pane(black));
        for (int i = 0; i < 9; i++) inv.setItem(i, GuiUtil.pane(cat.getGlass()));

        inv.setItem(SLOT_INFO, GuiUtil.glow(GuiUtil.categoryIcon(plugin, cat, cat.getDisplayName(),
                "&8━━━━━━━━━━━━━━━━━━",
                "&7Elige qué tipo de ítems quieres ver.")));

        for (int i = 0; i < filters.size() && i < FILTER_SLOTS.length; i++) {
            ShopFilter f = filters.get(i);
            boolean sel = i == from.getFilterIndex();
            List<String> lore = new ArrayList<>();
            lore.add("&8━━━━━━━━━━━━━━━━━━");
            lore.add("&7Ítems: &f" + f.getItems().size());
            lore.add("");
            lore.add(sel ? "&a✔ Filtro activo" : "&eClic para ver");
            ItemStack icon = GuiUtil.named(f.getIcon(), f.getName(), lore.toArray(new String[0]));
            inv.setItem(FILTER_SLOTS[i], sel ? GuiUtil.glow(icon) : icon);
        }

        inv.setItem(SLOT_BACK, GuiUtil.named(Material.ARROW, "&e« Volver", "&7Regresar a la categoría"));
        holder.setRing(GuiUtil.RING_36);
        return inv;
    }
}
