package com.bekvon.bukkit.residence.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Witch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.event.ResidenceChangedEvent;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagCombo;
import com.bekvon.bukkit.residence.utils.Teleporting;
import com.bekvon.bukkit.residence.utils.Utils;

import net.Zrips.CMILib.ActionBar.CMIActionBar;
import net.Zrips.CMILib.Items.CMIMaterial;
import net.Zrips.CMILib.Version.Version;
import net.Zrips.CMILib.Version.Schedulers.CMIScheduler;

public class ResidenceListener1_09 implements Listener {

    private final Residence plugin;

    public ResidenceListener1_09(Residence plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void EntityToggleGlideEvent(EntityToggleGlideEvent event) {

        Entity entity = event.getEntity();

        if (FlagPermissions.shouldIgnoreCheck(Flags.elytra, entity)) {
            return;
        }
        if (!(entity instanceof Player))
            return;

        Player player = (Player) entity;

        if (FlagPermissions.shouldDenyAndNotify(player, player, Flags.elytra, null)) {
            event.setCancelled(true);
            CMIScheduler.runAtLocation(plugin, player.getLocation(), () -> {
                // Need to enable before disabling to prevent client side bug
                player.setGliding(true);
                player.setGliding(false);
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResidenceChange(ResidenceChangedEvent event) {
        // Disabling listener if flag disabled globally
        if (!Flags.elytra.isGlobalyEnabled())
            return;

        Player player = event.getPlayer();
        if (player == null)
            return;

        if (!player.isGliding())
            return;

        ClaimedResidence newRes = event.getTo();
        if (newRes == null)
            return;

        if (newRes.getPermissions().playerHas(player, Flags.elytra, FlagCombo.TrueOrNone))
            return;

        player.setGliding(false);

        CMIScheduler.runAtLocation(plugin, player.getLocation(), () -> {

            Location loc = ResidencePlayerListener.getSafeLocation(player.getLocation());
            if (loc == null) {
                // get defined land location in case no safe landing spot are found
                loc = plugin.getConfigManager().getFlyLandLocation();
                if (loc == null) {
                    // get main world spawn location in case valid location is not found
                    loc = Bukkit.getWorlds().get(0).getSpawnLocation();
                }
            }
            if (loc != null) {
                lm.Flag_Deny.sendMessage(player, Flags.elytra);
                player.closeInventory();
                Teleporting.teleport(player, loc);
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLingeringPotionSplash(LingeringPotionSplashEvent event) {

        ThrownPotion potion = event.getEntity();

        if (plugin.isDisabledWorldListener(potion)) {
            return;
        }
        ProjectileSource shooter = potion.getShooter();

        if (shooter instanceof Player) {
            if (!Flags.potionthrowing.isGlobalyEnabled()) {
                return;
            }
            Player shooterPlayer = (Player) shooter;
            if (FlagPermissions.shouldDenyAndNotify(shooterPlayer, potion, Flags.potionthrowing, null)) {
                event.setCancelled(true);
            }
            return;
        }
        if (!Flags.build.isGlobalyEnabled()) {
            return;
        }
        // Now handling LingeringPotion thrown by non-player entities
        // (e.g., dispensers, ominous item spawner)
        ClaimedResidence potionHitRes = ClaimedResidence.getByLoc(potion.getLocation());
        // There is no Residence at the hit location; skip the check
        if (potionHitRes == null) {
            return;
        }
        Location shooterLoc = null;

        if (shooter instanceof BlockProjectileSource) {
            shooterLoc = ((BlockProjectileSource) shooter).getBlock().getLocation();

        } else if (shooter instanceof Entity) {
            shooterLoc = ((Entity) shooter).getLocation();

        }
        ClaimedResidence shooterRes = ClaimedResidence.getByLoc(shooterLoc);
        // Non-player shooter and hit location in same Residence or same owner; skip check
        if (potionHitRes == shooterRes || (shooterRes != null && shooterRes.isOwner(potionHitRes.getOwner()))) {
            return;
        }
        // Prevent effect clouds from being spawned into a Residence from outside
        if (potionHitRes.getPermissions().has(Flags.build, FlagCombo.OnlyFalse)) {
            event.setCancelled(true);
        }
    }

    @SuppressWarnings("removal")
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAreaEffectCloudApply(AreaEffectCloudApplyEvent event) {

        AreaEffectCloud cloud = event.getEntity();

        if (plugin.isDisabledWorldListener(cloud)) {
            return;
        }
        ProjectileSource attacker = cloud.getSource();
        Location attackerLoc = null;

        if (attacker instanceof BlockProjectileSource) {
            attackerLoc = ((BlockProjectileSource) attacker).getBlock().getLocation();

        } else if (attacker instanceof Entity && !(attacker instanceof Player)) {
            attackerLoc = ((Entity) attacker).getLocation();

        }
        // Now handling effect clouds spawned by dispensers or ominous item spawner
        if (attackerLoc != null) {
            ClaimedResidence attackerRes = ClaimedResidence.getByLoc(attackerLoc);

            event.getAffectedEntities().removeIf(victim -> {
                ClaimedResidence victimRes = ClaimedResidence.getByLoc(victim.getLocation());
                // There is no Residence at the hit location; skip the check
                if (victimRes == null) {
                    return false;
                }
                // Non-player shooter and hit location in same Residence or same owner; skip check
                if (victimRes == attackerRes || (attackerRes != null && attackerRes.isOwner(victimRes.getOwner()))) {
                    return false;
                }
                // Prevent clouds generated by external blocks from taking effect inside the Residence
                return victimRes.getPermissions().has(Flags.build, FlagCombo.OnlyFalse);
            });
            return;
        }
        // Now handling AreaEffectClouds spawned by player throws or unknown sources
        boolean isHealingCloud = false;
        boolean isDamageCloud = false;
        boolean isHarmfulCloud = false;

        PotionType potionType = null;
        // Start - Get AreaEffectCloud effect type
        if (Version.isCurrentEqualOrHigher(Version.v1_20_2)) {
            potionType = cloud.getBasePotionType();
            if (potionType == null) {
                return;
            }
            for (PotionEffect effect : potionType.getPotionEffects()) {
                PotionEffectType type = effect.getType();
                if (Utils.isPotionEffectType(type, "Healing")) {
                    isHealingCloud = true;
                    break;

                } else if (Utils.isPotionEffectType(type, "Damage")) {
                    isDamageCloud = true;
                    break;

                } else if (Utils.isPotionEffectType(type, "Harmful")) {
                    isHarmfulCloud = true;
                    break;

                }
            }
        } else {
            org.bukkit.potion.PotionData data = cloud.getBasePotionData();
            if (data != null) {
                potionType = data.getType();
            }
            if (potionType == null) {
                return;
            }
            PotionEffectType type = potionType.getEffectType();
            if (Utils.isPotionEffectType(type, "Healing")) {
                isHealingCloud = true;

            } else if (Utils.isPotionEffectType(type, "Damage")) {
                isDamageCloud = true;

            } else if (Utils.isPotionEffectType(type, "Harmful")) {
                isHarmfulCloud = true;

            }
        }
        // End - Get AreaEffectCloud effect type
        if (isHealingCloud && Flags.mobkilling.isGlobalyEnabled()) {
            boolean isPlayerAttacker = attacker instanceof Player;
            event.getAffectedEntities().removeIf(victim -> shouldDenyHealingEffect(victim, attacker, isPlayerAttacker));

        } else if (isDamageCloud) {
            boolean isPlayerAttacker = attacker instanceof Player;
            ClaimedResidence attackerRes = isPlayerAttacker
                    ? ClaimedResidence.getByLoc(((Player) attacker).getLocation())
                    : null;
            event.getAffectedEntities().removeIf(victim -> shouldDenyDamageEffect(victim, attacker, attackerRes, isPlayerAttacker));

        } else if (isHarmfulCloud) {
            boolean isPlayerAttacker = attacker instanceof Player;
            ClaimedResidence attackerRes = isPlayerAttacker
                    ? ClaimedResidence.getByLoc(((Player) attacker).getLocation())
                    : null;
            event.getAffectedEntities().removeIf(victim -> shouldDenyHarmfulEffect(victim, attacker, attackerRes, isPlayerAttacker));

        }
    }

    public static boolean shouldDenyHealingEffect(LivingEntity victim, ProjectileSource attacker, boolean isPlayerAttacker) {
        // healing potions damaging undead mobs
        if (isPlayerAttacker) {
            if (Utils.isUndead(victim)) {
                return FlagPermissions.shouldDenyAndNotify((Player) attacker, victim, Flags.mobkilling, null);
            }
        } else {
            if (Utils.isUndead(victim)) {
                return FlagPermissions.has(victim.getLocation(), Flags.mobkilling, FlagCombo.OnlyFalse);
            }
        }
        return false;
    }

    public static boolean shouldDenyDamageEffect(LivingEntity victim, ProjectileSource attacker, ClaimedResidence attackerRes, boolean isPlayerAttacker) {
        if (isPlayerAttacker) {
            Player player = (Player) attacker;
            if (Flags.pvp.isGlobalyEnabled() && victim instanceof Player) {
                FlagPermissions attackerPerms = (attackerRes != null)
                        ? attackerRes.getPermissions()
                        : Residence.getInstance().getWorldFlags().getPerms(player.getWorld());
                if (attackerPerms.has(Flags.pvp, FlagCombo.OnlyFalse)) {
                    lm.Flag_Deny.sendMessage(player, Flags.pvp);
                    return true;
                }
                ClaimedResidence victimRes = ClaimedResidence.getByLoc(victim.getLocation());
                FlagPermissions victimPerms = victimRes != null
                        ? victimRes.getPermissions()
                        : Residence.getInstance().getWorldFlags().getPerms(victim.getWorld());
                if (victimPerms.has(Flags.pvp, FlagCombo.OnlyFalse)) {
                    lm.Flag_Deny.sendMessage(player, Flags.pvp);
                    return true;
                }
                if (attackerRes != null && attackerRes == victimRes
                        && attackerPerms.playerHas(player, Flags.friendlyfire, FlagCombo.OnlyFalse)
                        && attackerPerms.playerHas((Player) victim, Flags.friendlyfire, FlagCombo.OnlyFalse)) {
                    CMIActionBar.send(player, Residence.getInstance().getLM().getMessage(lm.General_NoFriendlyFire));
                    return true;
                }
            } else if (Flags.animalkilling.isGlobalyEnabled() && Utils.isAnimal(victim)) {
                return FlagPermissions.shouldDenyAndNotify(player, victim, Flags.animalkilling, null);

            } else if (Utils.isUndead(victim)) {
                // Damage cloud is not harmful to undead
                return false;

            } else if (Flags.mobkilling.isGlobalyEnabled() && Utils.isMonster(victim)) {
                return FlagPermissions.shouldDenyAndNotify(player, victim, Flags.mobkilling, null);

            }
        } else {
            if (Flags.pvp.isGlobalyEnabled() && victim instanceof Player) {
                if (attacker instanceof Witch) {
                    return false;
                }
                return FlagPermissions.has(victim.getLocation(), Flags.pvp, FlagCombo.OnlyFalse);

            } else if (Flags.animalkilling.isGlobalyEnabled() && Utils.isAnimal(victim)) {
                return FlagPermissions.has(victim.getLocation(), Flags.animalkilling, FlagCombo.OnlyFalse);

            } else if (Utils.isUndead(victim)) {
                // Damage cloud is not harmful to undead
                return false;

            } else if (Flags.mobkilling.isGlobalyEnabled() && Utils.isMonster(victim)) {
                return FlagPermissions.has(victim.getLocation(), Flags.mobkilling, FlagCombo.OnlyFalse);

            }
        }
        return false;
    }

    public static boolean shouldDenyHarmfulEffect(LivingEntity victim, ProjectileSource attacker, ClaimedResidence attackerRes, boolean isPlayerAttacker) {
        if (isPlayerAttacker) {
            Player player = (Player) attacker;
            if (Flags.pvp.isGlobalyEnabled() && victim instanceof Player) {
                FlagPermissions attackerPerms = (attackerRes != null)
                        ? attackerRes.getPermissions()
                        : Residence.getInstance().getWorldFlags().getPerms(player.getWorld());
                if (attackerPerms.has(Flags.pvp, FlagCombo.OnlyFalse)) {
                    lm.Flag_Deny.sendMessage(player, Flags.pvp);
                    return true;
                }
                ClaimedResidence victimRes = ClaimedResidence.getByLoc(victim.getLocation());
                FlagPermissions victimPerms = victimRes != null
                        ? victimRes.getPermissions()
                        : Residence.getInstance().getWorldFlags().getPerms(victim.getWorld());
                if (victimPerms.has(Flags.pvp, FlagCombo.OnlyFalse)) {
                    lm.Flag_Deny.sendMessage(player, Flags.pvp);
                    return true;
                }
                if (attackerRes != null && attackerRes == victimRes
                        && attackerPerms.playerHas(player, Flags.friendlyfire, FlagCombo.OnlyFalse)
                        && attackerPerms.playerHas((Player) victim, Flags.friendlyfire, FlagCombo.OnlyFalse)) {
                    CMIActionBar.send(player, Residence.getInstance().getLM().getMessage(lm.General_NoFriendlyFire));
                    return true;
                }
            } else if (Flags.animalkilling.isGlobalyEnabled() && Utils.isAnimal(victim)) {
                return FlagPermissions.shouldDenyAndNotify(player, victim, Flags.animalkilling, null);

            } else if (Flags.mobkilling.isGlobalyEnabled() && Utils.isMonster(victim)) {
                return FlagPermissions.shouldDenyAndNotify(player, victim, Flags.mobkilling, null);

            }
        } else {
            if (Flags.pvp.isGlobalyEnabled() && victim instanceof Player) {
                if (attacker instanceof Witch) {
                    return false;
                }
                return FlagPermissions.has(victim.getLocation(), Flags.pvp, FlagCombo.OnlyFalse);

            } else if (Flags.animalkilling.isGlobalyEnabled() && Utils.isAnimal(victim)) {
                return FlagPermissions.has(victim.getLocation(), Flags.animalkilling, FlagCombo.OnlyFalse);

            } else if (Flags.mobkilling.isGlobalyEnabled() && Utils.isMonster(victim)) {
                return FlagPermissions.has(victim.getLocation(), Flags.mobkilling, FlagCombo.OnlyFalse);

            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerEatChorusFruit(PlayerItemConsumeEvent event) {

        Player player = event.getPlayer();

        if (FlagPermissions.shouldIgnoreCheck(Flags.chorustp, player)) {
            return;
        }
        if (CMIMaterial.get(event.getItem()) != CMIMaterial.CHORUS_FRUIT) {
            return;
        }
        if (FlagPermissions.shouldDenyAndNotify(player, player, Flags.chorustp, null)) {
            event.setCancelled(true);
        }
    }

    // Bucket, glass_bottle, potion, pattern banners & dyed shulker_boxes change cauldron level
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCauldronLevelChange(CauldronLevelChangeEvent event) {

        Entity entity = event.getEntity();
        if (entity == null) {
            return;
        }
        if (FlagPermissions.shouldIgnoreCheck(Flags.build, entity)) {
            return;
        }
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player) entity;

        if (FlagPermissions.shouldDenyAndNotify(player, event.getBlock(), Flags.build, null)) {
            event.setCancelled(true);
        }
    }

    // Supported 1.9+
    public static Material getHeldMaterial(PlayerInteractEntityEvent event) {
        return event.getHand() == EquipmentSlot.OFF_HAND
                ? event.getPlayer().getInventory().getItemInOffHand().getType()
                : event.getPlayer().getInventory().getItemInMainHand().getType();
    }
}
