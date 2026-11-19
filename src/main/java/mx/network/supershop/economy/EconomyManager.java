package mx.network.supershop.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Hook a Vault. No nos conectamos directo a Essentials/CMI/etc,
 * Vault se encarga de hablar con el plugin de economía que tenga el server.
 */
public class EconomyManager {

    private static Economy econ = null;

    public static boolean setup(JavaPlugin plugin) {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy get() {
        return econ;
    }
}
