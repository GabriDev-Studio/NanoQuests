package org.nanodev.nanoquests.listener;

import org.nanodev.nanoquests.NanoQuests;
import org.nanodev.nanoquests.menu.QuestMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MenuListener implements Listener {

    private final NanoQuests plugin;

    public MenuListener(NanoQuests plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        QuestMenu menu = plugin.getQuestMenu();

        // Главное меню — используем константу из QuestMenu
        if (title.equals(QuestMenu.c(QuestMenu.MAIN_TITLE_RAW))) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            Material type = e.getCurrentItem().getType();
            if (type == Material.AIR
                    || type == Material.BLACK_STAINED_GLASS_PANE
                    || type == Material.GRAY_STAINED_GLASS_PANE
                    || type == Material.NETHER_STAR
            ) return;

            int slot = e.getSlot();
            org.bukkit.configuration.ConfigurationSection cats =
                    plugin.getConfig().getConfigurationSection("categories");
            if (cats == null) return;
            for (String key : cats.getKeys(false)) {
                int catSlot = plugin.getConfig().getInt("categories." + key + ".slot", -1);
                if (catSlot == slot) {
                    menu.openCategory(p, key);
                    return;
                }
            }
            return;
        }

        // Меню категории
        String category = menu.getTitleToCategory().get(title);
        if (category != null) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            if (e.getCurrentItem().getType() == Material.ARROW) {
                menu.openMain(p);
            }
        }
    }
}
