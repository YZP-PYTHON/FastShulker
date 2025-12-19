package org.random11999.fastShulker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

public final class FastShulker extends JavaPlugin implements Listener {

    private static final String META_SLOT = "fastshulker_slot";

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("FastShulker 1.0.0.1 加载成功");
    }

    /* ===============================
       右键打开潜影盒
       =============================== */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isShulkerBox(item)) return;

        // Paper 1.21+ 必须显式禁止使用物品
        event.setUseItemInHand(Result.DENY);
        event.setCancelled(true);

        openShulker(player);
    }

    private void openShulker(Player player) {
        int slot = player.getInventory().getHeldItemSlot();
        ItemStack item = player.getInventory().getItem(slot);

        if (!isShulkerBox(item)) return;

        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        if (meta == null) return;

        ShulkerBox box = (ShulkerBox) meta.getBlockState();
        Inventory inv = Bukkit.createInventory(player, 27, "潜影盒");

        inv.setContents(box.getInventory().getContents());

        player.setMetadata(META_SLOT, new FixedMetadataValue(this, slot));
        player.openInventory(inv);
    }

    /* ===============================
       保存潜影盒内容
       =============================== */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!player.hasMetadata(META_SLOT)) return;

        int slot = player.getMetadata(META_SLOT).get(0).asInt();
        player.removeMetadata(META_SLOT, this);

        ItemStack item = player.getInventory().getItem(slot);
        if (!isShulkerBox(item)) return;

        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        if (meta == null) return;

        ShulkerBox box = (ShulkerBox) meta.getBlockState();
        box.getInventory().setContents(event.getInventory().getContents());

        meta.setBlockState(box);
        item.setItemMeta(meta);

        player.getInventory().setItem(slot, item);
    }

    /* ===============================
       工具方法
       =============================== */
    private boolean isShulkerBox(ItemStack item) {
        if (item == null) return false;
        Material m = item.getType();
        return m.name().endsWith("SHULKER_BOX");
    }
}
