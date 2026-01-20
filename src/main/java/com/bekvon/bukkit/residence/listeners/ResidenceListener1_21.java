package com.bekvon.bukkit.residence.listeners;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffectType;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.ResAdmin;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagCombo;
import com.bekvon.bukkit.residence.utils.Utils;

import net.Zrips.CMILib.Entities.CMIEntityType;
import net.Zrips.CMILib.Items.CMIMaterial;
import net.Zrips.CMILib.Logs.CMIDebug;

public class ResidenceListener1_21 implements Listener {

    private Residence plugin;

    public ResidenceListener1_21(Residence plugin) {
        this.plugin = plugin;
    }

    HashMap<UUID, Long> boats = new HashMap<UUID, Long>();

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        boats.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void OnVehicleEnterEvent(VehicleEnterEvent event) {
        // Disabling listener if flag disabled globally
        if (!Flags.boarding.isGlobalyEnabled())
            return;
        // disabling event on world
        if (plugin.isDisabledWorldListener(event.getVehicle().getWorld()))
            return;

        Entity entity = event.getEntered();

        if (!(entity instanceof LivingEntity))
            return;

        if (!Utils.isAnimal(entity))
            return;

        if (FlagPermissions.getPerms(entity.getLocation()).has(Flags.boarding, FlagCombo.OnlyFalse)) {
            event.setCancelled(true);
            return;
        }

        ClaimedResidence res = plugin.getResidenceManager().getByLoc(entity.getLocation());
        if (res == null)
            return;

        Player closest = null;
        double dist = 32D;

        for (Player player : res.getPlayersInResidence()) {

            double tempDist = player.getLocation().distance(entity.getLocation());

            if (tempDist < dist) {
                closest = player;
                dist = tempDist;
            }
        }

        if (closest == null)
            return;

        if (res.getPermissions().playerHas(closest, Flags.leash, FlagCombo.OnlyFalse)) {
            Long time = boats.computeIfAbsent(closest.getUniqueId(), k -> 0L);

            if (time + 1000L < System.currentTimeMillis()) {
                boats.put(closest.getUniqueId(), System.currentTimeMillis());
                lm.Residence_FlagDeny.sendMessage(closest, Flags.leash, res.getName());
            }

            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void OnEntityDeath(EntityDeathEvent event) {
        // Disabling listener if flag disabled globally
        if (!Flags.build.isGlobalyEnabled())
            return;
        // disabling event on world
        LivingEntity ent = event.getEntity();
        if (ent == null)
            return;
        if (plugin.isDisabledWorldListener(ent.getWorld()))
            return;
        if (!ent.hasPotionEffect(PotionEffectType.WEAVING))
            return;

        Location loc = ent.getLocation();
        FlagPermissions perms = FlagPermissions.getPerms(loc);
        if (perms.has(Flags.build, FlagCombo.TrueOrNone))
            return;

        // Removing weaving effect on death as there is no other way to properly handle
        // this effect inside residence
        ent.removePotionEffect(PotionEffectType.WEAVING);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteractCopperGolem(PlayerInteractEntityEvent event) {
        // Disabling listener if flag disabled globally
        if (!Flags.copper.isGlobalyEnabled())
            return;

        Entity entity = event.getRightClicked();
        // disabling event on world
        if (plugin.isDisabledWorldListener(entity.getWorld()))
            return;

        if (CMIEntityType.get(entity) != CMIEntityType.COPPER_GOLEM)
            return;

        Player player = event.getPlayer();
        if (ResAdmin.isResAdmin(player))
            return;

        if (entity instanceof LivingEntity) {

            EntityEquipment gloemInv = ((LivingEntity) entity).getEquipment();
            // Right-click to remove items from holding copper_golem
            if (gloemInv != null && (!gloemInv.getItemInMainHand().isEmpty() || !gloemInv.getItemInOffHand().isEmpty())) {

                if (FlagPermissions.has(entity.getLocation(), player, Flags.container, true))
                    return;

                lm.Flag_Deny.sendMessage(player, Flags.container);
                event.setCancelled(true);
                return;
            }
        }
        // Copper_golem has no item in hand

        Material held = (event.getHand() == EquipmentSlot.OFF_HAND)
                ? player.getInventory().getItemInOffHand().getType()
                : player.getInventory().getItemInMainHand().getType();

        // Avoid overwriting Leash Flag, Lead Shears
        if (held != Material.HONEYCOMB && !isItemTag(held, "axes"))
            return;

        FlagPermissions perms = FlagPermissions.getPerms(entity.getLocation(), player);
        if (perms.playerHas(player, Flags.copper, perms.playerHas(player, Flags.animalkilling, true)))
            return;

        lm.Flag_Deny.sendMessage(player, Flags.copper);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFishingBobberHit(ProjectileHitEvent event) {
        // Disabling listener if flag disabled globally
        if (!Flags.hook.isGlobalyEnabled())
            return;
        // anti NPE
        Entity HitEntity = event.getHitEntity();
        if (HitEntity == null)
            return;
        // disabling event on world
        if (plugin.isDisabledWorldListener(HitEntity.getWorld()))
            return;

        Projectile hook = event.getEntity();
        // only fishing_bobber
        if (CMIEntityType.get(hook) != CMIEntityType.FISHING_BOBBER)
            return;
        // have player source
        if (!(hook.getShooter() instanceof Player))
            return;

        Player player = (Player) hook.getShooter();
        if (ResAdmin.isResAdmin(player))
            return;

        FlagPermissions perms = FlagPermissions.getPerms(HitEntity.getLocation(), player);
        if (perms.playerHas(player, Flags.hook, true))
            return;

        lm.Flag_Deny.sendMessage(player, Flags.hook);
        event.setCancelled(true);
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

        Material held = event.getHand() == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand().getType()
                : player.getInventory().getItemInMainHand().getType();

        CMIEntityType type = CMIEntityType.get(entity.getType());

        if (!isFeedingAnimal(type, held))
            return;

        if (ResAdmin.isResAdmin(player))
            return;

        FlagPermissions perms = FlagPermissions.getPerms(entity.getLocation(), player);
        if (perms.playerHas(player, Flags.animalfeeding, perms.playerHas(player, Flags.animalkilling, true)))
            return;

        lm.Flag_Deny.sendMessage(player, Flags.animalfeeding);
        event.setCancelled(true);

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

    private static boolean isItemTag(Material item, String tagName) {
        return ResidenceListener1_14.isItemTag(item, tagName);
    }
}
