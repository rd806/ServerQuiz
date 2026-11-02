package org.rd806.serverquiz.gui;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.rd806.serverquiz.ServerQuiz;

public class QuizEvent implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        InventoryView inventoryView = event.getView();
        if (!inventoryView.getTitle().equalsIgnoreCase(ServerQuiz.config.getString("gui.title", "Quiz Menu"))) {
            return;
        }
        // 只处理答题界面内的点击，避免误触玩家背包中相同编号的槽位
        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(inventoryView.getTopInventory())) {
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        switch (event.getSlot()) {
            // 选项：切换选中状态
            case 19, 21, 23, 25 -> ServerQuiz.main.quizGui.toggleOption(player, event.getSlot());
            // 提交按钮
            case 40 -> ServerQuiz.main.quizGui.submit(player);
            // 填空题答题纸
            case 22 -> ServerQuiz.main.quizGui.createGUI(player);
            // 插件信息
            case 44 -> {
                TextComponent component = new TextComponent("§aClick here to visit the plugin website.");
                // 设置点击事件为打开网址
                component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth.com/plugin/serverquiz"));
                player.spigot().sendMessage(component);
                player.closeInventory();
            }
            default -> {
            }
        }
    }

    // 关闭界面时清除答题状态
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getView().getTitle().equalsIgnoreCase(ServerQuiz.config.getString("gui.title", "Quiz Menu"))) {
            ServerQuiz.main.quizGui.clearSession(player.getUniqueId());
        }
    }

    // 创建计分表
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (ServerQuiz.main.quizConfig.createScoreBoard(player.getName(), player.getUniqueId())) {
            return;
        }
        ServerQuiz.logger.warning("Failed to create scoreboard for " + player.getName());
    }
}
