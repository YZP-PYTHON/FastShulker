package org.random11999.fastShulker;

import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

public final class FastShulker extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("FastShulker 插件已启用 - 右键点击潜影盒可直接打开!");
    }

    @Override
    public void onDisable() {
        getLogger().info("FastShulker 插件已禁用");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();

        // 只处理右键
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // 判断是否潜影盒
        if (!isShulkerBox(item)) {
            return;
        }

        // ✅ 右键空气：打开潜影盒并取消事件，防止放置
        if (action == Action.RIGHT_CLICK_AIR) {
            event.setCancelled(true);
            openShulkerInventory(player, item);
            return;
        }

        // ✅ 右键方块：允许默认行为，不取消事件，让它能放下去
        if (action == Action.RIGHT_CLICK_BLOCK) {
            // 不 event.setCancelled(true)，Minecraft 会正常放置潜影盒
            // 如果你想放置时也打开GUI，可以在这里调用 openShulkerInventory
            // 但建议不调用，避免冲突
        }
    }

    private void openShulkerInventory(Player player, ItemStack item) {
        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        if (meta == null) return;

        ShulkerBox shulkerBox = (ShulkerBox) meta.getBlockState();
        Inventory inventory = shulkerBox.getInventory();

        // 保存潜影盒引用到玩家元数据中，用于关闭时保存
        player.setMetadata("openedShulker", new FixedMetadataValue(this, item));

        // 打开GUI界面
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // 获取之前保存的潜影盒物品
        if (!player.hasMetadata("openedShulker")) return;

        ItemStack shulkerItem = (ItemStack) player.getMetadata("openedShulker").get(0).value();
        if (shulkerItem == null || !isShulkerBox(shulkerItem)) return;

        // 更新潜影盒物品的NBT数据
        BlockStateMeta meta = (BlockStateMeta) shulkerItem.getItemMeta();
        if (meta == null) return;

        ShulkerBox shulkerBox = (ShulkerBox) meta.getBlockState();

        // 将关闭的库存内容保存回潜影盒
        shulkerBox.getInventory().setContents(event.getInventory().getContents());
        meta.setBlockState(shulkerBox);
        shulkerItem.setItemMeta(meta);

        // 更新玩家手中的物品
        player.getInventory().setItemInMainHand(shulkerItem);

        // 移除元数据
        player.removeMetadata("openedShulker", this);
    }

    // 判断物品是否是潜影盒
    private boolean isShulkerBox(ItemStack item) {
        if (item == null || item.getType().isEmpty()) {
            return false;
        }

        return switch (item.getType()) {
            case SHULKER_BOX, WHITE_SHULKER_BOX, ORANGE_SHULKER_BOX,
                 MAGENTA_SHULKER_BOX, LIGHT_BLUE_SHULKER_BOX, YELLOW_SHULKER_BOX,
                 LIME_SHULKER_BOX, PINK_SHULKER_BOX, GRAY_SHULKER_BOX,
                 LIGHT_GRAY_SHULKER_BOX, CYAN_SHULKER_BOX, PURPLE_SHULKER_BOX,
                 BLUE_SHULKER_BOX, BROWN_SHULKER_BOX, GREEN_SHULKER_BOX,
                 RED_SHULKER_BOX, BLACK_SHULKER_BOX -> true;
            default -> false;
        };
    }
}
