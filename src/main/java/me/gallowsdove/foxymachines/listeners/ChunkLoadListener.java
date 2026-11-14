package me.gallowsdove.foxymachines.listeners;

import me.gallowsdove.foxymachines.abstracts.CustomMob;
import me.gallowsdove.foxymachines.implementation.materials.GhostBlock;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;

import javax.annotation.Nonnull;

/**
 * Re-caches custom mobs and ghost blocks whenever their entities are loaded.
 * Entities are loaded separately from chunks (1.17+), and the tickers drop
 * unloaded entities from the caches, so this must run on every load.
 */
public class ChunkLoadListener implements Listener {
    @EventHandler
    public void onEntitiesLoad(@Nonnull EntitiesLoadEvent e) {
        for (Entity entity : e.getEntities()) {
            CustomMob customMob = CustomMob.getByEntity(entity);
            if (customMob != null) {
                customMob.cacheEntity(entity);
            }

            if (entity instanceof FallingBlock && GhostBlock.isGhostBlock(entity)) {
                GhostBlock.BLOCK_CACHE.add(entity.getUniqueId());
            }
        }
    }
}
