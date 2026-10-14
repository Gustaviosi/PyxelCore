package ce.ajneb97.utils.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Abstracao de scheduler pra funcionar em Bukkit/Paper/Purpur E Folia.
 * Em Folia, cada regiao tem sua propria thread, entao "rodar sync" nao faz sentido global.
 */
public interface SchedulerAdapter {

    /** Roda globalmente sync (server start, config load, etc). */
    void runSync(Plugin plugin, Runnable task);

    /** Roda em thread async (I/O, banco de dados, etc). */
    void runAsync(Plugin plugin, Runnable task);

    /** Roda depois de N ticks, global sync. */
    void runDelayed(Plugin plugin, Runnable task, long delayTicks);

    /** Roda na thread da regiao de uma entidade (player, mob, etc). */
    void runAtEntity(Plugin plugin, Entity entity, Runnable task);

    /** Roda na thread da regiao de uma entidade, com delay. */
    void runAtEntityDelayed(Plugin plugin, Entity entity, Runnable task, long delayTicks);

    /** Roda na thread da regiao de uma localizacao. */
    void runAtLocation(Plugin plugin, Location location, Runnable task);

    /** Timer async repetitivo. */
    void runTimerAsync(Plugin plugin, Runnable task, long initialDelayTicks, long periodTicks);
}