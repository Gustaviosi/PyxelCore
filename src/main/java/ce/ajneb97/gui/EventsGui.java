package ce.ajneb97.gui;

import ce.ajneb97.ConditionalEvents;
import ce.ajneb97.managers.MessagesManager;
import ce.ajneb97.model.CEEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EventsGui implements Listener {

    private static final Map<UUID, List<String>> pageEvents = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> pageIndex = new ConcurrentHashMap<>();

    private static FileConfiguration cfg() {
        return ConditionalEvents.getPlugin(ConditionalEvents.class).getConfig();
    }

    private static String c(String path) {
        return MessagesManager.getLegacyColoredMessage(cfg().getString(path, ""));
    }

    private static String replace(String s, String... pairs) {
        for(int i = 0; i < pairs.length; i += 2) {
            s = s.replace(pairs[i], pairs[i+1]);
        }
        return s;
    }

    private static List<String> cList(String path, String... pairs) {
        List<String> result = new ArrayList<>();
        for(String l : cfg().getStringList(path)) {
            result.add(replace(MessagesManager.getLegacyColoredMessage(l), pairs));
        }
        return result;
    }

    private static ItemStack fromConfig(String base, String... pairs) {
        Material mat;
        try {
            mat = Material.valueOf(cfg().getString(base + ".Material", "PAPER").toUpperCase());
        } catch(Exception e) { mat = Material.PAPER; }
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if(cfg().contains(base + ".Name")) m.setDisplayName(replace(c(base + ".Name"), pairs));
        if(cfg().contains(base + ".Lore")) m.setLore(cList(base + ".Lore", pairs));
        it.setItemMeta(m);
        return it;
    }

    public static void openMain(Player p){
        openPage(p, 0);
    }

    public static void openPage(Player p, int pagina){
        ConditionalEvents ce = ConditionalEvents.getPlugin(ConditionalEvents.class);
        List<CEEvent> events = ce.getEventsManager().getEvents();
        int totalPag = Math.max(1, (events.size() + 44) / 45);
        if(pagina < 0) pagina = 0;
        if(pagina >= totalPag) pagina = totalPag - 1;

        String title = replace(c("Gui.EventsMenu.Title"), "%page%", (pagina+1)+"", "%total%", totalPag+"");
        Inventory inv = Bukkit.createInventory(null, 54, title);

        int start = pagina * 45;
        int end = Math.min(start + 45, events.size());
        List<String> ids = new ArrayList<>();
        int slot = 0;
        for(int i = start; i < end; i++){
            CEEvent ev = events.get(i);
            ids.add(ev.getName());
            String base = ev.isEnabled() ? "Gui.EventsMenu.EventAtivo" : "Gui.EventsMenu.EventInativo";
            inv.setItem(slot++, fromConfig(base,
                    "%event%", ev.getName(),
                    "%type%", ev.getEventType().name()));
        }
        pageEvents.put(p.getUniqueId(), ids);
        pageIndex.put(p.getUniqueId(), pagina);

        if(pagina > 0) inv.setItem(45, fromConfig("Gui.EventsMenu.Anterior"));
        if(pagina < totalPag - 1) inv.setItem(53, fromConfig("Gui.EventsMenu.Proximo"));
        inv.setItem(49, fromConfig("Gui.EventsMenu.Fechar"));
        inv.setItem(48, fromConfig("Gui.EventsMenu.Info", "%n%", events.size()+""));
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        String titleTpl = c("Gui.EventsMenu.Title");
        String titleAtual = e.getView().getTitle();
        // Compara ignorando a parte dinâmica (page/total)
        String prefixo = titleTpl.split("%")[0];
        if(!titleAtual.startsWith(prefixo)) return;

        e.setCancelled(true);
        int slot = e.getRawSlot();
        int pagina = pageIndex.getOrDefault(p.getUniqueId(), 0);
        ConditionalEvents ce = ConditionalEvents.getPlugin(ConditionalEvents.class);

        if(slot == 45){ openPage(p, pagina - 1); return; }
        if(slot == 53){ openPage(p, pagina + 1); return; }
        if(slot == 49){ p.closeInventory(); return; }
        if(slot < 0 || slot >= 45) return;

        List<String> ids = pageEvents.get(p.getUniqueId());
        if(ids == null || slot >= ids.size()) return;
        String nome = ids.get(slot);
        CEEvent ev = ce.getEventsManager().getEvent(nome);
        if(ev == null) return;

        if(e.isShiftClick()){
            p.closeInventory();
            p.performCommand("pc debug " + nome);
            return;
        }
        if(e.isRightClick()){
            p.closeInventory();
            p.performCommand("pc call " + nome + " player:" + p.getName());
            return;
        }
        if(ev.isEnabled()){
            ev.disable();
            ce.getInterruptEventManager().interruptEvent(nome, null);
            p.sendMessage(MessagesManager.getLegacyColoredMessage(c("Messages.eventDisabled").replace("%event%", nome)));
        } else {
            ev.enable();
            p.sendMessage(MessagesManager.getLegacyColoredMessage(c("Messages.eventEnabled").replace("%event%", nome)));
        }
        ce.getConfigsManager().saveEvent(ev);
        openPage(p, pagina);
    }
}