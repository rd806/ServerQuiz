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
import org.rd806.serverquiz.quiz.QuizType;

import java.util.*;

public class QuizGui {

    // 选择题选项对应的界面槽位
    private static final int[] OPTION_SLOTS = {19, 21, 23, 25};
    // 选择题选项对应的物品
    private static final Material[] OPTION_MATERIALS = {
            Material.RED_WOOL, Material.YELLOW_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL
    };
    private static final int SELECTED_SLOT = 38;
    private static final int SUBMIT_SLOT = 40;

    // 每位玩家的答题状态（各自的选项顺序与已选标签）
    private final Map<UUID, PlayerSession> sessions = new HashMap<>();

    /**
     * 单个玩家的答题状态，避免并发错误
     */
    public static class PlayerSession {
        private final QuizEntry entry;
        private final Inventory menu;
        private final Set<String> selectedLabels;

        private PlayerSession(QuizEntry entry, Inventory menu) {
            this.entry = entry;
            this.menu = menu;
            this.selectedLabels = new LinkedHashSet<>();
        }

        public QuizEntry getEntry() { return entry; }
        public Inventory getMenu() { return menu; }
        public Set<String> getSelectedLabels() { return selectedLabels; }
    }

    /**
     * Quiz界面
     */
    public Inventory createQuizMenu(Player player) {
        Inventory quizMenu = Bukkit.createInventory(null, 45,
                ServerQuiz.config.getString("gui.title", "Quiz Menu"));

        QuizEntry source = ServerQuiz.main.quiz;
        // 当前没有可答的题目时返回空界面
        if (source == null || source.getType() == null) {
            player.sendMessage(ServerQuiz.config.getString("messages.noQuiz", "There is no quiz available right now!"));
            return quizMenu;
        }

        // 复制一份题目，让每位玩家的选项顺序互不影响
        QuizEntry entry = source.copy();
        // 打乱选项后重新映射标签，保证界面标签与选项内容一一对应
        if (entry.getType() == QuizType.Choice
                && ServerQuiz.config.getBoolean("gui.choice.random", false)) {
            Collections.shuffle(entry.getOptions());
            entry.remapLabels();
        }

        PlayerSession session = new PlayerSession(entry, quizMenu);
        sessions.put(player.getUniqueId(), session);
        render(player, session);
        return quizMenu;
    }

    // 渲染整个答题界面
    private void render(Player player, PlayerSession session) {
        Inventory quizMenu = session.getMenu();
        QuizEntry entry = session.getEntry();
        quizMenu.clear();

        // 左上角
        AddItemToMenu(quizMenu, 0, Material.CRAFTING_TABLE,
                "§r" + "ServerQuiz",
                "Click the block to answer the Quiz", "Win the reward if your answer is correct!");
        // 设置题目
        AddItemToMenu(quizMenu, 4, Material.PAPER,
                "§r" + ServerQuiz.config.getString("gui.question", "Question"),
                "§r§b" + entry.getQuestion());
        // 设置奖品与分数
        ItemStack reward = entry.getReward();
        Material rewardMaterial = reward == null ? Material.BARRIER : reward.getType();
        AddItemToMenu(quizMenu, 8, rewardMaterial,
                "§r" + ServerQuiz.config.getString("gui.reward", "Reward"),
                "§r§b" + entry.getRewardName(),
                "§r§b" + ServerQuiz.config.getString("gui.score", "Score") + ": " + entry.getScore());

        // 设置回答区域
        switch (entry.getType()) {
            case Choice -> renderChoices(session);
            case Blank -> renderBlank(quizMenu);
        }

        // 玩家信息（左下角）
        quizMenu.setItem(36, createPlayerHead(player));
        // 插件信息（右下角）
        AddItemToMenu(quizMenu, 44, Material.REDSTONE_BLOCK,
                "§r" + "About the plugin", "Click to view the source code!");
    }

    /**
     * 向菜单中添加物品
     * <br>
     * String... 类型可以一次性添加多个字符串，但必须作为最后一个参数
     */
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

    /**
     * 创建玩家积分信息
     */
    private ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

        // 获取计分表信息
        String title = ServerQuiz.config.getString("gui.scoreBoard.title", "Your quiz score board");
        List<String> lines = ServerQuiz.config.getStringList("gui.scoreBoard.lore");
        lines = ServerQuiz.main.placeHolder.resolve(player, lines);

        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(title);
            skullMeta.setLore(lines);
            head.setItemMeta(skullMeta);
        }
        return head;
    }

    /**
     * 选择题界面
     */
    private void renderChoices(PlayerSession session) {
        List<QuizEntry.QuizOption> options = session.getEntry().getOptions();
        for (int i = 0; i < OPTION_SLOTS.length && i < options.size(); i++) {
            renderOption(session, i);
        }
        // 展示玩家已选择的选项
        renderSelected(session);
        // 提交按钮
        AddItemToMenu(session.getMenu(), SUBMIT_SLOT, Material.BOOKSHELF,
                "§r" + ServerQuiz.config.getString("gui.choice.submit", "Submit"),
                "§7Click to submit your answer");
    }

    // 渲染单个选项
    private void renderOption(PlayerSession session, int index) {
        QuizEntry.QuizOption option = session.getEntry().getOptions().get(index);
        List<String> lore = new ArrayList<>();
        lore.add("§r§b" + option.text);
        if (session.selectedLabels.contains(option.label)) {
            lore.add("§a✔ " + ServerQuiz.config.getString("gui.choice.selectedMark", "Selected"));
        } else {
            lore.add("§7Click to select");
        }
        AddItemToMenu(session.getMenu(), OPTION_SLOTS[index], OPTION_MATERIALS[index],
                "§r" + ServerQuiz.config.getString("gui.choice.option", "Option") + option.label,
                lore.toArray(new String[0]));
    }

    // 渲染已选答案
    private void renderSelected(PlayerSession session) {
        List<String> lore = new ArrayList<>();
        if (session.selectedLabels.isEmpty()) {
            lore.add("§7" + ServerQuiz.config.getString("gui.choice.noneSelected", "None"));
        } else {
            for (String label : session.selectedLabels) {
                String content = session.getEntry().getContentByLabel(label);
                lore.add("§a" + label + (content == null ? "" : ". §r§b" + content));
            }
        }
        AddItemToMenu(session.getMenu(), SELECTED_SLOT, Material.PAPER,
                "§r" + ServerQuiz.config.getString("gui.choice.selected", "Selected"),
                lore.toArray(new String[0]));
    }

    // 切换选项的选中状态（先选择，再提交）
    public void toggleOption(Player player, int slot) {
        PlayerSession session = sessions.get(player.getUniqueId());
        if (session == null || session.getEntry().getType() != QuizType.Choice) return;

        int index = optionIndex(slot);
        List<QuizEntry.QuizOption> options = session.getEntry().getOptions();
        if (index < 0 || index >= options.size()) return;

        String label = options.get(index).label;
        if (!session.selectedLabels.remove(label)) {
            session.selectedLabels.add(label);
        }
        renderOption(session, index);
        renderSelected(session);
    }

    // 提交已选答案
    public void submit(Player player) {
        PlayerSession session = sessions.get(player.getUniqueId());
        if (session == null || session.getEntry().getType() != QuizType.Choice) return;

        if (session.selectedLabels.isEmpty()) {
            player.sendMessage(ServerQuiz.config.getString("messages.noSelection",
                    "Please select at least one option!"));
            return;
        }
        // 用户提交的标签集合 → 内容集合，再与正确答案的内容集合比较（顺序无关）
        Set<String> selectedContents = session.getEntry().labelsToContents(session.selectedLabels);
        sessions.remove(player.getUniqueId());
        ServerQuiz.main.quizConfig.checkChoice(player, selectedContents);
    }

    // 槽位对应的选项下标，非选项槽位返回 -1
    private int optionIndex(int slot) {
        for (int i = 0; i < OPTION_SLOTS.length; i++) {
            if (OPTION_SLOTS[i] == slot) return i;
        }
        return -1;
    }

    // 清除玩家答题状态
    public void clearSession(UUID uuid) {
        sessions.remove(uuid);
    }

    /**
     * 填空题界面
     */
    private void renderBlank(Inventory quizMenu) {
        AddItemToMenu(quizMenu, 22, Material.ANVIL,
                ServerQuiz.config.getString("gui.blank.answerSheet", "§r" + "Answer Sheet"));
    }

    // 创建界面
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
