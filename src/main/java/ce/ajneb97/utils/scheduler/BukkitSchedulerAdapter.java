package ce.ajneb97.utils.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Implementacao tradicional - Bukkit/Paper/Purpur/Spigot.
 * Tudo roda na main thread (sync) ou numa thread async gerenciada.
 */
public class BukkitSchedulerAdapter implements SchedulerAdapter {

    @Override
    public void runSync(Plugin plugin, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runAsync(Plugin plugin, Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runDelayed(Plugin plugin, Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    @Override
    public void runAtEntity(Plugin plugin, Entity entity, Runnable task) {
        // Em Bukkit nao existe regiao - qualquer lugar e a main thread
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runAtEntityDelayed(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    @Override
    public void runAtLocation(Plugin plugin, Location location, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runTimerAsync(Plugin plugin, Runnable task, long initialDelayTicks, long periodTicks) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, initialDelayTicks, periodTicks);
    }
}