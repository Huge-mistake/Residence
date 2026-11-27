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
            return;
        }
        // If Flags.golemopenchest is allowed, check cross-residence access
        if (!Flags.entityorigin.isGlobalyEnabled()) {
            return;
        }
        ClaimedResidence golemSpawnRes = ClaimedResidence.getByLoc(entity.getOrigin());

        // Source and Chest not in Res
        if (golemSpawnRes == null && chestRes == null) {
            return;
        }

        // Source and Chest in Res
        if (golemSpawnRes != null && chestRes != null) {
            // In Same Res, or have Same Res owner
            if (golemSpawnRes == chestRes || golemSpawnRes.isOwner(chestRes.getOwner())) {
                return;
            }

            // Treat copper golems like hoppers since both can take and insert items
            FlagPermissions golemSpawnPerms = golemSpawnRes.getPermissions();
            if (!golemSpawnPerms.has(Flags.entityorigin, true) && !chestPerms.has(Flags.entityorigin, true)) {
                return;
            }
            if (golemSpawnPerms.has(Flags.container, true) && chestPerms.has(Flags.container, true)) {
                return;
            }

            // Source in Res, Chest definitely not in Res
        } else if (golemSpawnRes != null) {
            // Also check Residence-origin copper golems inserting items into external chests
            FlagPermissions golemSpawnPerms = golemSpawnRes.getPermissions();
            if (!golemSpawnPerms.has(Flags.entityorigin, true) || golemSpawnPerms.has(Flags.container, true)) {
                return;
            }

            // Chest in Res, Source definitely not in Res
        } else {
            if (!chestPerms.has(Flags.entityorigin, true) || chestPerms.has(Flags.container, true)) {
                return;
            }

        }
        event.setAllowed(false);
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
