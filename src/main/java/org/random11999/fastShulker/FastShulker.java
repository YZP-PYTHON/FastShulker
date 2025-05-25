package org.random11999.fastShulker;

import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class FastShulker extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("FastShulker插件已启用 - 右键点击潜影盒可直接打开!");
    }

    @Override
    public void onDisable() {
        getLogger().info("FastShulker插件已禁用");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 只处理右键点击事件
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // 检查物品是否是潜影盒
        if (!isShulkerBox(item)) {
            return;
        }

        // 取消默认的放置行为
        event.setCancelled(true);

        // 获取潜影盒的库存
        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        if (meta == null) return;

        ShulkerBox shulkerBox = (ShulkerBox) meta.getBlockState();
        Inventory inventory = shulkerBox.getInventory();

        // 打开GUI界面
        player.openInventory(inventory);
    }

    // 检查物品是否是潜影盒
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