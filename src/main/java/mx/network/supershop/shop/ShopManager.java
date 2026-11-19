package mx.network.supershop.shop;

import mx.network.supershop.SuperShop;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Carga shop.yml en memoria. /shop reload vuelve a llamar a load().
 * Usamos LinkedHashMap para mantener el orden del yml (afecta el orden
 * en el que se listan las categorías y los ítems dentro del GUI).
 */
public class ShopManager {

    private final SuperShop plugin;
    private final Map<String, ShopCategory> categories = new LinkedHashMap<>();
    private File shopFile;
    private FileConfiguration shopConfig;

    public ShopManager(SuperShop plugin) {
        this.plugin = plugin;
    }

    public void load() {
        categories.clear();

        shopFile = new File(plugin.getDataFolder(), "shop.yml");
        if (!shopFile.exists()) {
            plugin.saveResource("shop.yml", false);
        }
        shopConfig = YamlConfiguration.loadConfiguration(shopFile);

        ConfigurationSection categoriesSection = shopConfig.getConfigurationSection("categories");
        if (categoriesSection == null) {
            plugin.getLogger().warning("shop.yml no tiene ninguna categoría configurada.");
            return;
        }

        for (String catId : categoriesSection.getKeys(false)) {
            ConfigurationSection catSection = categoriesSection.getConfigurationSection(catId);
            if (catSection == null) continue;

            String displayName = catSection.getString("display-name", "&f" + catId);
            Material icon = parseMaterial(catSection.getString("icon", "CHEST"), Material.CHEST);
            Material glass = parseMaterial(catSection.getString("glass", "BLACK_STAINED_GLASS_PANE"), Material.BLACK_STAINED_GLASS_PANE);
            int slot = catSection.getInt("main-menu-slot", 0);

            ShopCategory category = new ShopCategory(catId, displayName, icon, glass, slot);

            ConfigurationSection itemsSection = catSection.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String itemId : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
                    if (itemSection == null) continue;

                    Material material = parseMaterial(itemSection.getString("material"), Material.STONE);
                    String name = itemSection.getString("name", "&f" + itemId);
                    double buy = itemSection.getDouble("buy", -1);
                    double sell = itemSection.getDouble("sell", -1);

                    EntityType spawnerEntity = null;
                    if (material == Material.SPAWNER && itemSection.contains("entity")) {
                        try {
                            spawnerEntity = EntityType.valueOf(itemSection.getString("entity"));
                        } catch (IllegalArgumentException ex) {
                            plugin.getLogger().warning("Entidad inválida para el spawner " + itemId + ": " + itemSection.getString("entity"));
                        }
                    }

                    category.addItem(new ShopItem(itemId, material, name, buy, sell, spawnerEntity));
                }
            }

            categories.put(catId, category);
        }

        plugin.getLogger().info("Cargadas " + categories.size() + " categorías de la tienda.");
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        try {
            return Material.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().log(Level.WARNING, "Material inválido en shop.yml: " + name + ", usando " + fallback);
            return fallback;
        }
    }

    public Map<String, ShopCategory> getCategories() {
        return categories;
    }

    public ShopCategory getCategory(String id) {
        return categories.get(id);
    }
}
