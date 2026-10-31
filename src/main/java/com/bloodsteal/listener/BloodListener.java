package com.bloodsteal.listener;

import com.bloodsteal.blood.BloodManager;
import com.bloodsteal.gem.GemManager;
import com.bloodsteal.rules.PvpRules;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class BloodListener implements Listener {

    private final BloodManager blood;
    private final GemManager gems;
    private final PvpRules rules;

    public BloodListener(BloodManager blood, GemManager gems, PvpRules rules) {
        this.blood = blood;
        this.gems = gems;
        this.rules = rules;
    }

    /** Team protection + PvP rules for melee and projectile damage. */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        Player attacker = resolveAttacker(e.getDamager());
        if (attacker == null) return;
        if (!rules.canDamage(attacker, victim)) e.setCancelled(true);
    }

    /** Only player-on-player kills steal blood. Normal deaths change nothing. */
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;
        if (!rules.pvpEnabled()) return;
        if (rules.isTeammate(killer, victim)) return;
        blood.handlePvpKill(victim, killer);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        gems.discoverRecipes(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        blood.save();
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player p1) return p1;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p2) return p2;
        return null;
    }
}
