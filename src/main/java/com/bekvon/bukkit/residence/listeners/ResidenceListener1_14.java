package com.bekvon.bukkit.residence.listeners;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.ResAdmin;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagCombo;
import com.bekvon.bukkit.residence.utils.Utils;

import net.Zrips.CMILib.Entities.CMIEntityType;
import net.Zrips.CMILib.Logs.CMIDebug;
import net.Zrips.CMILib.Items.CMIMaterial;

public class ResidenceListener1_14 implements Listener {

    private Residence plugin;

    public ResidenceListener1_14(Residence plugin) {
        this.plugin = plugin;
    }

    private static final Map<String, Tag<Material>> BLOCK_TAG_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Tag<Material>> ITEM_TAG_CACHE = new ConcurrentHashMap<>();

    public boolean isBlockTag(Material block, String tagName) {
        if (block == null || tagName == null) {
            return false;
        }
        Tag<Material> tag = BLOCK_TAG_CACHE.computeIfAbsent(tagName, key ->
                Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft(key), Material.class)
        );
        return tag != null && tag.isTagged(block);
    }

    public boolean isItemTag(Material item, String tagName) {
        if (item == null || tagName == null) {
            return false;
        }
        Tag<Material> tag = ITEM_TAG_CACHE.computeIfAbsent(tagName, key ->
                Bukkit.getTag(Tag.REGISTRY_ITEMS, NamespacedKey.minecraft(key), Material.class)
        );
        return tag != null && tag.isTagged(item);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLecternBookTake(PlayerTakeLecternBookEvent event) {
        // Disabling listener if flag disabled globally
        if (!Flags.container.isGlobalyEnabled())
            return;
        // disabling event on world
        if (plugin.isDisabledWorldListener(event.getLectern().getWorld()))
            return;

        Player player = event.getPlayer();

        if (ResAdmin.isResAdmin(player))
            return;

        FlagPermissions perms = FlagPermissions.getPerms(event.getLectern().getLocation(), player);
        if (perms.playerHas(player, Flags.container, true))
            return;

        lm.Flag_Deny.sendMessage(player, Flags.container);

        event.setCancelled(true);

    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onRavager(EntityChangeBlockEvent event) {
        // Disabling listener if flag disabled globally
        if (!Flags.destroy.isGlobalyEnabled())
            return;
        // disabling event on world
        if (plugin.isDisabledWorldListener(event.getEntity().getWorld()))
            return;

        if (event.getEntity().getType() != EntityType.RAVAGER)
            return;

        FlagPermissions perms = FlagPermissions.getPerms(event.getBlock().getLocation());
        if (perms.has(Flags.destroy, true))
            return;

        event.setCancelled(true);

    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVehicleDamage(VehicleDamageEvent event) {
        // Disabling listener if flag disabled globally
        if (!Flags.vehicledestroy.isGlobalyEnabled())
            return;
        // disabling event on world
        if (plugin.isDisabledWorldListener(event.getVehicle().getWorld()))
            return;

        Entity attacker = event.getAttacker();
        if (attacker instanceof Player) {

            Player player = (Player) attacker;

            if (ResAdmin.isResAdmin(player))
                return;

            FlagPermissions perms = FlagPermissions.getPerms(event.getVehicle().getLocation(), player);
            if (perms.playerHas(player, Flags.vehicledestroy, true))
                return;

            lm.Flag_Deny.sendMessage(player, Flags.vehicledestroy);

            event.setCancelled(true);

        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectileHitBell(ProjectileHitEvent event) {
        // Disabling listener if flag disabled globally
        if (!Flags.use.isGlobalyEnabled())
            return;

        Block block = event.getHitBlock();
        if (block == null)
            return;
        // disabling event on world
        if (plugin.isDisabledWorldListener(block.getWorld()))
            return;

        if (block.getType() != Material.BELL)
            return;

        Player player = Utils.potentialProjectileToPlayer(event.getEntity());
        if (player != null) {

            if (ResAdmin.isResAdmin(player))
                return;

            if (FlagPermissions.has(block.getLocation(), player, Flags.use, true))
                return;

            lm.Flag_Deny.sendMessage(player, Flags.use);
            event.setCancelled(true);

        } else {
            // Entity not player source
            // Check potential block as a shooter which should be allowed if its inside same
            // residence
            if (Utils.isSourceBlockInsideSameResidence(event.getEntity(), ClaimedResidence.getByLoc(block.getLocation())))
                return;

            if (FlagPermissions.has(block.getLocation(), Flags.use, true))
                return;

            event.setCancelled(true);

        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerharvest(PlayerInteractEvent event) {
        // Disabling listener if flag disabled globally
        if (!Flags.harvest.isGlobalyEnabled())
            return;

        Block block = event.getClickedBlock();
        if (block == null)
            return;
        // disabling event on world
        if (plugin.isDisabledWorldListener(block.getWorld()))
            return;

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        CMIMaterial mat = CMIMaterial.get(block.getType());

        if (mat != CMIMaterial.SWEET_BERRY_BUSH && mat !=CMIMaterial.CAVE_VINES && mat != CMIMaterial.CAVE_VINES_PLANT) {
            return;
        }

        Player player = event.getPlayer();
        if (ResAdmin.isResAdmin(player))
            return;

        if (FlagPermissions.has(block.getLocation(), player, Flags.harvest, true))
            return;

        lm.Flag_Deny.sendMessage(player, Flags.harvest);
        event.setCancelled(true);

    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCoralDryFade(BlockFadeEvent event) {
        // Disabling listener if flag disabled globally
        if (!Flags.coraldryup.isGlobalyEnabled())
            return;

        Block block = event.getBlock();
        // disabling event on world
        if (plugin.isDisabledWorldListener(block.getWorld()))
            return;

        Material mat = block.getType();

        if (isBlockTag(mat, "corals") || isBlockTag(mat, "coral_blocks") || isBlockTag(mat, "wall_corals")) {

            if (FlagPermissions.has(block.getLocation(), Flags.coraldryup, FlagCombo.OnlyFalse))
                event.setCancelled(true);

        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAnimalFeeding(PlayerInteractEntityEvent event) {
        // Disabling listener if flag disabled globally
        if (!Flags.animalfeeding.isGlobalyEnabled())
            return;

        Entity entity = event.getRightClicked();
        // disabling event on world
        if (plugin.isDisabledWorldListener(entity.getWorld()))
            return;

        if (!(entity instanceof Animals))
            return;

        Player player = event.getPlayer();

        ItemStack item = event.getHand() == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR)
            return;

        CMIEntityType type = CMIEntityType.get(entity.getType());
        Material held = item.getType();

        if (isFeedingAnimal(type, held)) {

            if (ResAdmin.isResAdmin(player))
                return;

            FlagPermissions perms = FlagPermissions.getPerms(entity.getLocation(), player);
            if (perms.playerHas(player, Flags.animalfeeding, perms.playerHas(player, Flags.animalkilling, true)))
                return;

            lm.Flag_Deny.sendMessage(player, Flags.animalfeeding);
            event.setCancelled(true);

        }
    }

    private boolean isFeedingAnimal(CMIEntityType type, Material held) {

        switch (type) {
            case ARMADILLO:
                if (isItemTag(held, "armadillo_food")) return true;
                break;
            case AXOLOTL:
                if (isItemTag(held, "axolotl_food")) return true;
                break;
            case BEE:
                if (isItemTag(held, "bee_food")) return true;
                break;
            case CAMEL:
                if (isItemTag(held, "camel_food")) return true;
                break;
            case CAMEL_HUSK:
                if (isItemTag(held, "camel_husk_food")) return true;
                break;
            case CAT:
                if (isItemTag(held, "cat_food")) return true;
                break;
            case CHICKEN:
                if (isItemTag(held, "chicken_food")) return true;
                break;
            case COW:
                if (isItemTag(held, "cow_food")) return true;
                break;
            case DONKEY:
                if (isItemTag(held, "horse_food")) return true;
                break;
            case FOX:
                if (isItemTag(held, "fox_food")) return true;
                break;
            case FROG:
                if (isItemTag(held, "frog_food")) return true;
                break;
            case GOAT:
                if (isItemTag(held, "goat_food")) return true;
                break;
            case HAPPY_GHAST:
                if (isItemTag(held, "happy_ghast_food")) return true;
                break;
            case HOGLIN:
                if (isItemTag(held, "hoglin_food")) return true;
                break;
            case HORSE:
                if (isItemTag(held, "horse_food")) return true;
                break;
            case LLAMA:
                if (isItemTag(held, "llama_food")) return true;
                break;
            case MOOSHROOM:
                if (isItemTag(held, "cow_food")) return true;
                break;
            case MULE:
                if (isItemTag(held, "horse_food")) return true;
                break;
            case NAUTILUS:
                if (isItemTag(held, "nautilus_food")) return true;
                break;
            case OCELOT:
                if (isItemTag(held, "ocelot_food")) return true;
                break;
            case PANDA:
                if (isItemTag(held, "panda_food")) return true;
                break;
            case PARROT:
                if (isItemTag(held, "parrot_food")) return true;
                break;
            case PIG:
                if (isItemTag(held, "pig_food")) return true;
                break;
            case RABBIT:
                if (isItemTag(held, "rabbit_food")) return true;
                break;
            case SHEEP:
                if (isItemTag(held, "sheep_food")) return true;
                break;
            case SNIFFER:
                if (isItemTag(held, "sniffer_food")) return true;
                break;
            case STRIDER:
                if (isItemTag(held, "strider_food")) return true;
                break;
            case TRADER_LLAMA:
                if (isItemTag(held, "llama_food")) return true;
                break;
            case TURTLE:
                if (isItemTag(held, "turtle_food")) return true;
                break;
            case WOLF:
                if (isItemTag(held, "wolf_food")) return true;
                break;
            case ZOMBIE_HORSE:
                if (held == Material.RED_MUSHROOM) return true;
                break;
            case ZOMBIE_NAUTILUS:
                if (isItemTag(held, "nautilus_food")) return true;
                break;
            default:
                break;
        }
        return false;
    }
}
