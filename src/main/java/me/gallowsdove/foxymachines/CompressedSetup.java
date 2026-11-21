package me.gallowsdove.foxymachines;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import me.gallowsdove.foxymachines.implementation.machines.ElectricGoldRefinery;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Registers the compressed machines (multiple machines combined into one), configured by compressed.yml.
 *
 * <p>Every machine has a compressed version for each multiplier (x8, x16, x32, x64). The first one is made of
 * 8 original machines in the Compressor, the others of 2 of the previous multiplier. Only the highest enabled
 * multiplier is listed in the guide, the lower ones are still craftable.</p>
 */
final class CompressedSetup {
    static final int[] MULTIPLIERS = { 8, 16, 32, 64 };

    private static final String FILE_NAME = "compressed.yml";
    // the energy numbers in the lore (J/s, J/t, J Capacity), they are multiplied
    private static final Pattern ENERGY_PATTERN = Pattern.compile("((?:§.)*)(\\d[\\d,]*)(?= J)");

    /**
     * The compressed versions of every machine, to add them to the research of their original machine.
     */
    static final List<SlimefunItemStack> ELECTRIC_GOLD_REFINERY = new ArrayList<>();

    private CompressedSetup() {}

    private record Entry(@Nonnull String section, @Nonnull SlimefunItemStack base, @Nonnull List<SlimefunItemStack> registered,
                         @Nonnull BiFunction<Args, SlimefunItem, SlimefunItem> factory) {}

    /**
     * The arguments of a compressed machine.
     */
    record Args(@Nonnull ItemGroup itemGroup, @Nonnull SlimefunItemStack item, @Nonnull ItemStack[] recipe, int multiplier) {}

    private static List<Entry> entries() {
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry("machines", Items.ELECTRIC_GOLD_REFINERY, ELECTRIC_GOLD_REFINERY, (args, base) ->
                new ElectricGoldRefinery(args.itemGroup(), args.item(), RecipeType.COMPRESSOR, args.recipe(), args.multiplier())));
        return entries;
    }

    static void setup() {
        FoxyMachines plugin = FoxyMachines.getInstance();
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
        // read the file itself, so the missing options are written into it
        Config cfg = new Config(file);
        List<Entry> entries = entries();

        // add the missing options, existing values are kept
        cfg.setDefaultValue("enabled", true);
        for (int multiplier : MULTIPLIERS) {
            cfg.setDefaultValue("multipliers.x" + multiplier, true);
        }
        for (Entry entry : entries) {
            cfg.setDefaultValue(entry.section() + "." + entry.base().getItemId(), true);
        }
        cfg.save();

        int count = 0;
        if (cfg.getBoolean("enabled")) {
            // a multiplier needs all the lower ones, their machines are in its recipe
            List<Integer> multipliers = new ArrayList<>();
            for (int multiplier : MULTIPLIERS) {
                if (!cfg.getBoolean("multipliers.x" + multiplier)) {
                    break;
                }
                multipliers.add(multiplier);
            }

            for (Entry entry : entries) {
                if (multipliers.isEmpty() || !cfg.getBoolean(entry.section() + "." + entry.base().getItemId())) {
                    continue;
                }
                SlimefunItem baseItem = SlimefunItem.getById(entry.base().getItemId());
                if (baseItem == null || baseItem.isDisabled()) {
                    continue;
                }
                count += register(entry, baseItem, multipliers);
            }
        }

        if (count == 0) {
            Items.MAIN_ITEM_GROUP.removeSubGroup(Items.COMPRESSED_ITEM_GROUP);
        } else {
            FoxyMachines.log(Level.INFO, "Registered " + count + " compressed machines.");
        }
    }

    private static int register(@Nonnull Entry entry, @Nonnull SlimefunItem baseItem, @Nonnull List<Integer> multipliers) {
        int count = 0;
        SlimefunItemStack previous = entry.base();
        int previousMultiplier = 1;
        int highest = multipliers.get(multipliers.size() - 1);

        for (int multiplier : multipliers) {
            SlimefunItemStack stack = createItemStack(entry.base(), multiplier);

            ItemStack ingredient = new SlimefunItemStack(previous, multiplier / previousMultiplier);
            ItemStack[] recipe = { ingredient, null, null, null, null, null, null, null, null };

            // only the highest multiplier is listed in the guide
            ItemGroup group = multiplier == highest ? Items.COMPRESSED_ITEM_GROUP : Items.HIDDEN_ITEM_GROUP;
            SlimefunItem item = entry.factory().apply(new Args(group, stack, recipe, multiplier), baseItem);
            item.register(FoxyMachines.getInstance());
            entry.registered().add(stack);
            count++;

            previous = stack;
            previousMultiplier = multiplier;
        }
        return count;
    }

    @Nonnull
    private static SlimefunItemStack createItemStack(@Nonnull SlimefunItemStack base, int multiplier) {
        ItemStack item = new ItemStack(base);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(meta.getDisplayName() + ChatColor.translateAlternateColorCodes('&', " &a(" + multiplier + ")"));

        List<String> lore = new ArrayList<>();
        if (meta.getLore() != null) {
            for (String line : meta.getLore()) {
                lore.add(scaleEnergy(line, multiplier));
            }
        }
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Combines &f" + multiplier + " &7machines into one."));
        meta.setLore(lore);
        item.setItemMeta(meta);

        return new SlimefunItemStack(base.getItemId() + "_X" + multiplier, item);
    }

    /**
     * Multiplies the energy numbers of a lore line.
     */
    @Nonnull
    private static String scaleEnergy(@Nonnull String line, int multiplier) {
        if (!line.contains(" J")) {
            return line;
        }
        Matcher matcher = ENERGY_PATTERN.matcher(line);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            long value;
            try {
                value = Long.parseLong(matcher.group(2).replace(",", ""));
            } catch (NumberFormatException e) {
                continue;
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(
                    matcher.group(1) + String.format(Locale.US, "%,d", value * multiplier)));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
