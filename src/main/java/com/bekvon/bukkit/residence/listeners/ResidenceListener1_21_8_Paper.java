package com.bekvon.bukkit.residence.listeners;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.lm;
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
        if (plugin.isDisabledWorldListener(event.getEntity())) {
            return;
        }
        if (shouldCancelKnockBack(event.getEntity(), event.getPushedBy()))
            event.setCancelled(true);
    }

    public static boolean shouldCancelKnockBack(Entity victim, Entity pushedBy) {
        Player attackerPlayer = Utils.getPlayerSource(pushedBy);

        if (victim instanceof Player) {
            // Monster-on-player knockback doesn't need to check Flags.pvp
            // Skip the check when players knock themselves back (e.g., by Wind Charges)
            if (!Flags.pvp.isGlobalyEnabled() || attackerPlayer == null || attackerPlayer == victim) {
                return false;
            }
            if (FlagPermissions.has(attackerPlayer.getLocation(), Flags.pvp, FlagCombo.OnlyFalse)
                    || FlagPermissions.has(victim.getLocation(), Flags.pvp, FlagCombo.OnlyFalse)) {
                lm.Flag_Deny.sendMessage(attackerPlayer, Flags.pvp);
                return true;
            }
            return false;
        }

        Flags mainFlag = null;
        Flags subFlag = null;

        if (victim instanceof ArmorStand) {
            mainFlag = Flags.destroy;

        } else if (victim instanceof Boat || victim instanceof Minecart) {
            mainFlag = Flags.vehicledestroy;

        } else if (Utils.isAnimal(victim)) {
            // SulfurCube containing blocks are immune to damage
            // Use Flags.push instead of Flags.animalkilling on Paper 26.2+
            // to ensure consistent push flag behavior for SulfurCube
            if (Version.isCurrentEqualOrHigher(Version.v26_2_0) && Version.isPaperBranch()
                    && ResidenceListener26_2_Paper.isSulfurCubeHoldingBlock(victim)) {
                mainFlag = Flags.push;
                subFlag = Flags.animalkilling;
                // SulfurCube without blocks still checks Flags.animalkilling
            } else {
                mainFlag = Flags.animalkilling;

            }
        } else if (Utils.isMonster(victim)) {
            mainFlag = Flags.mobkilling;

        }
        if (mainFlag == null || !mainFlag.isGlobalyEnabled()) {
            return false;
        }
        return shouldDeny(victim, attackerPlayer, mainFlag, subFlag);
    }

    private static boolean shouldDeny(Entity victim, Player attackerPlayer, Flags mainFlag, Flags subFlag) {
        if (attackerPlayer != null) {

            return FlagPermissions.shouldDenyAndNotify(attackerPlayer, victim, mainFlag, subFlag);

        } else {
            FlagPermissions perms = FlagPermissions.getPerms(victim.getLocation());
            boolean result = (subFlag == null || perms.has(subFlag, true));

            return !perms.has(mainFlag, result);
        }
    }
}
