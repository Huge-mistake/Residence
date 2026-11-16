package com.bekvon.bukkit.residence.listeners;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.FlagPermissions;

import net.Zrips.CMILib.Items.CMIMaterial;

public class ResidenceListener1_10 implements Listener {

    private final Residence plugin;

    public ResidenceListener1_10(Residence plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamageByMagmaBlock(EntityDamageByBlockEvent event) {

        Block damager = event.getDamager();
        if (damager == null) {
            return;
        }
        if (FlagPermissions.shouldIgnoreCheck(Flags.hotfloor, damager)) {
            return;
        }
        if (CMIMaterial.get(damager.getType()) != CMIMaterial.MAGMA_BLOCK) {
            return;
        }
        if (FlagPermissions.has(event.getEntity().getLocation(), Flags.hotfloor, true)) {
            return;
        }
        event.setCancelled(true);
    }
}
