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
import java.util.stream.Collectors;

public class AnswerGui {

    // 选择题选项：选项下标、界面槽位、显示物品
    private record OptionSlot(int index, int slot, Material material) {}

    // 列表顺序对应选项 A、B、C、D，按下标直接读取
    private static final List<OptionSlot> OPTION_SLOTS = List.of(
            new OptionSlot(0, 19, Material.RED_WOOL),
            new OptionSlot(1, 21, Material.YELLOW_WOOL),
            new OptionSlot(2, 23, Material.BLUE_WOOL),
            new OptionSlot(3, 25, Material.GREEN_WOOL)
    );
    // 槽位 → 选项，按槽位直接读取
    private static final Map<Integer, OptionSlot> OPTION_BY_SLOT =  OPTION_SLOTS.stream()
            .collect(Collectors.toUnmodifiableMap(OptionSlot::slot, option -> option));
    // 提交按钮
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

        QuizEntry source = ServerQuiz.main.currentQuiz;
        // 当前没有可答的题目时返回 NULL
        if (source == null || source.getType() == null) {
            player.sendMessage(ServerQuiz.config.getString("messages.noQuiz", "There is no quiz available right now!"));
            return null;
        }
        // 复制一份题目，让每位玩家的选项顺序互不影响
        QuizEntry entry = source.copy();
        // 打乱选项后重新映射标签，保证界面标签与选项内容一一对应
        if (entry.getType() == QuizType.Choice && ServerQuiz.config.getBoolean("gui.choice.random", false)) {
            Collections.shuffle(entry.getOptions());
            entry.remapLabels();
        }

        PlayerSession session = new PlayerSession(entry, quizMenu);
        sessions.put(player.getUniqueId(), session);
        render(player, session);
        return quizMenu;
    }

    /**
     * 通用方法：向菜单中添加物品
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
     * 渲染整个答题界面的通用部分
     */
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
        renderStatistics(player, quizMenu);
        // 插件信息（右下角）
        AddItemToMenu(quizMenu, 44, Material.REDSTONE_BLOCK,
                "§r" + "About the plugin", "Click to view the source code!");
    }

    /**
     * 创建玩家积分信息
     */
    private void renderStatistics(Player player, Inventory quizMenu) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        // 获取计分表信息
        String title = ServerQuiz.config.getString("gui.scoreBoard.title", "Your quiz score board");
        List<String> lines = ServerQuiz.config.getStringList("gui.scoreBoard.lore");
        lines = ServerQuiz.placeHolder.resolve(player, lines);
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(title);
            skullMeta.setLore(lines);
            head.setItemMeta(skullMeta);
        }
        quizMenu.setItem(36, head);
    }

    /**
     * 选择题界面
     */
    private void renderChoices(PlayerSession session) {
        List<QuizEntry.QuizOption> options = session.getEntry().getOptions();
        // 选项数量可能少于 4 个
        for (int i = 0; i < OPTION_SLOTS.size() && i < options.size(); i++) {
            renderOption(session, i);
        }
        // 提交按钮上展示玩家已选择的选项
        renderSubmitButton(session);
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
        OptionSlot optionSlot = OPTION_SLOTS.get(index);
        AddItemToMenu(session.getMenu(), optionSlot.slot(), optionSlot.material(),
                "§r" + ServerQuiz.config.getString("gui.choice.option", "Option") + option.label,
                lore.toArray(new String[0]));
    }

    // 渲染提交按钮，并显示当前已选答案
    private void renderSubmitButton(PlayerSession session) {
        List<String> lore = selectedLore(session);
        lore.add("§7Click to submit your answer");
        AddItemToMenu(session.getMenu(), SUBMIT_SLOT, Material.WRITABLE_BOOK,
                "§r" + ServerQuiz.config.getString("gui.choice.submit", "Submit"),
                lore.toArray(new String[0]));
    }

    // 已选答案的展示文本
    private List<String> selectedLore(PlayerSession session) {
        List<String> lore = new ArrayList<>();
        if (session.selectedLabels.isEmpty()) {
            lore.add("§7" + ServerQuiz.config.getString("gui.choice.noneSelected", "None"));
        } else {
            for (String label : session.selectedLabels) {
                String content = session.getEntry().getContentByLabel(label);
                lore.add("§a" + label + (content == null ? "" : ". §r§b" + content));
            }
        }
        return lore;
    }

    // 切换选项的选中状态（先选择，再提交）
    public void toggleOption(Player player, int slot) {
        PlayerSession session = sessions.get(player.getUniqueId());
        if (session == null || session.getEntry().getType() != QuizType.Choice) return;

        // 直接按槽位读取选项，点击非选项槽位时返回
        OptionSlot optionSlot = OPTION_BY_SLOT.get(slot);
        if (optionSlot == null) return;
        int index = optionSlot.index();

        List<QuizEntry.QuizOption> options = session.getEntry().getOptions();
        if (index < 0 || index >= options.size()) return;

        String label = options.get(index).label;
        if (!session.selectedLabels.remove(label)) {
            session.selectedLabels.add(label);
        }
        renderOption(session, index);
        renderSubmitButton(session);
    }

    // 提交已选答案
    public void submit(Player player) {
        PlayerSession session = sessions.get(player.getUniqueId());
        if (session == null || session.getEntry().getType() != QuizType.Choice) return;

        // 界面打开时题目可能已经切换，避免把旧题目的答案提交到新题目
        QuizEntry current = ServerQuiz.main.currentQuiz;
        if (current == null || current.getId() != session.getEntry().getId()) {
            player.sendMessage(ServerQuiz.config.getString("messages.expired",
                    "This quiz is no longer active!"));
            sessions.remove(player.getUniqueId());
            player.closeInventory();
            return;
        }

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

                    ServerQuiz.main.quizConfig.checkBlank(player, stateSnapshot.getText());
                    return List.of(AnvilGUI.ResponseAction.close());
                })
                .text("Good luck!")
                .title(ServerQuiz.config.getString("gui.blank.ansewerTitle", "Type your Answer"))
                .itemLeft(new ItemStack(Material.PAPER))
                .plugin(ServerQuiz.main)
                .open(player);
    }
}
