package mx.network.supershop.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import mx.network.supershop.SuperShop;
import mx.network.supershop.economy.EconomyManager;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Registra %shop_balance% para usar el saldo del jugador en scoreboards,
 * chat, tab list, etc. Se registra solo si PlaceholderAPI está instalado.
 */
public class ShopPlaceholders extends PlaceholderExpansion {

    private final SuperShop plugin;

    public ShopPlaceholders(SuperShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "shop";
    }

    @Override
    public @NotNull String getAuthor() {
        return "TuServidor";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || EconomyManager.get() == null) return "";

        if (params.equalsIgnoreCase("balance")) {
            return String.format("%,.2f", EconomyManager.get().getBalance(player));
        }
        if (params.equalsIgnoreCase("balance_raw")) {
            return String.valueOf(EconomyManager.get().getBalance(player));
        }
        return null;
    }
}
