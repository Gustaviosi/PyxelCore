package ce.ajneb97.utils.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * Implementacao Folia - usa os schedulers regionizados.
 * Documentacao: https://docs.papermc.io/folia/reference/region-logic
 */
public class FoliaSchedulerAdapter implements SchedulerAdapter {

    @Override
    public void runSync(Plugin plugin, Runnable task) {
        // Folia: "sync global" e o GlobalRegionScheduler
        Bukkit.getGlobalRegionScheduler().run(plugin, s -> task.run());
    }

    @Override
    public void runAsync(Plugin plugin, Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, s -> task.run());
    }

    @Override
    public void runDelayed(Plugin plugin, Runnable task, long delayTicks) {
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, s -> task.run(), delayTicks);
    }

    @Override
    public void runAtEntity(Plugin plugin, Entity entity, Runnable task) {
        entity.getScheduler().run(plugin, s -> task.run(), null);
    }

    @Override
    public void runAtEntityDelayed(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
        entity.getScheduler().runDelayed(plugin, s -> task.run(), null, delayTicks);
    }

    @Override
    public void runAtLocation(Plugin plugin, Location location, Runnable task) {
        Bukkit.getRegionScheduler().run(plugin, location, s -> task.run());
    }

    @Override
    public void runTimerAsync(Plugin plugin, Runnable task, long initialDelayTicks, long periodTicks) {
        Bukkit.getAsyncScheduler().runAtFixedRate(
                plugin,
                s -> task.run(),
                initialDelayTicks * 50L,
                periodTicks * 50L,
                TimeUnit.MILLISECONDS
        );
    }
}