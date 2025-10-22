package com.bekvon.bukkit.residence.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagCombo;

public class ResidenceListener1_19 implements Listener {

    private Residence plugin;

    public ResidenceListener1_19(Residence plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSignInteract(PlayerInteractEvent event) {

        if (!Flags.goathorn.isGlobalyEnabled())
            return;

        if (event.getPlayer() == null)
            return;
        // disabling event on world
        if (plugin.isDisabledWorldListener(event.getPlayer().getWorld()))
            return;

        Player player = event.getPlayer();
        if (player.hasMetadata("NPC"))
            return;

        ItemStack horn = event.getItem();

        if (horn == null)
            return;

        if (!horn.getType().equals(Material.GOAT_HORN))
            return;

        ClaimedResidence res = ClaimedResidence.getByLoc(event.getPlayer().getLocation());
        if (res == null)
            return;

        if (event.getPlayer().hasMetadata("NPC"))
            return;

        if (res.getPermissions().playerHas(event.getPlayer(), Flags.goathorn, FlagCombo.TrueOrNone))
            return;

        event.setCancelled(true);

        lm.Residence_FlagDeny.sendMessage(player, Flags.goathorn, res.getName());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        // Disabling listener if flag disabled globally
        if (!Flags.skulk.isGlobalyEnabled())
            return;
        // disabling event on world
        if (plugin.isDisabledWorldListener(event.getBlock().getWorld()))
            return;

        if (!Material.SCULK_CATALYST.equals(event.getSource().getType()))
            return;

        Location loc = event.getBlock().getLocation();
        FlagPermissions perms = FlagPermissions.getPerms(loc);
        if (!perms.has(Flags.skulk, true)) {
            event.setCancelled(true);
        }
    }

    private void breakHopper(Inventory hopperInventory) {

        if (hopperInventory.getHolder() instanceof HopperMinecart) {
            HopperMinecart entity = (HopperMinecart) hopperInventory.getHolder();
            entity.remove();
            return;
        }

        Location hopperLoc = hopperInventory.getLocation();
        if (hopperLoc == null)
            return;

        Block block = hopperLoc.getBlock();
        if (block.getType() != Material.HOPPER)
            return;

        block.breakNaturally();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHopperCrossRes(InventoryMoveItemEvent event) {

        Inventory chest = event.getSource();
        Inventory hopper = event.getDestination();
        if (chest == null || hopper == null)
            return;

        ClaimedResidence chestRes = ClaimedResidence.getByLoc(chest.getLocation());
        // Container not in Residence
        if (chestRes == null)
            return;

        ClaimedResidence hopperRes = ClaimedResidence.getByLoc(hopper.getLocation());
        // Hopper not in Residence
        if (hopperRes == null) {
            event.setCancelled(true);
            breakHopper(hopper);
            return;
        }
        // Container & Hopper in Same Residence
        if (chestRes == hopperRes)
            return;

        event.setCancelled(true);
        breakHopper(hopper);
    }
}
