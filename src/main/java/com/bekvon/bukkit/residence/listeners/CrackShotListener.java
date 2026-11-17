package com.bekvon.bukkit.residence.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.ResAdmin;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagCombo;
import com.bekvon.bukkit.residence.utils.Utils;
import com.shampaggon.crackshot.events.WeaponDamageEntityEvent;

public class CrackShotListener implements Listener {
    private Residence plugin;

    public CrackShotListener(Residence plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void AnimalKilling(WeaponDamageEntityEvent event) {
        // disabling event on world
        if (plugin.isDisabledWorldListener(event.getPlayer()))
            return;
        Entity damager = event.getDamager();

        if ((!(damager instanceof Arrow)) && (!(damager instanceof Player))) {
            return;
        }

        Player cause = null;
        if ((damager instanceof Arrow) && (!(((Arrow) damager).getShooter() instanceof Player))) {
            return;
        }

        if (damager instanceof Player) {
            cause = (Player) damager;
        } else {
            cause = (Player) ((Arrow) damager).getShooter();
        }

        if (cause == null)
            return;

        if (ResAdmin.isResAdmin(cause)) {
            return;
        }

        if (!(event.getVictim() instanceof LivingEntity))
            return;

        Entity entity = event.getVictim();
        ClaimedResidence res = plugin.getResidenceManager().getByLoc(entity.getLocation());

        if (Utils.isAnimal(entity) && res != null && res.getPermissions().playerHas(cause, Flags.animalkilling, FlagCombo.OnlyFalse)) {
            lm.Residence_FlagDeny.sendMessage(cause, Flags.animalkilling, res.getName());
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamageByEntityEvent(WeaponDamageEntityEvent event) {
        // disabling event on world
        if (plugin.isDisabledWorldListener(event.getPlayer()))
            return;
        if (!(event.getVictim() instanceof ItemFrame) && !Utils.isArmorStand(event.getVictim()))
            return;

        Entity dmgr = event.getDamager();
        Player player;
        if (event.getDamager() instanceof Player) {
            player = (Player) event.getDamager();
        } else {
            if (dmgr instanceof Projectile && ((Projectile) dmgr).getShooter() instanceof Player) {
                player = (Player) ((Projectile) dmgr).getShooter();
            } else
                return;
        }

        if (ResAdmin.isResAdmin(player))
            return;

        // Note: Location of entity, not player; otherwise player could stand outside of
        // res and still damage
        Location loc = event.getVictim().getLocation();
        ClaimedResidence res = plugin.getResidenceManager().getByLoc(loc);
        if (res != null && res.getPermissions().playerHas(player, Flags.container, FlagCombo.OnlyFalse)) {
            event.setCancelled(true);
            lm.Residence_FlagDeny.sendMessage(player, Flags.container, res.getName());
        }

    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(WeaponDamageEntityEvent event) {

        Entity victim = event.getVictim();

        if (FlagPermissions.shouldIgnoreCheck(Flags.pvp, victim)) {
            return;
        }
        if (!(victim instanceof Player)) {
            return;
        }
        Player attacker = event.getPlayer();

        if (attacker == null) {
            attacker = Utils.getPlayerSource(event.getDamager());
        }
        if (attacker == null) {
            return;
        }
        if (ResidenceEntityListener.shouldDenyPlayerDamagePlayer(attacker, (Player) victim)) {
            event.setCancelled(true);
        }
    }
}
