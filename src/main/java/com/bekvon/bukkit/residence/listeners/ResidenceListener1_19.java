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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ResidenceListener1_19 implements Listener {

    private Residence plugin;

    public ResidenceListener1_19(Residence plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            cleanExpiredDebounceEntries(currentTime);
        }, 0, 600);
    }

    private final Map<String, Long> moveItemDebounce = new ConcurrentHashMap<>();

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

        String worldName = hopperBlock.getWorld().getName();

        Location hLoc = hopperBlock.getLocation();
        Location cLoc = chestBlock.getLocation();
        return String.format("%s_%d_%d_%d_%d_%d_%d",
                worldName,
                hLoc.getBlockX(), hLoc.getBlockY(), hLoc.getBlockZ(),
                cLoc.getBlockX(), cLoc.getBlockY(), cLoc.getBlockZ()
        );
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
            if (currentTime - lastProcessedTime < DEBOUNCE_THRESHOLD) {
                event.setCancelled(true);
                return;
            }
        }

        if (hopperBlock == null || !hopperBlock.getType().name().contains("hopper"))
            return;
        if (chestBlock == null)
            return;

        ClaimedResidence chestRes = ClaimedResidence.getByLoc(chestBlock.getLocation());
        if (chestRes == null || chestRes.getName() == null)
            return;

        ClaimedResidence hopperRes = ClaimedResidence.getByLoc(hopperBlock.getLocation());
        if (hopperRes == null || hopperRes.getName() == null) {
            event.setCancelled(true);
            return;
        }
        if (Objects.equals(chestRes.getName(), hopperRes.getName()))
            return;

        event.setCancelled(true);
        if (!moveItemDebounce.containsKey(eventId) || currentTime - moveItemDebounce.get(eventId) > DEBOUNCE_THRESHOLD) {
            moveItemDebounce.put(eventId, currentTime);
        }
        cleanExpiredDebounceEntries(currentTime);
    }
}
