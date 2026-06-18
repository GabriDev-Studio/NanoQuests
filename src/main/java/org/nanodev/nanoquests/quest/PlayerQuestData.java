package org.nanodev.nanoquests.quest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerQuestData {

    private final Map<String, Integer> progress = new HashMap<>();
    private final Set<String> completed = new HashSet<>();
    // сколько скипов игрок уже использовал в каждой категории
    private final Map<String, Integer> skipsUsed = new HashMap<>();

    public int getProgress(String questId) {
        return progress.getOrDefault(questId, 0);
    }

    public void addProgress(String questId, int amount) {
        progress.merge(questId, amount, Integer::sum);
    }

    public void setProgress(String questId, int amount) {
        progress.put(questId, amount);
    }

    public boolean isCompleted(String questId) {
        return completed.contains(questId);
    }

    public void markCompleted(String questId) {
        completed.add(questId);
    }

    // ---------- скипы ----------
    public int getSkipsUsed(String category) {
        return skipsUsed.getOrDefault(category, 0);
    }

    public void addSkip(String category) {
        skipsUsed.merge(category, 1, Integer::sum);
    }

    public void setSkipsUsed(String category, int amount) {
        skipsUsed.put(category, amount);
    }

    public Set<String> getCompleted()        { return completed; }
    public Map<String, Integer> getProgressMap() { return progress; }
    public Map<String, Integer> getSkipsUsedMap() { return skipsUsed; }
}
