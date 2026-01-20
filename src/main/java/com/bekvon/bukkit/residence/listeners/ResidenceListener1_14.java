package com.bekvon.bukkit.residence.listeners;

import org.bukkit.Material;
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

        if (!Tag.CORALS.isTagged(mat) && !Tag.CORAL_BLOCKS.isTagged(mat) && !Tag.WALL_CORALS.isTagged(mat))
            return;

        if (FlagPermissions.has(block.getLocation(), Flags.coraldryup, FlagCombo.OnlyFalse)) {
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
        try {
            switch (type) {
                case ARMADILLO:
                    if (Tag.ITEMS_ARMADILLO_FOOD.isTagged(held)) return true;
                    break;
                case AXOLOTL:
                    if (Tag.ITEMS_AXOLOTL_FOOD.isTagged(held)) return true;
                    break;
                case BEE:
                    if (Tag.ITEMS_BEE_FOOD.isTagged(held)) return true;
                    break;
                case CAMEL:
                    if (Tag.ITEMS_CAMEL_FOOD.isTagged(held)) return true;
                    break;
                case CAMEL_HUSK:
                    if (Tag.ITEMS_CAMEL_HUSK_FOOD.isTagged(held)) return true;
                    break;
                case CAT:
                    if (Tag.ITEMS_CAT_FOOD.isTagged(held)) return true;
                    break;
                case CHICKEN:
                    if (Tag.ITEMS_CHICKEN_FOOD.isTagged(held)) return true;
                    break;
                case COW:
                    if (Tag.ITEMS_COW_FOOD.isTagged(held)) return true;
                    break;
                case DONKEY:
                    if (Tag.ITEMS_HORSE_FOOD.isTagged(held)) return true;
                    break;
                case FOX:
                    if (Tag.ITEMS_FOX_FOOD.isTagged(held)) return true;
                    break;
                case FROG:
                    if (Tag.ITEMS_FROG_FOOD.isTagged(held)) return true;
                    break;
                case GOAT:
                    if (Tag.ITEMS_GOAT_FOOD.isTagged(held)) return true;
                    break;
                case HAPPY_GHAST:
                    if (Tag.ITEMS_HAPPY_GHAST_FOOD.isTagged(held)) return true;
                    break;
                case HOGLIN:
                    if (Tag.ITEMS_HOGLIN_FOOD.isTagged(held)) return true;
                    break;
                case HORSE:
                    if (Tag.ITEMS_HORSE_FOOD.isTagged(held)) return true;
                    break;
                case LLAMA:
                    if (Tag.ITEMS_LLAMA_FOOD.isTagged(held)) return true;
                    break;
                case MOOSHROOM:
                    if (Tag.ITEMS_COW_FOOD.isTagged(held)) return true;
                    break;
                case MULE:
                    if (Tag.ITEMS_HORSE_FOOD.isTagged(held)) return true;
                    break;
                case NAUTILUS:
                    if (Tag.ITEMS_NAUTILUS_FOOD.isTagged(held)) return true;
                    break;
                case OCELOT:
                    if (Tag.ITEMS_OCELOT_FOOD.isTagged(held)) return true;
                    break;
                case PANDA:
                    if (Tag.ITEMS_PANDA_FOOD.isTagged(held)) return true;
                    break;
                case PARROT:
                    if (Tag.ITEMS_PARROT_FOOD.isTagged(held)) return true;
                    break;
                case PIG:
                    if (Tag.ITEMS_PIG_FOOD.isTagged(held)) return true;
                    break;
                case RABBIT:
                    if (Tag.ITEMS_RABBIT_FOOD.isTagged(held)) return true;
                    break;
                case SHEEP:
                    if (Tag.ITEMS_SHEEP_FOOD.isTagged(held)) return true;
                    break;
                case SNIFFER:
                    if (Tag.ITEMS_SNIFFER_FOOD.isTagged(held)) return true;
                    break;
                case STRIDER:
                    if (Tag.ITEMS_STRIDER_FOOD.isTagged(held)) return true;
                    break;
                case TRADER_LLAMA:
                    if (Tag.ITEMS_LLAMA_FOOD.isTagged(held)) return true;
                    break;
                case TURTLE:
                    if (Tag.ITEMS_TURTLE_FOOD.isTagged(held)) return true;
                    break;
                case WOLF:
                    if (Tag.ITEMS_WOLF_FOOD.isTagged(held)) return true;
                    break;
                case ZOMBIE_HORSE:
                    if (held == Material.RED_MUSHROOM) return true;
                    break;
                case ZOMBIE_NAUTILUS:
                    if (Tag.ITEMS_NAUTILUS_FOOD.isTagged(held)) return true;
                    break;
                default:
                    break;
            }
        } catch (Throwable e) {
        }
        return false;
    }
}
