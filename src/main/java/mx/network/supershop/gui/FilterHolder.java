package mx.network.supershop.gui;

import mx.network.supershop.shop.ShopCategory;

import java.util.List;

/** Holder del menú donde se elige el filtro de una categoría. */
public class FilterHolder extends ShopHolder {

    private final ShopCategory category;
    private final List<ShopFilter> filters;
    private final int selected;
    private final int returnPage;

    public FilterHolder(ShopCategory category, List<ShopFilter> filters, int selected, int returnPage) {
        this.category = category;
        this.filters = filters;
        this.selected = selected;
        this.returnPage = returnPage;
    }

    public ShopCategory getCategory() {
        return category;
    }

    public List<ShopFilter> getFilters() {
        return filters;
    }

    public int getSelected() {
        return selected;
    }

    public int getReturnPage() {
        return returnPage;
    }
}
