package com.bekvon.bukkit.residence.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagCombo;

import java.util.HashMap;
import java.util.Map;

public class ResidenceListener1_19 implements Listener {

    private Residence plugin;

    public ResidenceListener1_19(Residence plugin) {
        this.plugin = plugin;
    }

    private final Map<String, Long> moveItemDebounce = new HashMap<>();

    private static final long DEBOUNCE_THRESHOLD = 2000;

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

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHopper(InventoryMoveItemEvent event) {

        Inventory initiator = event.getInitiator();
        Block hopperBlock = getBlockFromHolder(initiator.getHolder());
        Inventory source = event.getSource();
        Block chestBlock = getBlockFromHolder(source.getHolder());


        String eventId = generateEventId(hopperBlock, chestBlock);
        if (eventId == null) return;

        long currentTime = System.currentTimeMillis();
        if (moveItemDebounce.containsKey(eventId)) {
            long lastProcessedTime = moveItemDebounce.get(eventId);
            if (currentTime - lastProcessedTime < DEBOUNCE_THRESHOLD)
                event.setCancelled(true);
                return;
        }

        if (hopperBlock == null || !hopperBlock.getType().name().contains("HOPPER"))
            return;
        if (chestBlock == null)
            return;

        ClaimedResidence sourceRes = ClaimedResidence.getByLoc(chestBlock.getLocation());
        if (sourceRes == null)
            return;

        ClaimedResidence hopperRes = ClaimedResidence.getByLoc(hopperBlock.getLocation());
        if (hopperRes != null && hopperRes.equals(sourceRes))
            return;

        event.setCancelled(true);
        moveItemDebounce.put(eventId, currentTime);
        cleanExpiredDebounceEntries(currentTime);
    }

    private void cleanExpiredDebounceEntries(long currentTime) {
        moveItemDebounce.entrySet().removeIf(
                entry -> currentTime - entry.getValue() > DEBOUNCE_THRESHOLD * 2
        );
    }

    private Block getBlockFromHolder(InventoryHolder holder) {
        if (holder instanceof BlockState) {
            return ((BlockState) holder).getBlock();
        }
        return null;
    }

    private String generateEventId(Block hopperBlock, Block chestBlock) {
        if (hopperBlock == null || chestBlock == null) return null;
        return hopperBlock.getWorld().getUID().toString() + "_" +
                hopperBlock.getX() + "_" + hopperBlock.getY() + "_" + hopperBlock.getZ() + "_" +
                chestBlock.getX() + "_" + chestBlock.getY() + "_" + chestBlock.getZ();
    }
}
