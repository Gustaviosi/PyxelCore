package ce.ajneb97.managers;

import ce.ajneb97.ConditionalEvents;
import ce.ajneb97.managers.dependencies.DiscordSRVManager;
import ce.ajneb97.managers.dependencies.PacketEventsManager;
import ce.ajneb97.managers.dependencies.ProtocolLibManager;
import org.bukkit.Bukkit;

public class DependencyManager {

    private boolean placeholderAPI;
    private boolean citizens;
    private boolean worldGuardEvents;
    private boolean worldGuard;
    private boolean itemsAdder;
    private boolean paper;
    private ProtocolLibManager protocolLibManager;
    private DiscordSRVManager discordSRVManager;
    private PacketEventsManager packetEventsManager;

    public DependencyManager(ConditionalEvents plugin){
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null
            && Bukkit.getPluginManager().getPlugin("PlaceholderAPI").isEnabled()){
            placeholderAPI = true;
        }
        if(Bukkit.getPluginManager().getPlugin("Citizens") != null
            && Bukkit.getPluginManager().getPlugin("Citizens").isEnabled()){
            citizens = true;
        }
        if(Bukkit.getPluginManager().getPlugin("WorldGuard") != null
            && Bukkit.getPluginManager().getPlugin("WorldGuard").isEnabled()){
            worldGuard = true;
        }
        if(Bukkit.getPluginManager().getPlugin("ProtocolLib") != null
                && Bukkit.getPluginManager().getPlugin("ProtocolLib").isEnabled()){
            protocolLibManager = new ProtocolLibManager(plugin);
        }
        if(Bukkit.getPluginManager().getPlugin("DiscordSRV") != null
                && Bukkit.getPluginManager().getPlugin("DiscordSRV").isEnabled()){
            discordSRVManager = new DiscordSRVManager(plugin);
        }
        if(Bukkit.getPluginManager().getPlugin("ItemsAdder") != null
                && Bukkit.getPluginManager().getPlugin("ItemsAdder").isEnabled()){
            itemsAdder = true;
        }

        try{
            Class.forName("com.destroystokyo.paper.ParticleBuilder");
            paper = true;
        }catch(Exception e){

        }

        // PacketEvents desabilitado - zip file closed no Folia
    }

    public boolean isPlaceholderAPI() {
        return placeholderAPI;
    }

    public boolean isCitizens() {
        return citizens;
    }

    public boolean isWorldGuardEvents() {
        return worldGuardEvents;
    }

    public boolean isWorldGuard() {
        return worldGuard;
    }

    public ProtocolLibManager getProtocolLibManager() {
        return protocolLibManager;
    }

    public DiscordSRVManager getDiscordSRVManager() {
        return discordSRVManager;
    }

    public boolean isPaper() {
        return paper;
    }

    public boolean isItemsAdder() {
        return itemsAdder;
    }

    public PacketEventsManager getPacketEventsManager() {
        return packetEventsManager;
    }
}
