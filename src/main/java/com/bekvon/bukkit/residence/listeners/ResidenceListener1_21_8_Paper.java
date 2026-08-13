package com.bekvon.bukkit.residence.listeners;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.ResAdmin;
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

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onKnockback(EntityPushedByEntityAttackEvent event) {
        // disabling event on world
        if (plugin.isDisabledWorldListener(event.getEntity().getWorld())) {
            return;
        }
        if (shouldCancelKnockBack(event.getEntity(), event.getPushedBy()))
            event.setCancelled(true);
    }

    public static boolean shouldCancelKnockBack(Entity target, Entity pushedBy) {

        Player player = Utils.potentialProjectileToPlayer(pushedBy);

        if (player != null && Version.isCurrentEqualOrHigher(Version.v26_2_0) && Version.isPaperBranch()) {
            // Disabling listener if flag disabled globally
            if (!Flags.push.isGlobalyEnabled()) {
                return false;
            }
            return ResidenceListener26_2_Paper.shouldDenyPush(target, player);
        }
        if (target instanceof ArmorStand) {
            return flagCheck(target, player, Flags.destroy);
        }
        if (target instanceof Boat || target instanceof Minecart) {
            return flagCheck(target, player, Flags.vehicledestroy);
        }
        if (target instanceof Player) {
            // Monster-on-player knockback doesn't need to check Flags.pvp
            return player != null && FlagPermissions.has(target.getLocation(), Flags.pvp, FlagCombo.OnlyFalse);
        }
        if (Utils.isAnimal(target)) {
            return flagCheck(target, player, Flags.animalkilling);
        }
        if (ResidenceEntityListener.isMonster(target)) {
            return flagCheck(target, player, Flags.mobkilling);
        }
        return false;
    }

    private static boolean flagCheck(Entity target, Player pushedBy, Flags flag) {
        if (pushedBy != null) {
            if (pushedBy.hasMetadata("NPC") || ResAdmin.isResAdmin(pushedBy)) {
                return false;
            }
            return FlagPermissions.has(target.getLocation(), pushedBy, flag, FlagCombo.OnlyFalse);
        } else {
            return FlagPermissions.has(target.getLocation(), flag, FlagCombo.OnlyFalse);
        }
    }
}
