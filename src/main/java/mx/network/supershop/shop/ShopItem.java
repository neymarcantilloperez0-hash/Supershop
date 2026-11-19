package mx.network.supershop.shop;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class ShopItem {

    private final String id;
    private final Material material;
    private final String displayName;
    private final double buyPrice;
    private final double sellPrice;
    private final EntityType spawnerEntity; // solo usado si material == SPAWNER

    public ShopItem(String id, Material material, String displayName,
                     double buyPrice, double sellPrice, EntityType spawnerEntity) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.spawnerEntity = spawnerEntity;
    }

    public String getId() { return id; }
    public Material getMaterial() { return material; }
    public String getDisplayName() { return displayName; }
    public double getBuyPrice() { return buyPrice; }
    public double getSellPrice() { return sellPrice; }
    public EntityType getSpawnerEntity() { return spawnerEntity; }

    public boolean canBuy() { return buyPrice >= 0; }
    public boolean canSell() { return sellPrice >= 0; }
}
