package com.bekvon.bukkit.residence.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

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

//    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
//    public void onKnockback(EntityPushedByEntityAttackEvent event) {
//        // disabling event on world
//        if (plugin.isDisabledWorldListener(event.getEntity().getWorld())) {
//            return;
//        }
//        if (shouldCancelKnockBack(event.getEntity(), event.getPushedBy()))
//            event.setCancelled(true);
//    }

    public static boolean shouldCancelKnockBack(Entity entity, Entity pushedBy) {
        Player player = Utils.potentialProjectileToPlayer(pushedBy);

        if (player != null && Version.isCurrentEqualOrHigher(Version.v26_2_0) && Version.isPaperBranch()) {
            return shouldDenyPlayerPushEntity26_2_Paper(player, entity);
        }
        Location loc = entity.getLocation();

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

    private static boolean shouldDenyPlayerPushEntity26_2_Paper(@NotNull Player player, @NotNull Entity entity) {
        // Disabling listener if flag disabled globally
        if (!Flags.push.isGlobalyEnabled()) {
            return false;
        }
        if (ResidenceListener26_2_Paper.shouldDenyPush(player, entity)) {

            if (DenyMessageCache.shouldSendDenyMessage(player, Flags.push)) {
                lm.Flag_Deny.sendMessage(player, Flags.push);
            }
            return true;
        }
        return false;
    }
}
