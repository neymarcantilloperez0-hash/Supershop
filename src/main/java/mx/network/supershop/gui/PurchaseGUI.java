package mx.network.supershop.gui;

import mx.network.supershop.SuperShop;
import mx.network.supershop.economy.EconomyManager;
import mx.network.supershop.shop.ShopCategory;
import mx.network.supershop.shop.ShopItem;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Menú de 36 slots para elegir la cantidad antes de comprar o vender.
 *
 *   9  10[-64] 11[-10] 12[-1]  13[ÍTEM]  14[+1] 15[+10] 16[+64]  17
 *  18  19[reset] 20[1 stack] 21[9 stacks] 22[RESUMEN] 23[27 stacks] 24[36 stacks] 25[máx] 26
 *  27  28  29[volver]        31[CONFIRMAR]       33[cancelar] 34  35
 */
public class PurchaseGUI {

    public static final int SLOT_M64 = 10, SLOT_M10 = 11, SLOT_M1 = 12;
    public static final int SLOT_ITEM = 13;
    public static final int SLOT_P1 = 14, SLOT_P10 = 15, SLOT_P64 = 16;
    public static final int SLOT_RESET = 19, SLOT_SUMMARY = 22, SLOT_MAX = 25;
    /** Atajos por stacks (1, 9 = una fila, 27 = tres filas, 36 = inventario completo). */
    public static final int SLOT_STACK1 = 20, SLOT_STACK9 = 21, SLOT_STACK27 = 23, SLOT_STACK36 = 24;
    public static final int SLOT_BACK = 29, SLOT_CONFIRM = 31, SLOT_CANCEL = 33;

    private final SuperShop plugin;

    public PurchaseGUI(SuperShop plugin) {
        this.plugin = plugin;
    }

    public int maxAmount() {
        return Math.max(1, plugin.getConfig().getInt("purchase-menu.max-amount", 2304));
    }

    /** Límite de cantidad según el modo (comprar / vender). */
    public int limitFor(Player p, PurchaseHolder h) {
        if (h.isSell()) {
            return Math.max(1, Math.min(maxAmount(), GuiUtil.count(p, h.getItem())));
        }
        return maxAmount();
    }

    /** Cantidad máxima razonable: lo que puedes pagar y lo que te cabe (o lo que tienes si vendes). */
    public int maxFor(Player p, PurchaseHolder h) {
        ShopItem it = h.getItem();
        if (h.isSell()) {
            return limitFor(p, h);
        }
        int byMoney = maxAmount();
        Economy econ = EconomyManager.get();
        if (econ != null && it.getBuyPrice() > 0) {
            byMoney = (int) Math.min((double) maxAmount(), Math.floor(econ.getBalance(p) / it.getBuyPrice()));
        }
        int bySpace = GuiUtil.freeSpace(p, it);
        return Math.max(1, Math.min(byMoney, Math.min(bySpace, maxAmount())));
    }

    public Inventory build(Player p, ShopCategory cat, ShopItem item, boolean sell, int amount, int returnPage) {
        return build(p, cat, item, sell, amount, 0, returnPage);
    }

    public Inventory build(Player p, ShopCategory cat, ShopItem item, boolean sell, int amount, int returnFilter, int returnPage) {
        PurchaseHolder holder = new PurchaseHolder(cat, item, sell, amount, returnFilter, returnPage);
        String title = GuiUtil.title("&8" + (sell ? "Vender" : "Comprar") + " &8» &r" + item.getDisplayName());
        Inventory inv = Bukkit.createInventory(holder, 36, title);
        holder.setInventory(inv);
        render(inv, holder, p);
        return inv;
    }

    /** Pinta (o repinta) todo el menú con la cantidad y los precios actuales. */
    public void render(Inventory inv, PurchaseHolder h, Player p) {
        ShopItem it = h.getItem();
        boolean sell = h.isSell();
        int a = Math.max(1, h.getAmount());
        Economy econ = EconomyManager.get();
        double balance = econ != null ? econ.getBalance(p) : 0;

        double unit = sell ? it.getSellPrice() : it.getBuyPrice();
        double total = GuiUtil.round2(unit * a);
        double after = sell ? balance + total : balance - total;
        int maxStack = Math.max(1, it.getMaterial().getMaxStackSize());
        int stacks = (a + maxStack - 1) / maxStack;
        String stackText = stacks + (stacks == 1 ? " stack" : " stacks");

        // problemas que impiden confirmar
        List<String> problems = new ArrayList<>();
        if (!sell) {
            if (balance + 1e-9 < total) problems.add("&c✖ No tienes suficiente dinero");
            if (GuiUtil.freeSpace(p, it) < a) problems.add("&c✖ No tienes espacio en el inventario");
        } else {
            if (GuiUtil.count(p, it) < a) problems.add("&c✖ No tienes suficientes ítems");
        }
        boolean ok = problems.isEmpty();

        // ---- fondo ----
        Material black = Material.BLACK_STAINED_GLASS_PANE;
        Material accent = sell ? Material.ORANGE_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE;
        for (int i = 0; i < 36; i++) inv.setItem(i, GuiUtil.pane(black));
        Material top = h.getCategory() != null ? h.getCategory().getGlass() : Material.GRAY_STAINED_GLASS_PANE;
        for (int i = 0; i < 9; i++) inv.setItem(i, GuiUtil.pane(top));
        for (int i : new int[]{9, 17, 18, 26, 27, 35}) inv.setItem(i, GuiUtil.pane(accent));

        // ---- botones de cantidad ----
        inv.setItem(SLOT_M64, GuiUtil.named(Material.RED_STAINED_GLASS_PANE, 64, "&c&l-64", "&7Restar &f64"));
        inv.setItem(SLOT_M10, GuiUtil.named(Material.RED_STAINED_GLASS_PANE, 10, "&c&l-10", "&7Restar &f10"));
        inv.setItem(SLOT_M1, GuiUtil.named(Material.RED_STAINED_GLASS_PANE, 1, "&c&l-1", "&7Restar &f1"));
        inv.setItem(SLOT_P1, GuiUtil.named(Material.LIME_STAINED_GLASS_PANE, 1, "&a&l+1", "&7Sumar &f1"));
        inv.setItem(SLOT_P10, GuiUtil.named(Material.LIME_STAINED_GLASS_PANE, 10, "&a&l+10", "&7Sumar &f10"));
        inv.setItem(SLOT_P64, GuiUtil.named(Material.LIME_STAINED_GLASS_PANE, 64, "&a&l+64", "&7Sumar &f64 &8(1 stack)"));

        // ---- ítem central ----
        ItemStack display = GuiUtil.named(it.getMaterial(), Math.min(a, 64), it.getDisplayName(),
                "&8━━━━━━━━━━━━━━━━",
                "&7Cantidad: &f&l" + a + " &8(" + stackText + ")",
                "&7Precio por unidad: " + (sell ? "&e" : "&a") + "$" + GuiUtil.money(unit),
                "&7Total: " + (sell ? "&a&l$" : "&c&l$") + GuiUtil.money(total));
        inv.setItem(SLOT_ITEM, GuiUtil.glow(display));

        // ---- utilidades ----
        inv.setItem(SLOT_RESET, GuiUtil.named(Material.CLOCK, "&e↺ Restablecer", "&7Vuelve a la cantidad &f1"));
        inv.setItem(SLOT_MAX, GuiUtil.named(Material.NETHER_STAR, "&b★ Máximo",
                sell ? "&7Todo lo que tienes de este ítem" : "&7Lo máximo que puedes pagar",
                sell ? "&7y que cabe de una vez." : "&7y que te cabe en el inventario."));

        // ---- atajos por stacks ----
        int[] stackSlots = {SLOT_STACK1, SLOT_STACK9, SLOT_STACK27, SLOT_STACK36};
        int[] stackCounts = {1, 9, 27, 36};
        String[] stackNames = {"1 stack", "9 stacks &8(1 fila)", "27 stacks &8(3 filas)", "36 stacks &8(inventario)"};
        for (int i = 0; i < stackSlots.length; i++) {
            int qty = stackCounts[i] * maxStack;
            boolean here = a == Math.min(qty, maxAmount());
            ItemStack b = GuiUtil.named(it.getMaterial(), Math.min(64, stackCounts[i]),
                    "&b&l▣ " + stackNames[i],
                    "&7Cantidad: &f" + qty,
                    "&7Total: " + (sell ? "&a$" : "&c$") + GuiUtil.money(GuiUtil.round2(unit * qty)),
                    "", here ? "&a✔ Seleccionado" : "&eClic para fijar la cantidad");
            inv.setItem(stackSlots[i], here ? GuiUtil.glow(b) : b);
        }

        // ---- resumen (cofre) ----
        List<String> lore = new ArrayList<>();
        lore.add("&8━━━━━━━━━━━━━━━━━━━━");
        lore.add("&7Ítem: &r" + it.getDisplayName());
        lore.add("&7Cantidad: &f" + a + " &8(" + stackText + ")");
        lore.add("&7Precio unitario: " + (sell ? "&e" : "&a") + "$" + GuiUtil.money(unit));
        lore.add("");
        lore.add(sell ? "&7Vas a recibir: &a&l$" + GuiUtil.money(total)
                      : "&7Vas a gastar: &c&l$" + GuiUtil.money(total));
        lore.add("");
        lore.add("&7Tu saldo: &e$" + GuiUtil.money(balance));
        lore.add("&7Saldo después: " + (after >= 0 ? "&a$" : "&c$") + GuiUtil.money(after));
        if (!sell && balance > 0) {
            double ratio = total / balance;
            lore.add("&7Usas el &f" + Math.min(999, Math.round(ratio * 100)) + "% &7de tu saldo");
            lore.add(GuiUtil.bar(ratio, 20));
        }
        if (!ok) {
            lore.add("");
            lore.addAll(problems);
        }
        inv.setItem(SLOT_SUMMARY, GuiUtil.named(Material.CHEST,
                sell ? "&6&lResumen de venta" : "&6&lResumen de compra", lore.toArray(new String[0])));

        // ---- confirmar / volver / cancelar ----
        if (ok) {
            inv.setItem(SLOT_CONFIRM, GuiUtil.glow(GuiUtil.named(Material.LIME_CONCRETE,
                    sell ? "&a&l✔ CONFIRMAR VENTA" : "&a&l✔ CONFIRMAR COMPRA",
                    "&7" + a + "x &r" + it.getDisplayName(),
                    sell ? "&7Recibirás: &a$" + GuiUtil.money(total) : "&7Pagarás: &c$" + GuiUtil.money(total),
                    "", "&eClic para confirmar")));
        } else {
            List<String> bad = new ArrayList<>(problems);
            bad.add(0, "&7No se puede completar:");
            inv.setItem(SLOT_CONFIRM, GuiUtil.named(Material.RED_CONCRETE, "&c&l✖ NO DISPONIBLE",
                    bad.toArray(new String[0])));
        }
        inv.setItem(SLOT_BACK, GuiUtil.named(Material.ARROW, "&e« Volver", "&7Regresar a la categoría"));
        inv.setItem(SLOT_CANCEL, GuiUtil.named(Material.BARRIER, "&c✖ Cancelar", "&7Cerrar el menú"));

        h.setRing(GuiUtil.RING_36);
    }
}
