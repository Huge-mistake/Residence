package com.bekvon.bukkit.residence.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SulfurCube;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.ResAdmin;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.containers.lm;

import org.jetbrains.annotations.NotNull;

public class ResidenceListener26_2 implements Listener {

    private Residence plugin;

    public ResidenceListener26_2(Residence plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerIgniteTntSulfurCube(PlayerInteractEntityEvent event) {
        // Disabling listener if flag disabled globally
        if (!Flags.ignite.isGlobalyEnabled()) {
            return;
        }
        Entity entity = event.getRightClicked();
        // disabling event on world
        if (plugin.isDisabledWorldListener(entity.getWorld())) {
            return;
        }
        if (!(entity instanceof SulfurCube)) {
            return;
        }
        EntityEquipment equipment = ((SulfurCube) entity).getEquipment();
        if (equipment != null && equipment.getItem(EquipmentSlot.BODY).getType() != Material.TNT) {
            return;
        }
        Material held = ResidenceListener1_09.getHeldMaterial(event);

        if (held != Material.FLINT_AND_STEEL && held != Material.FIRE_CHARGE) {
            return;
        }
        Player player = event.getPlayer();
        if (ResAdmin.isResAdmin(player)) {
            return;
        }
        FlagPermissions perms = FlagPermissions.getPerms(entity.getLocation(), player);
        if (perms.playerHas(player, Flags.ignite, perms.playerHas(player, Flags.animalkilling, true))) {
            return;
        }
        lm.Flag_Deny.sendMessage(player, Flags.ignite);
        event.setCancelled(true);

    }

//    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
//    public void onPlayerSpawnSulfurCube(PlayerInteractEntityEvent event) {
//        // Disabling listener if flag disabled globally
//        if (!Flags.build.isGlobalyEnabled()) {
//            return;
//        }
//        Entity entity = event.getRightClicked();
//        // disabling event on world
//        if (plugin.isDisabledWorldListener(entity.getWorld())) {
//            return;
//        }
//        Material held = ResidenceListener1_09.getHeldMaterial(event);
//
//        if (shouldDenyPlaceSulfurCube(held, entity.getLocation(), event.getPlayer())) {
//            event.setCancelled(true);
//        }
//    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerSpawnSulfurCube(PlayerInteractEvent event) {
//        // Disabling listener if flag disabled globally
//        if (!Flags.build.isGlobalyEnabled()) {
//            return;
//        }
//        Location loc;
//        Block block = event.getClickedBlock();
//        if (block != null) {
//            loc = block.getLocation();
//        } else {
//            loc = event.getInteractionPoint();
//        }
//        if (loc == null) {
//            return;
//        }
//        // disabling event on world
//        if (plugin.isDisabledWorldListener(loc.getWorld())) {
//            return;
//        }
        if (event.getItem() == null) {
            return;
        }
        Material held = event.getItem().getType();
        if (held != Material.SULFUR_CUBE_BUCKET) {
            return;
        }
        Player player = event.getPlayer();
        if (ResAdmin.isResAdmin(player)) {
            return;
        }
        if (FlagPermissions.has(player.getLocation(),  player, Flags.build, true)) {
            return;
        }
        lm.Flag_Deny.sendMessage(player, Flags.build);
        event.setCancelled(true);
    }
}
