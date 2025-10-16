package com.bekvon.bukkit.residence.listeners;

import net.Zrips.CMILib.Entities.CMIEntityType;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagCombo;
import com.bekvon.bukkit.residence.utils.Utils;

import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent;
import io.papermc.paper.event.entity.ItemTransportingEntityValidateTargetEvent;

public class ResidenceListener1_21_8_Paper implements Listener {

    private Residence plugin;

    public ResidenceListener1_21_8_Paper(Residence plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onKnockback(EntityPushedByEntityAttackEvent event) {

        if (shouldCancelKnockBack(event.getEntity(), event.getPushedBy()))
            event.setCancelled(true);
    }

    public static boolean shouldCancelKnockBack(Entity entity, Entity pushedBy) {
        Location loc = entity.getLocation();

        Player player = Utils.potentialProjectileToPlayer(pushedBy);

        if (Utils.isAnimal(entity))
            return flagCheck(loc, player, Flags.animalkilling);

        if (ResidenceEntityListener.isMonster(entity))
            return flagCheck(loc, player, Flags.mobkilling);

        if (entity instanceof Player) {
            if (FlagPermissions.has(loc, Flags.pvp, FlagCombo.OnlyFalse))
                return true;
            return false;
        }

        if (entity.getType().equals(EntityType.ARMOR_STAND))
            return flagCheck(loc, player, Flags.destroy);

        return false;
    }

    private static boolean flagCheck(Location loc, Player pushedBy, Flags flag) {
        if (pushedBy != null) {
            if (FlagPermissions.has(loc, pushedBy, flag, FlagCombo.OnlyFalse))
                return true;
        } else {
            if (FlagPermissions.has(loc, flag, FlagCombo.OnlyFalse))
                return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCopperGolemInteract(ItemTransportingEntityValidateTargetEvent event) {

        // disabling event on world
        if (plugin.isDisabledWorldListener(event.getEntity().getWorld()))
            return;

        //Entity entity = event.getEntity();
        if (CMIEntityType.get(event.getEntity()) != CMIEntityType.COPPER_GOLEM)
            return;

        //Block block = event.getBlock();
        //CMIMaterial cmat = CMIMaterial.get(block.getType());
        //boolean isCopperChest = cmat.isCopperBlock();
        //boolean isChest = (cmat == CMIMaterial.CHEST || cmat == CMIMaterial.TRAPPED_CHEST);
        //if (!isCopperChest && !isChest)
        //    return;

        FlagPermissions perms = FlagPermissions.getPerms(event.getBlock().getLocation());
        if (perms.has(Flags.container, true))
            return;

        event.setAllowed(false);

    }
}
