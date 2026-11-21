package me.gallowsdove.foxymachines.utils;

import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * A machine that can have compressed versions (multiple machines combined into one).
 */
public interface Compressible {

    /**
     * The amount of machines combined into this one, 1 for the normal machine.
     */
    int getMultiplier();

    /**
     * Multiplies the value, calculated with a long and capped at max so it never overflows.
     */
    static int scale(int value, int multiplier, int max) {
        return (int) Math.min((long) value * multiplier, max);
    }

    /**
     * Splits the amount of the item into valid stacks (at most the max stack size each).
     */
    @Nonnull
    static ItemStack[] toStacks(@Nonnull ItemStack item, int amount) {
        int maxStack = Math.max(1, item.getMaxStackSize());
        List<ItemStack> stacks = new ArrayList<>();
        while (amount > 0) {
            ItemStack stack = item.clone();
            int stackAmount = Math.min(amount, maxStack);
            stack.setAmount(stackAmount);
            stacks.add(stack);
            amount -= stackAmount;
        }
        return stacks.toArray(new ItemStack[0]);
    }

    /**
     * The "Multiplier: xN" info shown on the guide page of a compressed machine.
     */
    @Nonnull
    static ItemStack multiplierInfo(int multiplier) {
        return new CustomItemStack(Material.PISTON, "&aMultiplier: &fx" + multiplier,
                "", "&7Combines &f" + multiplier + " &7machines into one");
    }
}
