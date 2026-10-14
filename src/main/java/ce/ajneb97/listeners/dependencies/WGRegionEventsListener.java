package ce.ajneb97.listeners.dependencies;

import ce.ajneb97.ConditionalEvents;
import ce.ajneb97.model.EventType;
import ce.ajneb97.model.StoredVariable;
import ce.ajneb97.model.internal.ConditionEvent;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema nativo de deteccao de entrada/saida em regioes do WorldGuard 7.x.
 *
 * SUBSTITUI o plugin externo WorldGuardEvents (abandonado).
 *
 * Como funciona:
 *  - PlayerMoveEvent (priority MONITOR) dispara 20x/s, MAS
 *    a gente so processa se o player MUDOU DE BLOCO. Reduz ~95%.
 *  - Compara o set de regioes atuais com o ultimo salvo em cache.
 *  - Se entrou em regiao nova -> WGEVENTS_REGION_ENTER
 *  - Se saiu de regiao -> WGEVENTS_REGION_LEAVE
 *
 * Variaveis fornecidas:
 *  - %region%         -> ID da regiao
 *  - %region_priority%-> prioridade da regiao
 *  - %region_owner%   -> UUID do dono (pode ser vazio)
 *  - %region_members% -> lista de membros separada por virgula
 */
public class WGRegionEventsListener implements Listener {

    private final ConditionalEvents plugin;
    private final Map<UUID, Set<String>> lastRegions = new ConcurrentHashMap<>();

    public WGRegionEventsListener(ConditionalEvents plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        processRegionChange(event.getPlayer(), to);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null) return;
        processRegionChange(event.getPlayer(), event.getTo());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastRegions.remove(event.getPlayer().getUniqueId());
    }

    /** Metodo publico pra forcar refresh (usado pelo REGION_ENTER de outras fontes). */
    public void refresh(Player player) {
        processRegionChange(player, player.getLocation());
    }

    private void processRegionChange(Player player, Location to) {
        if (plugin.getDependencyManager() == null) return;
        if (!plugin.getDependencyManager().isWorldGuard()) return;

        // Cache: se nao tem nenhum evento WG configurado, nem processa
        if (plugin.getEventsManager().getValidEvents(EventType.WGEVENTS_REGION_ENTER).isEmpty()
                && plugin.getEventsManager().getValidEvents(EventType.WGEVENTS_REGION_LEAVE).isEmpty()) {
            return;
        }

        Set<String> current = getRegionsAt(to);
        Set<String> previous = lastRegions.getOrDefault(player.getUniqueId(), new HashSet<>());

        if (current.equals(previous)) return;

        // Regioes que ENTRARAM
        for (String region : current) {
            if (!previous.contains(region)) {
                trigger(player, region, EventType.WGEVENTS_REGION_ENTER, to);
            }
        }
        // Regioes que SAIRAM
        for (String region : previous) {
            if (!current.contains(region)) {
                trigger(player, region, EventType.WGEVENTS_REGION_LEAVE, to);
            }
        }

        lastRegions.put(player.getUniqueId(), current);
    }

    private void trigger(Player player, String regionId, EventType type, Location loc) {
        ConditionEvent ce = new ConditionEvent(plugin, player, null, type, null)
                .addVariables(new StoredVariable("%region%", regionId));

        // Variaveis extras da regiao
        try {
            RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(loc.getWorld()));
            if (rm != null) {
                ProtectedRegion region = rm.getRegion(regionId);
                if (region != null) {
                    ce.addVariables(
                            new StoredVariable("%region_priority%", region.getPriority() + ""),
                            new StoredVariable("%region_owner%",
                                    region.getOwners().getUniqueIds().isEmpty()
                                            ? "" : region.getOwners().getUniqueIds().iterator().next().toString()),
                            new StoredVariable("%region_members%",
                                    String.join(",", region.getMembers().getUniqueIds().stream()
                                            .map(UUID::toString).toArray(String[]::new)))
                    );
                }
            }
        } catch (Throwable ignored) {}

        ce.checkEvent();
    }

    private Set<String> getRegionsAt(Location loc) {
        Set<String> regions = new HashSet<>();
        try {
            com.sk89q.worldedit.util.Location wgLoc = BukkitAdapter.adapt(loc);
            ApplicableRegionSet set = WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .createQuery().getApplicableRegions(wgLoc);
            for (ProtectedRegion region : set) {
                regions.add(region.getId());
            }
        } catch (Throwable ignored) {}
        return regions;
    }
}