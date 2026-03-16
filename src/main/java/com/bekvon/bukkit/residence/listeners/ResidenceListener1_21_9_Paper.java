package com.bekvon.bukkit.residence.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagCombo;
import com.bekvon.bukkit.residence.protection.ResidencePermissions;

import io.papermc.paper.event.entity.ItemTransportingEntityValidateTargetEvent;

public class ResidenceListener1_21_9_Paper implements Listener {

    private Residence plugin;

    public ResidenceListener1_21_9_Paper(Residence plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCopperGolemOpenChest(ItemTransportingEntityValidateTargetEvent event) {

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

        Block block = event.getBlock();

        ClaimedResidence chestRes = ClaimedResidence.getByLoc(block.getLocation());

        if (chestRes != null) {

            ResidencePermissions chestPerms = chestRes.getPermissions();

            if (chestPerms.has(Flags.golemopenchest, FlagCombo.OnlyFalse)) {
                event.setAllowed(false);
                return;
            }
            if (chestPerms.has(Flags.golemopenchest, FlagCombo.OnlyTrue))
                return;

            if (chestPerms.has(Flags.container, FlagCombo.TrueOrNone))
                return;

            // When Flags.golemopenchest is None & Flags.container is False
            // Prevent external Copper Golems from opening chests in Residence
            // but do not block Copper Golems spawned inside the Residence
            if (isSameResOrOwner(chestRes, event.getEntity()))
                return;

            event.setAllowed(false);

        } else {

            FlagPermissions perms = FlagPermissions.getPerms(block.getLocation());
            if (perms.has(Flags.golemopenchest, perms.has(Flags.container, true)))
                return;

            event.setAllowed(false);

        }
    }

    private boolean isSameResOrOwner(ClaimedResidence blockRes, Entity entity) {
        if (blockRes == null || entity == null)
            return false;

        Location entSpawnLoc = entity.getOrigin();
        if (entSpawnLoc == null)
            return false;

        ClaimedResidence entSpawnRes = ClaimedResidence.getByLoc(entSpawnLoc);
        if (entSpawnRes == null)
            return false;

        return blockRes == entSpawnRes || blockRes.isOwner(entSpawnRes.getOwner());
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
        if (statueRes != null) {

            if (isSameResOrOwner(statueRes, event.getEntity()))
                return;

            if (statueRes.getPermissions().has(Flags.build, true))
                return;

            event.setCancelled(true);

        }
    }
}
