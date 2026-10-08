package com.bekvon.bukkit.residence.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Witch;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;

import net.Zrips.CMILib.ActionBar.CMIActionBar;
import net.Zrips.CMILib.Version.Version;

public class PotionUtils {

    public PotionUtils() {
    }

    // Start - 1.7.10 ~ 1.20.4
    private static List<String> getLegacyBeneficialList() {
        return Arrays.asList("ABSORPTION", "CONDUIT_POWER", "DAMAGE_RESISTANCE", "DOLPHINS_GRACE",
                "FAST_DIGGING", "FIRE_RESISTANCE", "HEAL", "HEALTH_BOOST", "HERO_OF_THE_VILLAGE",
                "INCREASE_DAMAGE", "INVISIBILITY", "JUMP", "LUCK", "NIGHT_VISION", "REGENERATION",
                "SATURATION", "SLOW_FALLING", "SPEED", "WATER_BREATHING");
    }

    private static List<String> getLegacyHarmfulList() {
        return Arrays.asList("BAD_OMEN", "BLINDNESS", "CONFUSION", "DARKNESS", "HARM", "HUNGER",
                "LEVITATION", "POISON", "SLOW", "SLOW_DIGGING", "UNLUCK", "WEAKNESS", "WITHER");
    }

    private static List<String> getLegacyNeutralList() {
        return Collections.singletonList("GLOWING");
    }
    // End - 1.7.10 ~ 1.20.4

    private static boolean isLegacyPotionEffectType(PotionEffectType potionEffectType, String effect) {

        String name = potionEffectType.getName();
        //
        System.out.println("PotionEffectType name = [" + name + "]");
        //
        if (effect.equalsIgnoreCase("BENEFICIAL")) {
            for (String string : getLegacyBeneficialList()){
                if (string.equalsIgnoreCase(name)) {
                    return true;
                }
            }

        } else if (effect.equalsIgnoreCase("HARMFUL")) {
            for (String string : getLegacyHarmfulList()){
                if (string.equalsIgnoreCase(name)) {
                    return true;
                }
            }

        } else if (effect.equalsIgnoreCase("NEUTRAL")) {
            for (String string : getLegacyNeutralList()){
                if (string.equalsIgnoreCase(name)) {
                    return true;
                }
            }

        } else if (effect.equalsIgnoreCase("Healing")) {
            return name.equalsIgnoreCase("HEAL");

        } else if (effect.equalsIgnoreCase("Damage")) {
            return name.equalsIgnoreCase("HARM");

        }
        return false;
    }

    public static boolean isPotionEffectType(PotionEffectType potionEffectType, String effect) {

        if (potionEffectType == null) {
            return false;
        }
        if (Version.isCurrentEqualOrHigher(Version.v1_20_5)) {
            if (effect.equalsIgnoreCase("BENEFICIAL")) {
                return potionEffectType.getEffectCategory() == PotionEffectType.Category.BENEFICIAL;
            }
            if (effect.equalsIgnoreCase("HARMFUL")) {
                return potionEffectType.getEffectCategory() == PotionEffectType.Category.HARMFUL;
            }
            if (effect.equalsIgnoreCase("NEUTRAL")) {
                return potionEffectType.getEffectCategory() == PotionEffectType.Category.NEUTRAL;
            }
            if (effect.equalsIgnoreCase("Healing")) {
                return potionEffectType == PotionEffectType.INSTANT_HEALTH;
            }
            if (effect.equalsIgnoreCase("Damage")) {
                return potionEffectType == PotionEffectType.INSTANT_DAMAGE;
            }
            return false;
        }
        return isLegacyPotionEffectType(potionEffectType, effect);
    }

    public static boolean shouldDenyHealingEffect(LivingEntity victim, ProjectileSource attacker, boolean isPlayerAttacker) {
        // healing potions damaging undead mobs
        if (isPlayerAttacker) {
            if (Utils.isUndead(victim)) {
                return FlagPermissions.shouldDenyAndNotify((Player) attacker, victim, Flags.mobkilling, null);
            }
        } else {
            if (Utils.isUndead(victim)) {
                return FlagPermissions.has(victim.getLocation(), Flags.mobkilling, FlagPermissions.FlagCombo.OnlyFalse);
            }
        }
        return false;
    }

    public static boolean shouldDenyDamageEffect(LivingEntity victim, ProjectileSource attacker, ClaimedResidence attackerRes, boolean isPlayerAttacker) {
        if (isPlayerAttacker) {
            Player player = (Player) attacker;
            if (Flags.pvp.isGlobalyEnabled() && victim instanceof Player) {
                if (attacker == victim) {
                    return false;
                }
                FlagPermissions attackerPerms = (attackerRes != null)
                        ? attackerRes.getPermissions()
                        : Residence.getInstance().getWorldFlags().getPerms(player.getWorld());
                if (attackerPerms.has(Flags.pvp, FlagPermissions.FlagCombo.OnlyFalse)) {
                    lm.Flag_Deny.sendMessage(player, Flags.pvp);
                    return true;
                }
                ClaimedResidence victimRes = ClaimedResidence.getByLoc(victim.getLocation());
                FlagPermissions victimPerms = victimRes != null
                        ? victimRes.getPermissions()
                        : Residence.getInstance().getWorldFlags().getPerms(victim.getWorld());
                if (victimPerms.has(Flags.pvp, FlagPermissions.FlagCombo.OnlyFalse)) {
                    lm.Flag_Deny.sendMessage(player, Flags.pvp);
                    return true;
                }
                if (attackerRes != null && attackerRes == victimRes
                        && attackerPerms.playerHas(player, Flags.friendlyfire, FlagPermissions.FlagCombo.OnlyFalse)
                        && attackerPerms.playerHas((Player) victim, Flags.friendlyfire, FlagPermissions.FlagCombo.OnlyFalse)) {
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
                if (attacker instanceof Witch || attacker instanceof DragonFireball) {
                    return false;
                }
                return FlagPermissions.has(victim.getLocation(), Flags.pvp, FlagPermissions.FlagCombo.OnlyFalse);

            } else if (Flags.animalkilling.isGlobalyEnabled() && Utils.isAnimal(victim)) {
                return FlagPermissions.has(victim.getLocation(), Flags.animalkilling, FlagPermissions.FlagCombo.OnlyFalse);

            } else if (Utils.isUndead(victim)) {
                // Damage cloud is not harmful to undead
                return false;

            } else if (Flags.mobkilling.isGlobalyEnabled() && Utils.isMonster(victim)) {
                return FlagPermissions.has(victim.getLocation(), Flags.mobkilling, FlagPermissions.FlagCombo.OnlyFalse);

            }
        }
        return false;
    }

    public static boolean shouldDenyHarmfulEffect(LivingEntity victim, ProjectileSource attacker, ClaimedResidence attackerRes, boolean isPlayerAttacker) {
        if (isPlayerAttacker) {
            Player player = (Player) attacker;
            if (Flags.pvp.isGlobalyEnabled() && victim instanceof Player) {
                if (attacker == victim) {
                    return false;
                }
                FlagPermissions attackerPerms = (attackerRes != null)
                        ? attackerRes.getPermissions()
                        : Residence.getInstance().getWorldFlags().getPerms(player.getWorld());
                if (attackerPerms.has(Flags.pvp, FlagPermissions.FlagCombo.OnlyFalse)) {
                    lm.Flag_Deny.sendMessage(player, Flags.pvp);
                    return true;
                }
                ClaimedResidence victimRes = ClaimedResidence.getByLoc(victim.getLocation());
                FlagPermissions victimPerms = victimRes != null
                        ? victimRes.getPermissions()
                        : Residence.getInstance().getWorldFlags().getPerms(victim.getWorld());
                if (victimPerms.has(Flags.pvp, FlagPermissions.FlagCombo.OnlyFalse)) {
                    lm.Flag_Deny.sendMessage(player, Flags.pvp);
                    return true;
                }
                if (attackerRes != null && attackerRes == victimRes
                        && attackerPerms.playerHas(player, Flags.friendlyfire, FlagPermissions.FlagCombo.OnlyFalse)
                        && attackerPerms.playerHas((Player) victim, Flags.friendlyfire, FlagPermissions.FlagCombo.OnlyFalse)) {
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
                return FlagPermissions.has(victim.getLocation(), Flags.pvp, FlagPermissions.FlagCombo.OnlyFalse);

            } else if (Flags.animalkilling.isGlobalyEnabled() && Utils.isAnimal(victim)) {
                return FlagPermissions.has(victim.getLocation(), Flags.animalkilling, FlagPermissions.FlagCombo.OnlyFalse);

            } else if (Flags.mobkilling.isGlobalyEnabled() && Utils.isMonster(victim)) {
                return FlagPermissions.has(victim.getLocation(), Flags.mobkilling, FlagPermissions.FlagCombo.OnlyFalse);

            }
        }
        return false;
    }
}
