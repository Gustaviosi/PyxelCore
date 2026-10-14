package ce.ajneb97.managers;

import ce.ajneb97.ConditionalEvents;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aplica efeitos de pocao enquanto um item com PDC especifico esta equipado.
 *
 * Formato do PDC (no item):
 *   pyxelcore:effect_0  = "SLOW_FALLING|999999|0"
 *   pyxelcore:effect_1  = "SPEED|200|1"
 *
 * Onde: <potion>|<duration_ticks>|<amplifier>
 *
 * Enquanto o item estiver na mao/armadura/offhand, o efeito eh reaplicado.
 * Quando o item sai, o efeito eh removido.
 */
public class ItemEffectManager {

    private final ConditionalEvents plugin;
    private final NamespacedKey effectKeyPrefix;
    private boolean running = false;

    // Cache: quais efeitos cada player tem ativo por item
    private final Map<UUID, Set<String>> appliedEffects = new ConcurrentHashMap<>();

    public ItemEffectManager(ConditionalEvents plugin) {
        this.plugin = plugin;
        this.effectKeyPrefix = new NamespacedKey(plugin, "effect_");
    }

    public void start() {
        running = true;
        ConditionalEvents.scheduler.runTimerAsync(plugin, new Runnable() {
            @Override public void run() {
                if(!running) return;
                ConditionalEvents.scheduler.runSync(plugin, () -> tick());
            }
        }, 20L, 20L);
    }

    public void stop() {
        running = false;
        appliedEffects.clear();
    }

    private void tick() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Set<String> activeEffects = new HashSet<>();
            // Varre todos os slots relevantes (mao, offhand, armadura)
            for (ItemStack item : collectEquipped(p)) {
                if (item == null || item.getType().isAir()) continue;
                activeEffects.addAll(getEffectsFromItem(item));
            }

            Set<String> previously = appliedEffects.getOrDefault(p.getUniqueId(), new HashSet<>());

            // Aplica efeitos novos
            for (String effectStr : activeEffects) {
                if (previously.contains(effectStr)) continue;
                applyEffect(p, effectStr);
            }

            // Remove efeitos que sumiram (mas so se nao forem do vanilla)
            for (String effectStr : previously) {
                if (activeEffects.contains(effectStr)) continue;
                PotionEffectType type = parseType(effectStr);
                if (type != null) {
                    PotionEffect current = p.getPotionEffect(type);
                    // so remove se a duracao for "infinita" (a gente aplicou)
                    if (current != null && current.getDuration() > 100000) {
                        p.removePotionEffect(type);
                    }
                }
            }

            appliedEffects.put(p.getUniqueId(), activeEffects);
        }
    }

    private List<ItemStack> collectEquipped(Player p) {
        List<ItemStack> list = new ArrayList<>();
        list.add(p.getInventory().getItemInMainHand());
        list.add(p.getInventory().getItemInOffHand());
        list.add(p.getInventory().getHelmet());
        list.add(p.getInventory().getChestplate());
        list.add(p.getInventory().getLeggings());
        list.add(p.getInventory().getBoots());
        return list;
    }

    private Set<String> getEffectsFromItem(ItemStack item) {
        Set<String> effects = new HashSet<>();
        if (!item.hasItemMeta()) return effects;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        for (int i = 0; i < 10; i++) {
            NamespacedKey key = new NamespacedKey(effectKeyPrefix.getNamespace(), "effect_" + i);
            String val = pdc.get(key, PersistentDataType.STRING);
            if (val != null && !val.isEmpty()) effects.add(val);
        }
        return effects;
    }

    private void applyEffect(Player p, String effectStr) {
        String[] parts = effectStr.split("\\|");
        if (parts.length < 3) return;
        try {
            PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
            if (type == null) return;
            int duration = Integer.parseInt(parts[1]);
            int amplifier = Integer.parseInt(parts[2]);
            p.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, false));
        } catch (Exception ignored) {}
    }

    private PotionEffectType parseType(String effectStr) {
        String[] parts = effectStr.split("\\|");
        if (parts.length < 1) return null;
        return PotionEffectType.getByName(parts[0].toUpperCase());
    }

    // =========================================================
    // API pra adicionar/remover efeitos no item
    // =========================================================

    public static boolean addEffect(ConditionalEvents plugin, ItemStack item, String potion, int duration, int amplifier) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        // Acha slot livre
        int freeSlot = -1;
        for (int i = 0; i < 10; i++) {
            NamespacedKey key = new NamespacedKey(plugin, "effect_" + i);
            if (!pdc.has(key, PersistentDataType.STRING)) {
                freeSlot = i;
                break;
            }
        }
        if (freeSlot == -1) return false;
        NamespacedKey key = new NamespacedKey(plugin, "effect_" + freeSlot);
        pdc.set(key, PersistentDataType.STRING, potion.toUpperCase() + "|" + duration + "|" + amplifier);
        item.setItemMeta(meta);
        return true;
    }

    public static int clearEffects(ItemStack item, ConditionalEvents plugin) {
        if (item == null || item.getType().isAir()) return 0;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int removed = 0;
        for (int i = 0; i < 10; i++) {
            NamespacedKey key = new NamespacedKey(plugin, "effect_" + i);
            if (pdc.has(key, PersistentDataType.STRING)) {
                pdc.remove(key);
                removed++;
            }
        }
        item.setItemMeta(meta);
        return removed;
    }

    public static java.util.List<String> listEffects(ItemStack item, ConditionalEvents plugin) {
        java.util.List<String> list = new java.util.ArrayList<>();
        if (item == null || item.getType().isAir()) return list;
        if (!item.hasItemMeta()) return list;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        for (int i = 0; i < 10; i++) {
            NamespacedKey key = new NamespacedKey(plugin, "effect_" + i);
            String val = pdc.get(key, PersistentDataType.STRING);
            if (val != null) list.add(val);
        }
        return list;
    }

    public static boolean removeEffect(ItemStack item, ConditionalEvents plugin, String potion) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean removed = false;
        for (int i = 0; i < 10; i++) {
            NamespacedKey key = new NamespacedKey(plugin, "effect_" + i);
            String val = pdc.get(key, PersistentDataType.STRING);
            if (val != null && val.toUpperCase().startsWith(potion.toUpperCase() + "|")) {
                pdc.remove(key);
                removed = true;
            }
        }
        item.setItemMeta(meta);
        return removed;
    }
}