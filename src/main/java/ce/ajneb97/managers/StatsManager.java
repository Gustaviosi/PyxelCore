package ce.ajneb97.managers;
import ce.ajneb97.ConditionalEvents;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.*;
public class StatsManager {
    private ConditionalEvents plugin; private File file;
    private Map<String,long[]> data = new HashMap<>();
    public StatsManager(ConditionalEvents plugin){ this.plugin=plugin; this.file=new File(plugin.getDataFolder(),"stats.yml"); load(); }
    public void load(){
        data.clear(); if(!file.exists()) return;
        YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
        if(c.isConfigurationSection("events")) for(String k : c.getConfigurationSection("events").getKeys(false))
            data.put(k, new long[]{c.getLong("events."+k+".count"), c.getLong("events."+k+".totalMs"), c.getLong("events."+k+".last")});
    }
    public void save(){
        YamlConfiguration c = new YamlConfiguration();
        for(Map.Entry<String,long[]> e : data.entrySet()){
            c.set("events."+e.getKey()+".count", e.getValue()[0]);
            c.set("events."+e.getKey()+".totalMs", e.getValue()[1]);
            c.set("events."+e.getKey()+".last", e.getValue()[2]);
        }
        try { c.save(file); } catch(Exception ex){}
    }
    public void record(String event, long ms){
        long[] d = data.computeIfAbsent(event, k -> new long[]{0,0,0});
        d[0]++; d[1]+=ms; d[2]=System.currentTimeMillis();
    }
    public long count(String e){ long[] d=data.get(e); return d==null?0:d[0]; }
    public long avg(String e){ long[] d=data.get(e); return (d==null||d[0]==0)?0:d[1]/d[0]; }
    public long last(String e){ long[] d=data.get(e); return d==null?0:d[2]; }
    public Set<String> keys(){ return data.keySet(); }
    public void reset(String e){ data.remove(e); }
}