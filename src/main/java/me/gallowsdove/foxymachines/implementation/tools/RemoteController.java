package me.gallowsdove.foxymachines.implementation.tools;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.attributes.Rechargeable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import me.gallowsdove.foxymachines.Items;
import me.gallowsdove.foxymachines.implementation.machines.ForcefieldDome;
import me.gallowsdove.foxymachines.utils.SimpleLocation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import javax.annotation.Nonnull;
import java.util.UUID;

public class RemoteController extends SlimefunItem implements NotPlaceable, Rechargeable {
    private static final float COST = 50F;

    public RemoteController() {
        super(Items.TOOLS_ITEM_GROUP, Items.REMOTE_CONTROLLER, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                Items.DAMIENIUM, Items.WIRELESS_TRANSMITTER, Items.DAMIENIUM,
                Items.DAMIENIUM, Items.WIRELESS_TRANSMITTER, Items.DAMIENIUM,
                Items.DAMIENIUM, Items.WIRELESS_TRANSMITTER, Items.DAMIENIUM
        });
    }

    @Override
    public void preRegister() {
        addItemHandler(onUse());
    }

    @Nonnull
    protected ItemUseHandler onUse() {
        return e -> {
            ItemStack item = e.getItem();
            ItemStack itemInInventory = e.getPlayer().getInventory().getItemInMainHand();
            ItemMeta meta = itemInInventory.getItemMeta();
            PersistentDataContainer container = meta.getPersistentDataContainer();

            if (e.getPlayer().isSneaking()) {
                if (e.getClickedBlock().isPresent()) {
                    Block b = e.getClickedBlock().get();
                    if (StorageCacheUtils.getData(b.getLocation(), "owner") != null && StorageCacheUtils.getData(b.getLocation(), "active") != null) {

                        SimpleLocation loc = new SimpleLocation(b.getX(), b.getY(), b.getZ(), b.getWorld().getUID().toString(), "forcefield");

                        loc.storePersistently(container);
                        itemInInventory.setItemMeta(meta);
                        e.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "Forcefield Dome bound to the Remote Controller");
                    } else {
                        e.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "You must bind a Forcefield Dome");
                    }
                }
            } else {
                SimpleLocation loc = SimpleLocation.fromPersistentStorage(container, "forcefield");

                if (loc != null) {
                    World world = Bukkit.getWorld(UUID.fromString(loc.getWorldUUID()));

                    Block b = world.getBlockAt(loc.getX(), loc.getY(), loc.getZ());

                    if (StorageCacheUtils.getData(b.getLocation(), "owner") != null && StorageCacheUtils.getData(b.getLocation(), "active") != null) {
                        if (removeItemCharge(item, COST)) {
                            ForcefieldDome.INSTANCE.switchActive(b, e.getPlayer());
                        } else {
                            e.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "The Remote Controller needs to be charged");
                        }
                    } else {
                        e.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "Could not find the bound Forcefield Dome");
                    }
                } else {
                    e.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "Shift + Right Click to bind!");
                }
            }
        };
    }

    public float getMaxItemCharge(@Nonnull ItemStack item) {
        return 1000;
    }
}
