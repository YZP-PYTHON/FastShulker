package org.random11999.fastShulker;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ShulkerHolder implements InventoryHolder {
    private final String uuid;

    public ShulkerHolder(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
