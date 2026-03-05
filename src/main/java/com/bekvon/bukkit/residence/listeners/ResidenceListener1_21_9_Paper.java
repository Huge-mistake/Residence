package com.bekvon.bukkit.residence.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;

import io.papermc.paper.event.entity.ItemTransportingEntityValidateTargetEvent;

public class ResidenceListener1_21_9_Paper implements Listener {

    private Residence plugin;

    public ResidenceListener1_21_9_Paper(Residence plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCopperGolemInteract(ItemTransportingEntityValidateTargetEvent event) {

        if (!event.isAllowed())
            return;
        // Disabling listener if flag disabled globally
        if (!Flags.golemopenchest.isGlobalyEnabled())
            return;

        // disabling event on world
        if (plugin.isDisabledWorldListener(event.getEntity().getWorld()))
            return;

        if (event.getEntityType() != EntityType.COPPER_GOLEM)
            return;

        FlagPermissions perms = FlagPermissions.getPerms(event.getBlock().getLocation());
        if (perms.has(Flags.golemopenchest, perms.has(Flags.container, true)))
            return;

        event.setAllowed(false);

    }

    // Prevent external copper golems from forming statues inside Residence
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCopperGolemStatueForm(EntityChangeBlockEvent event) {
        // Disabling listener if flag disabled globally
        if (!Flags.build.isGlobalyEnabled())
            return;

        Block block = event.getBlock();
        // disabling event on world
        if (plugin.isDisabledWorldListener(block.getWorld()))
            return;

        if (event.getEntityType() != EntityType.COPPER_GOLEM)
            return;

        if (event.getTo() != Material.OXIDIZED_COPPER_GOLEM_STATUE)
            return;

        ClaimedResidence statueRes = ClaimedResidence.getByLoc(block.getLocation());
        if (statueRes == null)
            return;

        Location entSpawnLoc = event.getEntity().getOrigin();
        if (entSpawnLoc != null) {

            ClaimedResidence entSpawnRes = ClaimedResidence.getByLoc(entSpawnLoc);

            if (entSpawnRes != null && (entSpawnRes == statueRes || entSpawnRes.isOwner(statueRes.getOwner())))
                return;

        }

        if (statueRes.getPermissions().has(Flags.build, true))
            return;

        event.setCancelled(true);

    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityKnockbackByEntityEvent(com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent event) {

        org.bukkit.entity.Entity hitBy = event.getHitBy();

        if (!(hitBy instanceof org.bukkit.entity.Player))
            return;

        if (event.getCause() != io.papermc.paper.event.entity.EntityKnockbackEvent.Cause.ENTITY_ATTACK)
            return;

        org.bukkit.entity.Player player = (org.bukkit.entity.Player) hitBy;

        if (com.bekvon.bukkit.residence.containers.ResAdmin.isResAdmin(player))
            return;

        org.bukkit.entity.Entity entity = event.getEntity();

        if (com.bekvon.bukkit.residence.utils.Utils.isAnimal(entity)) {

            if (FlagPermissions.has(entity.getLocation(), player, Flags.animalkilling, true))
                return;

            com.bekvon.bukkit.residence.containers.lm.Flag_Deny.sendMessage(player, Flags.animalkilling);
            event.setCancelled(true);

        } else if (ResidenceEntityListener.isMonster(entity)) {

            if (FlagPermissions.has(entity.getLocation(), player, Flags.mobkilling, true))
                return;

            com.bekvon.bukkit.residence.containers.lm.Flag_Deny.sendMessage(player, Flags.mobkilling);
            event.setCancelled(true);

        }
    }
}
