package ce.ajneb97.utils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * Inspecao completa do inventario do jogador.
 *
 * Variaveis expostas via %inv_*%:
 *  - %inv_count_<material>%           -> quantos itens de um material
 *  - %inv_has_<material>%             -> true/false se tem
 *  - %inv_first_slot_<material>%      -> primeiro slot com esse material (-1 se nenhum)
 *  - %inv_slot_<n>%                   -> material do slot N (AIR se vazio)
 *  - %inv_amount_slot_<n>%            -> quantidade de itens no slot N
 *  - %inv_name_slot_<n>%              -> nome custom do item no slot N
 *  - %inv_lore_slot_<n>%              -> lore do slot N separado por |
 *  - %inv_empty_slots%                -> quantos slots vazios
 *  - %inv_used_slots%                 -> quantos slots usados
 *  - %inv_full%                       -> true/false
 *  - %inv_armor_<slot>%               -> item no slot de armadura (helmet/chestplate/leggings/boots)
 *  - %inv_offhand%                    -> item na offhand
 */
public class InventoryInspector {

    public static int countMaterial(Player p, String materialName) {
        if(p == null) return 0;
        Material mat = parseMaterial(materialName);
        if(mat == null) return 0;
        int total = 0;
        for(ItemStack it : p.getInventory().getStorageContents()) {
            if(it != null && it.getType() == mat) total += it.getAmount();
        }
        return total;
    }

    public static boolean hasMaterial(Player p, String materialName) {
        return countMaterial(p, materialName) > 0;
    }

    public static int firstSlotOf(Player p, String materialName) {
        if(p == null) return -1;
        Material mat = parseMaterial(materialName);
        if(mat == null) return -1;
        ItemStack[] contents = p.getInventory().getStorageContents();
        for(int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if(it != null && it.getType() == mat) return i;
        }
        return -1;
    }

    public static String materialAtSlot(Player p, int slot) {
        if(p == null || !isValidSlot(slot)) return "AIR";
        ItemStack it = p.getInventory().getItem(slot);
        return it == null ? "AIR" : it.getType().name();
    }

    public static int amountAtSlot(Player p, int slot) {
        if(p == null || !isValidSlot(slot)) return 0;
        ItemStack it = p.getInventory().getItem(slot);
        return it == null ? 0 : it.getAmount();
    }

    public static String nameAtSlot(Player p, int slot) {
        if(p == null || !isValidSlot(slot)) return "";
        ItemStack it = p.getInventory().getItem(slot);
        if(it == null || !it.hasItemMeta()) return "";
        ItemMeta meta = it.getItemMeta();
        return meta.hasDisplayName() ? meta.getDisplayName() : "";
    }

    public static String loreAtSlot(Player p, int slot) {
        if(p == null || !isValidSlot(slot)) return "";
        ItemStack it = p.getInventory().getItem(slot);
        if(it == null || !it.hasItemMeta()) return "";
        ItemMeta meta = it.getItemMeta();
        if(!meta.hasLore()) return "";
        return String.join("|", meta.getLore());
    }

    public static int emptySlots(Player p) {
        if(p == null) return 0;
        int count = 0;
        for(ItemStack it : p.getInventory().getStorageContents()) {
            if(it == null || it.getType() == Material.AIR) count++;
        }
        return count;
    }

    public static int usedSlots(Player p) {
        if(p == null) return 0;
        return p.getInventory().getStorageContents().length - emptySlots(p);
    }

    public static boolean isFull(Player p) {
        return emptySlots(p) == 0;
    }

    public static String armorAt(Player p, String slotName) {
        if(p == null) return "AIR";
        PlayerInventory inv = p.getInventory();
        ItemStack it = null;
        switch(slotName.toLowerCase()) {
            case "helmet": it = inv.getHelmet(); break;
            case "chestplate": it = inv.getChestplate(); break;
            case "leggings": it = inv.getLeggings(); break;
            case "boots": it = inv.getBoots(); break;
        }
        return it == null ? "AIR" : it.getType().name();
    }

    public static String offhand(Player p) {
        if(p == null) return "AIR";
        ItemStack it = p.getInventory().getItemInOffHand();
        return it == null ? "AIR" : it.getType().name();
    }

    // =========================================================
    // ACOES
    // =========================================================

    /** giveitem_slot: <slot> <material> [amount] */
    public static boolean giveItemToSlot(Player p, int slot, String materialName, int amount) {
        if(p == null || !isValidSlot(slot)) return false;
        Material mat = parseMaterial(materialName);
        if(mat == null) return false;
        ItemStack stack = new ItemStack(mat, Math.max(1, Math.min(amount, 64)));
        p.getInventory().setItem(slot, stack);
        return true;
    }

    /** additem_slot: <slot> <material> [amount] - soma com o que ja tem */
    public static boolean addItemToSlot(Player p, int slot, String materialName, int amount) {
        if(p == null || !isValidSlot(slot)) return false;
        Material mat = parseMaterial(materialName);
        if(mat == null) return false;
        ItemStack current = p.getInventory().getItem(slot);
        if(current != null && current.getType() == mat) {
            int newAmount = Math.min(64, current.getAmount() + amount);
            current.setAmount(newAmount);
            p.getInventory().setItem(slot, current);
        } else {
            giveItemToSlot(p, slot, materialName, amount);
        }
        return true;
    }

    /** removeitem_slot: <slot> [amount] - amount=0 remove tudo */
    public static boolean removeItemAtSlot(Player p, int slot, int amount) {
        if(p == null || !isValidSlot(slot)) return false;
        ItemStack it = p.getInventory().getItem(slot);
        if(it == null) return false;
        if(amount <= 0 || amount >= it.getAmount()) {
            p.getInventory().setItem(slot, null);
        } else {
            it.setAmount(it.getAmount() - amount);
            p.getInventory().setItem(slot, it);
        }
        return true;
    }

    /** removeitems_material: <material> [amount] - remove do inventario todo */
    public static int removeMaterialFromInventory(Player p, String materialName, int amount) {
        if(p == null) return 0;
        Material mat = parseMaterial(materialName);
        if(mat == null) return 0;
        int remaining = amount <= 0 ? Integer.MAX_VALUE : amount;
        int removed = 0;
        ItemStack[] contents = p.getInventory().getStorageContents();
        for(int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack it = contents[i];
            if(it == null || it.getType() != mat) continue;
            int take = Math.min(it.getAmount(), remaining);
            int left = it.getAmount() - take;
            if(left <= 0) p.getInventory().setItem(i, null);
            else { it.setAmount(left); p.getInventory().setItem(i, it); }
            removed += take;
            remaining -= take;
        }
        return removed;
    }

    // =========================================================
    // Helpers
    // =========================================================

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot <= 40;
    }

    private static Material parseMaterial(String name) {
        if(name == null) return null;
        try {
            Material m = Material.matchMaterial(name.toUpperCase());
            if(m != null) return m;
            return Material.valueOf(name.toUpperCase());
        } catch(Exception e) {
            return null;
        }
    }


    /** giveitem_smart: <material> [amount] - acha slot livre, senao dropa no chao */
    public static boolean giveItemSmart(Player p, String materialName, int amount) {
        if(p == null) return false;
        Material mat = parseMaterial(materialName);
        if(mat == null) return false;
        ItemStack stack = new ItemStack(mat, Math.max(1, Math.min(amount, 64)));
        java.util.Map<Integer, ItemStack> leftover = p.getInventory().addItem(stack);
        for(ItemStack drop : leftover.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), drop);
        }
        return true;
    }

    /** Le PDC string de um item no slot. Retorna "" se nao existir. */
    public static String pdcStringAtSlot(Player p, int slot, String key, org.bukkit.plugin.Plugin plugin) {
        if(p == null || !isValidSlot(slot)) return "";
        ItemStack it = p.getInventory().getItem(slot);
        if(it == null || !it.hasItemMeta()) return "";
        org.bukkit.NamespacedKey nk = new org.bukkit.NamespacedKey(plugin, key.toLowerCase());
        org.bukkit.persistence.PersistentDataContainer pdc = it.getItemMeta().getPersistentDataContainer();
        if(pdc.has(nk, org.bukkit.persistence.PersistentDataType.STRING))
            return pdc.get(nk, org.bukkit.persistence.PersistentDataType.STRING);
        if(pdc.has(nk, org.bukkit.persistence.PersistentDataType.INTEGER))
            return pdc.get(nk, org.bukkit.persistence.PersistentDataType.INTEGER).toString();
        return "";
    }

    /** Nome sem cor (so o texto). */
    public static String nameCleanAtSlot(Player p, int slot) {
        String n = nameAtSlot(p, slot);
        return n.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }

    /** Tem lore? */
    public static boolean hasLoreAtSlot(Player p, int slot) {
        if(p == null || !isValidSlot(slot)) return false;
        ItemStack it = p.getInventory().getItem(slot);
        if(it == null || !it.hasItemMeta()) return false;
        return it.getItemMeta().hasLore();
    }

    /** Snapshot completo (debug/API). */
    public static Map<String, Object> snapshot(Player p) {
        Map<String, Object> map = new HashMap<>();
        if(p == null) return map;
        map.put("empty_slots", emptySlots(p));
        map.put("used_slots", usedSlots(p));
        map.put("full", isFull(p));
        return map;
    }
}