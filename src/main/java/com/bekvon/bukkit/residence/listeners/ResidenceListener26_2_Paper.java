package com.bekvon.bukkit.residence.listeners;

import java.util.List;

import com.bekvon.bukkit.residence.containers.ResAdmin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.lm;
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
        Entity other;
        if (entity1 instanceof Player) {
            player = (Player) entity1;
            other = entity2;
        } else if (entity2 instanceof Player) {
            player = (Player) entity2;
            other = entity1;
        } else {
            // Only handle collisions involving a player
            return;
        }
        if (player.hasMetadata("NPC") || ResAdmin.isResAdmin(player)) {
            return;
        }
        FlagPermissions perms = FlagPermissions.getPerms(other.getLocation(), player);
        boolean shouldDenyCollision;

        if (Utils.isAnimal(other)) {
            shouldDenyCollision = !perms.playerHas(player, Flags.playercollision, perms.playerHas(player, Flags.animalkilling, true));

        } else if (ResidenceEntityListener.isMonster(other)) {
            shouldDenyCollision = !perms.playerHas(player, Flags.playercollision, perms.playerHas(player, Flags.mobkilling, true));

        } else {
            shouldDenyCollision = !perms.playerHas(player, Flags.playercollision, true);

        }
        if (shouldDenyCollision) {
            lm.Flag_Deny.sendMessage(player, Flags.playercollision);
            event.setCancelled(true);
        }
    }
}
