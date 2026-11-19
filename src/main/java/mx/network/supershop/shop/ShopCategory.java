package mx.network.supershop.shop;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class ShopCategory {

    private final String id;
    private final String displayName;
    private final Material icon;
    private final Material glass;
    private final int mainMenuSlot;
    private final List<ShopItem> items = new ArrayList<>();

    public ShopCategory(String id, String displayName, Material icon, Material glass, int mainMenuSlot) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.glass = glass;
        this.mainMenuSlot = mainMenuSlot;
    }

    public void addItem(ShopItem item) { items.add(item); }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }
    public Material getGlass() { return glass; }
    public int getMainMenuSlot() { return mainMenuSlot; }
    public List<ShopItem> getItems() { return items; }
}
