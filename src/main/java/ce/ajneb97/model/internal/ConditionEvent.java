package ce.ajneb97.model.internal;

import ce.ajneb97.ConditionalEvents;
import ce.ajneb97.configs.MainConfigManager;
import ce.ajneb97.model.CEEvent;
import ce.ajneb97.model.EventType;
import ce.ajneb97.model.StoredVariable;
import ce.ajneb97.model.internal.minimessage.GetVariablesItemLore;
import ce.ajneb97.utils.BlockUtils;
import ce.ajneb97.utils.MiniMessageUtils;
import ce.ajneb97.utils.OtherUtils;
import ce.ajneb97.utils.ServerVersion;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

// Represent an event that is being executed and conditions need
// to be checked.
public class ConditionEvent {

    private ConditionalEvents plugin;
    private Player player;
    private LivingEntity target;
    private ArrayList<StoredVariable> eventVariables;
    private Event minecraftEvent;
    private EventType eventType;
    private CEEvent currentEvent; //The current event that is being checked
    private AdditionalEventStorage additionalEventStorage;

    private boolean async;

    public ConditionEvent(ConditionalEvents plugin,Player player, Event minecraftEvent, EventType eventType
        , LivingEntity target) {
        this.plugin = plugin;
        this.player = player;
        // OTIMIZACAO: eventVariables e additionalEventStorage sao lazy-init
        // (a maioria dos triggers nao usa nenhum dos dois)
        this.minecraftEvent = minecraftEvent;
        this.eventType = eventType;
        this.target = target;
        this.async = false;
    }

    public Player getPlayer() {
        return player;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public ArrayList<StoredVariable> getEventVariables() {
        // OTIMIZACAO: lazy init - so cria quando realmente usado
        if(eventVariables == null) {
            eventVariables = new ArrayList<>();
        }
        return eventVariables;
    }

    public Event getMinecraftEvent() {
        return minecraftEvent;
    }

    public EventType getEventType() {
        return eventType;
    }

    public CEEvent getCurrentEvent() {
        return currentEvent;
    }

    public void setCurrentEvent(CEEvent currentEvent) {
        this.currentEvent = currentEvent;
        // Seed local vars declaradas com valores iniciais
        if(currentEvent != null && currentEvent.getLocalVariables() != null) {
            for(java.util.Map.Entry<String,String> e : currentEvent.getLocalVariables().entrySet()) {
                this.getAdditionalEventStorage().getLocalValues().putIfAbsent(e.getKey(), e.getValue());
            }
        }
    }

    public void checkEvent(){
        plugin.getEventsManager().checkEvent(this);
    }

    public boolean containsValidEvents(){
        ArrayList<CEEvent> validEvents = plugin.getEventsManager().getValidEvents(eventType);
        if(validEvents.size() == 0){
            return false;
        }
        return true;
    }

    public ConditionEvent addVariables(StoredVariable... storedVariables){
        for(StoredVariable v : storedVariables){
            getEventVariables().add(v);
        }
        return this;
    }

    public ConditionEvent addVariables(ArrayList<StoredVariable> storedVariables){
        getEventVariables().addAll(storedVariables);
        return this;
    }

    public ConditionEvent setCommonBlockVariables(Block block){
        Location l = block.getLocation();
        getEventVariables().add(new StoredVariable("%block_x%",l.getBlockX()+""));
        getEventVariables().add(new StoredVariable("%block_y%",l.getBlockY()+""));
        getEventVariables().add(new StoredVariable("%block_z%",l.getBlockZ()+""));
        getEventVariables().add(new StoredVariable("%block_world%",l.getWorld().getName()));
        getEventVariables().add(new StoredVariable("%block%",block.getType().name()));
        getEventVariables().add(new StoredVariable("%block_head_texture%", BlockUtils.getHeadTextureData(block)));
        if(OtherUtils.isLegacy()){
            getEventVariables().add(new StoredVariable("%block_data%",block.getData()+""));
        }else{
            getEventVariables().add(new StoredVariable("%block_data%",
                    BlockUtils.getBlockDataStringFromObject(block.getBlockData())));
        }

        return this;
    }

    public ConditionEvent setCommonActionVariables(Action action,Player player){
        String actionVariable = null;
        String actionName = action.name();
        if(player.isSneaking()) {
            if(actionName.contains("RIGHT_CLICK")) {
                actionVariable = "SHIFT_RIGHT_CLICK";
            }else if(actionName.contains("LEFT_CLICK")) {
                actionVariable = "SHIFT_LEFT_CLICK";
            }else if(action.equals(Action.PHYSICAL)) {
                actionVariable = "PHYSICAL";
            }
        }else {
            if(actionName.contains("RIGHT_CLICK")) {
                actionVariable = "RIGHT_CLICK";
            }else if(actionName.contains("LEFT_CLICK")) {
                actionVariable = "LEFT_CLICK";
            }else if(action.equals(Action.PHYSICAL)) {
                actionVariable = "PHYSICAL";
            }
        }

        if(actionVariable != null){
            getEventVariables().add(new StoredVariable("%action_type%",actionVariable));
        }
        return this;
    }

    public ConditionEvent setCommonVictimVariables(Entity entity){
        String victimType = entity.getType().name();
        String victimName = "";
        String victimNameColorFormat = "";
        String victimUuid = entity.getUniqueId().toString();

        if(entity.getCustomName() != null) {
            if(plugin.getConfigsManager().getMainConfigManager().isUseMiniMessage()){
                victimName = MiniMessageUtils.getEntityCustomNamePlain(entity);
                victimNameColorFormat = MiniMessageUtils.getEntityCustomNameMiniMessage(entity);
            }else{
                victimName = ChatColor.stripColor(entity.getCustomName());
                victimNameColorFormat = entity.getCustomName().replace("§", "&");
            }
        }
        Location location = entity.getLocation();
        double health = 0;
        if(entity instanceof LivingEntity){
            health = ((LivingEntity)entity).getHealth();
        }

        getEventVariables().add(new StoredVariable("%victim%",victimType));
        getEventVariables().add(new StoredVariable("%victim_name%",victimName));
        getEventVariables().add(new StoredVariable("%victim_health%",health+""));
        getEventVariables().add(new StoredVariable("%victim_color_format_name%",victimNameColorFormat));
        getEventVariables().add(new StoredVariable("%victim_block_x%",location.getBlockX()+""));
        getEventVariables().add(new StoredVariable("%victim_block_y%",location.getBlockY()+""));
        getEventVariables().add(new StoredVariable("%victim_block_z%",location.getBlockZ()+""));
        getEventVariables().add(new StoredVariable("%victim_block_world%",location.getWorld().getName()));
        getEventVariables().add(new StoredVariable("%victim_uuid%",victimUuid));
        return this;
    }

    public ConditionEvent setCommonItemVariables(ItemStack item,String otherItemTag){
        String name = "";
        String colorFormatName = "";
        String material = "";
        String loreString = "";
        String colorFormatLoreString = "";
        short durability = 0;
        int amount = 0;
        List<String> loreList = new ArrayList<>();
        List<String> colorFormatLoreList = new ArrayList<>();
        int customModelData = 0;
        String itemModel = "";
        String metaString = "";

        if(item != null) {
            durability = item.getDurability();
            material = item.getType().name();
            amount = item.getAmount();
            if(item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();

                MainConfigManager mainConfigManager = plugin.getConfigsManager().getMainConfigManager();
                boolean itemMetaVariableEnabled = mainConfigManager.isItemMetaVariableEnabled();
                boolean useMiniMessage = mainConfigManager.isUseMiniMessage();
                if(itemMetaVariableEnabled){
                    metaString = meta.toString();
                }else{
                    metaString = "variable disabled";
                }

                if(meta.hasDisplayName()) {
                    if(useMiniMessage){
                        name = MiniMessageUtils.getItemNamePlain(meta);
                        colorFormatName = MiniMessageUtils.getItemNameMiniMessage(meta);
                    }else{
                        name = ChatColor.stripColor(meta.getDisplayName());
                        colorFormatName = meta.getDisplayName().replace("§", "&");
                    }
                }
                if(meta.hasLore()) {
                    if(useMiniMessage){
                        GetVariablesItemLore getVariablesItemLore = MiniMessageUtils.getVariablesItemLore(
                                meta,loreList,colorFormatLoreList,loreString,colorFormatLoreString);
                        loreList = getVariablesItemLore.getLoreList();
                        colorFormatLoreList = getVariablesItemLore.getColorFormatLoreList();
                        loreString = getVariablesItemLore.getLoreString();
                        colorFormatLoreString = getVariablesItemLore.getColorFormatLoreString();
                    }else{
                        List<String> lore = meta.getLore();
                        for(int i=0;i<lore.size();i++) {
                            loreList.add(ChatColor.stripColor(lore.get(i)));
                            colorFormatLoreList.add(lore.get(i).replace("§", "&"));
                            if(i == lore.size()-1) {
                                loreString = loreString+ChatColor.stripColor(lore.get(i));
                                colorFormatLoreString = colorFormatLoreString+lore.get(i).replace("§", "&");
                            }else {
                                loreString = loreString+ChatColor.stripColor(lore.get(i))+" ";
                                colorFormatLoreString = colorFormatLoreString+lore.get(i).replace("§", "&")+" ";
                            }
                        }
                    }
                }

                ServerVersion serverVersion = ConditionalEvents.serverVersion;
                if(OtherUtils.isNew() && meta.hasCustomModelData()){
                    if(!serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_21_R3)){
                        customModelData = meta.getCustomModelData();
                    }
                }
                if(serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_21_R3)){
                    if(meta.hasItemModel()){
                        NamespacedKey key = meta.getItemModel();
                        itemModel = key.getNamespace()+":"+key.getKey();
                    }
                }
            }

            getAdditionalEventStorage().setItem(otherItemTag,item);
        }

        //Example: %offhand:<variable>%
        if(otherItemTag == null){
            otherItemTag = "%";
        }else{
            otherItemTag = "%"+otherItemTag+":";
        }

        getEventVariables().add(new StoredVariable(otherItemTag+"item%",material));
        getEventVariables().add(new StoredVariable(otherItemTag+"item_name%",name));
        getEventVariables().add(new StoredVariable(otherItemTag+"item_color_format_name%",colorFormatName));
        getEventVariables().add(new StoredVariable(otherItemTag+"item_durability%",durability+""));
        getEventVariables().add(new StoredVariable(otherItemTag+"item_amount%",amount+""));
        getEventVariables().add(new StoredVariable(otherItemTag+"item_lore%",loreString));
        getEventVariables().add(new StoredVariable(otherItemTag+"item_color_format_lore%",colorFormatLoreString));
        getEventVariables().add(new StoredVariable(otherItemTag+"item_custom_model_data%",customModelData+""));
        getEventVariables().add(new StoredVariable(otherItemTag+"item_model%",itemModel));
        getEventVariables().add(new StoredVariable(otherItemTag+"item_meta%",metaString));
        for(int i=0;i<loreList.size();i++) {
            getEventVariables().add(new StoredVariable(otherItemTag+"item_lore_line_"+(i+1)+"%", loreList.get(i)));
        }
        for(int i=0;i<colorFormatLoreList.size();i++) {
            getEventVariables().add(new StoredVariable(otherItemTag+"item_color_format_lore_line_"+(i+1)+"%", colorFormatLoreList.get(i)));
        }

        return this;
    }

    public ConditionEvent setCommonEntityVariables(Entity entity){
        String entityType = entity.getType().name();
        String entityName = "";
        String entityNameColorFormat = "";
        String entityUuid = entity.getUniqueId().toString();
        if(entity.getCustomName() != null) {
            if(plugin.getConfigsManager().getMainConfigManager().isUseMiniMessage()){
                entityName = MiniMessageUtils.getEntityCustomNamePlain(entity);
                entityNameColorFormat = MiniMessageUtils.getEntityCustomNameMiniMessage(entity);
            }else{
                entityName = ChatColor.stripColor(entity.getCustomName());
                entityNameColorFormat = entity.getCustomName().replace("§", "&");
            }
        }
        Location location = entity.getLocation();

        getEventVariables().add(new StoredVariable("%entity%",entityType));
        getEventVariables().add(new StoredVariable("%entity_name%",entityName));
        getEventVariables().add(new StoredVariable("%entity_color_format_name%",entityNameColorFormat));
        getEventVariables().add(new StoredVariable("%entity_x%",location.getBlockX()+""));
        getEventVariables().add(new StoredVariable("%entity_y%",location.getBlockY()+""));
        getEventVariables().add(new StoredVariable("%entity_z%",location.getBlockZ()+""));
        getEventVariables().add(new StoredVariable("%entity_world%",location.getWorld().getName()));
        getEventVariables().add(new StoredVariable("%entity_uuid%",entityUuid));
        return this;
    }

    public boolean isAsync() {
        return async;
    }

    public ConditionEvent setAsync(boolean async) {
        this.async = async;
        return this;
    }

    public AdditionalEventStorage getAdditionalEventStorage() {
        // OTIMIZACAO: lazy init - so cria quando realmente usado
        if(additionalEventStorage == null) {
            additionalEventStorage = new AdditionalEventStorage();
        }
        return additionalEventStorage;
    }

    public void setAdditionalEventStorage(AdditionalEventStorage additionalEventStorage) {
        this.additionalEventStorage = additionalEventStorage;
    }
}
