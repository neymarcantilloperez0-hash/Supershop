package mx.network.supershop.gui;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Base de todos los menús de la tienda. Guarda el "anillo" de vidrios que
 * {@link MenuAnimator} anima (marco que brilla, arcoíris, destello al comprar).
 */
public abstract class ShopHolder implements InventoryHolder {

    private Inventory inventory;
    private int[] ring = new int[0];
    private Material[] ringBase = new Material[0];
    private Material[] ringPainted = new Material[0];
    private int frame;
    private int flashTicks;
    private Material flashGlass = Material.LIME_STAINED_GLASS_PANE;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    /** Define el anillo animado y recuerda el vidrio original de cada slot (si es un vidrio). */
    public void setRing(int[] slots) {
        this.ring = slots;
        this.ringBase = new Material[slots.length];
        this.ringPainted = new Material[slots.length];
        if (inventory == null) return;
        for (int i = 0; i < slots.length; i++) {
            ItemStack it = inventory.getItem(slots[i]);
            Material m = isPane(it) ? it.getType() : null;
            ringBase[i] = m;
            ringPainted[i] = m;
        }
    }

    public static boolean isPane(ItemStack it) {
        return it != null && it.getType().name().endsWith("GLASS_PANE");
    }

    public int[] ring() {
        return ring;
    }

    public Material[] ringBase() {
        return ringBase;
    }

    public Material[] ringPainted() {
        return ringPainted;
    }

    public int frame() {
        return frame;
    }

    public void nextFrame() {
        frame++;
        if (frame < 0) frame = 0;
    }

    public int flashTicks() {
        return flashTicks;
    }

    public Material flashGlass() {
        return flashGlass;
    }

    /** Destello del marco (por ejemplo al confirmar una compra). */
    public void flash(Material glass, int ticks) {
        this.flashGlass = glass;
        this.flashTicks = Math.max(0, ticks);
    }

    public void tickFlash() {
        if (flashTicks > 0) flashTicks--;
    }

    /** true = el marco recorre una paleta de colores en vez de un destello blanco. */
    public boolean usesRainbow() {
        return false;
    }
}
