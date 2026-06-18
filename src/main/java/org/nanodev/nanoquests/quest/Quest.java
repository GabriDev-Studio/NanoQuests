package org.nanodev.nanoquests.quest;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.List;

public class Quest {

    public enum ObjectiveType {
        KILL_MOB, MINE_BLOCK, CRAFT_ITEM, COLLECT_ITEM, TRAVEL, FISH
    }

    private final String id;
    private final String displayName;
    private final String description;
    private final Material icon;
    private final String category;
    private final ObjectiveType objectiveType;
    private final int amount;
    private final String rewardKit;
    private final List<String> rewardDisplay;
    private final int fixedSlot;
    private final String requires;

    private int order = 99;
    private EntityType mob;
    private Material targetMaterial;

    public Quest(String id, String displayName, String description, Material icon,
                 String category, ObjectiveType objectiveType, int amount,
                 String rewardKit, List<String> rewardDisplay, int fixedSlot, String requires) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.category = category;
        this.objectiveType = objectiveType;
        this.amount = amount;
        this.rewardKit = rewardKit;
        this.rewardDisplay = rewardDisplay;
        this.fixedSlot = fixedSlot;
        this.requires = requires;
    }

    public boolean isUnlocked(PlayerQuestData data) {
        if (requires == null || requires.isEmpty()) return true;
        return data.isCompleted(requires);
    }

    public String getId()              { return id; }
    public String getDisplayName()     { return displayName; }
    public String getDescription()     { return description; }
    public Material getIcon()          { return icon; }
    public String getCategory()        { return category; }
    public ObjectiveType getObjectiveType() { return objectiveType; }
    public int getAmount()             { return amount; }
    public String getRewardKit()       { return rewardKit; }
    public List<String> getRewardDisplay() { return rewardDisplay; }
    public int getFixedSlot()          { return fixedSlot; }
    public String getRequires()        { return requires; }
    public int getOrder()              { return order; }
    public EntityType getMob()         { return mob; }
    public Material getTargetMaterial(){ return targetMaterial; }

    public void setOrder(int order)             { this.order = order; }
    public void setMob(EntityType mob)          { this.mob = mob; }
    public void setTargetMaterial(Material mat) { this.targetMaterial = mat; }
}
