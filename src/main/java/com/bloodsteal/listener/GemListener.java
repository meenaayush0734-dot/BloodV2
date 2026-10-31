package com.bloodsteal.listener;

import com.bloodsteal.ability.AbilityManager;
import com.bloodsteal.gem.Gem;
import com.bloodsteal.gem.GemManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public final class GemListener implements Listener {

    private final GemManager gems;
    private final AbilityManager abilities;

    public GemListener(GemManager gems, AbilityManager abilities) {
        this.gems = gems;
        this.abilities = abilities;
    }

    /** Right-click with a gem in the main hand triggers its ability. Gems never place blocks (e.g. redstone). */
    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Gem gem = gems.fromItem(e.getItem());
        if (gem == null) return;

        e.setUseItemInHand(Event.Result.DENY);
        e.setUseInteractedBlock(Event.Result.DENY);
        abilities.activate(e.getPlayer(), gem, e.getPlayer().isSneaking());
    }

    /** Supreme Blood (Averon passive). */
    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (gems.activeGem(p) != Gem.AVERON) return;
        double max = p.getAttribute(Attribute.MAX_HEALTH).getValue();
        double after = p.getHealth() - e.getFinalDamage();
        abilities.trySupremeBlood(p, after, max);
    }

    /** Validation: a higher-tier gem can only be crafted if the previous tier gem is in the grid. */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        ItemStack result = e.getInventory().getResult();
        Gem gem = gems.fromItem(result);
        if (gem == null) return;

        Gem prev = gem.previous();
        if (prev == null) return;

        boolean hasPrev = Arrays.stream(e.getInventory().getMatrix())
                .anyMatch(item -> gems.fromItem(item) == prev);
        if (!hasPrev) e.getInventory().setResult(null);
    }
}
