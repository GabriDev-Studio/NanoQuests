package org.nanodev.nanoquests.command;

import org.nanodev.nanoquests.NanoQuests;
import org.nanodev.nanoquests.manager.QuestManager;
import org.nanodev.nanoquests.quest.PlayerQuestData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuestCommand implements CommandExecutor {

    private final NanoQuests plugin;

    // { команда, категория или null }
    private static final String[][] CMD_MAP = {
            { "quests",       null         },
            { "questcombat",  "COMBAT"     },
            { "qcombat",      "COMBAT"     },
            { "questmine",    "MINER"      },
            { "qmine",        "MINER"      },
            { "questfish",    "FISHER"     },
            { "qfish",        "FISHER"     },
            { "questwood",    "LUMBERJACK" },
            { "qwood",        "LUMBERJACK" },
    };

    // маппинг алиасов категорий для /questskip
    private static final String[][] CATEGORY_ALIASES = {
            { "combat",      "COMBAT"     },
            { "боец",        "COMBAT"     },
            { "mine",        "MINER"      },
            { "miner",       "MINER"      },
            { "шахтёр",      "MINER"      },
            { "шахтер",      "MINER"      },
            { "fish",        "FISHER"     },
            { "fisher",      "FISHER"     },
            { "рыбак",       "FISHER"     },
            { "wood",        "LUMBERJACK" },
            { "lumberjack",  "LUMBERJACK" },
            { "лесник",      "LUMBERJACK" },
    };

    public QuestCommand(NanoQuests plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName().toLowerCase();

        // ─── /questadmin ───────────────────────────────────────────────
        if (name.equals("questadmin")) {
            if (!sender.hasPermission("nanoquests.admin")) {
                sender.sendMessage(QuestManager.color("&c[✗] Недостаточно прав."));
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(QuestManager.color("&eИспользование: /questadmin <reload|reset <игрок>|skips <игрок> <категория> <кол-во>>"));
                return true;
            }

            // reload
            if (args[0].equalsIgnoreCase("reload")) {
                plugin.reloadConfig();
                plugin.getQuestManager().loadQuests();
                sender.sendMessage(QuestManager.color("&a[✔] NanoQuests перезагружен! by t.me/ERROR_92"));
                return true;
            }

            // reset <игрок>
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

            // skips <игрок> <категория> <кол-во>  — задать использованные скипы
            if (args[0].equalsIgnoreCase("skips") && args.length >= 4) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(QuestManager.color("&c[✗] Игрок не найден или офлайн."));
                    return true;
                }
                String cat = resolveCategory(args[2]);
                if (cat == null) {
                    sender.sendMessage(QuestManager.color("&c[✗] Неизвестная категория: " + args[2]));
                    return true;
                }
                int amount;
                try { amount = Integer.parseInt(args[3]); } catch (Exception e) {
                    sender.sendMessage(QuestManager.color("&c[✗] Укажи число."));
                    return true;
                }
                PlayerQuestData data = plugin.getQuestManager().getData(target);
                data.setSkipsUsed(cat, Math.max(0, amount));
                plugin.getQuestManager().savePlayerData(target);
                sender.sendMessage(QuestManager.color("&a[✔] Использованных скипов " + target.getName() + " в " + cat + ": " + amount));
                return true;
            }

            sender.sendMessage(QuestManager.color("&eИспользование: /questadmin <reload|reset <игрок>|skips <игрок> <категория> <кол-во>>"));
            return true;
        }

        // ─── /questskip <категория> <номер квеста> ─────────────────────
        if (name.equals("questskip")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Только для игроков.");
                return true;
            }
            Player p = (Player) sender;

            if (!p.hasPermission("nanoquests.use")) {
                p.sendMessage(QuestManager.color(plugin.getConfig().getString("messages.no_perm", "&c[✗] Нет прав.")));
                return true;
            }

            if (args.length < 2) {
                p.sendMessage(QuestManager.color("&8[&aКвесты&8] &eИспользование: &f/questskip <цепочка> <номер>"));
                p.sendMessage(QuestManager.color("&7Цепочки: &bcombat&7, &bmine&7, &bfish&7, &bwood"));
                p.sendMessage(QuestManager.color("&7Пример: &f/questskip mine 3 &7— скипнуть 3-й квест в линии Шахтёра"));
                return true;
            }

            String category = resolveCategory(args[0]);
            if (category == null) {
                p.sendMessage(QuestManager.color("&c[✗] Неизвестная цепочка: &e" + args[0]
                        + "&c. Доступны: combat, mine, fish, wood"));
                return true;
            }

            int orderNum;
            try {
                orderNum = Integer.parseInt(args[1]);
            } catch (Exception e) {
                p.sendMessage(QuestManager.color("&c[✗] Номер квеста должен быть числом."));
                return true;
            }
            if (orderNum < 1) {
                p.sendMessage(QuestManager.color("&c[✗] Номер квеста должен быть >= 1."));
                return true;
            }

            // показать сколько скипов осталось если просто /questskip <cat> info
            String result = plugin.getQuestManager().skipQuest(p, category, orderNum);
            if (result.startsWith("\u00a7r")) {
                // успех
                p.sendMessage(result.substring(2));
            } else {
                // ошибка
                p.sendMessage(result);
            }
            return true;
        }

        // ─── Квест-меню команды ────────────────────────────────────────
        if (!(sender instanceof Player)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("nanoquests.use")) {
            p.sendMessage(QuestManager.color(plugin.getConfig().getString("messages.no_perm", "&c[✗] Нет прав.")));
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

    /** Разрешает alias категории в её ID */
    private String resolveCategory(String input) {
        if (input == null) return null;
        String low = input.toLowerCase();
        for (String[] pair : CATEGORY_ALIASES) {
            if (pair[0].equals(low)) return pair[1];
        }
        // попробуем напрямую
        String upper = input.toUpperCase();
        if (plugin.getConfig().getConfigurationSection("categories." + upper) != null) return upper;
        return null;
    }
}
