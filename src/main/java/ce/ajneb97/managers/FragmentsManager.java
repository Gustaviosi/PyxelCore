package ce.ajneb97.managers;
import ce.ajneb97.ConditionalEvents;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.*;
public class FragmentsManager {
    private ConditionalEvents plugin; private File file;
    private Map<String,List<String>> frags = new HashMap<>();
    public FragmentsManager(ConditionalEvents plugin){ this.plugin=plugin; this.file=new File(plugin.getDataFolder(),"fragments.yml"); reload(); }
    public void reload(){
        frags.clear(); if(!file.exists()) return;
        YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
        if(c.isConfigurationSection("Fragments")) for(String k : c.getConfigurationSection("Fragments").getKeys(false))
            frags.put(k, c.getStringList("Fragments."+k));
    }
    public List<String> expand(List<String> lines){
        if(lines == null) return lines;
        List<String> out = new ArrayList<>();
        for(String l : lines){
            if(l != null && l.startsWith("include: ")){
                String name = l.substring(9).trim();
                List<String> frag = frags.get(name);
                if(frag != null) out.addAll(frag);
                else out.add(l);
            } else out.add(l);
        }
        return out;
    }
    public Set<String> names(){ return frags.keySet(); }
}