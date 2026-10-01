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
import org.rd806.serverquiz.quiz.QuizEntry;
import org.rd806.serverquiz.quiz.RewardData;

import java.util.*;

public class QuizGui {

    // 复制一份 QuizEntry 供打乱选项
    public QuizEntry entry;
    private final Inventory quizMenu;

    public QuizGui() {
        quizMenu = Bukkit.createInventory(null, 45, ServerQuiz.config.getString("gui.title", "Quiz Menu"));
    }

    // 打乱选项
    private void shuffle(QuizEntry entry) {
        List<QuizEntry.QuizOption> options = entry.getOptions();
        Collections.shuffle(options);
    }

    // Quiz界面
    public Inventory createQuizMenu(Player player) {
        quizMenu.clear();
        entry = ServerQuiz.main.quiz;
        // 左上角
        AddItemToMenu(quizMenu, 0, Material.CRAFTING_TABLE,
                "§r"+ "ServerQuiz",
                "Click the block to answer the Quiz", "Win the reward if your answer is correct!");
        // 设置题目
        AddItemToMenu(quizMenu, 4, Material.PAPER,
                "§r" + ServerQuiz.config.getString("gui.question", "Question"),
                "§r§b" + entry.getQuestion());
        // 设置奖品
        AddItemToMenu(quizMenu, 8, entry.getReward().item().getType(),
                "§r" + ServerQuiz.config.getString("gui.reward", "Reward"),
                "§r§b" + RewardData.getRewardData(entry));

        // 设置回答区域
        switch (entry.getType()) {
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
        AddItemToMenu(quizMenu, 44, Material.REDSTONE_BLOCK,
                "§r" + "About the plugin", "Click to view the source code!");

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

        // 获取计分表信息
        String title = ServerQuiz.config.getString("gui.scoreBoard.title", "Your quiz score board");
        List<String> lines = ServerQuiz.config.getStringList("gui.scoreBoard.lore");
        lines = ServerQuiz.main.placeholder.resolve(player, lines);

        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(title);
            skullMeta.setLore(lines);
            head.setItemMeta(skullMeta);
        }
        return head;
    }

    // 选择题界面
    private void multipleChoices() {
        // 打乱选项
        if (ServerQuiz.config.getBoolean("gui.choice.random", false)) {
            shuffle(entry);
        }
        // 设置选项
        AddItemToMenu(quizMenu, 19, Material.RED_WOOL,
                "§r" + ServerQuiz.config.getString("gui.choice.option", "Option") + "A",
                "§r§b" + entry.getOptions().get(0).text);
        AddItemToMenu(quizMenu, 21, Material.YELLOW_WOOL,
                "§r" + ServerQuiz.config.getString("gui.choice.option", "Option") + "B",
                "§r§b" + entry.getOptions().get(1).text);
        AddItemToMenu(quizMenu, 23, Material.BLUE_WOOL,
                "§r" + ServerQuiz.config.getString("gui.choice.option", "Option") + "C",
                "§r§b" + entry.getOptions().get(2).text);
        AddItemToMenu(quizMenu, 25, Material.GREEN_WOOL,
                "§r" + ServerQuiz.config.getString("gui.choice.option", "Option") + "D",
                "§r§b" + entry.getOptions().get(3).text);
    }

    // 填空题界面
    private void fillBlank() {
        AddItemToMenu(quizMenu, 22, Material.ANVIL,
                ServerQuiz.config.getString("gui.blank.answerSheet", "§r" + "Answer Sheet"));
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
                .title(ServerQuiz.config.getString("gui.blank.ansewerTitle", "Type your Answer"))
                .itemLeft(new ItemStack(Material.PAPER))
                .plugin(ServerQuiz.main)
                .open(player);
    }
}
