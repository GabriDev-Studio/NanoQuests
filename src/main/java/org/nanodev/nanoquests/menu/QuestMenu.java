package org.nanodev.nanoquests.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nanodev.nanoquests.NanoQuests;
import org.nanodev.nanoquests.manager.QuestManager;
import org.nanodev.nanoquests.quest.PlayerQuestData;
import org.nanodev.nanoquests.quest.Quest;

public class QuestMenu {

    private final NanoQuests plugin;
    private final Map<String, String> titleToCategory = new HashMap<>();

    // Заголовок главного меню — константа, чтобы MenuListener мог его сравнить
    public static final String MAIN_TITLE_RAW = "&8&l[ Квесты ]";

    public QuestMenu(NanoQuests plugin) {
        this.plugin = plugin;
    }

    // ===================== ГЛАВНОЕ МЕНЮ =====================
    public void openMain(Player player) {
        String title = c(MAIN_TITLE_RAW);
        Inventory inv = Bukkit.createInventory(null, 54, title);

        fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        ItemStack gray = make(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 10; i <= 43; i++) {
            int col = i % 9;
            if (col != 0 && col != 8) inv.setItem(i, gray);
        }

        inv.setItem(4, make(Material.NETHER_STAR, c("&a&l★ Квесты"),
                Arrays.asList(c(" "), c(" &7Выбери линию квестов"), c(" &7нажав на иконку выше.")))
        );

        QuestManager mgr = plugin.getQuestManager();
        PlayerQuestData data = mgr.getData(player);
        ConfigurationSection cats = plugin.getConfig().getConfigurationSection("categories");

        if (cats != null) {
            for (String key : cats.getKeys(false)) {
                ConfigurationSection cat = cats.getConfigurationSection(key);
                if (cat == null) continue;

                String displayName = c(cat.getString("display", key));
                Material icon = parseMat(cat.getString("icon", "DIRT"), Material.DIRT);
                int slot = cat.getInt("slot", 22);
                String cmdHint = cat.getString("command", "/quest" + key.toLowerCase());

                List<Quest> catQuests = mgr.getQuests().values().stream()
                        .filter(q -> q.getCategory().equals(key))
                        .collect(Collectors.toList());
                // считаем только разблокированные
                long done = catQuests.stream().filter(q -> data.isCompleted(q.getId())).count();
                int total = catQuests.size();
                long visible = total - done;

                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(c("&fВыполнено: &a" + done + "&7/&e" + total));
                lore.add(c(miniBar((int) done, total, 16)));
                lore.add(c("&7Осталось: &e" + visible));
                lore.add("");
                lore.add(c("&7Команда: &b" + cmdHint));
                lore.add(c(" &aНажми, &7чтобы &aначать/продолжить"));
                lore.add(c(" &aВыполнение квестов!"));

                inv.setItem(slot, make(icon, displayName, lore));
            }
        }
        player.openInventory(inv);
    }

    public void openCategory(Player player, String category) {
        QuestManager mgr = plugin.getQuestManager();
        PlayerQuestData data = mgr.getData(player);

        // Все квесты категории
        List<Quest> allQuests = mgr.getQuests().values().stream()
                .filter(q -> q.getCategory().equals(category))
                .collect(Collectors.toList());

        // ===== СОРТИРОВКА ПО ПОРЯДКУ (order) =====
        allQuests.sort(Comparator.comparingInt(Quest::getOrder));

        ConfigurationSection catCfg = plugin.getConfig().getConfigurationSection("categories." + category);
        String catDisplay = catCfg != null ? c(catCfg.getString("display", category)) : category;
        Material borderMat = parseMat(
                catCfg != null ? catCfg.getString("color", "GRAY_STAINED_GLASS_PANE") : "GRAY_STAINED_GLASS_PANE",
                Material.GRAY_STAINED_GLASS_PANE);

        String title = c("&8[ " + catDisplay + " &8]");
        titleToCategory.put(title, category);

        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Заливаем всё серым стеклом (фон)
        ItemStack filler = make(borderMat, " ", null);
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        // Кнопка назад — слот 49
        inv.setItem(49, make(Material.ARROW, c("&c◀ Назад"),
                Arrays.asList(c("&7В главное меню"))));

        // Статистика — слот 4
        long doneCount = allQuests.stream().filter(q -> data.isCompleted(q.getId())).count();
        inv.setItem(4, make(Material.BOOK, catDisplay,
                Arrays.asList(
                        c(""),
                        c("&fВыполнено: &a" + doneCount + "&7/&e" + allQuests.size()),
                        c(miniBar((int) doneCount, allQuests.size(), 22)),
                        c(""),
                        c(" &7В конце каждого квеста вы получите мини-презент!"),
                        c(" &7После завершиня квестовой-линии"),
                        c(" &7вы получите Главный Приз!")
                )));

        // ========== РАЗМЕЩЕНИЕ КВЕСТОВ ==========
        // Все внутренние слоты (4 ряда по 7 колонок) — максимум 28 квестов
        int[] questSlots = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
        };

        // Проходим по слотам, пока есть квесты
        for (int i = 0; i < questSlots.length && i < allQuests.size(); i++) {
            int slot = questSlots[i];
            Quest q = allQuests.get(i);
            if (q.isUnlocked(data)) {
                // Разблокирован — ставим иконку квеста
                inv.setItem(slot, buildQuestItem(q, data));
            } else {
                // Заблокирован — барьер с пояснением
                ItemStack barrier = make(Material.BARRIER,
                        c("&c&lЗАБЛОКИРОВАНО"),
                        Arrays.asList(
                                c("&7Этот квест пока недоступен."),
                                c("&7Выполните условия,"),
                                c("&7чтобы открыть его.")
                        ));
                inv.setItem(slot, barrier);
            }
        }

        // Открываем инвентарь
        player.openInventory(inv);
    }

    // ===================== ПОСТРОИТЬ ИКОНКУ КВЕСТА =====================
    private ItemStack buildQuestItem(Quest q, PlayerQuestData data) {
        boolean isDone = data.isCompleted(q.getId());
        int current = Math.min(data.getProgress(q.getId()), q.getAmount());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(c("&7" + q.getDescription()));
        lore.add("");
        lore.add(c("&fПрогресс: &e" + current + "&7/&e" + q.getAmount()));
        lore.add(c(progressBar(current, q.getAmount(), 22)));
        lore.add("");
        lore.add(c("&aНаграды:"));
        for (String r : q.getRewardDisplay()) lore.add(r);
        lore.add("");
        if (isDone) {
            lore.add(c("&a&l✔ ВЫПОЛНЕНО"));
        } else {
            lore.add(c("&e▸ В процессе..."));
        }

        Material icon = isDone ? Material.LIME_DYE : q.getIcon();
        return make(icon, q.getDisplayName(), lore);
    }

    // ===================== УТИЛИТЫ =====================
    private void fillBorder(Inventory inv, Material mat) {
        int size = inv.getSize();
        int rows = size / 9;
        ItemStack b = make(mat, " ", null);
        for (int col = 0; col < 9; col++) {
            inv.setItem(col, b);
            inv.setItem(size - 9 + col, b);
        }
        for (int row = 1; row < rows - 1; row++) {
            inv.setItem(row * 9, b);
            inv.setItem(row * 9 + 8, b);
        }
    }

    private String progressBar(int cur, int max, int len) {
        int filled = max == 0 ? 0 : (int) ((double) cur / max * len);
        StringBuilder bar = new StringBuilder("&8[");
        for (int i = 0; i < len; i++) bar.append(i < filled ? "&a▌" : "&7▌");
        bar.append("&8]");
        return c(bar.toString());
    }

    private String miniBar(int cur, int max, int len) {
        int filled = max == 0 ? 0 : (int) ((double) cur / max * len);
        StringBuilder bar = new StringBuilder("&8[");
        for (int i = 0; i < len; i++) bar.append(i < filled ? "&2█" : "&8█");
        bar.append("&8]");
        return c(bar.toString());
    }

    private ItemStack make(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore != null) meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static String c(String s) {
        return s == null ? "" : s.replace("&", "\u00a7");
    }

    private Material parseMat(String s, Material def) {
        try { return Material.valueOf(s.toUpperCase()); } catch (Exception e) { return def; }
    }

    public Map<String, String> getTitleToCategory() {
        return titleToCategory;
    }
}
