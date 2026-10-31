package com.bloodsteal.blood;

import com.bloodsteal.gem.Gem;
import com.bloodsteal.gem.GemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class BloodManager {

    private static final int BAR_SEGMENTS = 20;

    private final JavaPlugin plugin;
    private final GemManager gems;
    private final Map<UUID, Double> blood = new HashMap<>();
    private final NamespacedKey vialKey;
    private final File dataFile;

    public BloodManager(JavaPlugin plugin, GemManager gems) {
        this.plugin = plugin;
        this.gems = gems;
        this.vialKey = new NamespacedKey(plugin, "vial_amount");
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        load();
    }

    private FileConfiguration cfg() {
        return plugin.getConfig();
    }

    /** Max blood depends on the gem the player holds (main hand or offhand). */
    public double getMaxBlood(Player p) {
        double base = cfg().getDouble("settings.base-max-blood", 200.0);
        Gem gem = gems.activeGem(p);
        if (gem == null) return base;
        double mult = gems.multiplierOf(gem);
        boolean setMode = "SET".equalsIgnoreCase(cfg().getString("settings.gem-multiplier-mode", "MULTIPLY"));
        return setMode ? mult * 100.0 : base * mult;
    }

    public double getBlood(Player p) {
        return blood.computeIfAbsent(p.getUniqueId(), id -> cfg().getDouble("settings.starting-blood", 200.0));
    }

    /** Sets blood clamped to [0, max]. Returns the stored value. */
    public double setBlood(Player p, double value) {
        double clamped = Math.max(0, Math.min(value, getMaxBlood(p)));
        blood.put(p.getUniqueId(), clamped);
        return clamped;
    }

    /** PvP kill: victim loses N%, killer gains what was actually taken. Normal deaths never call this. */
    public void handlePvpKill(Player victim, Player killer) {
        double amount = cfg().getDouble("settings.pvp-steal-amount", 10.0);
        double stolen = Math.min(amount, getBlood(victim));
        if (stolen <= 0) return;

        setBlood(victim, getBlood(victim) - stolen);
        setBlood(killer, getBlood(killer) + stolen);

        victim.sendMessage(Component.text(String.format(Locale.US, "Tumne %.1f%% blood kho diya!", stolen), NamedTextColor.RED));
        killer.sendMessage(Component.text(String.format(Locale.US, "+%.1f%% blood cheen liya!", stolen), NamedTextColor.DARK_RED));
        checkDepleted(victim);
    }

    /** /bwithdraw: moves up to `amount` blood into a Blood Vial item. Returns the amount actually taken. */
    public double withdraw(Player p, double amount) {
        double current = getBlood(p);
        double taken = Math.min(amount, current);
        if (taken <= 0) return 0;

        setBlood(p, current - taken);
        giveVial(p, taken);
        checkDepleted(p);
        return taken;
    }

    private void checkDepleted(Player p) {
        if (!cfg().getBoolean("settings.ban-on-zero", true)) return;
        if (getBlood(p) > 0) return;

        String reason = cfg().getString("settings.ban-reason", "Blood khatam ho gaya");
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getBanList(BanList.Type.NAME).addBan(p.getName(), reason, null, "BloodSteal");
            p.kickPlayer(reason);
        });
    }

    private void giveVial(Player p, double amount) {
        ItemStack vial = new ItemStack(Material.HONEY_BOTTLE);
        ItemMeta meta = vial.getItemMeta();
        meta.displayName(Component.text(String.format(Locale.US, "Blood Vial (%.1f%%)", amount), NamedTextColor.DARK_RED)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Blood se bhara hua vial", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        meta.setMaxStackSize(1); // every vial has its own amount, so never stack them
        meta.getPersistentDataContainer().set(vialKey, PersistentDataType.DOUBLE, amount);
        vial.setItemMeta(meta);

        p.getInventory().addItem(vial).values()
                .forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
    }

    /** Action bar (above hunger/hearts): blood bar, clamps blood when a gem is removed. */
    public void startTasks() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            boolean show = cfg().getBoolean("settings.actionbar-enabled", true);
            for (Player p : Bukkit.getOnlinePlayers()) {
                double max = getMaxBlood(p);
                double cur = setBlood(p, getBlood(p));
                if (show) p.sendActionBar(bar(cur, max));
            }
        }, 20L, 10L);
    }

    private Component bar(double cur, double max) {
        double safeMax = max <= 0 ? 1 : max;
        double ratio = Math.max(0, Math.min(1, cur / safeMax));
        int filled = (int) Math.round(ratio * BAR_SEGMENTS);
        return Component.text("🩸 ", NamedTextColor.DARK_RED)
                .append(Component.text("█".repeat(filled), NamedTextColor.RED))
                .append(Component.text("░".repeat(BAR_SEGMENTS - filled), NamedTextColor.DARK_GRAY))
                .append(Component.text(String.format(Locale.US, " %.1f%% / %.1f%%", cur, safeMax), NamedTextColor.WHITE));
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        blood.forEach((id, value) -> yml.set(id.toString(), value));
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save blood data: " + e.getMessage());
        }
    }

    private void load() {
        if (!dataFile.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : yml.getKeys(false)) {
            try {
                blood.put(UUID.fromString(key), yml.getDouble(key));
            } catch (IllegalArgumentException ignored) {
                // skip malformed key
            }
        }
    }
                            }
