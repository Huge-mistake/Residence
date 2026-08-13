package com.bekvon.bukkit.residence.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.ResAdmin;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.listenersCache.DenyMessageCache;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagCombo;
import com.bekvon.bukkit.residence.utils.Utils;

import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent;

import net.Zrips.CMILib.Version.Version;

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

        if (Utils.isAnimal(entity)) {
            // Paper 26.2 uses EntityCollideWithEntityEvent
            // to handle SulfurCube(block-containing) knockback.
            if (Version.isCurrentEqualOrHigher(Version.v26_2_0) && Version.isPaperBranch()
                    && Flags.push.isGlobalyEnabled() && entity instanceof org.bukkit.entity.SulfurCube) {

                EntityEquipment equipment = ((org.bukkit.entity.SulfurCube) entity).getEquipment();
                // Check if SulfurCube has a block inside
                if (equipment != null && !equipment.getItem(EquipmentSlot.BODY).isEmpty()) {
                    if (player != null) {
                        if (player.hasMetadata("NPC") || ResAdmin.isResAdmin(player)) {
                            return false;
                        }
                        FlagPermissions perms = FlagPermissions.getPerms(entity.getLocation(), player);
                        if (!perms.playerHas(player, Flags.push, perms.playerHas(player, Flags.animalkilling, true))) {
                            if (DenyMessageCache.shouldSendDenyMessage(player, Flags.push)) {
                                lm.Flag_Deny.sendMessage(player, Flags.push);
                            }
                            return true;
                        }

                    } else {
                        FlagPermissions perms = FlagPermissions.getPerms(entity.getLocation());
                        return !perms.has(Flags.push, perms.has(Flags.animalkilling, true));
                    }
                }
            }
            return flagCheck(loc, player, Flags.animalkilling);
        }
        if (ResidenceEntityListener.isMonster(entity))
            return flagCheck(loc, player, Flags.mobkilling);

        if (entity instanceof Player) {
            if (FlagPermissions.has(loc, Flags.pvp, FlagCombo.OnlyFalse))
                return true;
            return false;
        }
        if (entity instanceof org.bukkit.entity.Boat || entity instanceof org.bukkit.entity.Minecart) {
            return flagCheck(loc, player, Flags.vehicledestroy);
        }
        if (entity.getType().equals(EntityType.ARMOR_STAND))
            return flagCheck(loc, player, Flags.destroy);

        return false;
    }

    private static boolean flagCheck(Location loc, Player pushedBy, Flags flag) {
        if (pushedBy != null) {
            if (ResAdmin.isResAdmin(pushedBy))
                return false;
            if (FlagPermissions.has(loc, pushedBy, flag, FlagCombo.OnlyFalse))
                return true;
        } else {
            if (FlagPermissions.has(loc, flag, FlagCombo.OnlyFalse))
                return true;
        }
        return false;
    }
}
