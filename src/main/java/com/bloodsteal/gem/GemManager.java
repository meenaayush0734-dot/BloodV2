package com.bloodsteal.gem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GemManager {

    private final JavaPlugin plugin;
    private final NamespacedKey gemKey;
    private final List<NamespacedKey> recipeKeys = new ArrayList<>();

    public GemManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gemKey = new NamespacedKey(plugin, "gem_id");
    }

    /** Creates the custom gem item (name, lore, PDC tag, custom model data). */
    public ItemStack create(Gem gem) {
        ItemStack item = new ItemStack(gem.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(text(gem.displayName(), gem.color(), true));

        List<Component> lore = new ArrayList<>();
        lore.add(text("Blood Multiplier: " + (int) Math.round(multiplierOf(gem) * 100) + "%", NamedTextColor.GRAY, false));
        lore.add(text("Ability: " + gem.abilityLabel(), NamedTextColor.GOLD, false));
        lore.add(text("Main hand mein hold karke right-click karo", NamedTextColor.DARK_GRAY, false));
        if (gem.previous() != null) {
            lore.add(text("Upgrade of: " + gem.previous().displayName(), NamedTextColor.DARK_AQUA, false));
        }
        meta.lore(lore);

        meta.setCustomModelData(gem.modelData());
        meta.getPersistentDataContainer().set(gemKey, PersistentDataType.STRING, gem.id());
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    /** Returns the Gem this item is, or null if it's not a BloodSteal gem. */
    public Gem fromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(gemKey, PersistentDataType.STRING);
        if (id == null) return null;
        try {
            return Gem.valueOf(id.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** Gem held in main hand, otherwise in offhand. */
    public Gem activeGem(Player p) {
        Gem main = fromItem(p.getInventory().getItemInMainHand());
        return main != null ? main : fromItem(p.getInventory().getItemInOffHand());
    }

    public double multiplierOf(Gem gem) {
        return plugin.getConfig().getDouble("gems." + gem.id() + ".multiplier", gem.multiplier());
    }

    public void discoverRecipes(Player p) {
        if (!recipeKeys.isEmpty()) p.discoverRecipes(recipeKeys);
    }

    public void registerRecipes() {
        // Tier 1 - no gem needed
        register(Gem.BLOOD, new String[]{"RNR", "NDN", "RNR"},
                Map.of('R', Material.REDSTONE, 'N', Material.NETHER_WART, 'D', Material.DIAMOND), ' ');
        // Tier 2 - needs Blood Gem in centre
        register(Gem.CRIMSON, new String[]{"IRI", "RGR", "IRI"},
                Map.of('I', Material.IRON_INGOT, 'R', Material.REDSTONE), 'G');
        // Tier 3 - needs Crimson Gem in centre
        register(Gem.SHADOW, new String[]{"EOE", "OCO", "EOE"},
                Map.of('E', Material.ENDER_PEARL, 'O', Material.OBSIDIAN), 'C');
        // Tier 4 - needs Shadow Gem in centre
        register(Gem.INFERNO, new String[]{"MBM", "BSB", "MBM"},
                Map.of('M', Material.MAGMA_BLOCK, 'B', Material.BLAZE_POWDER), 'S');
        // Tier 5 - needs Inferno Gem in centre
        register(Gem.AVERON, new String[]{"NTN", "NFN", "NTN"},
                Map.of('N', Material.NETHERITE_INGOT, 'T', Material.TOTEM_OF_UNDYING), 'F');
    }

    private void register(Gem gem, String[] shape, Map<Character, Material> vanilla, char prevChar) {
        NamespacedKey key = new NamespacedKey(plugin, gem.id());
        Bukkit.removeRecipe(key); // safe on /reload
        ShapedRecipe recipe = new ShapedRecipe(key, create(gem));
        recipe.shape(shape);
        for (Map.Entry<Character, Material> e : vanilla.entrySet()) {
            recipe.setIngredient(e.getKey(), e.getValue());
        }
        Gem prev = gem.previous();
        if (prev != null) {
            // ExactChoice compares the full item (name, lore, PDC), so fake or renamed items are rejected.
            recipe.setIngredient(prevChar, new RecipeChoice.ExactChoice(create(prev)));
        }
        Bukkit.addRecipe(recipe);
        recipeKeys.add(key);
    }

    private static Component text(String s, TextColor color, boolean bold) {
        Component c = Component.text(s).color(color).decoration(TextDecoration.ITALIC, false);
        return bold ? c.decorate(TextDecoration.BOLD) : c;
    }
  }
