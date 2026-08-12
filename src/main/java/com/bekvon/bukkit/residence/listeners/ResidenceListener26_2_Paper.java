package com.bekvon.bukkit.residence.listeners;

import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.ResAdmin;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.listenersCache.PlayerCollideWithEntityCache;
import com.bekvon.bukkit.residence.listenersCache.PlayerCollisionDenyMessageCache;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.utils.Utils;

import io.papermc.paper.event.entity.EntityCollideWithEntityEvent;

public class ResidenceListener26_2_Paper implements Listener {

    private Residence plugin;

    public ResidenceListener26_2_Paper(Residence plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCollideWithEntity(EntityCollideWithEntityEvent event) {
        // Disabling listener if flag disabled globally
        if (!Flags.playercollision.isGlobalyEnabled()) {
            return;
        }
        // Get the two entities involved in the collision
        List<Entity> entities = event.getEntities();

        Entity entity1 = entities.get(0);
        // disabling event on world
        if (plugin.isDisabledWorldListener(entity1.getWorld())) {
            return;
        }
        Entity entity2 = entities.get(1);
        Player player;
        Entity entity;
        if (entity1 instanceof Player) {
            player = (Player) entity1;
            entity = entity2;
        } else if (entity2 instanceof Player) {
            player = (Player) entity2;
            entity = entity1;
        } else {
            // Only handle collisions involving a player
            return;
        }
        PlayerCollideWithEntityCache.PlayerCollideWithEntityKey key
                = new PlayerCollideWithEntityCache.PlayerCollideWithEntityKey(player, entity);

        if (PlayerCollideWithEntityCache.getOrCompute(key, () -> shouldDenyCollision(player, entity))) {
            if (PlayerCollisionDenyMessageCache.shouldSendDenyMessage(player)) {
                lm.Flag_Deny.sendMessage(player, Flags.playercollision);
            }
            event.setCancelled(true);
        }
    }

    private boolean shouldDenyCollision(Player player, Entity entity) {
        if (player.hasMetadata("NPC") || ResAdmin.isResAdmin(player)) {
            return false;
        }
        FlagPermissions perms = FlagPermissions.getPerms(entity.getLocation(), player);

        if (Utils.isAnimal(entity)) {
            return !perms.playerHas(player, Flags.playercollision, perms.playerHas(player, Flags.animalkilling, true));

        } else if (ResidenceEntityListener.isMonster(entity)) {
            return !perms.playerHas(player, Flags.playercollision, perms.playerHas(player, Flags.mobkilling, true));

        } else {
            return !perms.playerHas(player, Flags.playercollision, true);

        }
    }
}
