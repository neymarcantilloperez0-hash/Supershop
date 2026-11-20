package mx.network.supershop.gui;

import mx.network.supershop.shop.ShopCategory;
import mx.network.supershop.shop.ShopItem;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Holder del menú de una categoría (ítems paginados + filtro activo). */
public class BrowseHolder extends ShopHolder {

    private final ShopCategory category;
    private final List<ShopFilter> filters;
    private int filterIndex;
    private int page;
    private ItemStack[] pageIcons = new ItemStack[0];
    private int revealRow;
    private long sellArmedUntil;

    public BrowseHolder(ShopCategory category, List<ShopFilter> filters) {
        this.category = category;
        this.filters = filters;
    }

    public ShopCategory getCategory() {
        return category;
    }

    public List<ShopFilter> getFilters() {
        return filters;
    }

    public boolean hasFilters() {
        return filters.size() > 1;
    }

    public int getFilterIndex() {
        return filterIndex;
    }

    public void setFilterIndex(int idx) {
        this.filterIndex = filters.isEmpty() ? 0 : Math.max(0, Math.min(idx, filters.size() - 1));
    }

    public ShopFilter getFilter() {
        return filters.isEmpty() ? null : filters.get(filterIndex);
    }

    /** Ítems que se ven ahora mismo (los del filtro activo, o toda la categoría si no hay filtros). */
    public List<ShopItem> getVisible() {
        return filters.isEmpty() ? category.getItems() : filters.get(filterIndex).getItems();
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, page);
    }

    public ItemStack[] getPageIcons() {
        return pageIcons;
    }

    public void setPageIcons(ItemStack[] pageIcons) {
        this.pageIcons = pageIcons;
    }

    public int getRevealRow() {
        return revealRow;
    }

    public void setRevealRow(int revealRow) {
        this.revealRow = revealRow;
    }

    public long getSellArmedUntil() {
        return sellArmedUntil;
    }

    public void setSellArmedUntil(long sellArmedUntil) {
        this.sellArmedUntil = sellArmedUntil;
    }
}
