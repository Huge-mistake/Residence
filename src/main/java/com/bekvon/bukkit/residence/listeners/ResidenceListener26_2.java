package com.bekvon.bukkit.residence.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.FlagPermissions;

import net.Zrips.CMILib.Version.Version;

public class ResidenceListener26_2 implements Listener {

    private Residence plugin;

    public ResidenceListener26_2(Residence plugin) {
        this.plugin = plugin;
    }

    // fix https://github.com/PaperMC/Paper/issues/14149
    @EventHandler(priority = EventPriority.LOWEST) // Do not use (ignoreCancelled = true)
    public void onPlayerSulfurCubeBucketEmpty(PlayerInteractEvent event) {
        // https://github.com/PaperMC/Paper/pull/14150
        if (Version.isPaperBranch() && Version.isCurrentEqualOrHigher(Version.v26_3_0)) {
            return;
        }
        if (event.useItemInHand() == Result.DENY) {
            return;
        }
        Block block= event.getClickedBlock();
        if (block == null) {
            return;
        }
        if (FlagPermissions.shouldIgnoreCheck(Flags.build, block)) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getItem() == null || event.getItem().getType() != Material.SULFUR_CUBE_BUCKET) {
            return;
        }
        Player player = event.getPlayer();

        if (FlagPermissions.shouldDenyAndNotify(player, block.getRelative(event.getBlockFace()), Flags.build, null)) {
            event.setCancelled(true);
        }
    }
}