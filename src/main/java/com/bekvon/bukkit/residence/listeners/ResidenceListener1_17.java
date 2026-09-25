package com.bekvon.bukkit.residence.listeners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.ResidenceBlockData;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.permissions.PermissionManager.ResPerm;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagCombo;

import net.Zrips.CMILib.Items.CMIMC;
import net.Zrips.CMILib.Items.CMIMaterial;
import net.Zrips.CMILib.Version.Version;

public class ResidenceListener1_17 implements Listener {

    private final Residence plugin;

    public ResidenceListener1_17(Residence plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Disabling listener if flag disabled globally
        if (!Flags.place.isGlobalyEnabled()) {
            return;
        }
        Block block = event.getBlock();

        if (ResidenceBlockListener.canPlaceBlock(event.getPlayer(), block, true)) {
            return;
        }
        event.setCancelled(true);
        // https://github.com/PaperMC/Paper/pull/6751
        if (Version.isPaperBranch() && Version.isCurrentEqualOrHigher(Version.v1_18_2)) {
            return;
        }
        if (block.getType() != Material.POWDER_SNOW) {
            return;
        }
        ResidenceBlockData.updatePowderedSnow(block);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerBucketEntityEvent(PlayerBucketEntityEvent event) {

        Entity entity = event.getEntity();

        if (FlagPermissions.shouldIgnoreCheck(Flags.animalkilling, entity)) {
            return;
        }
        if (FlagPermissions.shouldDenyAndNotify(event.getPlayer(), entity, Flags.animalkilling, null)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCopperOxidation(BlockFormEvent event) {

        Block block = event.getBlock();

        if (FlagPermissions.shouldIgnoreCheck(Flags.copperoxidation, block)) {
            return;
        }
        if (!isUnwaxedCopper(block)) {
            return;
        }
        if (FlagPermissions.has(block.getLocation(), Flags.copperoxidation, FlagCombo.OnlyFalse)) {
            event.setCancelled(true);
        }
    }

    private boolean isUnwaxedCopper(Block block) {
        CMIMaterial mat = CMIMaterial.get(block.getType());
        return mat.containsCriteria(CMIMC.COPPER) && !mat.name().startsWith("WAXED_");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPowderSnowPhysics(BlockPhysicsEvent event) {
        // https://github.com/PaperMC/Paper/pull/6751
        if (Version.isPaperBranch() && Version.isCurrentEqualOrHigher(Version.v1_18_2)) {
            return;
        }
        Block block = event.getBlock();

        if (FlagPermissions.shouldIgnoreCheck(Flags.place, block)) {
            return;
        }
        Block sourceBlock = event.getSourceBlock();

        if (sourceBlock.getType() != Material.POWDER_SNOW || block.getType() == Material.AIR || block.getType() == Material.POWDER_SNOW) {
            return;
        }
        Location blockLoc = block.getLocation();

        if (blockLoc.getY() == sourceBlock.getLocation().getY()) {
            return;
        }
        if (ClaimedResidence.getByLoc(blockLoc) != null) {
            ResidenceBlockData.addPowderedSnow(sourceBlock, block);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockFertilizeEvent(BlockFertilizeEvent event) {

        Block block = event.getBlock();

        if (FlagPermissions.shouldIgnoreCheck(Flags.build, block)) {
            return;
        }
        Player player = event.getPlayer();

        if (player != null) {
            if (ResPerm.bypass_build.hasPermission(player, 10000L)) {
                return;
            }
            // cancel event if player has no build permission at click-position
            // non-saplings don't consume bone_meal on event cancel
            if (FlagPermissions.has(block.getLocation(), player, Flags.build, FlagCombo.OnlyFalse)) {
                lm.Flag_Deny.sendMessage(player, Flags.build);
                event.setCancelled(true);
                return;
            }
        }
        // player has build permission at click position, or event is not player-triggered
        // check build permission for spread blocks
        ClaimedResidence originRes = ClaimedResidence.getByLoc(block.getLocation());
        List<BlockState> denySpread = new ArrayList<>();

        for (BlockState spreadBlock : event.getBlocks()) {
            ClaimedResidence spreadRes = ClaimedResidence.getByLoc(spreadBlock.getLocation());
            // spread-block not in Res, skip check
            if (spreadRes == null) {
                continue;
            }
            // origin & spread-block in Same Res, or have Same Res owner, skip check
            if (originRes != null && (originRes == spreadRes || originRes.isOwner(spreadRes.getOwner()))) {
                continue;
            }
            // origin & spread-block not in Same Res, not Same Res owner
            if (player != null) {
                if (spreadRes.getPermissions().playerHas(player, Flags.build, FlagCombo.OnlyFalse)) {
                    denySpread.add(spreadBlock);
                }
            } else {
                if (spreadRes.getPermissions().has(Flags.build, FlagCombo.OnlyFalse)) {
                    denySpread.add(spreadBlock);
                }
            }
        }
        event.getBlocks().removeAll(denySpread);
    }
}
