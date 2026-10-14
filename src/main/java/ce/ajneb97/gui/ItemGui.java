package ce.ajneb97.gui;

import ce.ajneb97.ConditionalEvents;
import ce.ajneb97.managers.MessagesManager;
import ce.ajneb97.utils.ItemEditor;
import ce.ajneb97.managers.ItemEffectManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ItemGui implements Listener {

    private final ConditionalEvents plugin;
    private static final Map<UUID, String> openMenus = new ConcurrentHashMap<>();

    public ItemGui(ConditionalEvents plugin) {
        this.plugin = plugin;
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private static FileConfiguration cfg() {
        return ConditionalEvents.getPlugin(ConditionalEvents.class).getConfig();
    }

    private static String c(String path) {
        return MessagesManager.getLegacyColoredMessage(cfg().getString(path, ""));
    }

    private static List<String> cList(String path) {
        List<String> result = new ArrayList<>();
        for(String l : cfg().getStringList(path)) {
            result.add(MessagesManager.getLegacyColoredMessage(l));
        }
        return result;
    }

    private static String replace(String s, String... pairs) {
        for(int i = 0; i < pairs.length; i += 2) {
            s = s.replace(pairs[i], pairs[i+1]);
        }
        return s;
    }

    private static ItemStack fromConfig(String base, String... pairs) {
        Material mat;
        try {
            mat = Material.valueOf(cfg().getString(base + ".Material", "PAPER").toUpperCase());
        } catch(Exception e) { mat = Material.PAPER; }
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if(cfg().contains(base + ".Name")) {
            m.setDisplayName(replace(c(base + ".Name"), pairs));
        }
        if(cfg().contains(base + ".Lore")) {
            List<String> lore = new ArrayList<>();
            for(String l : cList(base + ".Lore")) lore.add(replace(l, pairs));
            m.setLore(lore);
        }
        it.setItemMeta(m);
        return it;
    }

    private static void setSlot(Inventory inv, String base, int slot, String... pairs) {
        inv.setItem(slot, fromConfig(base, pairs));
    }

    // =========================================================
    // MENU PRINCIPAL
    // =========================================================
    public static void openMain(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, c("Gui.MainItem.Title"));
        inv.setItem(4, p.getInventory().getItemInMainHand().clone());

        setSlot(inv, "Gui.MainItem.Nome", 19);
        setSlot(inv, "Gui.MainItem.Lore", 20);
        setSlot(inv, "Gui.MainItem.Comida", 21);
        setSlot(inv, "Gui.MainItem.Efeitos", 22);
        setSlot(inv, "Gui.MainItem.Pdc", 23);
        setSlot(inv, "Gui.MainItem.Unbreakable", 24);
        setSlot(inv, "Gui.MainItem.Info", 25);
        setSlot(inv, "Gui.MainItem.Salvar", 30);
        setSlot(inv, "Gui.MainItem.Limpar", 32);
        setSlot(inv, "Gui.MainItem.CopiarEfeitos", 39);
        setSlot(inv, "Gui.MainItem.CopiarItem", 41);

        ItemStack border = fromConfig("Gui.MainItem.Border");
        int[] slots = {0,1,2,3,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,49,50,51,52,53};
        for(int i : slots) if(inv.getItem(i) == null) inv.setItem(i, border);

        p.openInventory(inv);
        openMenus.put(p.getUniqueId(), "main");
    }

    // =========================================================
    // SUBMENU LORE
    // =========================================================
    public static void openLore(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, c("Gui.LoreMenu.Title"));
        ItemStack hand = p.getInventory().getItemInMainHand();
        if(hand == null || hand.getType().isAir()){ p.sendMessage(c("Messages.noItemInHand")); return; }
        inv.setItem(4, hand.clone());

        List<String> lore = hand.hasItemMeta() && hand.getItemMeta().hasLore() ? hand.getItemMeta().getLore() : new ArrayList<>();
        for(int i = 0; i < Math.min(lore.size(), 28); i++){
            setSlot(inv, "Gui.LoreMenu.Linha", 9 + i, "%n%", (i+1)+"", "%line%", lore.get(i));
        }
        setSlot(inv, "Gui.LoreMenu.Adicionar", 49);
        setSlot(inv, "Gui.LoreMenu.Voltar", 45);
        setSlot(inv, "Gui.LoreMenu.Limpar", 53);
        p.openInventory(inv);
        openMenus.put(p.getUniqueId(), "lore");
    }

    // =========================================================
    // SUBMENU EFFECT
    // =========================================================
    public static void openEffect(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, c("Gui.EffectMenu.Title"));
        ItemStack hand = p.getInventory().getItemInMainHand();
        if(hand == null || hand.getType().isAir()){ p.sendMessage(c("Messages.noItemInHand")); return; }
        inv.setItem(4, hand.clone());
        ConditionalEvents ce = ConditionalEvents.getPlugin(ConditionalEvents.class);
        List<String> efeitos = ce.getItemEffectManager() == null ? new ArrayList<>() : ItemEffectManager.listEffects(hand, ce);
        for(int i = 0; i < Math.min(efeitos.size(), 28); i++){
            String[] parts = efeitos.get(i).split("\\|");
            String potion = parts.length > 0 ? parts[0] : "?";
            String dur = parts.length > 1 ? parts[1] : "0";
            String amp = parts.length > 2 ? parts[2] : "0";
            setSlot(inv, "Gui.EffectMenu.Efeito", 9 + i, "%potion%", potion, "%dur%", dur, "%amp%", amp);
        }
        setSlot(inv, "Gui.EffectMenu.Adicionar", 49);
        setSlot(inv, "Gui.EffectMenu.Voltar", 45);
        setSlot(inv, "Gui.EffectMenu.Limpar", 53);
        p.openInventory(inv);
        openMenus.put(p.getUniqueId(), "effect");
    }

    // =========================================================
    // SUBMENU PDC
    // =========================================================
    public static void openPdc(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, c("Gui.PdcMenu.Title"));
        ItemStack hand = p.getInventory().getItemInMainHand();
        if(hand == null || hand.getType().isAir()){ p.sendMessage(c("Messages.noItemInHand")); return; }
        inv.setItem(4, hand.clone());
        Set<org.bukkit.NamespacedKey> keys = ItemEditor.listPdc(p);
        int idx = 0;
        if(keys != null) {
            for(org.bukkit.NamespacedKey k : keys){
                if(idx >= 28) break;
                String v = ItemEditor.getPdc(p, k.getKey());
                setSlot(inv, "Gui.PdcMenu.Chave", 9 + idx, "%key%", k.getKey(), "%value%", v == null ? "" : v);
                idx++;
            }
        }
        setSlot(inv, "Gui.PdcMenu.Setar", 49);
        setSlot(inv, "Gui.PdcMenu.Voltar", 45);
        setSlot(inv, "Gui.PdcMenu.Limpar", 53);
        p.openInventory(inv);
        openMenus.put(p.getUniqueId(), "pdc");
    }

    // =========================================================
    // COPIAR EFEITOS
    // =========================================================
    public static void openCopyEffects(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, c("Gui.CopyEffectsMenu.Title"));
        inv.setItem(4, p.getInventory().getItemInMainHand().clone());
        int idx = 0;
        ConditionalEvents ce = ConditionalEvents.getPlugin(ConditionalEvents.class);
        for(ItemStack it : p.getInventory().getContents()){
            if(it == null || it.getType().isAir()) continue;
            if(idx >= 45) break;
            List<String> effs = ItemEffectManager.listEffects(it, ce);
            if(effs.isEmpty()) continue;
            setSlot(inv, "Gui.CopyEffectsMenu.Item", 9 + idx, "%material%", it.getType().name(), "%n%", effs.size()+"");
            idx++;
        }
        if(idx == 0) setSlot(inv, "Gui.CopyEffectsMenu.Vazio", 22);
        setSlot(inv, "Gui.CopyEffectsMenu.Voltar", 45);
        p.openInventory(inv);
        openMenus.put(p.getUniqueId(), "copy_effects");
    }

    // =========================================================
    // COPIAR ITEM SALVO
    // =========================================================
    public static void openCopySaved(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, c("Gui.CopySavedMenu.Title"));
        inv.setItem(4, p.getInventory().getItemInMainHand().clone());
        int idx = 0;
        for(Map.Entry<String, ItemStack> entry : ConditionalEvents.getPlugin(ConditionalEvents.class).getSavedItemsManager().getSavedItems().entrySet()){
            if(idx >= 45) break;
            setSlot(inv, "Gui.CopySavedMenu.Item", 9 + idx, "%name%", entry.getKey(), "%material%", entry.getValue().getType().name());
            idx++;
        }
        if(idx == 0) setSlot(inv, "Gui.CopySavedMenu.Vazio", 22);
        setSlot(inv, "Gui.CopySavedMenu.Voltar", 45);
        p.openInventory(inv);
        openMenus.put(p.getUniqueId(), "copy_saved");
    }

    // =========================================================
    // DIALOGOS DE CHAT
    // =========================================================
    private static void prompt(Player p, String state, String... linhas){
        p.closeInventory();
        p.sendMessage("");
        p.sendMessage(c("Messages.needInput"));
        for(String l : linhas) p.sendMessage("§7" + l);
        p.sendMessage("");
        openMenus.put(p.getUniqueId(), state);
    }

    // =========================================================
    // CLICK LISTENER
    // =========================================================
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String menu = openMenus.get(p.getUniqueId());
        if(menu == null) return;
        String title = e.getView().getTitle();
        // Só processa se a GUI for de fato a nossa (título bate com algum config)
        if(!title.equals(c("Gui.MainItem.Title")) && !title.equals(c("Gui.LoreMenu.Title"))
                && !title.equals(c("Gui.EffectMenu.Title")) && !title.equals(c("Gui.PdcMenu.Title"))
                && !title.equals(c("Gui.CopyEffectsMenu.Title")) && !title.equals(c("Gui.CopySavedMenu.Title"))) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();
        ConditionalEvents ce = ConditionalEvents.getPlugin(ConditionalEvents.class);

        if(menu.equals("main")) {
            switch(slot) {
                case 19: prompt(p, "name_input", "Nome do item:"); return;
                case 20: openLore(p); return;
                case 21: prompt(p, "food_input", "<nutrition> <saturation>"); return;
                case 22: openEffect(p); return;
                case 23: openPdc(p); return;
                case 24: {
                    ItemStack hand = p.getInventory().getItemInMainHand();
                    if(hand == null || hand.getType().isAir()) return;
                    ItemMeta meta = hand.getItemMeta();
                    boolean novo = !meta.isUnbreakable();
                    meta.setUnbreakable(novo);
                    hand.setItemMeta(meta);
                    p.getInventory().setItemInMainHand(hand);
                    p.sendMessage(c("Messages.nameChanged").replace("%state%", novo+""));
                    openMain(p);
                    return;
                }
                case 25: p.closeInventory(); p.sendMessage(ItemEditor.dump(p)); return;
                case 30: prompt(p, "save_input", "Nome do item:"); return;
                case 32: p.getInventory().setItemInMainHand(null); p.sendMessage(c("Messages.itemCopied").replace("%saved%", "clear")); p.closeInventory(); return;
                case 39: openCopyEffects(p); return;
                case 41: openCopySaved(p); return;
            }
        }

        if(menu.equals("lore")) {
            if(slot == 45){ openMain(p); return; }
            if(slot == 53){ ItemEditor.clearLore(p); openLore(p); return; }
            if(slot == 49){ prompt(p, "lore_input", "Linha (use | pra várias)"); return; }
            if(slot >= 9 && slot <= 36) {
                int idx = slot - 9;
                if(ItemEditor.removeLore(p, idx)) openLore(p);
                return;
            }
        }

        if(menu.equals("effect")) {
            if(slot == 45){ openMain(p); return; }
            if(slot == 53){
                ItemStack hand = p.getInventory().getItemInMainHand();
                if(hand != null && !hand.getType().isAir()){
                    ItemEffectManager.clearEffects(hand, ce);
                    p.getInventory().setItemInMainHand(hand);
                }
                openEffect(p);
                return;
            }
            if(slot == 49){ prompt(p, "effect_input", "<potion> <duration> [amp]"); return; }
            if(slot >= 9 && slot <= 36){
                int idx = slot - 9;
                ItemStack hand = p.getInventory().getItemInMainHand();
                if(hand == null || hand.getType().isAir()) return;
                List<String> list = ItemEffectManager.listEffects(hand, ce);
                if(idx < list.size()){
                    String potion = list.get(idx).split("\\|")[0];
                    ItemEffectManager.removeEffect(hand, ce, potion);
                    p.getInventory().setItemInMainHand(hand);
                    openEffect(p);
                }
                return;
            }
        }

        if(menu.equals("pdc")) {
            if(slot == 45){ openMain(p); return; }
            if(slot == 49){ prompt(p, "pdc_input", "<chave> <valor>"); return; }
            if(slot == 53){
                Set<org.bukkit.NamespacedKey> keys = ItemEditor.listPdc(p);
                if(keys != null) for(org.bukkit.NamespacedKey k : new ArrayList<>(keys)) ItemEditor.removePdc(p, k.getKey());
                openPdc(p);
                return;
            }
            if(slot >= 9 && slot <= 36){
                int idx = slot - 9;
                Set<org.bukkit.NamespacedKey> keys = ItemEditor.listPdc(p);
                if(keys != null && idx < keys.size()){
                    List<org.bukkit.NamespacedKey> lista = new ArrayList<>(keys);
                    ItemEditor.removePdc(p, lista.get(idx).getKey());
                    openPdc(p);
                }
                return;
            }
        }

        if(menu.equals("copy_effects")) {
            if(slot == 45){ openMain(p); return; }
            if(slot >= 9 && slot <= 53) {
                ItemStack clicked = e.getCurrentItem();
                if(clicked == null || clicked.getType().isAir()) return;
                ItemStack source = null;
                for(ItemStack it : p.getInventory().getContents()){
                    if(it != null && it.getType() == clicked.getType()){
                        List<String> effs = ItemEffectManager.listEffects(it, ce);
                        if(!effs.isEmpty()){ source = it; break; }
                    }
                }
                if(source == null) return;
                ItemStack hand = p.getInventory().getItemInMainHand();
                if(hand == null || hand.getType().isAir()){ p.sendMessage(c("Messages.noItemInHand")); return; }
                int copied = 0;
                for(String eff : ItemEffectManager.listEffects(source, ce)){
                    String[] parts = eff.split("\\|");
                    if(parts.length >= 3){
                        try {
                            int dur = Integer.parseInt(parts[1]);
                            int amp = Integer.parseInt(parts[2]);
                            if(ItemEffectManager.addEffect(ce, hand, parts[0], dur, amp)) copied++;
                        } catch(Exception ignored){}
                    }
                }
                p.getInventory().setItemInMainHand(hand);
                p.sendMessage(c("Messages.effectsCopied").replace("%n%", copied+""));
                openMain(p);
                return;
            }
        }

        if(menu.equals("copy_saved")) {
            if(slot == 45){ openMain(p); return; }
            if(slot >= 9 && slot <= 53) {
                ItemStack clicked = e.getCurrentItem();
                if(clicked == null || clicked.getType().isAir()) return;
                String nome = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
                ItemStack source = ce.getSavedItemsManager().getItem(nome, p);
                if(source == null) return;
                ItemStack hand = p.getInventory().getItemInMainHand();
                if(hand == null || hand.getType().isAir()){ p.sendMessage(c("Messages.noItemInHand")); return; }
                if(source.hasItemMeta()) hand.setItemMeta(source.getItemMeta());
                p.getInventory().setItemInMainHand(hand);
                p.sendMessage(c("Messages.itemCopied").replace("%saved%", nome));
                openMain(p);
                return;
            }
        }
    }

    // =========================================================
    // CHAT INPUT LISTENER
    // =========================================================
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        String menu = openMenus.get(p.getUniqueId());
        if(menu == null) return;
        if(!menu.endsWith("_input")) return;
        e.setCancelled(true);
        String msg = e.getMessage();
        if(msg.equalsIgnoreCase("cancelar")){ openMenus.remove(p.getUniqueId()); p.sendMessage(c("Messages.inputCancelled")); return; }

        if(menu.equals("save_input")) {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if(hand == null || hand.getType().isAir()){ p.sendMessage(c("Messages.noItemInHand")); openMenus.remove(p.getUniqueId()); return; }
            plugin.getSavedItemsManager().addItem(msg, hand.clone());
            p.sendMessage(c("Messages.savedOk").replace("%name%", msg));
            openMenus.remove(p.getUniqueId());
            return;
        }

        if(menu.equals("name_input")) {
            ItemEditor.setName(p, msg);
            p.sendMessage(c("Messages.nameChanged"));
            openMenus.remove(p.getUniqueId());
            return;
        }

        if(menu.equals("food_input")) {
            String[] parts = msg.split(" ");
            if(parts.length < 2){ p.sendMessage(c("Messages.useCorrect")); openMenus.remove(p.getUniqueId()); return; }
            try {
                int n = Integer.parseInt(parts[0]);
                float s = Float.parseFloat(parts[1]);
                if(n == 0 && s == 0) { ItemEditor.clearFood(p); p.sendMessage(c("Messages.foodRemoved")); }
                else if(ItemEditor.setFood(p, n, s)) p.sendMessage(c("Messages.foodOk").replace("%n%", n+"").replace("%s%", s+""));
                else p.sendMessage(c("Messages.foodFailed"));
            } catch(Exception ex){ p.sendMessage(c("Messages.numericError")); }
            openMenus.remove(p.getUniqueId());
            return;
        }

        if(menu.equals("lore_input")) {
            List<String> linhas = Arrays.asList(msg.split("\\|"));
            ItemEditor.setLore(p, linhas);
            p.sendMessage(c("Messages.nameChanged"));
            openMenus.remove(p.getUniqueId());
            return;
        }

        if(menu.equals("effect_input")) {
            String[] parts = msg.split(" ");
            if(parts.length < 2){ p.sendMessage(c("Messages.useCorrect")); openMenus.remove(p.getUniqueId()); return; }
            ItemStack hand = p.getInventory().getItemInMainHand();
            if(hand == null || hand.getType().isAir()){ p.sendMessage(c("Messages.noItemInHand")); openMenus.remove(p.getUniqueId()); return; }
            try {
                int dur = Integer.parseInt(parts[1]);
                int amp = parts.length >= 3 ? Integer.parseInt(parts[2]) : 0;
                if(ItemEffectManager.addEffect(plugin, hand, parts[0], dur, amp)){
                    p.getInventory().setItemInMainHand(hand);
                    p.sendMessage(c("Messages.effectAdded").replace("%name%", parts[0]));
                } else p.sendMessage(c("Messages.useCorrect"));
            } catch(Exception ex){ p.sendMessage(c("Messages.numericError")); }
            openMenus.remove(p.getUniqueId());
            return;
        }

        if(menu.equals("pdc_input")) {
            String[] parts = msg.split(" ", 2);
            if(parts.length < 2){ p.sendMessage(c("Messages.useCorrect")); openMenus.remove(p.getUniqueId()); return; }
            try { ItemEditor.setPdcInt(p, parts[0], Integer.parseInt(parts[1])); }
            catch(Exception ex){ ItemEditor.setPdcString(p, parts[0], parts[1]); }
            p.sendMessage(c("Messages.pdcSet").replace("%key%", parts[0]));
            openMenus.remove(p.getUniqueId());
            return;
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if(!(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();
        String menu = openMenus.get(p.getUniqueId());
        if(menu == null) return;
        if(!menu.endsWith("_input")) {
            openMenus.remove(p.getUniqueId());
        }
    }
}