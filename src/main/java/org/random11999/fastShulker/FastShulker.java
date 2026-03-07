package org.random11999.fastShulker;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;

import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class FastShulker extends JavaPlugin implements Listener {

    private static final String META_OPENING = "fastshulker_opening"; // 标记玩家正在打开潜影盒
    private static final NamespacedKey KEY_UUID = new NamespacedKey("fastshulker", "uuid");

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("FastShulker 1.0.1 Load success");
    }

    @Override
    public void onDisable() {
        getLogger().info("FastShulker 1.0.1 Disable success");
    }

    /* ===============================
       右键打开潜影盒
       =============================== */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        // 处理右键空气和右键方块
        if (event.getAction() != Action.RIGHT_CLICK_AIR ) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isShulkerBox(item)) return;

        // 彻底拦截：防止方块放置、交互以及客户端的放置预览（选框）
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        event.setCancelled(true);

        // 延迟一刻打开 GUI，这通常能更好地解决客户端预览选框残留的问题，并确保事件流程完整
        Bukkit.getScheduler().runTask(this, () -> openShulker(player, item));
    }

    private void openShulker(Player player, ItemStack item) {
        if (!isShulkerBox(item)) return;

        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        if (meta == null) return;

        // 生成一个临时的 UUID 并标记该物品，以便在关闭时找回
        String uuid = UUID.randomUUID().toString();
        meta.getPersistentDataContainer().set(KEY_UUID, PersistentDataType.STRING, uuid);
        item.setItemMeta(meta);

        ShulkerBox box = (ShulkerBox) meta.getBlockState();
        Inventory inv = Bukkit.createInventory(player, 27, Component.text("潜影盒", NamedTextColor.DARK_PURPLE));
        inv.setContents(box.getInventory().getContents());

        // 记录玩家正在打开的潜影盒 UUID
        player.setMetadata(META_OPENING, new FixedMetadataValue(this, uuid));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, 1.0f, 1.0f);
    }

    /* ===============================
       安全限制：防止物品丢失、复制和嵌套
       =============================== */

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!player.hasMetadata(META_OPENING)) return;

        String openUuid = player.getMetadata(META_OPENING).get(0).asString();

        // 1. 防止移动正在被打开的那个潜影盒本身
        ItemStack clickedItem = event.getCurrentItem();
        if (isOpenedShulker(clickedItem, openUuid)) {
            event.setCancelled(true);
            return;
        }

        // 2. 防止嵌套：禁止通过各种方式将潜影盒放入当前打开的潜影盒 GUI
        // 如果点击的是潜影盒 GUI (通常是 Chest 类型)
        if (event.getInventory().getType() == InventoryType.CHEST) {
            ItemStack itemToPut = null;

            // 如果是常规点击
            if (event.getAction() == InventoryAction.PLACE_ALL || 
                event.getAction() == InventoryAction.PLACE_ONE || 
                event.getAction() == InventoryAction.PLACE_SOME ||
                event.getAction() == InventoryAction.SWAP_WITH_CURSOR) {
                
                // 仅当点击位置是上方的 GUI 容器时
                if (event.getRawSlot() < event.getInventory().getSize()) {
                    itemToPut = event.getCursor();
                }
            } 
            // 如果是快捷键数字交换 (Hotbar Swap)
            else if (event.getClick() == ClickType.NUMBER_KEY) {
                if (event.getRawSlot() < event.getInventory().getSize()) {
                    itemToPut = player.getInventory().getItem(event.getHotbarButton());
                }
            }
            // 如果是 Shift 点击（从玩家背包往 GUI 里塞）
            else if (event.getClick().isShiftClick()) {
                // 仅当点击位置是玩家背包时，且目标是 GUI
                if (event.getRawSlot() >= event.getInventory().getSize()) {
                    itemToPut = event.getCurrentItem();
                }
            }

            if (isShulkerBox(itemToPut)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("不能将潜影盒放入潜影盒中！", NamedTextColor.RED));
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!player.hasMetadata(META_OPENING)) return;

        // 禁止将潜影盒拖入 GUI
        if (isShulkerBox(event.getOldCursor())) {
            for (int slot : event.getRawSlots()) {
                if (slot < event.getInventory().getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata(META_OPENING)) {
            // 在打开潜影盒时，禁止交换手持物品（防止物品丢失或 UUID 校验失败）
            event.setCancelled(true);
        }
    }

    /* ===============================
       保存潜影盒内容
       =============================== */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!player.hasMetadata(META_OPENING)) return;

        String uuid = player.getMetadata(META_OPENING).get(0).asString();
        player.removeMetadata(META_OPENING, this);

        // 寻找持有对应 UUID 的潜影盒（全背包搜索，防止玩家在打开时移动了它）
        ItemStack shulkerItem = findOpenedShulker(player, uuid);

        if (shulkerItem == null) {
            player.sendMessage(Component.text("保存失败：未找到对应的潜影盒！物品已掉落在地。", NamedTextColor.RED));
            dropContents(player, event.getInventory());
            return;
        }

        BlockStateMeta meta = (BlockStateMeta) shulkerItem.getItemMeta();
        if (meta == null) return;

        ShulkerBox box = (ShulkerBox) meta.getBlockState();
        
        // 获取 GUI 中的所有物品
        ItemStack[] contents = event.getInventory().getContents();
        boolean hasIllegal = false;

        // 遍历并处理非法物品（潜影盒嵌套）
        for (int i = 0; i < contents.length; i++) {
            ItemStack content = contents[i];
            if (isShulkerBox(content)) {
                hasIllegal = true;
                // 将非法物品返还给玩家背包，如果背包满了则掉落在地
                player.getInventory().addItem(content).values().forEach(item -> 
                    player.getWorld().dropItemNaturally(player.getLocation(), item));
                // 从保存列表中移除该物品
                contents[i] = null;
            }
        }

        if (hasIllegal) {
            player.sendMessage(Component.text("已移除潜影盒中的非法嵌套潜影盒，并返还给你！", NamedTextColor.RED));
        }

        // 将处理后的物品列表保存到潜影盒中
        box.getInventory().setContents(contents);
        
        // 移除 UUID 标记
        meta.getPersistentDataContainer().remove(KEY_UUID);
        meta.setBlockState(box);
        shulkerItem.setItemMeta(meta);

        player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, 1.0f, 1.0f);
    }

    /* ===============================
       工具方法
       =============================== */

    private boolean isShulkerBox(ItemStack item) {
        return item != null && Tag.SHULKER_BOXES.isTagged(item.getType());
    }

    private boolean isOpenedShulker(ItemStack item, String uuid) {
        if (!isShulkerBox(item)) return false;
        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        if (meta == null) return false;
        String itemUuid = meta.getPersistentDataContainer().get(KEY_UUID, PersistentDataType.STRING);
        return uuid.equals(itemUuid);
    }

    private ItemStack findOpenedShulker(Player player, String uuid) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isOpenedShulker(item, uuid)) return item;
        }
        return null;
    }

    private void dropContents(Player player, Inventory inv) {
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
    }
}