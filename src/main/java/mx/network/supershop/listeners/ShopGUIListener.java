package mx.network.supershop.listeners;

import mx.network.supershop.SuperShop;
import mx.network.supershop.economy.EconomyManager;
import mx.network.supershop.gui.BrowseHolder;
import mx.network.supershop.gui.CategoryGUI;
import mx.network.supershop.gui.FilterGUI;
import mx.network.supershop.gui.FilterHolder;
import mx.network.supershop.gui.GuiUtil;
import mx.network.supershop.gui.MainHolder;
import mx.network.supershop.gui.MenuAnimator;
import mx.network.supershop.gui.PurchaseGUI;
import mx.network.supershop.gui.PurchaseHolder;
import mx.network.supershop.gui.ShopHolder;
import mx.network.supershop.shop.ShopCategory;
import mx.network.supershop.shop.ShopItem;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

public class ShopGUIListener implements Listener {

    private static final long SELL_ALL_CONFIRM_MS = 5000L;

    private final SuperShop plugin;
    private final PurchaseGUI purchaseGUI;
    private final FilterGUI filterGUI;

    public ShopGUIListener(SuperShop plugin) {
        this.plugin = plugin;
        this.purchaseGUI = new PurchaseGUI(plugin);
        this.filterGUI = new FilterGUI(plugin);
        MenuAnimator.start(plugin);
    }

    private boolean isOurs(InventoryHolder h) {
        return h instanceof ShopHolder;
    }

    // ------------------------------------------------------------------
    //  Apertura / cierre (animaciones)
    // ------------------------------------------------------------------

    @EventHandler
    public void onOpen(InventoryOpenEvent e) {
        InventoryHolder h = e.getInventory().getHolder();
        if (!(h instanceof ShopHolder) || !(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();
        MenuAnimator.track(p, (ShopHolder) h);
        if (h instanceof MainHolder || h instanceof BrowseHolder) sound(p, "open");
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player) {
            MenuAnimator.untrack((Player) e.getPlayer(), e.getInventory());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        MenuAnimator.untrack(e.getPlayer(), null);
    }

    // ------------------------------------------------------------------
    //  Clics
    // ------------------------------------------------------------------

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (isOurs(e.getView().getTopInventory().getHolder())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!isOurs(holder)) return;

        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (e.getClickedInventory() == null || e.getClickedInventory() != top) return;

        int slot = e.getRawSlot();
        ClickType click = e.getClick();

        if (holder instanceof MainHolder) {
            handleMainMenuClick(p, slot);
        } else if (holder instanceof BrowseHolder) {
            handleBrowseClick(p, (BrowseHolder) holder, slot, click);
        } else if (holder instanceof FilterHolder) {
            handleFilterClick(p, (FilterHolder) holder, slot);
        } else if (holder instanceof PurchaseHolder) {
            handlePurchaseClick(p, (PurchaseHolder) holder, slot);
        }
    }

    // ------------------------------------------------------------------
    //  Menú principal
    // ------------------------------------------------------------------

    private void handleMainMenuClick(Player p, int slot) {
        FileConfiguration cfg = plugin.getConfig();
        if (slot == cfg.getInt("gui.main-menu.close-slot", 53)) {
            sound(p, "click");
            p.closeInventory();
            return;
        }
        for (ShopCategory cat : plugin.getShopManager().getCategories().values()) {
            if (cat.getMainMenuSlot() == slot) {
                if (GuiUtil.categoryHidden(plugin, cat)) continue;
                if (!GuiUtil.categoryEnabled(plugin, cat)) {
                    msg(p, "category-disabled", "&cEsa categoría está deshabilitada por el momento.");
                    sound(p, "error");
                    return;
                }
                sound(p, "click");
                p.openInventory(plugin.getCategoryGUI().build(p, cat, 0));
                return;
            }
        }
    }

    // ------------------------------------------------------------------
    //  Menú de categoría
    // ------------------------------------------------------------------

    private void handleBrowseClick(Player p, BrowseHolder h, int slot, ClickType click) {
        FileConfiguration cfg = plugin.getConfig();
        CategoryGUI gui = plugin.getCategoryGUI();
        ShopCategory cat = h.getCategory();
        List<ShopItem> visible = h.getVisible();
        int page = h.getPage();
        int pages = CategoryGUI.pageCount(visible.size());
        String base = "gui.category-menu.";

        // Si el admin deshabilitó la categoría mientras el menú estaba abierto
        if (!GuiUtil.categoryEnabled(plugin, cat)) {
            msg(p, "category-disabled", "&cEsa categoría está deshabilitada por el momento.");
            sound(p, "error");
            p.closeInventory();
            return;
        }

        if (slot == cfg.getInt(base + "back-to-main-slot", 46)) {
            sound(p, "click");
            p.openInventory(plugin.getMainMenuGUI().build(p));
            return;
        }
        if (slot == cfg.getInt(base + "close-slot", 53)) {
            sound(p, "click");
            p.closeInventory();
            return;
        }

        // Filtro: clic = elegir, clic der. = siguiente, shift = ver todo
        if (slot == cfg.getInt(base + "filter-slot", 47)) {
            if (!h.hasFilters()) return;
            if (click.isShiftClick()) {
                h.setFilterIndex(0);
                h.setPage(0);
                sound(p, "page-turn");
                gui.render(h, p, true);
            } else if (click.isRightClick()) {
                h.setFilterIndex((h.getFilterIndex() + 1) % h.getFilters().size());
                h.setPage(0);
                sound(p, "page-turn");
                gui.render(h, p, true);
            } else {
                sound(p, "click");
                p.openInventory(filterGUI.build(p, h));
            }
            return;
        }

        // Las flechas SOLO funcionan si realmente hay otra página (arregla el bug de páginas infinitas)
        int jump = Math.max(2, cfg.getInt(base + "page-jump", 5));
        if (slot == cfg.getInt(base + "previous-page-slot", 48)) {
            if (page > 0) {
                int np = click.isShiftClick() ? 0 : (click.isRightClick() ? page - jump : page - 1);
                h.setPage(Math.max(0, np));
                sound(p, "page-turn");
                gui.render(h, p, true);
            }
            return;
        }
        if (slot == cfg.getInt(base + "next-page-slot", 51)) {
            if (page < pages - 1) {
                int np = click.isShiftClick() ? pages - 1 : (click.isRightClick() ? page + jump : page + 1);
                h.setPage(Math.min(pages - 1, np));
                sound(p, "page-turn");
                gui.render(h, p, true);
            }
            return;
        }

        // Vender todo: pide confirmar con un segundo clic
        if (slot == cfg.getInt(base + "sell-all-slot", 50)) {
            long now = System.currentTimeMillis();
            if (now > h.getSellArmedUntil()) {
                if (gui.estimateSellAll(p, visible) <= 0) {
                    msg(p, "nothing-to-sell", "&cNo tienes nada de esta categoría para vender.");
                    sound(p, "error");
                    return;
                }
                h.setSellArmedUntil(now + SELL_ALL_CONFIRM_MS);
                sound(p, "click");
                gui.render(h, p, false);
                return;
            }
            h.setSellArmedUntil(0L);
            sellAllOfList(p, visible);
            gui.render(h, p, false);
            return;
        }

        // Ítems
        int pos = CategoryGUI.indexOfSlot(slot);
        if (pos < 0) return;
        if (h.getInventory().getItem(slot) == null) return; // todavía no apareció (animación) o slot vacío
        int idx = page * CategoryGUI.ITEM_SLOTS.length + pos;
        if (idx >= visible.size()) return;
        ShopItem it = visible.get(idx);
        int fi = h.getFilterIndex();

        if (click.isShiftClick()) {
            // vender todo, con menú de confirmación
            if (!it.canSell()) {
                msg(p, "no-sell", "&cEste ítem no se puede vender.");
                sound(p, "error");
                return;
            }
            int have = GuiUtil.count(p, it);
            if (have <= 0) {
                msg(p, "not-enough-items", "&cNo tienes suficientes &f{item} &cpara vender.", "item", it.getDisplayName());
                sound(p, "error");
                return;
            }
            sound(p, "click");
            p.openInventory(purchaseGUI.build(p, cat, it, true, Math.min(have, purchaseGUI.maxAmount()), fi, page));
        } else if (click.isRightClick()) {
            if (!it.canSell()) {
                msg(p, "no-sell", "&cEste ítem no se puede vender.");
                sound(p, "error");
                return;
            }
            if (GuiUtil.count(p, it) <= 0) {
                msg(p, "not-enough-items", "&cNo tienes suficientes &f{item} &cpara vender.", "item", it.getDisplayName());
                sound(p, "error");
                return;
            }
            sound(p, "click");
            p.openInventory(purchaseGUI.build(p, cat, it, true, 1, fi, page));
        } else if (click.isLeftClick()) {
            if (!it.canBuy()) {
                msg(p, "no-buy", "&cEste ítem no se puede comprar.");
                sound(p, "error");
                return;
            }
            sound(p, "click");
            p.openInventory(purchaseGUI.build(p, cat, it, false, 1, fi, page));
        }
    }

    // ------------------------------------------------------------------
    //  Menú de filtros
    // ------------------------------------------------------------------

    private void handleFilterClick(Player p, FilterHolder fh, int slot) {
        if (slot == FilterGUI.SLOT_BACK) {
            sound(p, "click");
            p.openInventory(plugin.getCategoryGUI().build(p, fh.getCategory(), fh.getFilters(),
                    fh.getSelected(), fh.getReturnPage()));
            return;
        }
        int idx = FilterGUI.indexOfSlot(slot);
        if (idx < 0 || idx >= fh.getFilters().size()) return;
        sound(p, "page-turn");
        p.openInventory(plugin.getCategoryGUI().build(p, fh.getCategory(), fh.getFilters(), idx, 0));
    }

    // ------------------------------------------------------------------
    //  Menú de compra / venta
    // ------------------------------------------------------------------

    private void handlePurchaseClick(Player p, PurchaseHolder ph, int slot) {
        int a = ph.getAmount();
        int limit = purchaseGUI.limitFor(p, ph);

        switch (slot) {
            case PurchaseGUI.SLOT_M64: a -= 64; break;
            case PurchaseGUI.SLOT_M10: a -= 10; break;
            case PurchaseGUI.SLOT_M1: a -= 1; break;
            case PurchaseGUI.SLOT_P1: a += 1; break;
            case PurchaseGUI.SLOT_P10: a += 10; break;
            case PurchaseGUI.SLOT_P64: a += 64; break;
            case PurchaseGUI.SLOT_RESET: a = 1; break;
            case PurchaseGUI.SLOT_STACK1: a = ph.getItem().getMaterial().getMaxStackSize(); break;
            case PurchaseGUI.SLOT_STACK9: a = 9 * ph.getItem().getMaterial().getMaxStackSize(); break;
            case PurchaseGUI.SLOT_STACK27: a = 27 * ph.getItem().getMaterial().getMaxStackSize(); break;
            case PurchaseGUI.SLOT_STACK36: a = 36 * ph.getItem().getMaterial().getMaxStackSize(); break;
            case PurchaseGUI.SLOT_MAX: a = purchaseGUI.maxFor(p, ph); break;
            case PurchaseGUI.SLOT_BACK:
                sound(p, "click");
                p.openInventory(plugin.getCategoryGUI().build(p, ph.getCategory(), ph.getReturnFilter(), ph.getReturnPage()));
                return;
            case PurchaseGUI.SLOT_CANCEL:
                sound(p, "click");
                p.closeInventory();
                return;
            case PurchaseGUI.SLOT_CONFIRM:
                if (ph.isSell()) {
                    confirmSell(p, ph);
                } else {
                    confirmBuy(p, ph);
                }
                return;
            default:
                return;
        }

        a = Math.max(1, Math.min(a, limit));
        ph.setAmount(a);
        // el tono sube con la cantidad: se "siente" cuánto estás comprando
        float pitch = 0.6f + 1.4f * Math.min(1f, a / (float) Math.max(1, Math.min(limit, 256)));
        sound(p, "amount", pitch);
        purchaseGUI.render(ph.getInventory(), ph, p);
    }

    private void confirmBuy(Player p, PurchaseHolder ph) {
        ShopItem it = ph.getItem();
        int a = ph.getAmount();
        Economy econ = EconomyManager.get();
        if (econ == null) {
            p.sendMessage(GuiUtil.c("&cNo hay un sistema de economía disponible."));
            sound(p, "error");
            return;
        }
        double total = GuiUtil.round2(it.getBuyPrice() * a);
        double bal = econ.getBalance(p);
        if (bal + 1e-9 < total) {
            msg(p, "not-enough-money", "&cNo tienes suficiente dinero. Te faltan &f${missing}&c.",
                    "missing", GuiUtil.money(GuiUtil.round2(total - bal)));
            sound(p, "error");
            purchaseGUI.render(ph.getInventory(), ph, p);
            return;
        }
        if (GuiUtil.freeSpace(p, it) < a) {
            msg(p, "no-space", "&cNo tienes espacio suficiente en el inventario.");
            sound(p, "error");
            purchaseGUI.render(ph.getInventory(), ph, p);
            return;
        }
        EconomyResponse r = econ.withdrawPlayer(p, total);
        if (!r.transactionSuccess()) {
            msg(p, "not-enough-money", "&cNo tienes suficiente dinero. Te faltan &f${missing}&c.",
                    "missing", GuiUtil.money(total));
            sound(p, "error");
            return;
        }
        GuiUtil.give(p, it, a);
        msg(p, "bought", "&a¡Compraste &f{amount}x {item} &apor &f${price}&a!",
                "amount", String.valueOf(a), "item", it.getDisplayName(), "price", GuiUtil.money(total));
        sound(p, "buy");
        particle(p, "buy");
        finishTransaction(p, ph, Material.LIME_STAINED_GLASS_PANE);
    }

    private void confirmSell(Player p, PurchaseHolder ph) {
        ShopItem it = ph.getItem();
        int a = ph.getAmount();
        Economy econ = EconomyManager.get();
        if (econ == null) {
            p.sendMessage(GuiUtil.c("&cNo hay un sistema de economía disponible."));
            sound(p, "error");
            return;
        }
        if (GuiUtil.count(p, it) < a) {
            msg(p, "not-enough-items", "&cNo tienes suficientes &f{item} &cpara vender.", "item", it.getDisplayName());
            sound(p, "error");
            ph.setAmount(Math.max(1, Math.min(a, purchaseGUI.limitFor(p, ph))));
            purchaseGUI.render(ph.getInventory(), ph, p);
            return;
        }
        int removed = GuiUtil.remove(p, it, a);
        double total = GuiUtil.round2(it.getSellPrice() * removed);
        econ.depositPlayer(p, total);
        msg(p, "sold", "&a¡Vendiste &f{amount}x {item} &apor &f${price}&a!",
                "amount", String.valueOf(removed), "item", it.getDisplayName(), "price", GuiUtil.money(total));
        sound(p, "sell");
        particle(p, "sell");

        if (plugin.getConfig().getBoolean("purchase-menu.close-after-confirm", false)) {
            p.closeInventory();
        } else if (GuiUtil.count(p, it) <= 0) {
            // ya no queda nada que vender: volvemos a la categoría
            p.openInventory(plugin.getCategoryGUI().build(p, ph.getCategory(), ph.getReturnFilter(), ph.getReturnPage()));
        } else {
            finishTransaction(p, ph, Material.ORANGE_STAINED_GLASS_PANE);
        }
    }

    /** Después de una compra/venta: cierra o repinta el menú con un destello en el marco. */
    private void finishTransaction(Player p, PurchaseHolder ph, Material flashGlass) {
        if (plugin.getConfig().getBoolean("purchase-menu.close-after-confirm", false)) {
            p.closeInventory();
            return;
        }
        ph.setAmount(Math.max(1, Math.min(ph.getAmount(), purchaseGUI.limitFor(p, ph))));
        purchaseGUI.render(ph.getInventory(), ph, p);
        ph.flash(flashGlass, 12);
    }

    private void sellAllOfList(Player p, List<ShopItem> items) {
        Economy econ = EconomyManager.get();
        if (econ == null) {
            p.sendMessage(GuiUtil.c("&cNo hay un sistema de economía disponible."));
            sound(p, "error");
            return;
        }
        int totalItems = 0;
        double total = 0;
        for (ShopItem it : items) {
            if (!it.canSell()) continue;
            int have = GuiUtil.count(p, it);
            if (have <= 0) continue;
            int removed = GuiUtil.remove(p, it, have);
            totalItems += removed;
            total += removed * it.getSellPrice();
        }
        if (totalItems <= 0) {
            msg(p, "nothing-to-sell", "&cNo tienes nada de esta categoría para vender.");
            sound(p, "error");
            return;
        }
        total = GuiUtil.round2(total);
        econ.depositPlayer(p, total);
        msg(p, "sold-all", "&a¡Vendiste &f{amount} ítems &apor &f${price}&a!",
                "amount", String.valueOf(totalItems), "price", GuiUtil.money(total));
        sound(p, "sell");
        particle(p, "sell");
    }

    // ------------------------------------------------------------------
    //  Helpers: mensajes, sonidos, partículas
    // ------------------------------------------------------------------

    private void msg(Player p, String key, String def, String... kv) {
        FileConfiguration cfg = plugin.getConfig();
        String m = cfg.getString("messages." + key, def);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m = m.replace("{" + kv[i] + "}", kv[i + 1]);
        }
        String prefix = cfg.getString("messages.prefix", "&8[&6Shop&8] &r");
        p.sendMessage(GuiUtil.c(prefix + m));
    }

    private void sound(Player p, String key) {
        sound(p, key, 1.0f);
    }

    private void sound(Player p, String key, float pitch) {
        String name = plugin.getConfig().getString("sounds." + key);
        if (name == null || name.isEmpty()) return;
        try {
            Sound s = Sound.valueOf(name.toUpperCase());
            p.playSound(p.getLocation(), s, 1.0f, Math.max(0.5f, Math.min(2.0f, pitch)));
        } catch (IllegalArgumentException ignored) {
            // sonido inválido en config.yml
        }
    }

    private void particle(Player p, String key) {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("particles.enabled", true)) return;
        String configured = cfg.getString("particles." + key, "HAPPY_VILLAGER");
        Particle part = null;
        for (String cand : new String[]{configured, "HAPPY_VILLAGER", "VILLAGER_HAPPY"}) {
            try {
                part = Particle.valueOf(cand.toUpperCase());
                break;
            } catch (IllegalArgumentException ignored) {
                // probamos el siguiente nombre (cambia según versión)
            }
        }
        if (part != null) {
            p.spawnParticle(part, p.getLocation().add(0, 1, 0), 12, 0.4, 0.5, 0.4, 0.0);
        }
    }
}
