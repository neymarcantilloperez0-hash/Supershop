package mx.network.supershop.gui;

import mx.network.supershop.SuperShop;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Anima los menús de la tienda mientras alguien los tiene abiertos:
 *  - marco arcoíris en el menú principal,
 *  - brillo que recorre el marco en categorías, filtros y compra,
 *  - destello al confirmar una compra,
 *  - los ítems de cada página aparecen fila por fila.
 *
 * Solo toca los menús que hay abiertos (no recorre a todos los jugadores) y
 * solo manda al cliente los slots que realmente cambian.
 */
public final class MenuAnimator implements Runnable {

    private static MenuAnimator instance;
    private static BukkitTask task;
    private static final Map<UUID, ShopHolder> ACTIVE = new HashMap<>();
    private static final Map<Material, ItemStack> PANES = new EnumMap<>(Material.class);

    private final SuperShop plugin;
    private long tick;

    private boolean enabled = true;
    private boolean reveal = true;
    private int period = 3;
    private int revealDelay = 1;
    private int cometLength = 3;
    private Material comet = Material.WHITE_STAINED_GLASS_PANE;
    private List<Material> palette = defaultPalette();

    private MenuAnimator(SuperShop plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    //  Ciclo de vida
    // ------------------------------------------------------------------

    /** Arranca el animador (si ya había uno, lo reemplaza; los menús abiertos siguen animándose). */
    public static void start(SuperShop plugin) {
        if (task != null) {
            task.cancel();
            task = null;
        }
        instance = new MenuAnimator(plugin);
        instance.reloadSettings();
        task = Bukkit.getScheduler().runTaskTimer(plugin, instance, 1L, 1L);
    }

    public static void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        instance = null;
        ACTIVE.clear();
    }

    /** ¿Los ítems de una página deben aparecer fila por fila? */
    public static boolean revealEnabled() {
        return instance != null && instance.enabled && instance.reveal;
    }

    public static void track(Player p, ShopHolder h) {
        ACTIVE.put(p.getUniqueId(), h);
    }

    /** Deja de animar el menú de este jugador (si {@code closed} es null, sea cual sea). */
    public static void untrack(Player p, Inventory closed) {
        ShopHolder h = ACTIVE.get(p.getUniqueId());
        if (h != null && (closed == null || h.getInventory() == closed)) {
            ACTIVE.remove(p.getUniqueId());
        }
    }

    private static List<Material> defaultPalette() {
        List<Material> l = new ArrayList<>();
        l.add(Material.ORANGE_STAINED_GLASS_PANE);
        l.add(Material.YELLOW_STAINED_GLASS_PANE);
        l.add(Material.LIME_STAINED_GLASS_PANE);
        l.add(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        l.add(Material.PURPLE_STAINED_GLASS_PANE);
        l.add(Material.MAGENTA_STAINED_GLASS_PANE);
        return l;
    }

    private static ItemStack pane(Material m) {
        ItemStack it = PANES.get(m);
        if (it == null) {
            it = GuiUtil.pane(m);
            PANES.put(m, it);
        }
        return it;
    }

    private void reloadSettings() {
        FileConfiguration cfg = plugin.getConfig();
        enabled = cfg.getBoolean("animations.enabled", true);
        reveal = cfg.getBoolean("animations.reveal-items", true);
        period = Math.max(1, cfg.getInt("animations.period-ticks", 3));
        revealDelay = Math.max(1, cfg.getInt("animations.reveal-delay-ticks", 1));
        cometLength = Math.max(1, Math.min(8, cfg.getInt("animations.comet-length", 3)));
        comet = GuiUtil.mat(cfg.getString("animations.comet-glass"), Material.WHITE_STAINED_GLASS_PANE);
        List<Material> pal = new ArrayList<>();
        for (String name : cfg.getStringList("animations.main-palette")) {
            Material m = Material.matchMaterial(name == null ? "" : name.trim());
            if (m != null) pal.add(m);
        }
        palette = pal.isEmpty() ? defaultPalette() : pal;
    }

    // ------------------------------------------------------------------
    //  Tick
    // ------------------------------------------------------------------

    @Override
    public void run() {
        tick++;
        if (tick % 40 == 1) reloadSettings(); // /shop reload se nota en ~2 segundos
        if (ACTIVE.isEmpty()) return;

        Iterator<Map.Entry<UUID, ShopHolder>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ShopHolder> e = it.next();
            ShopHolder h = e.getValue();
            Player p = Bukkit.getPlayer(e.getKey());
            if (p == null || !p.isOnline() || h.getInventory() == null
                    || p.getOpenInventory().getTopInventory() != h.getInventory()) {
                it.remove();
                continue;
            }
            try {
                if (h instanceof BrowseHolder) stepReveal((BrowseHolder) h);
                if (enabled) stepRing(h);
            } catch (RuntimeException ex) {
                // un fallo de animación nunca debe romper la tienda
                it.remove();
            }
        }
    }

    private void stepReveal(BrowseHolder b) {
        if (b.getRevealRow() >= CategoryGUI.ROWS) return;
        if (!enabled || !reveal) {
            while (b.getRevealRow() < CategoryGUI.ROWS) revealNextRow(b);
            return;
        }
        if (tick % revealDelay == 0) revealNextRow(b);
    }

    private void revealNextRow(BrowseHolder b) {
        int row = b.getRevealRow();
        Inventory inv = b.getInventory();
        ItemStack[] icons = b.getPageIcons();
        for (int col = 0; col < CategoryGUI.COLS; col++) {
            int idx = row * CategoryGUI.COLS + col;
            if (idx < icons.length && idx < CategoryGUI.ITEM_SLOTS.length) {
                inv.setItem(CategoryGUI.ITEM_SLOTS[idx], icons[idx]);
            }
        }
        b.setRevealRow(row + 1);
    }

    private void stepRing(ShopHolder h) {
        int[] ring = h.ring();
        if (ring.length == 0) return;
        Material[] base = h.ringBase();
        Inventory inv = h.getInventory();
        int n = ring.length;

        if (h.flashTicks() > 0) {
            int ft = h.flashTicks();
            Material g = h.flashGlass();
            for (int i = 0; i < n; i++) {
                if (base[i] == null) continue;
                Material m = ((i + ft / 2) % 2 == 0) ? g : Material.WHITE_STAINED_GLASS_PANE;
                paint(h, inv, ring, i, m);
            }
            h.tickFlash();
            return;
        }

        if (tick % period != 0) return;
        h.nextFrame();
        int frame = h.frame();

        if (h.usesRainbow()) {
            int size = palette.size();
            for (int i = 0; i < n; i++) {
                if (base[i] == null) continue;
                paint(h, inv, ring, i, palette.get((i + frame) % size));
            }
            return;
        }

        int head = frame % n;
        for (int i = 0; i < n; i++) {
            if (base[i] == null) continue;
            int dist = (head - i + n) % n;
            Material m;
            if (dist == 0) m = comet;
            else if (dist < cometLength) m = Material.LIGHT_GRAY_STAINED_GLASS_PANE;
            else m = base[i];
            paint(h, inv, ring, i, m);
        }
    }

    private void paint(ShopHolder h, Inventory inv, int[] ring, int i, Material m) {
        Material[] painted = h.ringPainted();
        if (painted[i] == m) return;
        painted[i] = m;
        inv.setItem(ring[i], pane(m));
    }
}
