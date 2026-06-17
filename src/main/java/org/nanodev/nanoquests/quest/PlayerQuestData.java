package org.nanodev.nanoquests.quest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerQuestData {

    private final Map<String, Integer> progress = new HashMap<>();
    private final Set<String> completed = new HashSet<>();

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

    public Set<String> getCompleted() {
        return completed;
    }
    
    public Map<String, Integer> getProgressMap() {
        return progress;
    }
}
