package org.nanodev.nanoquests.manager;

import org.nanodev.nanoquests.NanoQuests;
import org.nanodev.nanoquests.quest.PlayerQuestData;
import org.nanodev.nanoquests.quest.Quest;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class QuestManager {

    private final NanoQuests plugin;
    private final Map<String, Quest> quests = new LinkedHashMap<>();
    private final Map<UUID, PlayerQuestData> playerData = new HashMap<>();

    public QuestManager(NanoQuests plugin) {
        this.plugin = plugin;
    }

    public void loadQuests() {
        quests.clear();
        File questFile = new File(plugin.getDataFolder(), "quests.yml");
        if (!questFile.exists()) plugin.saveResource("quests.yml", false);

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(questFile);
        ConfigurationSection sec = cfg.getConfigurationSection("quests");
        if (sec == null) {
            plugin.getLogger().warning("quests.yml: секция 'quests' не найдена!");
            return;
        }

        for (String id : sec.getKeys(false)) {
            ConfigurationSection q = sec.getConfigurationSection(id);
            if (q == null) continue;

            String displayName = color(q.getString("display_name", id));
            String description = color(q.getString("description", ""));
            Material icon = parseMat(q.getString("icon", "DIRT"), Material.DIRT);
            String category = q.getString("category", "MISC");
            int fixedSlot = q.getInt("slot", -1);
            String requires = q.getString("requires", "");
            int order = q.getInt("order", 99);

            Quest.ObjectiveType objType;
            try {
                objType = Quest.ObjectiveType.valueOf(q.getString("objective_type", "KILL_MOB"));
            } catch (Exception e) {
                plugin.getLogger().warning("Неверный objective_type у квеста " + id);
                continue;
            }

            int amount = q.getInt("amount", 1);
            String rewardKit = q.getString("reward_kit", id);
            List<String> rewardDisplay = new ArrayList<>();
            for (String line : q.getStringList("reward_display")) rewardDisplay.add(color(line));

            Quest quest = new Quest(id, displayName, description, icon, category,
                    objType, amount, rewardKit, rewardDisplay, fixedSlot, requires);
            quest.setOrder(order);

            switch (objType) {
                case KILL_MOB:
                    try { quest.setMob(EntityType.valueOf(q.getString("mob", "ZOMBIE"))); }
                    catch (Exception e) { plugin.getLogger().warning("Неверный моб у квеста " + id); }
                    break;
                case MINE_BLOCK:
                case CRAFT_ITEM:
                case COLLECT_ITEM:
                    quest.setTargetMaterial(parseMat(
                            q.getString("block", q.getString("item", "DIRT")), Material.DIRT));
                    break;
                default: break;
            }
            quests.put(id, quest);
        }
        plugin.getLogger().info("Загружено квестов: " + quests.size());
    }

    // ===================== СКИП =====================

    /**
     * Скипает квест под номером orderNum в категории category для игрока.
     * Учитывает лимит скипов из прав nanoquests.skip.N
     * @return сообщение об ошибке или null если всё ок
     */
    public String skipQuest(Player player, String category, int orderNum) {
        PlayerQuestData data = getData(player);

        // Найдём квест по категории и order
        Quest target = null;
        for (Quest q : quests.values()) {
            if (q.getCategory().equalsIgnoreCase(category) && q.getOrder() == orderNum) {
                target = q;
                break;
            }
        }
        if (target == null) {
            return color("&c[✗] Квест с номером &e" + orderNum + " &cв линии &e" + category + " &cне найден.");
        }

        // Уже выполнен
        if (data.isCompleted(target.getId())) {
            return color("&c[✗] Этот квест уже выполнен.");
        }

        // Квест заблокирован (предыдущий не сделан)
        if (!target.isUnlocked(data)) {
            return color("&c[✗] Сначала выполни предыдущий квест в цепочке.");
        }

        // Проверяем лимит скипов (право nanoquests.skip.N — сколько скипов доступно)
        int limit = getSkipLimit(player, category);
        if (limit == 0) {
            return color("&c[✗] У тебя нет прав на скип квестов.");
        }
        int used = data.getSkipsUsed(category);
        if (limit > 0 && used >= limit) {
            return color("&c[✗] Лимит скипов для линии &e" + category + " &cисчерпан (&e" + used + "&c/&e" + limit + "&c).");
        }

        // Выполняем скип — засчитываем квест и выдаём кит
        data.markCompleted(target.getId());
        data.setProgress(target.getId(), target.getAmount());
        data.addSkip(category);
        savePlayerData(player);

        // Выдаём кит через консоль
        String kitCmd = plugin.getConfig().getString("kit_command", "kit give {player} {kit}");
        kitCmd = kitCmd.replace("{player}", player.getName()).replace("{kit}", target.getRewardKit());
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), kitCmd);

        int remaining = (limit < 0) ? -1 : (limit - used - 1);
        String remainStr = remaining < 0 ? "∞" : String.valueOf(remaining);

        String msg = color("&8[&aКвесты&8] &fКвест &a" + target.getDisplayName()
                + " &f&l(скип)&f выполнен! Осталось скипов: &e" + remainStr);
        return "\u00a7r" + msg; // префикс §r означает «успех, не ошибка»
    }

    /**
     * Возвращает лимит скипов для категории:
     *   -1 = безлимит (право nanoquests.skip.unlimited)
     *    0 = нет прав
     *   N  = максимум N скипов (право nanoquests.skip.N)
     */
    public int getSkipLimit(Player player, String category) {
        // Безлимитное право
        if (player.hasPermission("nanoquests.skip.unlimited")
                || player.hasPermission("nanoquests.skip." + category.toLowerCase() + ".unlimited")) {
            return -1;
        }
        // Ищем наибольшее N из прав nanoquests.skip.<N> или nanoquests.skip.<category>.<N>
        int max = 0;
        for (int n = 1; n <= 100; n++) {
            if (player.hasPermission("nanoquests.skip." + n)
                    || player.hasPermission("nanoquests.skip." + category.toLowerCase() + "." + n)) {
                max = n;
            }
        }
        return max;
    }

    // ===================== ПРОГРЕСС =====================

    public void handleProgress(Player player, Quest.ObjectiveType type, Object target, int amount) {
        PlayerQuestData data = getData(player);
        for (Quest q : quests.values()) {
            if (data.isCompleted(q.getId())) continue;
            if (!q.isUnlocked(data)) continue;
            if (q.getObjectiveType() != type) continue;

            boolean match = false;
            switch (type) {
                case KILL_MOB:
                    match = q.getMob() != null && target == q.getMob(); break;
                case MINE_BLOCK: case CRAFT_ITEM: case COLLECT_ITEM:
                    match = q.getTargetMaterial() != null && target == q.getTargetMaterial(); break;
                case TRAVEL: case FISH:
                    match = true; break;
            }
            if (!match) continue;

            data.addProgress(q.getId(), amount);
            int current = data.getProgress(q.getId());

            String msg = plugin.getConfig().getString("messages.quest_progress",
                    "&8[&aКвесты&8] &f{quest}&8: &e{current}&7/&e{target}");
            player.sendMessage(color(msg
                    .replace("{quest}", q.getDisplayName())
                    .replace("{current}", String.valueOf(Math.min(current, q.getAmount())))
                    .replace("{target}", String.valueOf(q.getAmount()))));

            if (current >= q.getAmount()) completeQuest(player, q);
        }
    }

    public void completeQuest(Player player, Quest quest) {
        PlayerQuestData data = getData(player);
        data.markCompleted(quest.getId());

        String msg = plugin.getConfig().getString("messages.quest_complete",
                "&a&l[Квесты] &fКвест &a{quest} &fвыполнен! Лови &aнаграду");
        player.sendMessage(color(msg.replace("{quest}", quest.getDisplayName())));

        String kitCmd = plugin.getConfig().getString("kit_command", "kit give {player} {kit}");
        kitCmd = kitCmd.replace("{player}", player.getName()).replace("{kit}", quest.getRewardKit());
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), kitCmd);

        savePlayerData(player);
    }

    // ===================== ДАННЫЕ =====================

    public PlayerQuestData getData(Player player) {
        return playerData.computeIfAbsent(player.getUniqueId(), k -> loadPlayerData(player));
    }

    private PlayerQuestData loadPlayerData(Player player) {
        PlayerQuestData data = new PlayerQuestData();
        File f = playerFile(player);
        if (!f.exists()) return data;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        for (String q : cfg.getStringList("completed")) data.markCompleted(q);
        ConfigurationSection progSec = cfg.getConfigurationSection("progress");
        if (progSec != null)
            for (String k : progSec.getKeys(false)) data.setProgress(k, progSec.getInt(k));
        // загружаем скипы
        ConfigurationSection skipSec = cfg.getConfigurationSection("skips_used");
        if (skipSec != null)
            for (String k : skipSec.getKeys(false)) data.setSkipsUsed(k, skipSec.getInt(k));
        return data;
    }

    public void savePlayerData(Player player) {
        PlayerQuestData data = playerData.get(player.getUniqueId());
        if (data == null) return;
        File f = playerFile(player);
        f.getParentFile().mkdirs();
        FileConfiguration cfg = new YamlConfiguration();
        cfg.set("completed", new ArrayList<>(data.getCompleted()));
        for (Map.Entry<String, Integer> e : data.getProgressMap().entrySet())
            cfg.set("progress." + e.getKey(), e.getValue());
        for (Map.Entry<String, Integer> e : data.getSkipsUsedMap().entrySet())
            cfg.set("skips_used." + e.getKey(), e.getValue());
        try { cfg.save(f); }
        catch (Exception ex) {
            plugin.getLogger().warning("Ошибка сохранения " + player.getName() + ": " + ex.getMessage());
        }
    }

    public void resetPlayer(Player player) {
        playerData.remove(player.getUniqueId());
        playerFile(player).delete();
    }

    private File playerFile(Player player) {
        return new File(plugin.getDataFolder(), "playerdata/" + player.getUniqueId() + ".yml");
    }

    private Material parseMat(String s, Material def) {
        try { return Material.valueOf(s.toUpperCase()); } catch (Exception e) { return def; }
    }

    public static String color(String s) {
        return s == null ? "" : s.replace("&", "\u00a7");
    }

    public Map<String, Quest> getQuests() { return quests; }
    public Quest getQuest(String id) { return quests.get(id); }
}
