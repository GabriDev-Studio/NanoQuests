package org.nanodev.nanoquests;

import org.nanodev.nanoquests.command.QuestCommand;
import org.nanodev.nanoquests.listener.MenuListener;
import org.nanodev.nanoquests.listener.QuestListener;
import org.nanodev.nanoquests.manager.QuestManager;
import org.nanodev.nanoquests.menu.QuestMenu;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class NanoQuests extends JavaPlugin {

    private QuestManager questManager;
    private QuestMenu questMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        questManager = new QuestManager(this);
        questManager.loadQuests();

        questMenu = new QuestMenu(this);

        QuestCommand handler = new QuestCommand(this);
        getCommand("quests").setExecutor(handler);
        getCommand("questcombat").setExecutor(handler);
        getCommand("questmine").setExecutor(handler);
        getCommand("questfish").setExecutor(handler);
        getCommand("questwood").setExecutor(handler);
        getCommand("questadmin").setExecutor(handler);
        getCommand("questskip").setExecutor(handler);

        getServer().getPluginManager().registerEvents(new QuestListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);

        getLogger().info("NanoQuests включён | Квестов: " + questManager.getQuests().size());
    }

    @Override
    public void onDisable() {
        getLogger().warning("\n Выключение плагина...");
        for (Player p : getServer().getOnlinePlayers()) {
            questManager.savePlayerData(p);
        }
        getLogger().info("NanoQuests выключен | Данные сохранены.");
    }

    public QuestManager getQuestManager() {
        return questManager;
    }
    
    public QuestMenu getQuestMenu() {
        return questMenu;
    }
}
