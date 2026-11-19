package mx.network.supershop.gui;

import mx.network.supershop.shop.ShopCategory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Marca un inventario como el menú de una categoría, y recuerda en qué página está. */
public class CategoryHolder implements InventoryHolder {

    private Inventory inventory;
    private final ShopCategory category;
    private int page;

    public CategoryHolder(ShopCategory category, int page) {
        this.category = category;
        this.page = page;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public ShopCategory getCategory() { return category; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
}
