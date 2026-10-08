package com.bekvon.bukkit.residence.listeners;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
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

        if (FlagPermissions.shouldIgnoreCheck(Flags.potionthrowing, potion)) {
            return;
        }
        ProjectileSource shooter = potion.getShooter();

        if (shooter instanceof Player) {
            Player shooterPlayer = (Player) shooter;
            if (FlagPermissions.shouldDenyAndNotify(shooterPlayer, potion, Flags.potionthrowing, null)) {
                event.setCancelled(true);
            }

        } else {
            // Non-player-spawned area effect cloud
            // Prevent effect clouds from being spawned into a Residence from outside
            ClaimedResidence potionHitRes = ClaimedResidence.getByLoc(potion.getLocation());
            if (potionHitRes == null) {
                return;
            }
            Location shooterLoc = null;

            if (shooter instanceof Entity) {
                shooterLoc = ((Entity) shooter).getLocation();
            } else if (shooter instanceof BlockProjectileSource) {
                shooterLoc = ((BlockProjectileSource) shooter).getBlock().getLocation();
            }
            ClaimedResidence shooterRes = ClaimedResidence.getByLoc(shooterLoc);
            // Skip the check if the shooter and the hit location are in the same Residence
            if (potionHitRes == shooterRes) {
                return;
            }
            if (potionHitRes.getPermissions().has(Flags.potionthrowing, FlagCombo.OnlyFalse)
                    || shooterRes.getPermissions().has(Flags.potionthrowing, FlagCombo.OnlyFalse)) {
                event.setCancelled(true);
            }
        }
    }

    @SuppressWarnings("removal")
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAreaEffectCloudApply(AreaEffectCloudApplyEvent event) {

        AreaEffectCloud cloud = event.getEntity();

        if (plugin.isDisabledWorldListener(cloud)) {
            return;
        }
        ProjectileSource shooter = cloud.getSource();
        if (shooter instanceof BlockProjectileSource) {
            Location sourceLoc = ((BlockProjectileSource) shooter).getBlock().getLocation();
            ClaimedResidence shooterRes = ClaimedResidence.getByLoc(sourceLoc);

            event.getAffectedEntities().removeIf(victim -> {
                ClaimedResidence victimRes = ClaimedResidence.getByLoc(victim.getLocation());
                // Allow clouds generated by blocks within the Residence to bypass the check
                if (victimRes == null || victimRes == shooterRes) {
                    return false;
                }
                // Prevent clouds generated by external blocks from taking effect inside the Residence
                return victimRes.getPermissions().has(Flags.build, FlagCombo.OnlyFalse);
            });
            return;
        }
        boolean isHealingCloud = false;
        boolean isDamageCloud = false;
        boolean isHarmfulCloud = false;

        PotionType potionType = null;
        if (Version.isCurrentEqualOrHigher(Version.v1_20_2)) {
            potionType = cloud.getBasePotionType();
        } else {
            org.bukkit.potion.PotionData data = cloud.getBasePotionData();
            if (data != null) {
                potionType = data.getType();
            }
        }
        if (potionType == null) {
            return;
        }
        for (PotionEffect effect : potionType.getPotionEffects()) {
            PotionEffectType type = effect.getType();

            if (Utils.isPotionEffectType(type, "Healing")) {
                isHealingCloud = true;
                break;
            }
            if (Utils.isPotionEffectType(type, "Damage")) {
                isDamageCloud = true;
                break;
            }
            if (Utils.isPotionEffectType(type, "Harmful")) {
                isHarmfulCloud = true;
                break;
            }
        }

        if (isHealingCloud && Flags.mobkilling.isGlobalyEnabled()) {
            handleHealingCloud(event.getAffectedEntities(), shooter);

        } else if (isDamageCloud) {
            handleDamageCloud(event.getAffectedEntities(), shooter);

        } else if (isHarmfulCloud) {
            handleHarmfulCloud(event.getAffectedEntities(), shooter);

        }
    }

    private void handleHealingCloud(List<LivingEntity> affectedEntities, ProjectileSource shooter) {
        // healing potions damaging undead mobs
        if (shooter instanceof Player) {
            Player player = (Player) shooter;
            affectedEntities.removeIf(victim -> {
                if (Utils.isUndead(victim)) {
                    return FlagPermissions.shouldDenyAndNotify(player, victim, Flags.mobkilling, null);
                }
                return false;
            });
        } else {
            affectedEntities.removeIf(victim -> {
                if (Utils.isUndead(victim)) {
                    return FlagPermissions.has(victim.getLocation(), Flags.mobkilling, FlagCombo.OnlyFalse);
                }
                return false;
            });
        }
    }

    private void handleDamageCloud(List<LivingEntity> affectedEntities, ProjectileSource shooter) {
        if (shooter instanceof Player) {
            Player player = (Player) shooter;
            affectedEntities.removeIf(victim -> {
                if (Flags.pvp.isGlobalyEnabled() && victim instanceof Player) {
                    if (FlagPermissions.has(victim.getLocation(), Flags.pvp, FlagCombo.OnlyFalse)) {
                        lm.Flag_Deny.sendMessage(player, Flags.pvp);
                        return true;
                    }

                } else if (Flags.animalkilling.isGlobalyEnabled() && Utils.isAnimal(victim)) {
                    return FlagPermissions.shouldDenyAndNotify(player, victim, Flags.animalkilling, null);

                } else if (Utils.isUndead(victim)) {
                    // Damage cloud is not harmful to undead
                    return false;

                } else if (Utils.isMonster(victim)) {
                    return FlagPermissions.shouldDenyAndNotify(player, victim, Flags.mobkilling, null);

                }
                return false;
            });
        } else {
            affectedEntities.removeIf(victim -> {
                if (Flags.pvp.isGlobalyEnabled() && victim instanceof Player) {
                    return FlagPermissions.has(victim.getLocation(), Flags.pvp, FlagCombo.OnlyFalse);

                } else if (Flags.animalkilling.isGlobalyEnabled() && Utils.isAnimal(victim)) {
                    return FlagPermissions.has(victim.getLocation(), Flags.animalkilling, FlagCombo.OnlyFalse);

                } else if (Utils.isUndead(victim)) {
                    // Damage cloud is not harmful to undead
                    return false;

                } else if (Utils.isMonster(victim)) {
                    return FlagPermissions.has(victim.getLocation(), Flags.mobkilling, FlagCombo.OnlyFalse);

                }
                return false;
            });
        }
    }

    private void handleHarmfulCloud(List<LivingEntity> affectedEntities, ProjectileSource shooter) {
        if (shooter instanceof Player) {
            Player player = (Player) shooter;
            affectedEntities.removeIf(victim -> {
                if (Flags.pvp.isGlobalyEnabled() && victim instanceof Player) {
                    if (FlagPermissions.has(victim.getLocation(), Flags.pvp, FlagCombo.OnlyFalse)) {
                        lm.Flag_Deny.sendMessage(player, Flags.pvp);
                        return true;
                    }

                } else if (Flags.animalkilling.isGlobalyEnabled() && Utils.isAnimal(victim)) {
                    return FlagPermissions.shouldDenyAndNotify(player, victim, Flags.animalkilling, null);

                } else if (Flags.mobkilling.isGlobalyEnabled() && Utils.isMonster(victim)) {
                    return FlagPermissions.shouldDenyAndNotify(player, victim, Flags.mobkilling, null);

                }
                return false;
            });
        } else {
            affectedEntities.removeIf(victim -> {
                if (Flags.pvp.isGlobalyEnabled() && victim instanceof Player) {
                    return FlagPermissions.has(victim.getLocation(), Flags.pvp, FlagCombo.OnlyFalse);

                } else if (Flags.animalkilling.isGlobalyEnabled() && Utils.isAnimal(victim)) {
                    return FlagPermissions.has(victim.getLocation(), Flags.animalkilling, FlagCombo.OnlyFalse);

                } else if (Flags.mobkilling.isGlobalyEnabled() && Utils.isMonster(victim)) {
                    return FlagPermissions.has(victim.getLocation(), Flags.mobkilling, FlagCombo.OnlyFalse);

                }
                return false;
            });
        }
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
