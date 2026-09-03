package com.bekvon.bukkit.residence.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.FlagPermissions;

import net.Zrips.CMILib.Items.CMIMaterial;
import net.Zrips.CMILib.Version.Version;

public class ResidenceListener1_19 implements Listener {

    private Residence plugin;

    public ResidenceListener1_19(Residence plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onUseGoatHorn(PlayerInteractEvent event) {

        Player player = event.getPlayer();

        if (FlagPermissions.shouldIgnoreCheck(Flags.goathorn, player)) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        if (CMIMaterial.get(event.getItem()) != CMIMaterial.GOAT_HORN)
            return;

        if (FlagPermissions.shouldDenyAndNotify(player, player, Flags.goathorn, null)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {

        if (FlagPermissions.shouldIgnoreCheck(Flags.skulk, event.getBlock())) {
            return;
        }
        if (!Material.SCULK_CATALYST.equals(event.getSource().getType()))
            return;

        Location loc = event.getBlock().getLocation();
        FlagPermissions perms = FlagPermissions.getPerms(loc);
        if (!perms.has(Flags.skulk, true)) {
            event.setCancelled(true);
        }
    }

    // if Flag_riding is true
    // riding InventoryVehicle: check Flag_container when opening Vehicle Inventory
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerOpenVehicleInv(InventoryOpenEvent event) {

        Player player = (Player) event.getPlayer();

        if (FlagPermissions.shouldIgnoreCheck(Flags.container, player)) {
            return;
        }
        Entity vehicle = player.getVehicle();

        if (!canHaveContainer1_19(vehicle)) {
            return;
        }
        if (FlagPermissions.shouldDenyAndNotify(player, vehicle, Flags.container, null)) {
            event.setCancelled(true);
        }
    }

    // Cover All 1.19+ Vehicles with an Inventory interface
    public static boolean canHaveContainer1_19(Entity entity) {
        if (entity == null) {
            return false;
        }
        return entity instanceof AbstractHorse
                || entity instanceof ChestBoat
                || (Version.isCurrentEqualOrHigher(Version.v1_21_R7) && entity instanceof org.bukkit.entity.AbstractNautilus);
    }
}
