package com.bloodsteal.rules;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;

public final class PvpRules {

    private final JavaPlugin plugin;

    public PvpRules(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean pvpEnabled() {
        return plugin.getConfig().getBoolean("pvp.enabled", true);
    }

    public boolean allowTeamDamage() {
        return plugin.getConfig().getBoolean("pvp.allow-team-damage", false);
    }

    public boolean isTeammate(Player a, Player b) {
        Team team = a.getScoreboard().getEntryTeam(a.getName());
        return team != null && team.hasEntry(b.getName());
    }

    /** Central PvP rule: used by melee, projectiles, abilities and blood steal. */
    public boolean canDamage(Player attacker, Player victim) {
        if (attacker.equals(victim)) return false;
        if (!pvpEnabled()) return false;
        if (!allowTeamDamage() && isTeammate(attacker, victim)) return false;
        return true;
    }
}
