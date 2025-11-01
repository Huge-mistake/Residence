package com.bekvon.bukkit.residence.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
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
import com.bekvon.bukkit.residence.containers.ResAdmin;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagCombo;

import net.Zrips.CMILib.Version.Schedulers.CMIScheduler;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;

public class ResidenceListener1_19 implements Listener {

    private Residence plugin;

    public ResidenceListener1_19(Residence plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onUseGoatHorn(PlayerInteractEvent event) {
        // Disabling listener if flag disabled globally
        if (!Flags.goathorn.isGlobalyEnabled())
            return;

        Player player = event.getPlayer();
        if (player == null)
            return;
        // disabling event on world
        if (plugin.isDisabledWorldListener(player.getWorld()))
            return;

        if (player.hasMetadata("NPC"))
            return;

        ItemStack horn = event.getItem();

        if (horn == null)
            return;

        if (!horn.getType().equals(Material.GOAT_HORN))
            return;

        if (ResAdmin.isResAdmin(player))
            return;

        FlagPermissions perms = FlagPermissions.getPerms(player.getLocation(), player);
        if (perms.playerHas(player, Flags.goathorn, true))
            return;

        lm.Flag_Deny.sendMessage(player, Flags.goathorn);
        event.setCancelled(true);
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
        Location hopperLoc = hopperInventory.getLocation();
        if (hopperLoc == null)
            return;
        // delay 1 tick break, ensure after event cancel
        CMIScheduler.runAtLocationLater(plugin, hopperLoc, () -> {
            Block block = hopperLoc.getBlock();
            // only hopper
            if (block == null || !(block.getType().equals(Material.HOPPER)))
                return;
            block.breakNaturally();
        }, 1);
    }

    private final Cache<String, Boolean> keyCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .maximumSize(2000)
            .build();

    private String getKey(Location sourceLoc, Location destLoc) {
        World sourceWorld = sourceLoc.getWorld();
        World destWorld = destLoc.getWorld();
        if (sourceWorld == null || destWorld == null) {
            return null;
        }
        return String.format(
                "%s:%d:%d:%d|%s:%d:%d:%d",

                sourceWorld.getName(),
                sourceLoc.getBlockX(), sourceLoc.getBlockY(), sourceLoc.getBlockZ(),

                destWorld.getName(),
                destLoc.getBlockX(), destLoc.getBlockY(), destLoc.getBlockZ()
        );
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHopperCrossRes(InventoryMoveItemEvent event) {
        // Disabling listener if flag disabled globally
        if (!Flags.container.isGlobalyEnabled())
            return;

        Inventory source = event.getSource();
        Inventory dest = event.getDestination();
        if (source == null || dest == null) {
            return;
        }

        Location sourceLoc = source.getLocation();
        Location destLoc = dest.getLocation();
        if (sourceLoc == null || destLoc == null) {
            return;
        }

        String key = getKey(sourceLoc, destLoc);
        Boolean cacheResult = keyCache.getIfPresent(key);

        if (cacheResult != null) {
            if (!cacheResult) {
                event.setCancelled(true);
            }
            return;
        }

        ClaimedResidence sourceRes = ClaimedResidence.getByLoc(sourceLoc);
        ClaimedResidence destRes = ClaimedResidence.getByLoc(destLoc);

        // ignore source & dest not in Res
        if (sourceRes == null && destRes == null) {
            keyCache.put(key, true);
            return;
        }

        // ignore source & dest in Same Res
        if (sourceRes != null && destRes != null && sourceRes.equals(destRes)) {
            keyCache.put(key, true);
            return;
        }

        // source & dest not in Same Res
        if (sourceRes != null && destRes != null && !sourceRes.equals(destRes)) {
            if ((sourceRes.getPermissions().has(Flags.container, true)) &&
                (destRes.getPermissions().has(Flags.container, true))) {
                keyCache.put(key, true);
                return;
            }
            event.setCancelled(true);
            keyCache.put(key, false);
            return;
        }

        // source in Res, dest not in Res
        if (sourceRes != null && destRes == null) {
            if (sourceRes.getPermissions().has(Flags.container, true)) {
                keyCache.put(key, true);
                return;
            }
            event.setCancelled(true);
            keyCache.put(key, false);
            return;
        }

        // dest in Res, source not in Res
        if (sourceRes == null && destRes != null) {
            if (destRes.getPermissions().has(Flags.container, true)) {
                keyCache.put(key, true);
                return;
            }
            event.setCancelled(true);
            keyCache.put(key, false);
        }
    }
}
