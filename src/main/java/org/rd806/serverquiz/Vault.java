package org.rd806.serverquiz;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class Vault {
    public Economy economy;

    public Vault() {
        // 确认 Vault 插件存在
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            ServerQuiz.logger.warning("Failed to load Vault Economy. You may not be able to get a full usage.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = ServerQuiz.main.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
            ServerQuiz.logger.info("Vault Economy Registered!");
        }
    }

    // 检查经济插件是否成功连接
    public boolean isEconomyNotAvailable() {
        return this.economy == null;
    }

    public void addPlayerEconomy(Player player, int amount) {
        if (isEconomyNotAvailable()) {
            ServerQuiz.main.getLogger().warning("Failed to change player's vault. Have you installed related plugins?");
            return;
        }
        economy.depositPlayer(player, amount);
        player.sendMessage(amount + " has been added to your Account!");
    }
}
