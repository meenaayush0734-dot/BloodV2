package com.bloodsteal.ability;

import com.bloodsteal.gem.Gem;
import com.bloodsteal.rules.PvpRules;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AbilityManager {

    private final JavaPlugin plugin;
    private final PvpRules rules;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public AbilityManager(JavaPlugin plugin, PvpRules rules) {
        this.plugin = plugin;
        this.rules = rules;
    }

    private double num(String path, double def) {
        return plugin.getConfig().getDouble("abilities." + path, def);
    }

    /** Called from GemListener on right-click with the gem in main hand. */
    public void activate(Player p, Gem gem, boolean sneaking) {
        switch (gem) {
            case BLOOD -> run(p, "blood-burst", () -> bloodBurst(p));
            case CRIMSON -> run(p, "crimson-rage", () -> crimsonRage(p));
            case SHADOW -> run(p, "shadow-blink", () -> blink(p));
            case INFERNO -> run(p, "infernal-execution", () -> infernalExecution(p));
            case AVERON -> {
                if (sneaking) run(p, "averon-rift", () -> averonRift(p));
                else run(p, "blood-dominion", () -> bloodDominion(p));
            }
        }
    }

    /** Passive Supreme Blood: triggers when a hit leaves the holder at or below the health threshold. */
    public void trySupremeBlood(Player p, double healthAfter, double maxHealth) {
        if (healthAfter <= 0) return;                                           // lethal hit: nothing to save
        if (healthAfter > maxHealth * num("supreme-blood.health-threshold", 0.3)) return;
        if (cooldownLeftMs(p, "supreme-blood") > 0) return;                     // silent during cooldown

        startCooldown(p, "supreme-blood");
        int secs = (int) num("supreme-blood.resistance-seconds", 3);
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, secs * 20, 0, false, true, true));
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 0.8f, 1.4f);
        ring(p.getLocation(), 1.2, Color.fromRGB(200, 0, 40));
        p.sendMessage(Component.text("Supreme Blood activated!", NamedTextColor.DARK_RED));
    }

    // ---------- cooldown plumbing ----------

    private void run(Player p, String ability, Runnable action) {
        long left = cooldownLeftMs(p, ability);
        if (left > 0) {
            p.sendMessage(Component.text(String.format(Locale.US, "%s cooldown: %.1fs", ability, left / 1000.0),
                    NamedTextColor.RED));
            return;
        }
        startCooldown(p, ability);
        action.run();
    }

    private long cooldownLeftMs(Player p, String ability) {
        Map<String, Long> map = cooldowns.getOrDefault(p.getUniqueId(), Map.of());
        return Math.max(0, map.getOrDefault(ability, 0L) - System.currentTimeMillis());
    }

    private void startCooldown(Player p, String ability) {
        long readyAt = System.currentTimeMillis() + (long) (num(ability + ".cooldown", 10) * 1000);
        cooldowns.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).put(ability, readyAt);
    }

    /** PvP rules for ability damage: no teammates, no PvP when disabled. Mobs are always valid. */
    private boolean canHit(Player attacker, LivingEntity target) {
        if (target.equals(attacker)) return false;
        if (target instanceof Player other) return rules.canDamage(attacker, other);
        return true;
    }

    // ---------- abilities ----------

    /** Blood Burst: knocks back and damages enemies within radius. */
    private void bloodBurst(Player p) {
        double radius = num("blood-burst.radius", 4);
        double dmg = num("blood-burst.damage", 3);
        double knock = num("blood-burst.knockback", 1.2);
        Location c = p.getLocation();

        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity le) || !canHit(p, le)) continue;
            le.damage(dmg, p);
            Vector push = le.getLocation().toVector().subtract(c.toVector()).setY(0.4);
            if (push.lengthSquared() > 0) le.setVelocity(push.normalize().multiply(knock));
        }
        ring(c, radius, Color.fromRGB(170, 0, 0));
        c.getWorld().playSound(c, Sound.ENTITY_WITHER_HURT, 1f, 1.2f);
    }

    /** Crimson Rage: Strength I for a short time. */
    private void crimsonRage(Player p) {
        int secs = (int) num("crimson-rage.duration", 10);
        int level = (int) num("crimson-rage.strength-level", 1);
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, secs * 20, level - 1, false, true, true));
        p.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, p.getLocation().add(0, 2, 0), 8, 0.5, 0.4, 0.5, 0);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 0.8f, 1.2f);
    }

    /** Shadow Blink: teleport forward, stopping at walls. */
    private void blink(Player p) {
        double dist = num("shadow-blink.distance", 8);
        Location from = p.getLocation().clone();
        Location to = blinkDestination(p, dist);
        World w = p.getWorld();

        w.spawnParticle(Particle.PORTAL, from.clone().add(0, 1, 0), 40, 0.3, 0.5, 0.3, 0.1);
        w.playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        p.teleport(to);
        w.spawnParticle(Particle.PORTAL, to.clone().add(0, 1, 0), 40, 0.3, 0.5, 0.3, 0.1);
    }

    /** Infernal Execution: fire damage + burning + Slowness on the entity you are looking at. */
    private void infernalExecution(Player p) {
        double range = num("infernal-execution.range", 6);
        double dmg = num("infernal-execution.damage", 4);
        int fireSecs = (int) num("infernal-execution.fire-seconds", 5);
        int slowSecs = (int) num("infernal-execution.slowness-seconds", 5);
        int slowLevel = (int) num("infernal-execution.slowness-level", 2);

        Location eye = p.getEyeLocation();
        RayTraceResult r = p.getWorld().rayTraceEntities(eye, eye.getDirection(), range, 0.5,
                e -> e instanceof LivingEntity && !e.equals(p));
        if (r == null || !(r.getHitEntity() instanceof LivingEntity target)) {
            p.sendMessage(Component.text("Koi target nahi mila.", NamedTextColor.RED));
            return;
        }
        if (!canHit(p, target)) {
            p.sendMessage(Component.text("Ye player tumhara teammate hai ya PvP off hai.", NamedTextColor.RED));
            return;
        }

        target.damage(dmg, p);
        target.setFireTicks(fireSecs * 20);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowSecs * 20, slowLevel - 1, false, true, true));
        target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 40, 0.4, 0.5, 0.4, 0.02);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.8f);
    }

    /** Blood Dominion: short blood boost (Strength + Regeneration). */
    private void bloodDominion(Player p) {
        int secs = (int) num("blood-dominion.duration", 8);
        int level = (int) num("blood-dominion.strength-level", 2);
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, secs * 20, level - 1, false, true, true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, secs * 20, 0, false, true, true));
        ring(p.getLocation(), 1.5, Color.fromRGB(120, 0, 0));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
    }

    /** Averon Rift: long teleport that damages every enemy along the path. */
    private void averonRift(Player p) {
        double dist = num("averon-rift.distance", 10);
        double dmg = num("averon-rift.damage", 6);

        Location start = p.getLocation().clone();
        Location end = blinkDestination(p, dist);
        Vector path = end.toVector().subtract(start.toVector());
        double length = path.length();
        if (length < 0.5) return;

        Vector step = path.clone().normalize();
        Set<LivingEntity> hits = new HashSet<>();
        for (double d = 0; d <= length; d += 0.75) {
            Location point = start.clone().add(step.clone().multiply(d)).add(0, 1, 0);
            for (Entity e : point.getWorld().getNearbyEntities(point, 1.2, 1.2, 1.2)) {
                if (e instanceof LivingEntity le && canHit(p, le)) hits.add(le);
            }
            point.getWorld().spawnParticle(Particle.SOUL, point, 3, 0.1, 0.1, 0.1, 0.01);
        }
        hits.forEach(le -> le.damage(dmg, p));
        p.teleport(end);
        p.getWorld().playSound(start, Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.4f);
    }

    // ---------- helpers ----------

    /** Feet location after moving `distance` along the look direction, stopping before solid blocks. */
    private Location blinkDestination(Player p, double distance) {
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        double travel = distance;

        RayTraceResult hit = p.getWorld().rayTraceBlocks(eye, dir, distance, FluidCollisionMode.NEVER, true);
        if (hit != null && hit.getHitPosition() != null) {
            travel = Math.max(0, eye.toVector().distance(hit.getHitPosition()) - 0.7);
        }

        Location dest = eye.clone().add(dir.clone().multiply(travel));
        dest.subtract(0, p.getEyeHeight(), 0);
        dest.setYaw(eye.getYaw());
        dest.setPitch(eye.getPitch());
        return dest;
    }

    private void ring(Location center, double radius, Color color) {
        World w = center.getWorld();
        Particle.DustOptions opts = new Particle.DustOptions(color, 1.4f);
        for (int i = 0; i < 36; i++) {
            double a = Math.toRadians(i * 10);
            w.spawnParticle(Particle.DUST,
                    center.clone().add(Math.cos(a) * radius, 0.3, Math.sin(a) * radius),
                    1, 0, 0, 0, 0, opts);
        }
    }
    }
