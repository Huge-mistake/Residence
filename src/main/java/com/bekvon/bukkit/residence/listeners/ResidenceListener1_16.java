package com.bekvon.bukkit.residence.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.LightningStrikeEvent;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.ResAdmin;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagCombo;

import net.Zrips.CMILib.Entities.CMIEntityType;

public class ResidenceListener1_16 implements Listener {

    private Residence plugin;

    public ResidenceListener1_16(Residence plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLightningStrikeEvent(LightningStrikeEvent event) {

        if (!event.getCause().equals(LightningStrikeEvent.Cause.TRIDENT))
            return;
        // disabling event on world
        if (plugin.isDisabledWorldListener(event.getWorld()))
            return;

        FlagPermissions perms = FlagPermissions.getPerms(event.getLightning().getLocation());
        if (perms.has(Flags.animalkilling, true))
            return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractRespawn(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        if (event.getPlayer() == null)
            return;
        // disabling event on world
        if (plugin.isDisabledWorldListener(player.getWorld()))
            return;

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block block = event.getClickedBlock();
        if (block == null)
            return;

        Material mat = block.getType();

        if (mat.equals(Material.RESPAWN_ANCHOR)) {
            if (ResAdmin.isResAdmin(player))
                return;

            FlagPermissions perms = FlagPermissions.getPerms(block.getLocation(), player);
            if (perms.playerHas(player, Flags.anchor, perms.playerHas(player, Flags.destroy, true)))
                return;

            lm.Flag_Deny.sendMessage(player, Flags.anchor);
            event.setCancelled(true);
            return;
        }

        if (mat.equals(Material.REDSTONE_WIRE)) {
            if (ResAdmin.isResAdmin(player))
                return;

            FlagPermissions perms = FlagPermissions.getPerms(block.getLocation(), player);
            if (perms.playerHas(player, Flags.build, true))
                return;

            lm.Flag_Deny.sendMessage(player, Flags.build);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFishingBobberHit(ProjectileHitEvent event) {
        // anti NPE
        Entity HitEntity = event.getHitEntity();
        if (HitEntity == null)
            return;
        // disabling event on world
        if (plugin.isDisabledWorldListener(HitEntity.getWorld()))
            return;

        Projectile hook = event.getEntity();
        // only fishing_bobber
        if (CMIEntityType.get(hook) != CMIEntityType.FISHING_BOBBER)
            return;
        // have player source
        if (!(hook.getShooter() instanceof Player))
            return;

        Player player = (Player) hook.getShooter();
        if (ResAdmin.isResAdmin(player))
            return;

        FlagPermissions perms = FlagPermissions.getPerms(HitEntity.getLocation(), player);
        if (perms.playerHas(player, Flags.hook, true))
            return;

        lm.Flag_Deny.sendMessage(player, Flags.hook);
        event.setCancelled(true);
    }
}
