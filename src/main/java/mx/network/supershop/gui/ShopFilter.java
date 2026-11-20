package mx.network.supershop.gui;

import mx.network.supershop.SuperShop;
import mx.network.supershop.shop.ShopCategory;
import mx.network.supershop.shop.ShopItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Filtro dentro de una categoría (por ejemplo "Madera" o "Lana y terracota" en Bloques).
 * Se define en config.yml -> category-filters.
 */
public final class ShopFilter {

    public static final int MAX_FILTERS = 14;

    private final String id;
    private final String name;
    private final Material icon;
    private final List<Pattern> include = new ArrayList<>();
    private final List<Pattern> exclude = new ArrayList<>();
    private final List<ShopItem> items = new ArrayList<>();

    private ShopFilter(String id, String name, Material icon) {
        this.id = id;
        this.name = name;
        this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Material getIcon() {
        return icon;
    }

    public List<ShopItem> getItems() {
        return items;
    }

    private boolean matches(String materialName) {
        if (!include.isEmpty()) {
            boolean ok = false;
            for (Pattern p : include) {
                if (p.matcher(materialName).find()) {
                    ok = true;
                    break;
                }
            }
            if (!ok) return false;
        }
        for (Pattern p : exclude) {
            if (p.matcher(materialName).find()) return false;
        }
        return true;
    }

    private static Pattern compile(SuperShop plugin, String filterId, String regex) {
        try {
            return Pattern.compile(regex);
        } catch (PatternSyntaxException ex) {
            plugin.getLogger().warning("Filtro '" + filterId + "': expresión inválida '" + regex + "' (" + ex.getDescription() + ")");
            return null;
        }
    }

    /**
     * Carga los filtros de una categoría. Solo devuelve filtros que tengan ítems.
     * Si no hay al menos dos filtros útiles devuelve una lista vacía (no se muestra el botón de filtro).
     */
    public static List<ShopFilter> load(SuperShop plugin, ShopCategory cat) {
        List<ShopFilter> out = new ArrayList<>();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection(
                "category-filters." + GuiUtil.categoryKey(cat));
        if (root == null) return out;

        List<ShopItem> all = cat.getItems();
        for (String id : root.getKeys(false)) {
            if (out.size() >= MAX_FILTERS) break;
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            ShopFilter f = new ShopFilter(id, s.getString("name", id),
                    GuiUtil.mat(s.getString("icon"), Material.HOPPER));
            for (String rx : s.getStringList("match")) {
                Pattern p = compile(plugin, id, rx);
                if (p != null) f.include.add(p);
            }
            for (String rx : s.getStringList("exclude")) {
                Pattern p = compile(plugin, id, rx);
                if (p != null) f.exclude.add(p);
            }
            for (ShopItem it : all) {
                if (f.matches(it.getMaterial().name())) f.items.add(it);
            }
            if (!f.items.isEmpty()) out.add(f);
        }
        if (out.size() < 2) out.clear();
        return out;
    }
}
