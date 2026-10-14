package ce.ajneb97.api;

import ce.ajneb97.ConditionalEvents;
import ce.ajneb97.managers.MessagesManager;
import ce.ajneb97.model.CEEvent;
import ce.ajneb97.utils.TimeUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ConditionalEventsAPI {

    private static ConditionalEvents plugin;
    public ConditionalEventsAPI(ConditionalEvents plugin){
        this.plugin = plugin;
    }

    public static String getEventCooldown(Player player, String event){
        CEEvent ceEvent = plugin.getEventsManager().getEvent(event);
        MessagesManager messagesManager = plugin.getMessagesManager();

        if(ceEvent == null){
            return messagesManager.getPlaceholderAPICooldownNameError();
        }

        long eventCooldownMillis = plugin.getPlayerManager().getEventCooldown(event,player)+(ceEvent.getCooldown()*1000);
        long currentTimeMillis = System.currentTimeMillis();


        if(eventCooldownMillis > currentTimeMillis){
            return TimeUtils.getTime((eventCooldownMillis-currentTimeMillis)/1000, messagesManager);
        }else{
            return messagesManager.getPlaceholderAPICooldownReady();
        }
    }

    public static String getOneTimeReady(Player player, String event){
        CEEvent ceEvent = plugin.getEventsManager().getEvent(event);

        if(ceEvent == null){
            return "no";
        }

        boolean oneTime = plugin.getPlayerManager().getEventOneTime(event,player);
        if(oneTime){
            return "yes";
        }else{
            return "no";
        }
    }

    public static boolean isEventEnabled(String event) {
        CEEvent ceEvent = plugin.getEventsManager().getEvent(event);
        if(ceEvent == null){
            return false;
        }
        return ceEvent.isEnabled();
    }

    public static void registerApiActions(JavaPlugin p, ConditionalEventsAction... actions){
        plugin.getApiManager().registerApiActions(p,actions);
    }

    public static void unregisterApiActions(JavaPlugin p){
        plugin.getApiManager().unregisterApiActions(p);
    }

    public static ConditionalEvents getPlugin() {
        return plugin;
    }
    public static ce.ajneb97.variables.VariablesManager getVariablesManager() {
        return plugin.getVariablesManager();
    }

    public static String getVariable(String playerName, String variableName) {
        ce.ajneb97.variables.VariableResult r = plugin.getVariablesManager().get(playerName, variableName);
        return r.isError() ? null : r.getResultValue();
    }

    public static ce.ajneb97.variables.VariableResult setVariable(String playerName, String variableName, String value) {
        return plugin.getVariablesManager().set(playerName, variableName, value);
    }

    public static ce.ajneb97.variables.VariableResult addVariable(String playerName, String variableName, String delta) {
        return plugin.getVariablesManager().add(playerName, variableName, delta, true);
    }

    public static ce.ajneb97.variables.VariableResult reduceVariable(String playerName, String variableName, String delta) {
        return plugin.getVariablesManager().add(playerName, variableName, delta, false);
    }

    public static ce.ajneb97.variables.VariableResult resetVariable(String playerName, String variableName) {
        return plugin.getVariablesManager().reset(playerName, variableName);
    }
}