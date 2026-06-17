package org.nanodev.nanoquests.listener;

import org.nanodev.nanoquests.NanoQuests;
import org.nanodev.nanoquests.quest.Quest;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuestListener implements Listener {

    private final NanoQuests plugin;
    private final Map<UUID, Location> lastPos = new HashMap<>();

    public QuestListener(NanoQuests plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        if (e.getEntity().getKiller() == null) return;
        plugin.getQuestManager().handleProgress(
                e.getEntity().getKiller(), Quest.ObjectiveType.KILL_MOB, e.getEntityType(), 1);
    }

    @EventHandler
    public void onMine(BlockBreakEvent e) {
        plugin.getQuestManager().handleProgress(
                e.getPlayer(), Quest.ObjectiveType.MINE_BLOCK, e.getBlock().getType(), 1);
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (e.getRecipe() == null || e.getRecipe().getResult() == null) return;
        plugin.getQuestManager().handleProgress(
                (Player) e.getWhoClicked(), Quest.ObjectiveType.CRAFT_ITEM,
                e.getRecipe().getResult().getType(), 1);
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onPickup(PlayerPickupItemEvent e) {
        plugin.getQuestManager().handleProgress(
                e.getPlayer(), Quest.ObjectiveType.COLLECT_ITEM,
                e.getItem().getItemStack().getType(),
                e.getItem().getItemStack().getAmount());
    }

    @EventHandler
    public void onFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        plugin.getQuestManager().handleProgress(e.getPlayer(), Quest.ObjectiveType.FISH, null, 1);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        Location to = e.getTo();
        Location from = e.getFrom();

        if (to == null) return;
        if (to.getBlockX() == from.getBlockX() && to.getBlockZ() == from.getBlockZ()) return;

        UUID uid = p.getUniqueId();
        Location last = lastPos.get(uid);
        
        if (last == null) { lastPos.put(uid, to.clone()); return; }

        int dist = (int) last.distance(to);
        
        if (dist >= 1) {
            plugin.getQuestManager().handleProgress(p, Quest.ObjectiveType.TRAVEL, null, dist);
            lastPos.put(uid, to.clone());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getQuestManager().savePlayerData(e.getPlayer());
        lastPos.remove(e.getPlayer().getUniqueId());
    }
}
