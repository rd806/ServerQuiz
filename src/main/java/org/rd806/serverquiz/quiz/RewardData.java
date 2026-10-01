package org.rd806.serverquiz.quiz;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.text.NumberFormat;

public record RewardData(String rawData, RewardType rewardType, ItemStack item, int value) {
    public enum RewardType {
        Item,
        Vault
    }

    // 设置奖励格式
    public static void setRewardData(String rewardName, QuizEntry entry) {
        if (numberCheck(rewardName)) {
            // 设置为经济Vault格式
            entry.setReward(new RewardData(rewardName, RewardData.RewardType.Vault, new ItemStack(Material.EMERALD), Integer.parseInt(rewardName)));
        } else {
            // 设置为实物格式
            Material rewardMaterial = Material.matchMaterial(rewardName.toUpperCase());
            if (rewardMaterial != null) {
                ItemStack rewardItem = new ItemStack(rewardMaterial);
                entry.setReward(new RewardData(rewardName, RewardData.RewardType.Item, rewardItem,0));
            }
        }
    }

    // 获取奖励信息
    public static String getRewardData(QuizEntry entry) {
        switch (entry.getReward().rewardType) {
            case Item -> {
                return entry.getReward().item.getType().toString();
            }
            case Vault -> {
                return String.valueOf(entry.getReward().value);
            }
            default -> {
                return null;
            }
        }
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
