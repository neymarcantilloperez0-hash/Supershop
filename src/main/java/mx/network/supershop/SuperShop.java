package mx.network.supershop;

import mx.network.supershop.commands.ShopCommand;
import mx.network.supershop.economy.EconomyManager;
import mx.network.supershop.gui.CategoryGUI;
import mx.network.supershop.gui.MainMenuGUI;
import mx.network.supershop.gui.PurchaseGUI;
import mx.network.supershop.listeners.ShopGUIListener;
import mx.network.supershop.placeholders.ShopPlaceholders;
import mx.network.supershop.shop.ShopManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SuperShop extends JavaPlugin {

    private ShopManager shopManager;
    private MainMenuGUI mainMenuGUI;
    private CategoryGUI categoryGUI;
    private PurchaseGUI purchaseGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!EconomyManager.setup(this)) {
            getLogger().severe("No se encontró Vault o un plugin de economía compatible. Desactivando SuperShop...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.shopManager = new ShopManager(this);
        this.shopManager.load();

        this.mainMenuGUI = new MainMenuGUI(this);
        this.categoryGUI = new CategoryGUI(this);
        this.purchaseGUI = new PurchaseGUI(this);

        getCommand("shop").setExecutor(new ShopCommand(this));
        getServer().getPluginManager().registerEvents(new ShopGUIListener(this), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ShopPlaceholders(this).register();
            getLogger().info("PlaceholderAPI detectado. Placeholder %shop_balance% registrado.");
        }

        getLogger().info("SuperShop habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SuperShop deshabilitado.");
    }

    public ShopManager getShopManager() { return shopManager; }
    public MainMenuGUI getMainMenuGUI() { return mainMenuGUI; }
    public CategoryGUI getCategoryGUI() { return categoryGUI; }
    public PurchaseGUI getPurchaseGUI() { return purchaseGUI; }
}
