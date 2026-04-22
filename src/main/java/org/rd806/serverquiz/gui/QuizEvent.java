package org.rd806.serverquiz.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.rd806.serverquiz.ServerQuiz;

public class QuizEvent implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        InventoryView inventoryView = event.getView();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null) return;

        // 处理点击事件
        if (inventoryView.getTitle().equalsIgnoreCase("Quiz Menu")) {
            event.setCancelled(true);
            String response;
            switch (clicked.getType()) {
                case RED_WOOL:
                    response = "A";
                    ServerQuiz.main.quizConfig.check(player, response);
                    break;
                case YELLOW_WOOL:
                    response = "B";
                    ServerQuiz.main.quizConfig.check(player, response);
                    break;
                case BLUE_WOOL:
                    response = "C";
                    ServerQuiz.main.quizConfig.check(player, response);
                    break;
                case GREEN_WOOL:
                    response = "D";
                    ServerQuiz.main.quizConfig.check(player, response);
                    break;
                default:
                    break;
            }
        }

    }
}
