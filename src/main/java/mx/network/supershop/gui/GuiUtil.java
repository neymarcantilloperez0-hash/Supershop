package mx.network.supershop.gui;

import mx.network.supershop.SuperShop;
import mx.network.supershop.shop.ShopCategory;
import mx.network.supershop.shop.ShopItem;
import mx.network.supershop.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utilidades compartidas por los menús y el listener. */
public final class GuiUtil {

    private static final DecimalFormat FMT =
            new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));

    private GuiUtil() {}

    public static String money(double v) {
        return FMT.format(v);
    }

    public static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public static String c(String s) {
        return ItemBuilder.color(s == null ? "" : s);
    }

    public static Material mat(String name, Material def) {
        if (name == null) return def;
        Material m = Material.matchMaterial(name.trim());
        return m != null ? m : def;
    }

    public static ItemStack pane(Material m) {
        return named(m, " ");
    }

    /** Ítem con nombre y lore (sin cursiva, sin atributos visibles). */
    public static ItemStack named(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;
        meta.setDisplayName(c("&r" + name));
        if (lore.length > 0) {
            List<String> l = new ArrayList<>();
            for (String line : lore) l.add(c("&r" + line));
            meta.setLore(l);
        }
        meta.addItemFlags(ItemFlag.values());
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack named(Material m, int amount, String name, String... lore) {
        ItemStack it = named(m, name, lore);
        it.setAmount(Math.max(1, Math.min(64, amount)));
        return it;
    }

    public static ItemStack glow(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;
        meta.addEnchant(Enchantment.LURE, 1, true);
        meta.addItemFlags(ItemFlag.values());
        it.setItemMeta(meta);
        return it;
    }

    // ------------------------------------------------------------------
    //  Ítems físicos
    // ------------------------------------------------------------------

    public static ItemStack physical(ShopItem si, int qty) {
        ItemStack s = new ItemStack(si.getMaterial(), qty);
        if (si.getSpawnerEntity() != null && si.getMaterial() == Material.SPAWNER) {
            ItemMeta meta = s.getItemMeta();
            if (meta instanceof BlockStateMeta) {
                BlockStateMeta bsm = (BlockStateMeta) meta;
                BlockState st = bsm.getBlockState();
                if (st instanceof CreatureSpawner) {
                    CreatureSpawner cs = (CreatureSpawner) st;
                    cs.setSpawnedType(si.getSpawnerEntity());
                    bsm.setBlockState(cs);
                }
                bsm.setDisplayName(c(si.getDisplayName()));
                s.setItemMeta(bsm);
            }
        }
        return s;
    }

    /** ¿Este stack del jugador corresponde a este ítem de la tienda? */
    public static boolean matches(ItemStack s, ShopItem si) {
        if (s == null || s.getType() != si.getMaterial()) return false;
        ItemMeta meta = s.getItemMeta();
        if (si.getSpawnerEntity() != null) {
            if (meta instanceof BlockStateMeta) {
                BlockState st = ((BlockStateMeta) meta).getBlockState();
                if (st instanceof CreatureSpawner) {
                    return ((CreatureSpawner) st).getSpawnedType() == si.getSpawnerEntity();
                }
            }
            return false;
        }
        if (meta == null) return true;
        // No se venden ítems encantados, renombrados o con lore (evita vender cosas especiales)
        return !meta.hasEnchants() && !meta.hasDisplayName() && !meta.hasLore();
    }

    public static int count(Player p, ShopItem si) {
        int n = 0;
        for (ItemStack s : p.getInventory().getStorageContents()) {
            if (matches(s, si)) n += s.getAmount();
        }
        return n;
    }

    /** Quita hasta {@code amount} unidades; devuelve cuántas quitó realmente. */
    public static int remove(Player p, ShopItem si, int amount) {
        PlayerInventory inv = p.getInventory();
        ItemStack[] contents = inv.getStorageContents();
        int remaining = amount;
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack s = contents[i];
            if (!matches(s, si)) continue;
            int take = Math.min(remaining, s.getAmount());
            if (take >= s.getAmount()) {
                contents[i] = null;
            } else {
                s.setAmount(s.getAmount() - take);
            }
            remaining -= take;
        }
        inv.setStorageContents(contents);
        return amount - remaining;
    }

    /** Cuántas unidades de este ítem caben en el inventario. */
    public static int freeSpace(Player p, ShopItem si) {
        int max = Math.max(1, si.getMaterial().getMaxStackSize());
        ItemStack sample = physical(si, 1);
        int free = 0;
        for (ItemStack s : p.getInventory().getStorageContents()) {
            if (s == null || s.getType() == Material.AIR) {
                free += max;
            } else if (s.isSimilar(sample)) {
                free += Math.max(0, max - s.getAmount());
            }
        }
        return free;
    }

    public static void give(Player p, ShopItem si, int amount) {
        int max = Math.max(1, si.getMaterial().getMaxStackSize());
        int remaining = amount;
        while (remaining > 0) {
            int n = Math.min(max, remaining);
            Map<Integer, ItemStack> left = p.getInventory().addItem(physical(si, n));
            for (ItemStack l : left.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), l);
            }
            remaining -= n;
        }
    }

    // ------------------------------------------------------------------
    //  Anillos de los marcos (slots en el orden en que se anima el brillo)
    // ------------------------------------------------------------------

    /** Borde exterior de un inventario de 54 slots, en sentido horario. */
    public static final int[] RING_54 = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            17, 26, 35, 44,
            53, 52, 51, 50, 49, 48, 47, 46, 45,
            36, 27, 18, 9};

    /** Borde exterior de un inventario de 36 slots, en sentido horario. */
    public static final int[] RING_36 = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            17, 26,
            35, 34, 33, 32, 31, 30, 29, 28, 27,
            18, 9};

    // ------------------------------------------------------------------
    //  Textos
    // ------------------------------------------------------------------

    /** Clave de una categoría para config.yml: primera palabra del nombre, sin colores ni tildes, en minúsculas. */
    public static String categoryKey(ShopCategory cat) {
        String plain = ChatColor.stripColor(c(cat.getDisplayName()));
        if (plain == null) return "";
        String n = Normalizer.normalize(plain, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
        String[] parts = n.split("[^a-z0-9]+");
        for (String part : parts) {
            if (!part.isEmpty()) return part;
        }
        return "";
    }

    // ------------------------------------------------------------------
    //  Iconos de categoría (cabezas) y estado (habilitada / oculta)
    // ------------------------------------------------------------------

    private static final Pattern TEXTURE_URL = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern HASH = Pattern.compile("[0-9a-fA-F]{20,64}");

    /** ¿La categoría está habilitada? (config.yml -> category-settings.<clave>.enabled; por defecto sí) */
    public static boolean categoryEnabled(SuperShop plugin, ShopCategory cat) {
        return plugin.getConfig().getBoolean("category-settings." + categoryKey(cat) + ".enabled", true);
    }

    /** ¿La categoría se oculta del menú principal? (category-settings.<clave>.hidden) */
    public static boolean categoryHidden(SuperShop plugin, ShopCategory cat) {
        return plugin.getConfig().getBoolean("category-settings." + categoryKey(cat) + ".hidden", false);
    }

    /**
     * Icono de una categoría: cabeza con textura si hay una en config.yml (category-icons.<clave>.texture),
     * si no el ítem configurado en category-icons.<clave>.material, y si no el icono de shop.yml.
     */
    public static ItemStack categoryIcon(SuperShop plugin, ShopCategory cat, String name, String... lore) {
        Material fallback = cat.getIcon();
        String texture = null;
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("category-icons." + categoryKey(cat));
        if (s != null) {
            fallback = mat(s.getString("material"), fallback);
            texture = s.getString("texture");
        }
        if (texture != null && !texture.trim().isEmpty()) {
            ItemStack head = named(Material.PLAYER_HEAD, name, lore);
            ItemMeta meta = head.getItemMeta();
            if (meta instanceof SkullMeta && applyTexture(plugin, (SkullMeta) meta, texture.trim())) {
                head.setItemMeta(meta);
                return head;
            }
        }
        return named(fallback, name, lore);
    }

    /** Pone la textura (hash, URL o base64 de minecraft-heads) a una cabeza. false si no se pudo. */
    private static boolean applyTexture(SuperShop plugin, SkullMeta meta, String raw) {
        try {
            String url;
            if (raw.startsWith("http://") || raw.startsWith("https://")) {
                url = raw;
            } else if (HASH.matcher(raw).matches()) {
                url = "http://textures.minecraft.net/texture/" + raw;
            } else {
                String json = new String(Base64.getDecoder().decode(raw), StandardCharsets.UTF_8);
                Matcher m = TEXTURE_URL.matcher(json);
                if (!m.find()) return false;
                url = m.group(1);
            }
            PlayerProfile profile = Bukkit.createPlayerProfile(
                    UUID.nameUUIDFromBytes(("supershop:" + url).getBytes(StandardCharsets.UTF_8)), "SuperShop");
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(url));
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            return true;
        } catch (Exception ex) {
            plugin.getLogger().warning("Textura de cabeza inválida en category-icons: " + raw);
            return false;
        }
    }

    /** Título de inventario seguro (máx. 32 caracteres, por compatibilidad con versiones viejas). */
    public static String title(String raw) {
        String col = c(raw);
        if (col.length() <= 32) return col;
        String plain = ChatColor.stripColor(col);
        if (plain == null) plain = "";
        if (plain.length() > 31) plain = plain.substring(0, 30) + "…";
        return ChatColor.DARK_GRAY + plain;
    }

    public static String plain(String raw) {
        String p = ChatColor.stripColor(c(raw));
        return p == null ? "" : p;
    }

    /** Barra de progreso de texto: &a█████&8█████ */
    public static String bar(double ratio, int len) {
        double r = Double.isNaN(ratio) ? 0 : Math.max(0, Math.min(1, ratio));
        int filled = (int) Math.round(r * len);
        StringBuilder sb = new StringBuilder("&a");
        for (int i = 0; i < filled; i++) sb.append('█');
        sb.append("&8");
        for (int i = filled; i < len; i++) sb.append('█');
        return sb.toString();
    }
}
