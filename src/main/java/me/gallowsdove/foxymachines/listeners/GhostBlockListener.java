package me.gallowsdove.foxymachines.listeners;

import me.gallowsdove.foxymachines.implementation.materials.GhostBlock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import me.gallowsdove.foxymachines.FoxyMachines;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class GhostBlockListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onHitByFishingRod(PlayerFishEvent e) {
        if (e.getCaught() instanceof FallingBlock b &&
                b.getPersistentDataContainer().has(GhostBlock.KEY, PersistentDataType.STRING)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onEntityExplode(EntityExplodeEvent e) {
        // Most servers have no ghost blocks loaded, skip the entity scan entirely
        if (GhostBlock.BLOCK_CACHE.isEmpty()) {
            return;
        }

        Map<UUID, Location> preExplosionLocations = new HashMap<>();
        for (Entity ent : e.getLocation().getWorld().getNearbyEntities(e.getLocation(), 6, 6, 6,
                ent -> ent instanceof FallingBlock && GhostBlock.BLOCK_CACHE.contains(ent.getUniqueId()))) {
            preExplosionLocations.put(ent.getUniqueId(), ent.getLocation());
        }

        if (preExplosionLocations.isEmpty()) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(FoxyMachines.getInstance(), () -> {
            for (Map.Entry<UUID, Location> entry : preExplosionLocations.entrySet()) {
                Entity ent = Bukkit.getEntity(entry.getKey());
                if (ent instanceof FallingBlock fb && GhostBlock.isGhostBlock(fb)) {
                    fb.teleport(entry.getValue());
                    fb.setVelocity(new Vector(0, 0, 0));
                    fb.setGravity(false);
                }
            }
        }, 1L);
    }
}
