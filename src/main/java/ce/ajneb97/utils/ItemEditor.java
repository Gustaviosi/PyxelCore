package ce.ajneb97.utils;

import ce.ajneb97.ConditionalEvents;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Editor de item na mao do jogador.
 * Puro Bukkit API. Nao depende de CMI, NBT-API ou qualquer plugin.
 *
 * PDC = PersistentDataContainer (a API moderna do Bukkit pra guardar dados em item).
 *      Eh o jeito correto de guardar info custom sem hackear NBT.
 */
public class ItemEditor {

    private static ConditionalEvents plugin;

    public static void init(ConditionalEvents p) { plugin = p; }

    private static ItemStack getHand(Player p) {
        ItemStack it = p.getInventory().getItemInMainHand();
        if(it == null || it.getType().isAir()) return null;
        return it;
    }

    private static boolean commit(Player p, ItemStack it) {
        p.getInventory().setItemInMainHand(it);
        return true;
    }

    // =========================================================
    // NAME
    // =========================================================
    public static boolean setName(Player p, String name) {
        ItemStack it = getHand(p);
        if(it == null) return false;
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name.replace("&", "§"));
        it.setItemMeta(meta);
        return commit(p, it);
    }

    public static boolean clearName(Player p) {
        ItemStack it = getHand(p);
        if(it == null) return false;
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(null);
        it.setItemMeta(meta);
        return commit(p, it);
    }

    // =========================================================
    // LORE
    // =========================================================
    public static boolean setLore(Player p, List<String> lines) {
        ItemStack it = getHand(p);
        if(it == null) return false;
        ItemMeta meta = it.getItemMeta();
        List<String> colored = new ArrayList<>();
        for(String l : lines) colored.add(l.replace("&", "§"));
        meta.setLore(colored);
        it.setItemMeta(meta);
        return commit(p, it);
    }

    public static boolean addLore(Player p, String line) {
        ItemStack it = getHand(p);
        if(it == null) return false;
        ItemMeta meta = it.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add(line.replace("&", "§"));
        meta.setLore(lore);
        it.setItemMeta(meta);
        return commit(p, it);
    }

    public static boolean removeLore(Player p, int index) {
        ItemStack it = getHand(p);
        if(it == null) return false;
        ItemMeta meta = it.getItemMeta();
        if(!meta.hasLore()) return false;
        List<String> lore = new ArrayList<>(meta.getLore());
        if(index < 0 || index >= lore.size()) return false;
        lore.remove(index);
        meta.setLore(lore);
        it.setItemMeta(meta);
        return commit(p, it);
    }

    public static boolean clearLore(Player p) {
        ItemStack it = getHand(p);
        if(it == null) return false;
        ItemMeta meta = it.getItemMeta();
        meta.setLore(null);
        it.setItemMeta(meta);
        return commit(p, it);
    }

    // =========================================================
    // PDC (PersistentDataContainer)
    // =========================================================
    private static NamespacedKey key(String name) {
        return new NamespacedKey(plugin, name.toLowerCase().replace(" ", "_"));
    }

    public static boolean setPdcString(Player p, String key, String value) {
        ItemStack it = getHand(p);
        if(it == null) return false;
        ItemMeta meta = it.getItemMeta();
        meta.getPersistentDataContainer().set(key(key), PersistentDataType.STRING, value);
        it.setItemMeta(meta);
        return commit(p, it);
    }

    public static boolean setPdcInt(Player p, String k, int v) {
        ItemStack it = getHand(p);
        if(it == null) return false;
        ItemMeta meta = it.getItemMeta();
        meta.getPersistentDataContainer().set(key(k), PersistentDataType.INTEGER, v);
        it.setItemMeta(meta);
        return commit(p, it);
    }

    public static String getPdc(Player p, String k) {
        ItemStack it = getHand(p);
        if(it == null) return null;
        PersistentDataContainer pdc = it.getItemMeta().getPersistentDataContainer();
        if(pdc.has(key(k), PersistentDataType.STRING)) return pdc.get(key(k), PersistentDataType.STRING);
        if(pdc.has(key(k), PersistentDataType.INTEGER)) return pdc.get(key(k), PersistentDataType.INTEGER).toString();
        if(pdc.has(key(k), PersistentDataType.DOUBLE)) return pdc.get(key(k), PersistentDataType.DOUBLE).toString();
        return null;
    }

    public static boolean removePdc(Player p, String k) {
        ItemStack it = getHand(p);
        if(it == null) return false;
        ItemMeta meta = it.getItemMeta();
        meta.getPersistentDataContainer().remove(key(k));
        it.setItemMeta(meta);
        return commit(p, it);
    }

    public static Set<NamespacedKey> listPdc(Player p) {
        ItemStack it = getHand(p);
        if(it == null) return null;
        return it.getItemMeta().getPersistentDataContainer().getKeys();
    }

    // =========================================================
    // MISC
    // =========================================================
    public static boolean setUnbreakable(Player p, boolean value) {
        ItemStack it = getHand(p);
        if(it == null) return false;
        ItemMeta meta = it.getItemMeta();
        meta.setUnbreakable(value);
        it.setItemMeta(meta);
        return commit(p, it);
    }

    public static boolean clearItem(Player p) {
        p.getInventory().setItemInMainHand(null);
        return true;
    }


    // =========================================================
    // FOOD COMPONENT (Paper 1.20.5+)
    // Torna QUALQUER item comestivel (inclusive DIRT, STONE, etc)
    // =========================================================
    public static boolean setFood(Player p, int nutrition, float saturation) {
        ItemStack it = getHand(p);
        if(it == null) return false;
        ItemMeta meta = it.getItemMeta();
        try {
            org.bukkit.inventory.meta.components.FoodComponent food = meta.getFood();
            food.setNutrition(nutrition);
            food.setSaturation(saturation);
            meta.setFood(food);
        } catch(Throwable t) {
            return false;
        }
        it.setItemMeta(meta);
        return commit(p, it);
    }

    public static boolean clearFood(Player p) {
        ItemStack it = getHand(p);
        if(it == null) return false;
        ItemMeta meta = it.getItemMeta();
        try {
            meta.setFood(null);
        } catch(Throwable t) {
            return false;
        }
        it.setItemMeta(meta);
        return commit(p, it);
    }

    public static String dump(Player p) {
        ItemStack it = getHand(p);
        if(it == null) return "§cNenhum item na mao.";
        StringBuilder sb = new StringBuilder();
        sb.append("§e=== Item Info ===\n");
        sb.append("§7Material: §f").append(it.getType().name()).append("\n");
        sb.append("§7Amount: §f").append(it.getAmount()).append("\n");
        if(it.hasItemMeta()) {
            ItemMeta m = it.getItemMeta();
            if(m.hasDisplayName()) sb.append("§7Name: §f").append(m.getDisplayName()).append("\n");
            if(m.hasLore()) sb.append("§7Lore: §f").append(String.join("§7 | §f", m.getLore())).append("\n");
            sb.append("§7Unbreakable: §f").append(m.isUnbreakable()).append("\n");
            if(m.hasCustomModelData()) sb.append("§7CustomModelData: §f").append(m.getCustomModelData()).append("\n");
            PersistentDataContainer pdc = m.getPersistentDataContainer();
            if(!pdc.isEmpty()) {
                sb.append("§7PDC:\n");
                for(NamespacedKey k : pdc.getKeys()) {
                    String v = null;
                    if(pdc.has(k, PersistentDataType.STRING)) v = pdc.get(k, PersistentDataType.STRING);
                    else if(pdc.has(k, PersistentDataType.INTEGER)) v = pdc.get(k, PersistentDataType.INTEGER).toString();
                    else if(pdc.has(k, PersistentDataType.DOUBLE)) v = pdc.get(k, PersistentDataType.DOUBLE).toString();
                    sb.append("  §8- §e").append(k.getKey()).append("§7: §f").append(v).append("\n");
                }
            }
        } else {
            sb.append("§7Sem meta.\n");
        }
        return sb.toString();
    }
}