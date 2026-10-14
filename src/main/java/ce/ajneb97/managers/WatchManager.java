package ce.ajneb97.managers;
import ce.ajneb97.ConditionalEvents;
import ce.ajneb97.model.CEEvent;
import ce.ajneb97.model.EventType;
import ce.ajneb97.model.internal.ConditionEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
public class WatchManager {
    private final ConditionalEvents plugin;
    private final CEEvent ceEvent;
    private final long ticks;
    private final String placeholder;
    private volatile boolean mustEnd;
    private volatile boolean started;
    private final Map<UUID,String> lastValues = new ConcurrentHashMap<>();
    private WatchCacheStorage cacheStorage;
    public WatchManager(ConditionalEvents plugin, CEEvent ceEvent, long ticks, String placeholder){
        this.plugin = plugin; this.ceEvent = ceEvent; this.ticks = ticks; this.placeholder = placeholder;
    }
    public boolean isStarted(){ return started; }
    public void end(){ this.mustEnd = true; this.started = false; }
    public void start(){
        this.mustEnd = false; this.started = true;
        try { this.cacheStorage = WatchCacheStorage.get(plugin); this.lastValues.putAll(cacheStorage.loadForEvent(ceEvent.getName())); } catch(Exception ignored){}
        ConditionalEvents.scheduler.runTimerAsync(plugin, new Runnable(){
            private boolean finished = false;
            @Override public void run(){
                if(finished) return;
                if(mustEnd){ finished = true; return; }
                try { tick(); } catch(Exception ex){ finished = true; }
            }
        }, 0L, ticks);
    }
    private void tick(){
        if(!Bukkit.isPrimaryThread()){
            Bukkit.getScheduler().runTask(plugin, this::tick);
            return;
        }
        for(Player p : Bukkit.getOnlinePlayers()){
            String val;
            try { val = PlaceholderAPI.setPlaceholders(p, placeholder); }
            catch(Exception e){ continue; }
            String prev = lastValues.put(p.getUniqueId(), val);
            if(cacheStorage != null){ try { cacheStorage.put(p.getUniqueId(), ceEvent.getName(), val); } catch(Exception ignored){} }
            if(prev == null || prev.equals(val)) continue;
            ConditionEvent ce = new ConditionEvent(plugin, p, null, EventType.WATCH, null);
            plugin.getEventsManager().checkSingularEvent(ce, ceEvent);
        }
    }
}