package org.rd806.serverquiz.gui;

import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.rd806.serverquiz.ServerQuiz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class QuizGui {

    private final ServerQuiz plugin = ServerQuiz.main;
    private final Inventory quizMenu;

    public QuizGui() {
        quizMenu = Bukkit.createInventory(null, 45, ServerQuiz.config.getString("gui.title", "Quiz Menu"));
    }

    // Quiz界面
    public Inventory createQuizMenu(Player player) {
        quizMenu.clear();
        // 左上角
        AddItemToMenu(quizMenu, 0, Material.CRAFTING_TABLE, "ServerQuiz！",
                "Click the block to answer the Quiz",
                "Win the reward if your answer is correct!");
        // 设置题目
        AddItemToMenu(quizMenu, 4, Material.PAPER,
                "§r" + ServerQuiz.config.getString("gui.question", "Question"),
                "§r§b" + plugin.quiz.getQuestion());
        // 设置奖品
        AddItemToMenu(quizMenu, 8, plugin.quiz.getReward().getType(),
                "§r"+ ServerQuiz.config.getString("gui.reward", "Reward"));

        switch (plugin.quiz.getType()) {
            case Choice:
                multipleChoices();
                break;
            case Blank:
                fillBlank();
                break;
        }

        // 玩家信息（左下角）
        quizMenu.setItem(36, createPlayerHead(player));
        // 插件信息（右下角）
        AddItemToMenu(quizMenu, 44, Material.REDSTONE_BLOCK, "About the plugin", "Click to view the source code!");

        return quizMenu;
    }

    // 向菜单中添加物品
    // String... 类型可以一次性添加多个字符串，但必须作为最后一个参数
    private void AddItemToMenu(Inventory menu, int slot, Material material, String itemName, String... itemLore) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setDisplayName(itemName);
            // 追加lore内容
            List<String> lore = itemMeta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.addAll(Arrays.asList(itemLore));
            itemMeta.setLore(lore);
        }
        itemStack.setItemMeta(itemMeta);
        menu.setItem(slot, itemStack);
    }

    // 创建玩家头像
    private ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(player.getName());
            skullMeta.setLore(Arrays.asList(
                    "§7Click to show your player info",
                    "",
                    "§ePlayer: §f" + player.getName(),
                    "§eLevel: §f" + player.getLevel(),
                    "§eHealth: §f" + String.format("%.1f", player.getHealth()) + "❤"
            ));
            head.setItemMeta(skullMeta);
        }
        return head;
    }

    // 选择题界面
    private void multipleChoices() {
        // 设置选项
        AddItemToMenu(quizMenu, 19, Material.RED_WOOL,
                "§r" + ServerQuiz.config.getString("gui.option", "Option") + "A",
                "§r§b" + plugin.quiz.getOptions().get(0));
        AddItemToMenu(quizMenu, 21, Material.YELLOW_WOOL,
                "§r" + ServerQuiz.config.getString("gui.option", "Option") + "B",
                "§r§b" + plugin.quiz.getOptions().get(1));
        AddItemToMenu(quizMenu, 23, Material.BLUE_WOOL,
                "§r" + ServerQuiz.config.getString("gui.option", "Option") + "C",
                "§r§b" + plugin.quiz.getOptions().get(2));
        AddItemToMenu(quizMenu, 25, Material.GREEN_WOOL,
                "§r" + ServerQuiz.config.getString("gui.option", "Option") + "D",
                "§r§b" + plugin.quiz.getOptions().get(3));
    }

    // 填空题界面
    private void fillBlank() {
        AddItemToMenu(quizMenu, 22, Material.ANVIL, ServerQuiz.config.getString("gui.answerSheet", "Answer Sheet"));
    }

    public void createGUI(Player player) {
        new AnvilGUI.Builder()
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    ServerQuiz.main.quizConfig.check(player, stateSnapshot.getText());
                    return List.of(AnvilGUI.ResponseAction.close());
                })
                .text("Good luck!")
                .title(ServerQuiz.config.getString("gui.ansewerTitle", "Type your Answer"))
                .itemLeft(new ItemStack(Material.PAPER))
                .plugin(plugin)
                .open(player);
    }
}
