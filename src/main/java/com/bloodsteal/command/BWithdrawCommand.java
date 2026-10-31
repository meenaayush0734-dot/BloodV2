package com.bloodsteal.command;

import com.bloodsteal.blood.BloodManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class BWithdrawCommand implements CommandExecutor {

    private final BloodManager blood;

    public BWithdrawCommand(BloodManager blood) {
        this.blood = blood;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Ye command sirf players use kar sakte hain.");
            return true;
        }
        if (args.length != 1) {
            p.sendMessage(err("Usage: /bwithdraw <amount>"));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException ex) {
            p.sendMessage(err("Galat amount."));
            return true;
        }
        if (!Double.isFinite(amount) || amount <= 0) {
            p.sendMessage(err("Amount 0 se zyada hona chahiye."));
            return true;
        }
        amount = Math.floor(amount * 100) / 100.0;

        double taken = blood.withdraw(p, amount); // capped to current blood inside
        if (taken <= 0) {
            p.sendMessage(err("Withdraw karne ke liye blood nahi hai."));
            return true;
        }
        p.sendMessage(Component.text(String.format(Locale.US, "%.2f%% blood Blood Vial mein nikal gaya.", taken),
                NamedTextColor.GREEN));
        return true;
    }

    private static Component err(String s) {
        return Component.text(s, NamedTextColor.RED);
    }
}
