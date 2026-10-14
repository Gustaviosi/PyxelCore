package ce.ajneb97.utils;
import ce.ajneb97.ConditionalEvents;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
public class DocsGenerator {
    public static File generate(ConditionalEvents plugin) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("# PyxelCore - Referencia\n\n");
        sb.append("Gerado automaticamente em ").append(new java.util.Date()).append("\n\n");
        sb.append("## Triggers (EventType)\n\n");
        for(Object o : ce.ajneb97.model.EventType.values()) sb.append("- `").append(o).append("`\n");
        sb.append("\n## Condicoes (ConditionalType)\n\n");
        for(ce.ajneb97.model.ConditionalType t : ce.ajneb97.model.ConditionalType.values())
            sb.append("- `").append(t.getText()).append("`  (").append(t.name()).append(")\n");
        sb.append("\n## Acoes (ActionType)\n\n");
        for(Object o : ce.ajneb97.model.actions.ActionType.values()) sb.append("- `").append(o.toString().toLowerCase()).append(":`\n");
        File out = new File(plugin.getDataFolder(), "README.md");
        if(!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        Files.write(out.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        return out;
    }
}