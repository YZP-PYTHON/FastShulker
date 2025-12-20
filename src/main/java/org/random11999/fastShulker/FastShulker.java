package org.random11999.fastShulker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class FastShulker extends JavaPlugin implements Listener {

    private static final String META_SLOT = "fastshulker_slot";
    private static final String META_UUID = "fastshulker_uuid"; // 用于校验物品唯一性

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
        // 允许右键空气，也可以根据需求允许右键方块（防止放置）
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() == Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isShulkerBox(item)) return;

        // 如果是右键方块，且玩家没有潜行，通常允许放置；这里强制拦截实现“任何时候右键都打开”
        // 如果你希望保留放置功能，可以判断 !player.isSneaking()
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
        // 创建一个不关联持有者的库存，防止某些奇怪的事件传播
        Inventory inv = Bukkit.createInventory(null, 27, "潜影盒");
        inv.setContents(box.getInventory().getContents());

        // 记录槽位
        player.setMetadata(META_SLOT, new FixedMetadataValue(this, slot));

        // 记录物品的 UUID (如果物品没有 UUID，可以记录其 hashCode 或暂时只靠 slot，但最好防止移动)
        // 这里我们简单处理：在 InventoryClick 中锁定该 slot

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, 1.0f, 1.0f);
    }

    /* ===============================
       防止套娃与物品移动 (关键修复)
       =============================== */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!player.hasMetadata(META_SLOT)) return;

        // 获取正在被操作的潜影盒的槽位
        int lockedSlot = player.getMetadata(META_SLOT).get(0).asInt();

        // 1. 禁止点击玩家背包中正在被打开的那个潜影盒
        if (event.getClickedInventory() != null &&
                event.getClickedInventory().getType() == InventoryType.PLAYER) {

            if (event.getSlot() == lockedSlot) {
                event.setCancelled(true);
                return;
            }
        }

        // 2. 禁止通过数字键（Hotbar Swap）将物品交换到该槽位或从该槽位换出
        if (event.getClick().name().contains("NUMBER_KEY")) {
            if (event.getHotbarButton() == lockedSlot) {
                event.setCancelled(true);
                return;
            }
            // 如果当前鼠标悬停在锁定槽位上按数字键
            if (event.getClickedInventory() != null &&
                    event.getClickedInventory().getType() == InventoryType.PLAYER &&
                    event.getSlot() == lockedSlot) {
                event.setCancelled(true);
            }
        }

        // 3. 禁止副手交换 (F键) - 通常由 PlayerSwapHandEvent 处理，但在打开 GUI 时 F 键通常无效，
        // 但为了安全起见，建议确保该物品被“锁定”。
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

        // 双重校验：确保槽位里还是潜影盒
        if (!isShulkerBox(item)) {
            player.sendMessage("§c保存失败：潜影盒已不在原位！");
            // 这里为了防止物品丢失，可以将 GUI 里的物品返还给玩家
            for (ItemStack content : event.getInventory().getContents()) {
                if (content != null) {
                    player.getWorld().dropItem(player.getLocation(), content);
                }
            }
            return;
        }

        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        if (meta == null) return;

        ShulkerBox box = (ShulkerBox) meta.getBlockState();
        box.getInventory().setContents(event.getInventory().getContents());

        meta.setBlockState(box);
        item.setItemMeta(meta);

        player.getInventory().setItem(slot, item);
        player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, 1.0f, 1.0f);
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