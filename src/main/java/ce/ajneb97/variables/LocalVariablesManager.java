package ce.ajneb97.variables;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LocalVariablesManager {

    private final Map<String, Map<String, Map<String, String>>> store = new ConcurrentHashMap<>();

    private String key(Player player) {
        return player != null ? player.getUniqueId().toString() : "SERVER";
    }

    public Map<String, String> getScope(Player player, String eventName) {
        return store
                .computeIfAbsent(key(player), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(eventName, k -> new ConcurrentHashMap<>());
    }

    public String get(Player player, String eventName, String varName) {
        return getScope(player, eventName).get(varName);
    }

    public void set(Player player, String eventName, String varName, String value) {
        getScope(player, eventName).put(varName, value);
    }

    public void remove(Player player, String eventName, String varName) {
        getScope(player, eventName).remove(varName);
    }

    public void clearScope(Player player, String eventName) {
        Map<String, Map<String, String>> byPlayer = store.get(key(player));
        if(byPlayer != null) byPlayer.remove(eventName);
    }

    public void unregisterPlayer(UUID uuid) {
        store.remove(uuid.toString());
    }

    public void clear() {
        store.clear();
    }
}