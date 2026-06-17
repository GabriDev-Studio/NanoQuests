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
            // requires: id предыдущего квеста (или пустая строка)
            String requires = q.getString("requires", "");

            Quest.ObjectiveType objType;
            try {
                objType = Quest.ObjectiveType.valueOf(q.getString("objective_type", "KILL_MOB"));
            } catch (Exception e) {
                plugin.getLogger().warning("Неверный objective_type у квеста " + id);
                plugin.onDisable();
                continue;
            }

            int amount = q.getInt("amount", 1);
            String rewardKit = q.getString("reward_kit", id);
            List<String> rewardDisplay = new ArrayList<>();
            for (String line : q.getStringList("reward_display")) rewardDisplay.add(color(line));

            Quest quest = new Quest(id, displayName, description, icon, category,
                    objType, amount, rewardKit, rewardDisplay, fixedSlot, requires);

            switch (objType) {
                case KILL_MOB:
                    try {
                        quest.setMob(EntityType.valueOf(q.getString("mob", "ZOMBIE")));
                    }
                    catch (Exception e) {
                        plugin.getLogger().warning("Неверный моб у квеста " + id);
                        plugin.onDisable();
                    }
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
        try {
            cfg.save(f);
        }
        catch (Exception ex) {
            plugin.getLogger().warning("Ошибка сохранения " + player.getName() + ": " + ex.getMessage());
        }
    }

    public void handleProgress(Player player, Quest.ObjectiveType type, Object target, int amount) {
        PlayerQuestData data = getData(player);
        for (Quest q : quests.values()) {
            if (data.isCompleted(q.getId())) continue;
            // не засчитываем прогресс заблокированным квестам
            if (!q.isUnlocked(data)) continue;
            if (q.getObjectiveType() != type) continue;

            boolean match = false;
            switch (type) {
                case KILL_MOB:
                    match = q.getMob() != null && target == q.getMob();
                    break;
                case MINE_BLOCK:
                case CRAFT_ITEM:
                case COLLECT_ITEM:
                    match = q.getTargetMaterial() != null && target == q.getTargetMaterial();
                    break;
                case TRAVEL:
                case FISH:
                    match = true; 
                    break;
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

    private void completeQuest(Player player, Quest quest) {
        PlayerQuestData data = getData(player);
        data.markCompleted(quest.getId());

        String msg = plugin.getConfig().getString("messages.quest_complete",
                "&a&l[Квесты] &fКвест &a{quest} &fвыполнен! Лови &aнаграду");
        player.sendMessage(color(msg.replace("{quest}", quest.getDisplayName())));

        String kitCmd = plugin.getConfig().getString("kit_command", "kit give {player} {kit}");

        kitCmd = kitCmd
            .replace("{player}", player.getName())
            .replace("{kit}", quest.getRewardKit());
        
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), kitCmd);

        savePlayerData(player);
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

    public Map<String, Quest> getQuests() {
        return quests;
    }
    
    public Quest getQuest(String id) {
        return quests.get(id);
    }
}
