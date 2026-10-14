package ce.ajneb97.utils.scheduler;

/**
 * Detecta se o servidor atual e Folia ou Bukkit/Paper/Purpur
 * e retorna o adapter apropriado.
 *
 * Deteccao: tenta carregar uma classe que existe apenas em Folia.
 */
public class SchedulerAdapterFactory {

    private static SchedulerAdapter instance;

    public static SchedulerAdapter get() {
        if (instance == null) {
            instance = create();
        }
        return instance;
    }

    private static SchedulerAdapter create() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return new FoliaSchedulerAdapter();
        } catch (ClassNotFoundException e) {
            return new BukkitSchedulerAdapter();
        }
    }

    public static boolean isFolia() {
        return get() instanceof FoliaSchedulerAdapter;
    }
}