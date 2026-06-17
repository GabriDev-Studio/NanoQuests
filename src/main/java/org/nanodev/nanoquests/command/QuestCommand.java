package org.nanodev.nanoquests.command;

import org.nanodev.nanoquests.NanoQuests;
import org.nanodev.nanoquests.manager.QuestManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuestCommand implements CommandExecutor {

    private final NanoQuests plugin;

    // { имя_команды, категория_или_null }
    private static final String[][] CMD_MAP = {
            { "quests",       null        },
            { "questcombat",  "COMBAT"    },
            { "qcombat",      "COMBAT"    },
            { "questmine",    "MINER"     },
            { "qmine",        "MINER"     },
            { "questfish",    "FISHER"    },
            { "qfish",        "FISHER"    },
            { "questwood",    "LUMBERJACK"},
            { "qwood",        "LUMBERJACK"},
    };

    public QuestCommand(NanoQuests plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName().toLowerCase();

        // --- Админ ---
        if (name.equals("questadmin")) {
            if (!sender.hasPermission("nanoquests.admin")) {
                sender.sendMessage(QuestManager.color("&c[✗] Недостаточно прав."));
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(QuestManager.color("&eИспользование: /questadmin <reload|reset <игрок>>"));
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                plugin.reloadConfig();
                plugin.getQuestManager().loadQuests();
                sender.sendMessage(QuestManager.color("&a[✔] NanoQuests перезагружен! by t.me/ERROR_92"));
                return true;
            }
            if (args[0].equalsIgnoreCase("reset") && args.length >= 2) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(QuestManager.color("&c[✗] Игрок не найден или офлайн."));
                    return true;
                }
                plugin.getQuestManager().resetPlayer(target);
                sender.sendMessage(QuestManager.color("&a[✔] Прогресс " + target.getName() + " сброшен."));
                target.sendMessage(QuestManager.color("&c[!] Твой прогресс квестов был сброшен администратором."));
                target.sendMessage(QuestManager.color("&c[!] Если это была ошибка, обратитесь в тех.поддержку!"));
                target.sendMessage(QuestManager.color("&e[?] &lT.ME/KRIPSSUP"));
                return true;
            }
            sender.sendMessage(QuestManager.color("&eИспользование: /questadmin <reload|reset <игрок>>"));
            return true;
        }

        // --- Квест-команды ---
        if (!(sender instanceof Player)) {
            sender.sendMessage("Ты не игрок для квестов -_-");
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("nanoquests.use")) {
            String no_perm = plugin.getConfig().getString("messages.no_perm", "&c[✗] Нет прав.");
            p.sendMessage(QuestManager.color(no_perm));
            return true;
        }

        for (String[] entry : CMD_MAP) {
            if (name.equals(entry[0])) {
                if (entry[1] == null) {
                    plugin.getQuestMenu().openMain(p);
                } else {
                    plugin.getQuestMenu().openCategory(p, entry[1]);
                }
                return true;
            }
        }
        return false;
    }
}
