package mx.network.supershop.gui;

import mx.network.supershop.SuperShop;
import mx.network.supershop.shop.ShopCategory;
import mx.network.supershop.shop.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Menú de una categoría: ítems paginados, filtro por tipo, vender todo y navegación.
 *
 *  fila 0  marco de color de la categoría (slot 4 = info)
 *  filas 1-4  28 ítems por página (columnas 1-7)
 *  fila 5  45 · 46 volver · 47 filtro · 48 anterior · 49 saldo · 50 vender todo · 51 siguiente · 52 página · 53 cerrar
 */
public class CategoryGUI {

    public static final int ROWS = 4;
    public static final int COLS = 7;

    /** Slots de ítems: filas 1-4, columnas 1-7 (28 por página). */
    public static final int[] ITEM_SLOTS = buildItemSlots();

    private final SuperShop plugin;

    public CategoryGUI(SuperShop plugin) {
        this.plugin = plugin;
    }

    private static int[] buildItemSlots() {
        int[] slots = new int[ROWS * COLS];
        int i = 0;
        for (int row = 1; row <= ROWS; row++) {
            for (int col = 1; col <= COLS; col++) {
                slots[i++] = row * 9 + col;
            }
        }
        return slots;
    }

    public static int pageCount(int itemCount) {
        return Math.max(1, (itemCount + ITEM_SLOTS.length - 1) / ITEM_SLOTS.length);
    }

    /** Páginas de una categoría completa (sin filtro). */
    public static int pageCount(ShopCategory cat) {
        return pageCount(cat.getItems().size());
    }

    public static int indexOfSlot(int slot) {
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            if (ITEM_SLOTS[i] == slot) return i;
        }
        return -1;
    }

    private int slot(String key, int def) {
        return plugin.getConfig().getInt("gui.category-menu." + key, def);
    }

    private Material icon(String key, Material def) {
        return GuiUtil.mat(plugin.getConfig().getString("gui.category-menu." + key), def);
    }

    // ------------------------------------------------------------------
    //  Construcción
    // ------------------------------------------------------------------

    public Inventory build(Player player, ShopCategory cat, int page) {
        return build(player, cat, 0, page);
    }

    public Inventory build(Player player, ShopCategory cat, int filterIndex, int page) {
        return build(player, cat, ShopFilter.load(plugin, cat), filterIndex, page);
    }

    public Inventory build(Player player, ShopCategory cat, List<ShopFilter> filters, int filterIndex, int page) {
        BrowseHolder holder = new BrowseHolder(cat, filters);
        holder.setFilterIndex(filterIndex);
        holder.setPage(page);
        Inventory inv = Bukkit.createInventory(holder, 54, GuiUtil.title(cat.getDisplayName()));
        holder.setInventory(inv);
        render(holder, player, true);
        return inv;
    }

    /** Pinta (o repinta) el menú entero con la página y el filtro actuales, sin reabrirlo. */
    public void render(BrowseHolder h, Player player, boolean animateReveal) {
        Inventory inv = h.getInventory();
        ShopCategory cat = h.getCategory();
        List<ShopItem> items = h.getVisible();
        int pages = pageCount(items.size());
        h.setPage(Math.min(h.getPage(), pages - 1));
        int page = h.getPage();

        Material filler = icon("filler-glass", Material.BLACK_STAINED_GLASS_PANE);
        Material glass = cat.getGlass();
        for (int i = 0; i < 54; i++) inv.setItem(i, GuiUtil.pane(filler));

        // marco de color de la categoría
        for (int i = 0; i < 9; i++) inv.setItem(i, GuiUtil.pane(glass));
        for (int row = 1; row <= ROWS; row++) {
            inv.setItem(row * 9, GuiUtil.pane(glass));
            inv.setItem(row * 9 + 8, GuiUtil.pane(glass));
        }

        // ítems de la página (se muestran de golpe o aparecen fila por fila)
        ItemStack[] icons = new ItemStack[ITEM_SLOTS.length];
        int start = page * ITEM_SLOTS.length;
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            int idx = start + i;
            icons[i] = idx < items.size() ? buildItemIcon(items.get(idx)) : null;
        }
        h.setPageIcons(icons);
        boolean reveal = animateReveal && MenuAnimator.revealEnabled();
        h.setRevealRow(reveal ? 0 : ROWS);
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            inv.setItem(ITEM_SLOTS[i], reveal ? null : icons[i]);
        }

        // cabecera
        ShopFilter filter = h.getFilter();
        List<String> head = new ArrayList<>();
        head.add("&8━━━━━━━━━━━━━━━━━━");
        if (filter != null) head.add("&7Filtro: &r" + filter.getName());
        head.add("&7Ítems: &f" + items.size());
        head.add("&7Página: &f" + (page + 1) + "&7/&f" + pages);
        head.add("");
        head.add("&7Clic izq. en un ítem: &aComprar");
        head.add("&7Clic der. en un ítem: &eVender");
        head.add("&7Shift + clic: &6Vender todo");
        inv.setItem(4, GuiUtil.glow(GuiUtil.categoryIcon(plugin, cat, cat.getDisplayName(), head.toArray(new String[0]))));

        // navegación
        inv.setItem(slot("back-to-main-slot", 46), GuiUtil.named(icon("back-to-main-item", Material.NETHER_STAR),
                "&e« Menú principal", "&7Volver a las categorías"));

        if (h.hasFilters()) {
            inv.setItem(slot("filter-slot", 47), buildFilterButton(h));
        }

        if (page > 0) {
            int jump = Math.max(2, plugin.getConfig().getInt("gui.category-menu.page-jump", 5));
            inv.setItem(slot("previous-page-slot", 48), GuiUtil.named(icon("previous-page-item", Material.FEATHER),
                    "&e« Página anterior", "&7Ir a la página &f" + page,
                    "&8Clic der.: -" + jump + " páginas",
                    "&8Shift + clic: primera página"));
        }
        if (page < pages - 1) {
            int jump = Math.max(2, plugin.getConfig().getInt("gui.category-menu.page-jump", 5));
            inv.setItem(slot("next-page-slot", 51), GuiUtil.named(icon("next-page-item", Material.FIREWORK_ROCKET),
                    "&ePágina siguiente »", "&7Ir a la página &f" + (page + 2),
                    "&8Clic der.: +" + jump + " páginas",
                    "&8Shift + clic: última página"));
        }

        inv.setItem(slot("balance-head-slot", 49), plugin.getMainMenuGUI().buildBalanceHead(player));
        inv.setItem(slot("sell-all-slot", 50), buildSellAll(player, h));

        int from = items.isEmpty() ? 0 : start + 1;
        int to = Math.min(items.size(), start + ITEM_SLOTS.length);
        inv.setItem(slot("page-info-slot", 52), GuiUtil.named(icon("page-info-item", Material.PAPER),
                "&e&lPágina " + (page + 1) + "&7/&e" + pages,
                "&8━━━━━━━━━━━━━━━━",
                "&7Mostrando &f" + from + "&7-&f" + to + " &7de &f" + items.size()));

        inv.setItem(slot("close-slot", 53), GuiUtil.named(icon("close-item", Material.BARRIER),
                "&c✖ Cerrar", "&7Cerrar la tienda"));

        h.setRing(GuiUtil.RING_54);
    }

    private ItemStack buildFilterButton(BrowseHolder h) {
        ShopFilter cur = h.getFilter();
        List<String> lore = new ArrayList<>();
        lore.add("&8━━━━━━━━━━━━━━━━━━");
        lore.add("&7Filtro actual:");
        lore.add("&r" + cur.getName());
        lore.add("&7(" + cur.getItems().size() + " ítems)");
        lore.add("");
        lore.add("&eClic izq. &8» &7Elegir filtro");
        lore.add("&eClic der. &8» &7Siguiente filtro");
        lore.add("&eShift + clic &8» &7Ver todo");
        return GuiUtil.glow(GuiUtil.named(cur.getIcon(), "&b&l▼ Filtros", lore.toArray(new String[0])));
    }

    // ------------------------------------------------------------------
    //  Vender todo (solo lo que se está viendo)
    // ------------------------------------------------------------------

    /** Valor total que recibiría el jugador al vender todo lo que lleva de estos ítems. */
    public double estimateSellAll(Player player, List<ShopItem> items) {
        Map<Material, Integer> have = new EnumMap<>(Material.class);
        for (ItemStack s : player.getInventory().getStorageContents()) {
            if (s != null && s.getType() != Material.AIR) have.merge(s.getType(), s.getAmount(), Integer::sum);
        }
        double value = 0;
        for (ShopItem it : items) {
            if (!it.canSell()) continue;
            Integer n = have.get(it.getMaterial());
            if (n != null) value += n * it.getSellPrice();
        }
        return GuiUtil.round2(value);
    }

    private ItemStack buildSellAll(Player player, BrowseHolder h) {
        double value = estimateSellAll(player, h.getVisible());
        boolean armed = System.currentTimeMillis() < h.getSellArmedUntil();
        ShopFilter f = h.getFilter();
        String scope = f == null ? "esta categoría" : "este filtro";
        if (armed) {
            return GuiUtil.glow(GuiUtil.named(icon("sell-all-item", Material.HOPPER), "&c&l⚠ ¿Vender todo?",
                    "&8━━━━━━━━━━━━━━━━━━",
                    "&7Vas a vender todo lo que lleves de",
                    "&7" + scope + " por &a$" + GuiUtil.money(value),
                    "",
                    "&c&lClic otra vez para confirmar",
                    "&8(se cancela solo en 5 segundos)"));
        }
        return GuiUtil.named(icon("sell-all-item", Material.HOPPER), "&6&lVender todo",
                "&8━━━━━━━━━━━━━━━━━━",
                "&7Vende todo lo que lleves de",
                "&7" + scope + ".",
                "",
                "&7Valor estimado: &a$" + GuiUtil.money(value),
                "",
                "&eClic para vender");
    }

    private ItemStack buildItemIcon(ShopItem it) {
        List<String> lore = new ArrayList<>();
        lore.add("&8━━━━━━━━━━━━━━━━━━");
        lore.add(it.canBuy() ? "&7Compra: &a$" + GuiUtil.money(it.getBuyPrice()) : "&7Compra: &cNo disponible");
        lore.add(it.canSell() ? "&7Venta: &e$" + GuiUtil.money(it.getSellPrice()) : "&7Venta: &cNo disponible");
        lore.add("");
        if (it.canBuy()) lore.add("&aClic izq. &8» &7Comprar");
        if (it.canSell()) {
            lore.add("&eClic der. &8» &7Vender");
            lore.add("&6Shift + clic &8» &7Vender todo");
        }
        return GuiUtil.named(it.getMaterial(), it.getDisplayName(), lore.toArray(new String[0]));
    }
}
