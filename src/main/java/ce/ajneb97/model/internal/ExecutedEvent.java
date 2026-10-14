package ce.ajneb97.model.internal;

import ce.ajneb97.ConditionalEvents;
import ce.ajneb97.api.ConditionalEventsEvent;
import ce.ajneb97.managers.DebugManager;
import ce.ajneb97.managers.EventsManager;
import ce.ajneb97.model.CEEvent;
import ce.ajneb97.model.StoredVariable;
import ce.ajneb97.model.ToConditionGroup;
import ce.ajneb97.model.actions.*;
import ce.ajneb97.utils.ActionUtils;
import ce.ajneb97.utils.VariablesUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

// Represent an event that has accomplished all conditions and actions
// are being executed.
public class ExecutedEvent {
    private Player player;
    private LivingEntity target;
    private ArrayList<StoredVariable> eventVariables;
    private CEEvent event;
    private Event minecraftEvent;
    private AdditionalEventStorage additionalEventStorage;
    private String actionGroupName;
    private ConditionalEvents plugin;


    private boolean isPlaceholderAPI;
    private List<CEAction> actions;

    private boolean onWait;
    private int currentActionPos;

    public ExecutedEvent(Player player, ArrayList<StoredVariable> eventVariables, CEEvent event, String actionGroupName
        , Event minecraftEvent, LivingEntity target, ConditionalEvents plugin, AdditionalEventStorage additionalEventStorage) {
        this.player = player;
        this.eventVariables = eventVariables;
        this.event = event;
        this.actionGroupName = actionGroupName;
        this.target = target;
        this.minecraftEvent = minecraftEvent;
        this.additionalEventStorage = additionalEventStorage;
        this.plugin = plugin;

        this.onWait = false;
        this.currentActionPos = 0;
    }

    private VariablesProperties generateVariableProperties() {
        Player playerTarget = null;
        if(target instanceof Player){
            playerTarget = (Player)target;
        }
        return new VariablesProperties(
                eventVariables,player,playerTarget,isPlaceholderAPI,event,minecraftEvent,additionalEventStorage
        );
    }

    public boolean setActionGroup(String actionGroupName){
        //Check if parameters are present
        int pos = actionGroupName.indexOf("{");
        if(pos != -1){
            VariablesProperties variablesProperties = generateVariableProperties();

            String parameters = actionGroupName.substring(pos+1, actionGroupName.length()-1);
            String[] sep = parameters.split(";");
            for(String s : sep) {
                int eIndex = s.indexOf("=");
                String newVariable = s.substring(0, eIndex).trim();
                String variable = s.substring(eIndex + 1).trim();
                String variableValue = VariablesUtils.replaceAllVariablesInLine(variable, variablesProperties, false);
                eventVariables.add(new StoredVariable(newVariable, variableValue));
            }
            this.actionGroupName = actionGroupName.substring(0, pos);
        }

        ActionGroup actionGroup = event.getActionGroup(this.actionGroupName);
        if(actionGroup == null){
            return false;
        }
        this.actions = new ArrayList<CEAction>(actionGroup.getActions());
        return true;
    }

    public void executeActions(){
        this.isPlaceholderAPI = plugin.getDependencyManager().isPlaceholderAPI();
        if(!setActionGroup(actionGroupName)){
            return;
        }

        //For API only
        ConditionalEventsEvent ceEvent = new ConditionalEventsEvent(player, event.getName(), actionGroupName);

        //Check cancel event or prevent join, always first to prevent issues with async events.
        executeCancelActions();

        // =========================================================
        // DELAY PADRAO DE 0.1s (2 ticks) EM TODAS AS ACOES
        // Mudar pra 1L = 0.05s / 4L = 0.2s se precisar
        // =========================================================
        final long ACTION_DELAY_TICKS = 2L;

        Runnable finalTask = () -> {
            plugin.getServer().getPluginManager().callEvent(ceEvent);
            executeActionsFinal();
        };

        if(player != null) {
            ConditionalEvents.scheduler.runAtEntityDelayed(plugin, player, finalTask, ACTION_DELAY_TICKS);
        } else {
            ConditionalEvents.scheduler.runDelayed(plugin, finalTask, ACTION_DELAY_TICKS);
        }
    }

    public void executeCancelActions(){
        for(CEAction ceAction : actions){
            if(ceAction.getType().equals(ActionType.CANCEL_EVENT)){
                ActionUtils.cancelEvent(ceAction.getActionLine(),minecraftEvent);
                return;
            }else if(ceAction.getType().equals(ActionType.PREVENT_JOIN)){
                String actionLine = ceAction.getActionLine();
                VariablesProperties variablesProperties = generateVariableProperties();
                actionLine = VariablesUtils.replaceAllVariablesInLine(actionLine,variablesProperties,false);
                ActionUtils.preventJoin(actionLine,minecraftEvent);
                return;
            }
        }
    }

    public void continueWithActions(){
        this.onWait = false;
        executeActionsFinal();
    }

    private void executeActionsFinal(){
        DebugManager debugManager = plugin.getDebugManager();
        boolean isDebugActions = plugin.getConfigsManager().getMainConfigManager().isDebugActions();
        VariablesProperties variablesProperties = generateVariableProperties();

        for(int i=currentActionPos;i<actions.size();i++){
            CEAction action = actions.get(i);
            ActionType actionType = action.getType();
            String apiType = action.getApiType();

            //Replace variables
            String actionLine = action.getActionLine();

            ActionTargeter targeter = action.getTargeter();
            ActionTargeterType targeterType = targeter.getType();

            String parametersLine = targeter.getParameter();
            if(parametersLine != null){
                parametersLine = VariablesUtils.replaceAllVariablesInLine(parametersLine,variablesProperties,false);
            }

            if(targeterType.equals(ActionTargeterType.TO_ALL)) {
                for(Player globalPlayer : Bukkit.getOnlinePlayers()) {
                    executeActionsFromToTarget(variablesProperties,globalPlayer,actionLine,actionType,apiType,isDebugActions,targeter,debugManager);
                }
            }else if(targeterType.equals(ActionTargeterType.TO_TARGET)){
                executeActionsFromToTarget(variablesProperties,target,actionLine,actionType,apiType,isDebugActions,targeter,debugManager);
            }else if(targeterType.equals(ActionTargeterType.TO_WORLD)){
                String world = parametersLine;
                for(Player globalPlayer : Bukkit.getOnlinePlayers()) {
                    if(globalPlayer.getWorld().getName().equals(world)){
                        executeActionsFromToTarget(variablesProperties,globalPlayer,actionLine,actionType,apiType,isDebugActions,targeter,debugManager);
                    }
                }
            }else if(targeterType.equals(ActionTargeterType.TO_PLAYER)){
                String playerName = parametersLine;
                Player onlinePlayer = Bukkit.getPlayer(playerName);
                if(onlinePlayer != null){
                    executeActionsFromToTarget(variablesProperties,onlinePlayer,actionLine,actionType,apiType,isDebugActions,targeter,debugManager);
                }
            }else if(targeterType.equals(ActionTargeterType.TO_RANGE)){
                String[] sep = parametersLine.split(";");
                double range = Double.parseDouble(sep[0]);
                boolean includePlayer = Boolean.parseBoolean(sep[1]);
                boolean includeEntities = false;
                if(sep.length > 2){
                    includeEntities = Boolean.parseBoolean(sep[2]);
                }

                ArrayList<LivingEntity> globalEntities = new ArrayList<>();
                if(includePlayer){
                    globalEntities.add(player);
                }
                for(Entity e : player.getWorld().getNearbyEntities(player.getLocation(), range, range, range)) {
                    if(e instanceof Player) {
                        Player p = (Player) e;
                        if(!p.getName().equals(player.getName())){
                            globalEntities.add(p);
                        }
                    }else if(includeEntities && e instanceof LivingEntity){
                        globalEntities.add((LivingEntity) e);
                    }
                }
                for(LivingEntity globalEntity : globalEntities){
                    executeActionsFromToTarget(variablesProperties,globalEntity,actionLine,actionType,apiType,isDebugActions,targeter,debugManager);
                }
            }else if(targeterType.equals(ActionTargeterType.TO_CONDITION)) {
                String toConditionGroup = parametersLine;
                ToConditionGroup group = plugin.getConfigsManager().getMainConfigManager().getToConditionGroup(toConditionGroup);
                if(group == null){
                    continue;
                }
                EventsManager eventsManager = plugin.getEventsManager();
                ArrayList<Player> players = new ArrayList<Player>();
                for(Player globalPlayer : Bukkit.getOnlinePlayers()) {
                    //Check for conditions
                    variablesProperties.setToTarget(player);
                    boolean accomplishesConditions = eventsManager.checkToConditionAction(group.getConditions()
                            ,globalPlayer,isPlaceholderAPI,event,minecraftEvent,additionalEventStorage);
                    if(accomplishesConditions){
                        players.add(globalPlayer);
                    }
                }
                for(Player globalPlayer : players){
                    executeActionsFromToTarget(variablesProperties,globalPlayer,actionLine,actionType,apiType,isDebugActions,targeter,debugManager);
                }
            }
            else {
                actionLine = VariablesUtils.replaceAllVariablesInLine(actionLine,variablesProperties,false);
                //Debug if enabled
                if(isDebugActions){
                    debugManager.sendActionMessage(event.getName(), actionLine, player, actionType, targeter);
                }
                executeAction(player,actionType,apiType,actionLine);
            }

            if(onWait){
                currentActionPos = i+1;
                return;
            }
        }
    }

    private void executeActionsFromToTarget(VariablesProperties variablesProperties,LivingEntity livingEntity,String actionLine,ActionType actionType,
                                            String apiType,boolean isDebugActions,ActionTargeter targeter,DebugManager debugManager){
        //Replaces %to:<variable>% variables
        if(livingEntity instanceof Player){
            variablesProperties.setToTarget((Player)livingEntity);
        }

        String toActionLine = VariablesUtils.replaceAllVariablesInLine(actionLine,variablesProperties,false);
        if(isDebugActions){
            debugManager.sendActionMessage(event.getName(), toActionLine, livingEntity, actionType, targeter);
        }
        executeAction(livingEntity,actionType,apiType,toActionLine);
    }

    private void executeAction(LivingEntity livingEntity,ActionType type,String apiType,String actionLine){
        if(type == ActionType.WHILE){ executeWhile(livingEntity, actionLine); return; }
        if(type == ActionType.IF){ executeIf(livingEntity, actionLine); return; }
        if(type == ActionType.FOR){ executeFor(livingEntity, actionLine); return; }
        //Non player actions
        switch(type){
            case CONSOLE_MESSAGE:
                ActionUtils.consoleMessage(actionLine);
                return;
            case CONSOLE_COMMAND:
                ActionUtils.consoleCommand(actionLine);
                return;
            case WAIT:
                ActionUtils.wait(actionLine,this);
                return;
            case WAIT_TICKS:
                ActionUtils.waitTicks(actionLine,this);
                return;
            case KEEP_ITEMS:
                ActionUtils.keepItems(actionLine,minecraftEvent);
                return;
            case CANCEL_DROP:
                ActionUtils.cancelDrop(actionLine,minecraftEvent);
                return;
            case SET_DAMAGE:
                ActionUtils.setDamage(actionLine,minecraftEvent);
                return;
            case HIDE_JOIN_MESSAGE:
                ActionUtils.hideJoinMessage(actionLine,minecraftEvent);
                return;
            case HIDE_LEAVE_MESSAGE:
                ActionUtils.hideLeaveMessage(actionLine,minecraftEvent);
                return;
            case SET_DEATH_MESSAGE:
                ActionUtils.setDeathMessage(actionLine,minecraftEvent);
                return;
            case SET_EVENT_XP:
                ActionUtils.setEventXp(actionLine,minecraftEvent);
                return;
            case DISCORDSRV_EMBED:
                ActionUtils.discordSRVEmbed(actionLine,plugin);
                return;
            case DROP_ITEM:
                ActionUtils.dropItem(actionLine);
                return;
            case SET_BLOCK:
                ActionUtils.setBlock(actionLine);
                return;
            case SET_ITEM:
                ActionUtils.setItem(actionLine,minecraftEvent);
                return;
            case SUMMON:
                ActionUtils.summon(actionLine);
                return;
            case LIGHTNING_STRIKE:
                ActionUtils.lightningStrike(actionLine);
                return;
            // Could or could not be a Living Entity event
            case FIREWORK:
                ActionUtils.firework(livingEntity, actionLine, plugin);
                return;
            case PARTICLE:
                ActionUtils.particle(livingEntity, actionLine);
                return;
            case PLAYSOUND:
                ActionUtils.playSound(livingEntity, actionLine);
                return;
            case PLAYSOUND_RESOURCE_PACK:
                ActionUtils.playSoundResourcePack(livingEntity, actionLine);
                return;
            case CALL_EVENT:
                ActionUtils.callEvent(actionLine,livingEntity,plugin,eventVariables,target,additionalEventStorage);
                return;
            case EXECUTE_ACTION_GROUP:
                ActionUtils.executeActionGroup(actionLine,this,plugin);
                return;
            case API:
                plugin.getApiManager().executeAction(apiType,livingEntity,actionLine,minecraftEvent);
                return;
        }

        //LivingEntity actions
        if(livingEntity == null){
            return;
        }

        if(livingEntity instanceof Player){
            // Just player actions
            Player player = (Player)livingEntity;
            switch(type) {
                case MESSAGE:
                    ActionUtils.message(player, actionLine);
                    return;
                case CENTERED_MESSAGE:
                    ActionUtils.centeredMessage(player, actionLine);
                    return;
                case JSON_MESSAGE:
                    ActionUtils.jsonMessage(player, actionLine);
                    return;
                case MINI_MESSAGE:
                    ActionUtils.miniMessage(player, actionLine, plugin);
                    return;
                case PLAYER_COMMAND:
                    ActionUtils.playerCommand(player, actionLine);
                    return;
                case PLAYER_COMMAND_AS_OP:
                    ActionUtils.playerCommandAsOp(player, actionLine);
                    return;
                case PLAYER_SEND_CHAT:
                    ActionUtils.playerSendChat(player, actionLine);
                    return;
                case SEND_TO_SERVER:
                    ActionUtils.sendToServer(player, actionLine, plugin);
                    return;
                case GIVE_ITEM:
                    ActionUtils.giveItem(player, actionLine);
                    return;
                case REMOVE_ITEM:
                    ActionUtils.removeItem(player, actionLine);
                    return;
                case REMOVE_ITEM_SLOT:
                    ActionUtils.removeItemSlot(player, actionLine);
                    return;
                case KICK:
                    ActionUtils.kick(player, actionLine);
                    return;
                case ACTIONBAR:
                    ActionUtils.actionbar(player, actionLine, plugin);
                    return;
                case TITLE:
                    ActionUtils.title(player, actionLine);
                    return;
                case GAMEMODE:
                    ActionUtils.gamemode(player, actionLine);
                    return;
                case CLOSE_INVENTORY:
                    ActionUtils.closeInventory(player);
                    return;
                case CLEAR_INVENTORY:
                    ActionUtils.clearInventory(player);
                    return;
                case SET_FOOD_LEVEL:
                    ActionUtils.setFoodLevel(player, actionLine);
                    return;
                case STOPSOUND:
                    ActionUtils.stopSound(player, actionLine);
                    return;
                case STOPSOUND_RESOURCE_PACK:
                    ActionUtils.stopSoundResourcePack(player, actionLine);
                    return;
                case TAB_COMPLETE:
                    ActionUtils.tabComplete(actionLine,minecraftEvent);
                    return;
            case SET_VARIABLE:
                ActionUtils.setVariable(player, actionLine);
                return;
            case ADD_VARIABLE:
                ActionUtils.addVariable(player, actionLine, true);
                return;
            case REDUCE_VARIABLE:
                ActionUtils.addVariable(player, actionLine, false);
                return;
            case RESET_VARIABLE:
                ActionUtils.resetVariable(player, actionLine);
                return;
            case SHOW_VARIABLE:
                ActionUtils.showVariable(player, actionLine);
                return;
            case SUB_VARIABLE:
                ActionUtils.subVariable(player, actionLine);
                return;
            case MUL_VARIABLE:
                ActionUtils.mulVariable(player, actionLine);
                return;
            case DIV_VARIABLE:
                ActionUtils.divVariable(player, actionLine);
                return;
            case PERCENT_VARIABLE:
                ActionUtils.percentVariable(player, actionLine);
                return;
            case SET_LOCAL_VARIABLE:
                ActionUtils.setLocalVariable(this, actionLine);
                return;
            case ADD_LOCAL_VARIABLE:
                ActionUtils.localMath(this, actionLine, "ADD");
                return;
            case SUB_LOCAL_VARIABLE:
                ActionUtils.localMath(this, actionLine, "SUB");
                return;
            case MUL_LOCAL_VARIABLE:
                ActionUtils.localMath(this, actionLine, "MUL");
                return;
            case DIV_LOCAL_VARIABLE:
                ActionUtils.localMath(this, actionLine, "DIV");
                return;
            case RESET_LOCAL_VARIABLE:
                ActionUtils.resetLocalVariable(this, actionLine);
                return;
            case GIVE_ITEM_SLOT:
                ActionUtils.giveItemToSlot(player, actionLine);
                return;
            case ADD_ITEM_SLOT:
                ActionUtils.addItemToSlot(player, actionLine);
                return;
            case REMOVE_ITEM_MATERIAL:
                ActionUtils.removeItemMaterial(player, actionLine);
                return;
            case GIVE_ITEM_SMART:
                ActionUtils.giveItemSmart(player, actionLine);
                return;
            case MAKE_FOOD:
                ActionUtils.makeFood(player, actionLine);
                return;
            case UNMAKE_FOOD:
                ActionUtils.unmakeFood(player);
                return;
            }
        }

        // Rest of actions
        switch(type) {
            case TELEPORT:
                ActionUtils.teleport(livingEntity, actionLine, minecraftEvent);
                return;
            case GIVE_POTION_EFFECT:
                ActionUtils.givePotionEffect(livingEntity, actionLine);
                return;
            case REMOVE_POTION_EFFECT:
                ActionUtils.removePotionEffect(livingEntity, actionLine);
                return;
            case DAMAGE:
                ActionUtils.damage(livingEntity, actionLine);
                return;
            case SET_ON_FIRE:
                ActionUtils.setOnFire(livingEntity, actionLine);
                return;
            case FREEZE:
                ActionUtils.freeze(livingEntity, actionLine);
                return;
            case HEAL:
                ActionUtils.heal(livingEntity, actionLine);
                return;
            case THROW_DIRECTIONAL:
                ActionUtils.throwDirectional(livingEntity, actionLine);
                return;
            case THROW_COORDINATE:
                ActionUtils.throwCoordinate(livingEntity, actionLine);
                return;
        }

    }

    public void setOnWait(boolean onWait) {
        this.onWait = onWait;
    }

    public ConditionalEvents getPlugin() {
        return plugin;
    }

    public Player getPlayer() {
        return player;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public ArrayList<StoredVariable> getEventVariables() {
        return eventVariables;
    }

    public CEEvent getEvent() {
        return event;
    }

    public Event getMinecraftEvent() {
        return minecraftEvent;
    }
    public List<CEAction> getActions() {
        return actions;
    }

    public AdditionalEventStorage getAdditionalEventStorage() {
        return additionalEventStorage;
    }
    private void executeWhile(LivingEntity le, String line){
        int i = line.indexOf(" do: "); if(i < 0) return;
        String cond = line.substring(0, i).trim();
        String inner = line.substring(i + 5).trim();
        int guard = 0;
        while(evalCond(le, cond) && guard++ < 1000) executeInlineAction(le, inner);
    }

    private void executeIf(LivingEntity le, String line){
        int t = line.indexOf(" then: "); if(t < 0) return;
        String cond = line.substring(0, t).trim();
        String rest = line.substring(t + 7).trim();
        int e = rest.indexOf(" else: ");
        String thenAct; String elseAct = null;
        if(e >= 0){ thenAct = rest.substring(0, e).trim(); elseAct = rest.substring(e + 7).trim(); }
        else { thenAct = rest; }
        if(evalCond(le, cond)) executeInlineAction(le, thenAct);
        else if(elseAct != null) executeInlineAction(le, elseAct);
    }

    private void executeFor(LivingEntity le, String line){
        int i = line.indexOf(" do: "); if(i < 0) return;
        String cs = line.substring(0, i).trim();
        String inner = line.substring(i + 5).trim();
        int n; try { n = Integer.parseInt(cs); } catch(Exception ex){ return; }
        if(n > 1000) n = 1000;
        for(int j = 0; j < n; j++) executeInlineAction(le, inner);
    }

    private boolean evalCond(LivingEntity le, String cond){
        if(le instanceof Player) cond = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders((Player)le, cond);
        String[] ops = {"==","!=","<=",">=","<",">"};
        for(String op : ops){
            int i = cond.indexOf(op); if(i < 0) continue;
            String a = cond.substring(0, i).trim();
            String b = cond.substring(i + op.length()).trim();
            Double da = null, db = null;
            try { da = Double.parseDouble(a); } catch(Exception ignored){}
            try { db = Double.parseDouble(b); } catch(Exception ignored){}
            int cmp = (da != null && db != null) ? Double.compare(da, db) : a.compareTo(b);
            if(op.equals("==")) return cmp == 0;
            if(op.equals("!=")) return cmp != 0;
            if(op.equals("<")) return cmp < 0;
            if(op.equals(">")) return cmp > 0;
            if(op.equals("<=")) return cmp <= 0;
            if(op.equals(">=")) return cmp >= 0;
        }
        return false;
    }

    private void executeInlineAction(LivingEntity le, String inner){
        int i = inner.indexOf(":"); if(i < 0) return;
        String t = inner.substring(0, i).trim().toUpperCase();
        String arg = inner.substring(i + 1).trim();
        switch(t){
            case "GIVEITEM": t = "GIVE_ITEM"; break;
            case "GIVEITEM_SLOT": t = "GIVE_ITEM_SLOT"; break;
            case "GIVEITEM_SMART": t = "GIVE_ITEM_SMART"; break;
            case "ADDITEM_SLOT": t = "ADD_ITEM_SLOT"; break;
            case "REMOVEITEM_SLOT": t = "REMOVE_ITEM_SLOT"; break;
            case "REMOVEITEM_MATERIAL": t = "REMOVE_ITEM_MATERIAL"; break;
            case "SETVAR": t = "SET_VARIABLE"; break;
            case "ADDVAR": t = "ADD_VARIABLE"; break;
            case "SUBVAR": t = "SUB_VARIABLE"; break;
            case "RESETVAR": t = "RESET_VARIABLE"; break;
        }
        try { executeAction(le, ActionType.valueOf(t), null, arg); } catch(Exception ignored){}
    }
}
