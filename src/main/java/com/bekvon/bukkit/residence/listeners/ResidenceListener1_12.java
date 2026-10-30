package com.bekvon.bukkit.residence.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.permissions.PermissionManager.ResPerm;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagCombo;

import net.Zrips.CMILib.Items.CMIMaterial;

public class ResidenceListener1_12 implements Listener {

    private final Residence plugin;

    public ResidenceListener1_12(Residence plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityPickupItemEvent(EntityPickupItemEvent event) {

        Item item = event.getItem();

        if (FlagPermissions.shouldIgnoreCheck(Flags.itempickup, item)) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity.hasMetadata("NPC"))
            return;

        if (entity instanceof Player) {

            Player player = (Player) entity;

            if (FlagPermissions.has(item.getLocation(), player, Flags.itempickup, true)) {
                return;
            }
            if (ResPerm.bypass_itempickup.hasPermission(player, 10000L)) {
                return;
            }
            lm.Flag_Deny.sendMessage(player, Flags.itempickup);

        } else {
            if (FlagPermissions.has(item.getLocation(), Flags.itempickup, true)) {
                return;
            }
        }

        event.setCancelled(true);
        item.setPickupDelay(plugin.getConfigManager().getItemPickUpDelay() * 20);

    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerItemDamageEvent(PlayerItemDamageEvent event) {

        Player player = event.getPlayer();

        if (FlagPermissions.shouldIgnoreCheck(Flags.nodurability, player)) {
            return;
        }
        if (FlagPermissions.has(player.getLocation(), Flags.nodurability, FlagCombo.FalseOrNone))
            return;

        // https://github.com/Zrips/Residence/issues/359
        // not sure if we need to keep this check line
        if (CMIMaterial.get(event.getItem()) == CMIMaterial.TRIDENT)
            return;

        event.setCancelled(true);

    }
}
