package com.bloodsteal;

import com.bloodsteal.ability.AbilityManager;
import com.bloodsteal.blood.BloodManager;
import com.bloodsteal.command.BWithdrawCommand;
import com.bloodsteal.gem.GemManager;
import com.bloodsteal.listener.BloodListener;
import com.bloodsteal.listener.GemListener;
import com.bloodsteal.rules.PvpRules;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class BloodStealPlugin extends JavaPlugin {

    private GemManager gems;
    private BloodManager blood;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        gems = new GemManager(this);
        PvpRules rules = new PvpRules(this);
        blood = new BloodManager(this, gems);
        AbilityManager abilities = new AbilityManager(this, rules);

        getServer().getPluginManager().registerEvents(new BloodListener(blood, gems, rules), this);
        getServer().getPluginManager().registerEvents(new GemListener(gems, abilities), this);

        if (getCommand("bwithdraw") != null) {
            getCommand("bwithdraw").setExecutor(new BWithdrawCommand(blood));
        }

        gems.registerRecipes();
        for (Player p : Bukkit.getOnlinePlayers()) gems.discoverRecipes(p);
        blood.startTasks();

        getLogger().info("BloodSteal v2 enabled.");
    }

    @Override
    public void onDisable() {
        if (blood != null) blood.save();
    }
}
