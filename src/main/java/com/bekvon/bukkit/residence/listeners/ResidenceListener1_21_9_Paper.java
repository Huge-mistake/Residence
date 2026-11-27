package com.bekvon.bukkit.residence.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Entity;
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

    private final Residence plugin;

    public ResidenceListener1_21_9_Paper(Residence plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCopperGolemInteract(ItemTransportingEntityValidateTargetEvent event) {

        if (!event.isAllowed())
            return;

        Block chest = event.getBlock();

        if (FlagPermissions.shouldIgnoreCheck(Flags.golemopenchest, chest)) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof CopperGolem)) {
            return;
        }
        ClaimedResidence chestRes = ClaimedResidence.getByLoc(chest.getLocation());
        FlagPermissions chestPerms = FlagPermissions.getResOrWorldPerms(chestRes, chest);

        if (!chestPerms.has(Flags.golemopenchest, true)) {
            event.setAllowed(false);

            // If Flags.golemopenchest is allowed, check cross-residence access
            // Treat copper golems like hoppers since both can take and insert items
            // Also check Residence-origin copper golems inserting items into external chests
        } else if (ResidenceEntityListener.shouldCheckOrigin(chestRes, chestPerms)) {
            Location golemOrigin = entity.getOrigin();
            boolean hasOrigin = golemOrigin != null;
            ClaimedResidence golemOriginRes = hasOrigin ? ClaimedResidence.getByLoc(golemOrigin) : null;

            if (ResidenceEntityListener.shouldDenyExternalOrigin(chestRes, chestPerms, golemOriginRes, hasOrigin, Flags.container, Flags.use)) {
                event.setAllowed(false);
            }
        }
    }

    // Prevent external copper golems from forming statues inside Residence
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCopperGolemStatueForm(EntityChangeBlockEvent event) {

        Block block = event.getBlock();

        if (FlagPermissions.shouldIgnoreCheck(Flags.place, block)) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof CopperGolem)) {
            return;
        }

        if (event.getTo() != Material.OXIDIZED_COPPER_GOLEM_STATUE) {
            return;
        }

        ClaimedResidence statueRes = ClaimedResidence.getByLoc(block.getLocation());
        if (statueRes == null) {
            return;
        }

        Location golemSpawnLoc = entity.getOrigin();
        if (golemSpawnLoc == null) {
            return;
        }

        ClaimedResidence golemSpawnRes = ClaimedResidence.getByLoc(golemSpawnLoc);
        if (golemSpawnRes != null && (golemSpawnRes == statueRes || golemSpawnRes.isOwner(statueRes.getOwner()))) {
            return;
        }

        FlagPermissions perms = statueRes.getPermissions();
        if (perms.has(Flags.place, perms.has(Flags.build, true))) {
            return;
        }

        event.setCancelled(true);

    }
}
