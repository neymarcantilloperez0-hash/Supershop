package mx.network.supershop.gui;

import mx.network.supershop.shop.ShopCategory;
import mx.network.supershop.shop.ShopItem;

/** Holder del menú de compra/venta con selector de cantidad. */
public class PurchaseHolder extends ShopHolder {

    private final ShopCategory category;
    private final ShopItem item;
    private final boolean sell;
    private final int returnFilter;
    private final int returnPage;
    private int amount;

    public PurchaseHolder(ShopCategory category, ShopItem item, boolean sell, int amount, int returnFilter, int returnPage) {
        this.category = category;
        this.item = item;
        this.sell = sell;
        this.amount = amount;
        this.returnFilter = returnFilter;
        this.returnPage = returnPage;
    }

    public ShopCategory getCategory() {
        return category;
    }

    public ShopItem getItem() {
        return item;
    }

    public boolean isSell() {
        return sell;
    }

    public int getReturnFilter() {
        return returnFilter;
    }

    public int getReturnPage() {
        return returnPage;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
