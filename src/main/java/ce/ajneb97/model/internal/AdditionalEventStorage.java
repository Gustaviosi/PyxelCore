package ce.ajneb97.model.internal;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class AdditionalEventStorage {

    private final java.util.Map<String, String> localValues = new java.util.concurrent.ConcurrentHashMap<>();
    public java.util.Map<String, String> getLocalValues() { return localValues; }
    private Map<String,ItemStack> items;

    public AdditionalEventStorage() {
        this.items = new HashMap<>();
    }

    public void setItem(String tag, ItemStack item) {
        if(tag == null){
            items.put("normal",item);
        }else{
            items.put(tag,item);
        }
    }

    public ItemStack getStoredItem(String tag){
        return items.get(tag);
    }
}
