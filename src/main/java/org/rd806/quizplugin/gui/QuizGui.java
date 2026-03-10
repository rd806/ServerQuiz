package org.rd806.quizplugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.rd806.quizplugin.QuizPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuizGui {

    private final QuizPlugin plugin = QuizPlugin.main;
    private final Inventory quizMenu;

    public QuizGui() {
        quizMenu = Bukkit.createInventory(null, 45, "Quiz Menu");
    }

    // Quiz界面
    public Inventory createQuizMenu(Player player) {
        // 左上角
        AddItemToMenu(quizMenu, 0, Material.CRAFTING_TABLE, "有奖问答！",
                "点击对应的选项方块回答问题",
                "答对有奖！");
        // 设置题目
        AddItemToMenu(quizMenu, 4, Material.PAPER, "§r题目", "§r§b" + plugin.quiz.getQuestion());
        // 设置奖品
        AddItemToMenu(quizMenu, 8, plugin.quiz.getReward().getType(), "§r奖品", "货真价实！");
        // 设置选项
        AddItemToMenu(quizMenu, 19, Material.RED_WOOL, "§r选项A",  "§r§b" + plugin.quiz.getOptions().get(0));
        AddItemToMenu(quizMenu, 21, Material.YELLOW_WOOL, "§r选项B", "§r§b" + plugin.quiz.getOptions().get(1));
        AddItemToMenu(quizMenu, 23, Material.BLUE_WOOL, "§r选项C", "§r§b" + plugin.quiz.getOptions().get(2));
        AddItemToMenu(quizMenu, 25, Material.GREEN_WOOL, "§r选项D", "§r§b" + plugin.quiz.getOptions().get(3));
        // 玩家信息（左下角）
        quizMenu.setItem(36, createPlayerHead(player));
        // 插件信息（右下角）
        AddItemToMenu(quizMenu, 44, Material.REDSTONE_BLOCK, "关于插件", "点击访问插件仓库");

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
                    "§7点击查看您的玩家信息",
                    "",
                    "§e玩家: §f" + player.getName(),
                    "§e等级: §f" + player.getLevel(),
                    "§e生命值: §f" + String.format("%.1f", player.getHealth()) + "❤"
            ));
            head.setItemMeta(skullMeta);
        }
        return head;
    }
}
