package org.rd806.serverquiz.quiz.content;

import org.bukkit.inventory.ItemStack;

import java.text.NumberFormat;

public record RewardData(String rawData, RewardType rewardType, ItemStack item, int value) {
    public enum RewardType {
        Item,
        Vault
    }

    // 检查数字
    public static boolean numberCheck(String string) {
        try {
            NumberFormat.getInstance().parse(string);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
