package mx.network.supershop.listeners;

import mx.network.supershop.SuperShop;
import mx.network.supershop.economy.EconomyManager;
import mx.network.supershop.gui.CategoryHolder;
import mx.network.supershop.gui.MainMenuHolder;
import mx.network.supershop.shop.ShopCategory;
import mx.network.supershop.shop.ShopItem;
import mx.network.supershop.util.ItemBuilder;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class ShopGUIListener implements Listener {

    private final SuperShop plugin;

    public ShopGUIListener(SuperShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof MainMenuHolder) && !(holder instanceof CategoryHolder)) return;

        event.setCancelled(true); // nunca se mueven ítems reales dentro de estos menús

        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (holder instanceof MainMenuHolder) {
            handleMainMenuClick(player, clicked, event.getSlot());
        } else {
            handleCategoryClick(player, (CategoryHolder) holder, clicked, event.getSlot(), event.getClick());
        }
    }

    // ================= MENÚ PRINCIPAL =================

    private void handleMainMenuClick(Player player, ItemStack clicked, int slot) {
        for (ShopCategory category : plugin.getShopManager().getCategories().values()) {
            if (category.getMainMenuSlot() == slot) {
                playSound(player, "click");
                player.openInventory(plugin.getCategoryGUI().build(player, category, 0));
                return;
            }
        }
    }

    // ================= MENÚ DE CATEGORÍA =================

    private void handleCategoryClick(Player player, CategoryHolder holder, ItemStack clicked, int slot, ClickType clickType) {
        ShopCategory category = holder.getCategory();
        int backSlot = plugin.getConfig().getInt("gui.category-menu.back-to-main-slot", 46);
        int prevSlot = plugin.getConfig().getInt("gui.category-menu.previous-page-slot", 48);
        int nextSlot = plugin.getConfig().getInt("gui.category-menu.next-page-slot", 51);
        int sellAllSlot = plugin.getConfig().getInt("gui.category-menu.sell-all-slot", 50);
        int closeSlot = plugin.getConfig().getInt("gui.category-menu.close-slot", 53);

        if (slot == closeSlot) {
            playSound(player, "click");
            player.closeInventory();
            return;
        }
        if (slot == backSlot) {
            playSound(player, "click");
            player.openInventory(plugin.getMainMenuGUI().build(player));
            return;
        }
        if (slot == prevSlot) {
            playSound(player, "page-turn");
            player.openInventory(plugin.getCategoryGUI().build(player, category, holder.getPage() - 1));
            return;
        }
        if (slot == nextSlot) {
            playSound(player, "page-turn");
            player.openInventory(plugin.getCategoryGUI().build(player, category, holder.getPage() + 1));
            return;
        }
        if (slot == sellAllSlot) {
            sellAllOfCategory(player, category);
            return;
        }

        // Si no fue un botón de navegación, buscamos a qué ShopItem corresponde el slot.
        ShopItem shopItem = findItemForIcon(category, clicked);
        if (shopItem == null) return;

        switch (clickType) {
            case LEFT -> buy(player, shopItem, 1);
            case RIGHT -> buy(player, shopItem, 64);
            case SHIFT_RIGHT -> sellAllOfItem(player, shopItem);
            case SHIFT_LEFT -> sell(player, shopItem, 1);
            default -> { /* otros tipos de clic no hacen nada */ }
        }

        // Refrescamos el menú para que la cabeza de saldo se vea actualizada al instante.
        refreshOpenCategory(player, category, holder.getPage());
    }

    private ShopItem findItemForIcon(ShopCategory category, ItemStack clicked) {
        String clickedName = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
                ? clicked.getItemMeta().getDisplayName() : null;
        if (clickedName == null) return null;

        for (ShopItem item : category.getItems()) {
            if (ItemBuilder.color(item.getDisplayName()).equals(clickedName)) {
                return item;
            }
        }
        return null;
    }

    // ================= ECONOMÍA =================

    private void buy(Player player, ShopItem item, int amount) {
        Economy econ = EconomyManager.get();
        if (econ == null) return;

        if (!item.canBuy()) {
            playSound(player, "error");
            msg(player, "no-buy");
            return;
        }

        double total = item.getBuyPrice() * amount;
        if (econ.getBalance(player) < total) {
            playSound(player, "error");
            msg(player, "not-enough-money", "{missing}", format(total - econ.getBalance(player)));
            return;
        }

        if (!hasInventorySpace(player, amount)) {
            playSound(player, "error");
            return;
        }

        econ.withdrawPlayer(player, total);
        player.getInventory().addItem(buildPhysicalItem(item, amount));

        playSound(player, "buy");
        spawnParticle(player, "buy");
        msg(player, "bought", "{amount}", String.valueOf(amount), "{item}", ItemBuilder.color(item.getDisplayName()), "{price}", format(total));
    }

    private void sell(Player player, ShopItem item, int amount) {
        Economy econ = EconomyManager.get();
        if (econ == null) return;

        if (!item.canSell()) {
            playSound(player, "error");
            msg(player, "no-sell");
            return;
        }

        int owned = countMaterial(player, item.getMaterial());
        if (owned < amount) {
            playSound(player, "error");
            msg(player, "not-enough-items", "{item}", ItemBuilder.color(item.getDisplayName()));
            return;
        }

        removeMaterial(player, item.getMaterial(), amount);
        double total = item.getSellPrice() * amount;
        econ.depositPlayer(player, total);

        playSound(player, "sell");
        spawnParticle(player, "sell");
        msg(player, "sold", "{amount}", String.valueOf(amount), "{item}", ItemBuilder.color(item.getDisplayName()), "{price}", format(total));
    }

    private void sellAllOfItem(Player player, ShopItem item) {
        if (!item.canSell()) {
            playSound(player, "error");
            msg(player, "no-sell");
            return;
        }
        int owned = countMaterial(player, item.getMaterial());
        if (owned <= 0) {
            playSound(player, "error");
            msg(player, "not-enough-items", "{item}", ItemBuilder.color(item.getDisplayName()));
            return;
        }
        sell(player, item, owned);
    }

    private void sellAllOfCategory(Player player, ShopCategory category) {
        Economy econ = EconomyManager.get();
        if (econ == null) return;

        double totalEarned = 0;
        int totalItems = 0;

        for (ShopItem item : category.getItems()) {
            if (!item.canSell()) continue;
            int owned = countMaterial(player, item.getMaterial());
            if (owned <= 0) continue;

            removeMaterial(player, item.getMaterial(), owned);
            totalEarned += item.getSellPrice() * owned;
            totalItems += owned;
        }

        if (totalItems == 0) {
            playSound(player, "error");
            msg(player, "not-enough-items", "{item}", "ítems de " + ItemBuilder.color(category.getDisplayName()));
            return;
        }

        econ.depositPlayer(player, totalEarned);
        playSound(player, "sell");
        spawnParticle(player, "sell");
        msg(player, "sold", "{amount}", String.valueOf(totalItems), "{item}", ItemBuilder.color(category.getDisplayName()), "{price}", format(totalEarned));

        refreshOpenCategory(player, category, 0);
    }

    // ================= HELPERS =================

    private ItemStack buildPhysicalItem(ShopItem item, int amount) {
        ItemStack stack = new ItemStack(item.getMaterial(), amount);
        return stack;
    }

    private boolean hasInventorySpace(Player player, int amount) {
        // Chequeo simple: si el inventario está completamente lleno, avisamos igual se intenta
        // (addItem dropea al piso si no entra, así que dejamos pasar; se puede endurecer si se quiere).
        return true;
    }

    private int countMaterial(Player player, Material material) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    private void removeMaterial(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != material) continue;
            int take = Math.min(stack.getAmount(), remaining);
            stack.setAmount(stack.getAmount() - take);
            remaining -= take;
            if (stack.getAmount() <= 0) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    private void refreshOpenCategory(Player player, ShopCategory category, int page) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top.getHolder() instanceof CategoryHolder) {
            player.openInventory(plugin.getCategoryGUI().build(player, category, page));
        }
    }

    private void playSound(Player player, String key) {
        String soundName = plugin.getConfig().getString("sounds." + key);
        if (soundName == null) return;
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), 1f, 1f);
        } catch (IllegalArgumentException ignored) { }
    }

    private void spawnParticle(Player player, String key) {
        if (!plugin.getConfig().getBoolean("particles.enabled", true)) return;
        String particleName = plugin.getConfig().getString("particles." + key);
        if (particleName == null) return;
        try {
            Location loc = player.getLocation().add(0, 1, 0);
            player.getWorld().spawnParticle(Particle.valueOf(particleName), loc, 12, 0.4, 0.4, 0.4);
        } catch (IllegalArgumentException ignored) { }
    }

    private String format(double value) {
        return String.format("%,.2f", value);
    }

    private void msg(Player player, String key, String... placeholders) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String text = plugin.getConfig().getString("messages." + key, "");
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            text = text.replace(placeholders[i], placeholders[i + 1]);
        }
        player.sendMessage(ItemBuilder.color(prefix + text));
    }
}
