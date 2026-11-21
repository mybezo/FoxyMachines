package me.gallowsdove.foxymachines.implementation.machines;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.ItemUtils;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.gallowsdove.foxymachines.FoxyMachines;
import me.gallowsdove.foxymachines.Items;
import me.gallowsdove.foxymachines.utils.Compressible;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu.AdvancedMenuClickHandler;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;


public class ElectricGoldRefinery extends SlimefunItem implements EnergyNetComponent, Compressible, RecipeDisplayItem {
    // layout: row 1 = recipe button (slot 4), rows 3-4 = input, row 6 = output
    private static final int RECIPE_SLOT = 4;
    private static final int[] BACKGROUND = { 0, 1, 2, 3, 5, 6, 7, 8 };
    private static final int[] BORDER_IN = { 9, 10, 11, 12, 14, 15, 16, 17, 18, 26, 27, 35 };
    private static final int[] BORDER_OUT = { 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 53 };
    private static final int[] INPUT_SLOTS = { 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34 };
    private static final int[] OUTPUT_SLOTS = { 46, 47, 48, 49, 50, 51, 52 };
    private static final int PROGRESS_SLOT = 13;
    // the selectable recipes are the golds, then "None" (stops the machine)
    private static final int NONE_RECIPE = 11;
    private static final int RECIPE_COUNT = 12;
    private static final ItemStack[] GOLDS = { SlimefunItems.GOLD_4K, SlimefunItems.GOLD_6K, SlimefunItems.GOLD_8K, SlimefunItems.GOLD_10K, SlimefunItems.GOLD_12K, SlimefunItems.GOLD_14K, SlimefunItems.GOLD_16K, SlimefunItems.GOLD_18K, SlimefunItems.GOLD_20K, SlimefunItems.GOLD_22K, SlimefunItems.GOLD_24K };
    public static final int ENERGY_CONSUMPTION = 36;
    public static final int CAPACITY = 512;

    public static Map<Block, MachineRecipe> processing = new HashMap<>();
    public static Map<Block, Integer> progress = new HashMap<>();
    // the amount of refinings in the current operation (1 for the normal machine)
    public static Map<Block, Integer> units = new HashMap<>();

    private final int multiplier;

    public ElectricGoldRefinery() {
        this(Items.MACHINES_ITEM_GROUP, Items.ELECTRIC_GOLD_REFINERY, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                SlimefunItems.HARDENED_METAL_INGOT, SlimefunItems.ELECTRIC_MOTOR, SlimefunItems.HARDENED_METAL_INGOT,
                SlimefunItems.HEATING_COIL, SlimefunItems.ELECTRIC_SMELTERY, SlimefunItems.HEATING_COIL,
                SlimefunItems.HARDENED_METAL_INGOT, SlimefunItems.MEDIUM_CAPACITOR, SlimefunItems.HARDENED_METAL_INGOT
        }, 1);
    }

    /**
     * @param multiplier the amount of machines combined into this one (1 for the normal machine).
     *                   Every operation refines up to this many ingots at the same speed.
     */
    public ElectricGoldRefinery(@Nonnull ItemGroup itemGroup, @Nonnull SlimefunItemStack item, @Nonnull RecipeType recipeType,
                                @Nonnull ItemStack[] recipe, int multiplier) {
        super(itemGroup, item, recipeType, recipe);
        this.multiplier = Math.max(1, multiplier);

        String title = this.multiplier > 1 ? "&6Electric Gold Refinery &a(" + this.multiplier + ")" : "&6Electric Gold Refinery";

        new BlockMenuPreset(getId(), title) {

            @Override
            public void init() {
                constructMenu(this);
            }

            @Override
            public void newInstance(@Nonnull BlockMenu menu, @Nonnull Block b) {
                showRecipeButton(menu, b);
            }

            public boolean canOpen(@Nonnull Block b, @Nonnull Player p) {
                return p.hasPermission("slimefun.inventory.bypass")
                        || Slimefun.getProtectionManager().hasPermission(p, b.getLocation(),
                        Interaction.INTERACT_BLOCK
                );
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(@Nonnull ItemTransportFlow flow) {
                return new int[0];
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(@Nonnull DirtyChestMenu menu, @Nonnull ItemTransportFlow flow, @Nonnull ItemStack item) {
                if (flow == ItemTransportFlow.WITHDRAW) {
                    return getOutputSlots();
                }

                List<Integer> slots = new LinkedList<>();

                for (int slot : getInputSlots()) {
                    ItemStack stack = menu.getItemInSlot(slot);
                    if (stack != null) {
                        if (SlimefunUtils.isItemSimilar(stack, item, true, false)) {
                            if (stack.getAmount() < stack.getMaxStackSize()) {
                                slots.add(slot);
                            }
                        }
                    } else {
                        slots.add(slot);
                    }
                }

                if (slots.isEmpty()) {
                    return getInputSlots();
                } else {
                    int[] array = new int[slots.size()];

                    for (int i = 0; i < slots.size(); i++) {
                        array[i] = slots.get(i);
                    }

                    return array;
                }
            }
        };
    }

    public int[] getInputSlots() {
        return INPUT_SLOTS;
    }

    public int[] getOutputSlots() {
        return OUTPUT_SLOTS;
    }

    /**
     * The selected recipe: the index of the gold (4K to 24K), or {@link #NONE_RECIPE}.
     */
    private static int getSelectedRecipe(@Nonnull Block b) {
        String value = StorageCacheUtils.getData(b.getLocation(), "gold_recipe");
        if (value == null) {
            return NONE_RECIPE;
        }
        try {
            int recipe = Integer.parseInt(value);
            return recipe >= 0 && recipe < RECIPE_COUNT ? recipe : NONE_RECIPE;
        } catch (NumberFormatException e) {
            return NONE_RECIPE;
        }
    }

    /**
     * The button that switches the recipe: left click = next, right click = previous.
     */
    private void showRecipeButton(@Nonnull BlockMenu menu, @Nonnull Block b) {
        menu.replaceExistingItem(RECIPE_SLOT, getRecipeButton(getSelectedRecipe(b)));
        menu.addMenuClickHandler(RECIPE_SLOT, (p, slot, item, action) -> {
            int step = action.isRightClicked() ? RECIPE_COUNT - 1 : 1;
            int next = (getSelectedRecipe(b) + step) % RECIPE_COUNT;
            StorageCacheUtils.setData(b.getLocation(), "gold_recipe", Integer.toString(next));
            showRecipeButton(menu, b);
            return false;
        });
    }

    @Nonnull
    private static String getRecipeName(int recipe) {
        return recipe == NONE_RECIPE ? "None" : "Gold " + (4 + recipe * 2) + " Carat";
    }

    /**
     * Lists all the recipes, the selected one in green and the others in red.
     */
    @Nonnull
    private static ItemStack getRecipeButton(int selected) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (int recipe = 0; recipe < RECIPE_COUNT; recipe++) {
            lore.add(recipe == selected ? "&a\u25B6 " + getRecipeName(recipe) : "&c" + getRecipeName(recipe));
        }
        lore.add("");
        lore.add("&eLeft click &7for the next recipe");
        lore.add("&eRight click &7for the previous recipe");

        Material material = selected == NONE_RECIPE ? Material.BARRIER : Material.GOLD_INGOT;
        return new CustomItemStack(material, "&6Recipe: " + (selected == NONE_RECIPE ? "&c" : "&a") + getRecipeName(selected),
                lore.toArray(new String[0]));
    }

    @Nonnull
    @Override
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    @Override
    public int getCapacity() {
        return Compressible.scale(CAPACITY, multiplier, Integer.MAX_VALUE);
    }

    /**
     * The energy per tick of one refining, the energy of an operation is this multiplied by its units.
     */
    public int getEnergyConsumption() {
        return ENERGY_CONSUMPTION;
    }

    @Override
    public int getMultiplier() {
        return multiplier;
    }

    @Nonnull
    @Override
    public List<ItemStack> getDisplayRecipes() {
        if (multiplier <= 1) {
            return new ArrayList<>();
        }
        List<ItemStack> list = new ArrayList<>();
        list.add(Compressible.multiplierInfo(multiplier));
        return list;
    }

    public ItemStack getProgressBar() {
        return new ItemStack(Material.GOLDEN_CHESTPLATE);
    }

    public MachineRecipe getProcessing(Block b) {
        return processing.get(b);
    }

    public boolean isProcessing(Block b) {
        return getProcessing(b) != null;
    }

    private BlockBreakHandler onBreak() {
        return new BlockBreakHandler(false, false) {
            @Override
            public void onPlayerBreak(@Nonnull BlockBreakEvent e, @Nonnull ItemStack item, @Nonnull List<ItemStack> drops) {
                Block b = e.getBlock();
                BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());

                if (inv != null) {
                    inv.dropItems(b.getLocation(), getOutputSlots());
                    inv.dropItems(b.getLocation(), getInputSlots());
                }

                processing.remove(b);
                progress.remove(b);
                units.remove(b);
            }
        };
    }

    @Override
    public void preRegister() {
        addItemHandler(new BlockTicker() {

            @Override
            public void tick(@Nonnull Block b, @Nonnull SlimefunItem sf, @Nonnull Config data) {
                ElectricGoldRefinery.this.tick(b);
            }

            @Override
            public boolean isSynchronized() {
                return false;
            }
        }, onBreak());
    }

    protected void tick(@Nonnull Block b) {
        BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());

        if (isProcessing(b)) {
            int timeleft = progress.get(b);

            if (timeleft > 0) {
                ChestMenuUtils.updateProgressbar(inv, PROGRESS_SLOT, timeleft, processing.get(b).getTicks(), getProgressBar());

                if (isChargeable()) {
                    // only the refinings in this operation use energy
                    int energy = Compressible.scale(getEnergyConsumption(), units.getOrDefault(b, 1), Integer.MAX_VALUE);
                    if (getCharge(b.getLocation()) < energy) {
                        return;
                    }

                    removeCharge(b.getLocation(), energy);
                }
                progress.put(b, timeleft - 1);
            }
            else {
                inv.replaceExistingItem(PROGRESS_SLOT, getInputBorder());

                for (ItemStack output : processing.get(b).getOutput()) {
                    inv.pushItem(output.clone(), getOutputSlots());
                }

                progress.remove(b);
                processing.remove(b);
                units.remove(b);
            }
        }
        else {
            MachineRecipe next = findNextRecipe(inv, StorageCacheUtils.getData(b.getLocation(), "gold_recipe"));

            if (next != null) {
                processing.put(b, next);
                progress.put(b, next.getTicks());
                // the output holds one ingot per refining
                int amount = 0;
                for (ItemStack output : next.getOutput()) {
                    amount += output.getAmount();
                }
                units.put(b, Math.max(1, amount));
            }
        }
    }

    @Nullable
    protected MachineRecipe findNextRecipe(@Nonnull BlockMenu menu, @Nullable String g) {
        if (g == null) {
            return null;
        }
        int goldTier;
        try {
            goldTier = Integer.parseInt(g);
        } catch (NumberFormatException e) {
            return null;
        }
        if (goldTier < 0 || goldTier >= GOLDS.length) {
            return null;
        }

        // the gold dust of every input slot
        int[] inputSlots = getInputSlots();
        int[] dustPerSlot = new int[inputSlots.length];
        int goldDusts = 0;
        for (int i = 0; i < inputSlots.length; i++) {
            ItemStack item = menu.getItemInSlot(inputSlots[i]);
            if (item != null && SlimefunUtils.isItemSimilar(item, SlimefunItems.GOLD_DUST, true, false)) {
                dustPerSlot[i] = item.getAmount();
                goldDusts += item.getAmount();
            }
        }

        int costPerIngot = goldTier + 1;
        // refine as many ingots as the gold dust allows, up to the multiplier
        int amount = Math.min(multiplier, goldDusts / costPerIngot);
        if (amount > 0) {
            ItemStack output = GOLDS[goldTier];

            // only refine what fits in the output, so nothing is lost
            amount = Math.min(amount, getFreeSpace(menu, output));
            if (amount <= 0) {
                return null;
            }

            // the gold dust can come from several input slots
            int goldCost = costPerIngot * amount;
            for (int i = 0; i < inputSlots.length && goldCost > 0; i++) {
                int available = dustPerSlot[i];
                if (available == 0) {
                    continue;
                }
                int consumed = Math.min(available, goldCost);
                menu.consumeItem(inputSlots[i], consumed);
                goldCost -= consumed;
            }

            return new MachineRecipe((int) (3 + 0.5*goldTier), new ItemStack[] { new SlimefunItemStack(SlimefunItems.GOLD_DUST, goldTier) },
                    Compressible.toStacks(output, amount));
        }

        return null;
    }

    /**
     * How many of the item fit in the output slots, over all the slots together.
     */
    private int getFreeSpace(@Nonnull BlockMenu menu, @Nonnull ItemStack output) {
        int maxStack = output.getMaxStackSize();
        int free = 0;
        for (int slot : getOutputSlots()) {
            ItemStack stack = menu.getItemInSlot(slot);
            if (stack == null || stack.getType().isAir()) {
                free += maxStack;
            } else if (ItemUtils.canStack(stack, output)) {
                free += Math.max(0, maxStack - stack.getAmount());
            }
        }
        return free;
    }

    @Nonnull
    private static ItemStack getInputBorder() {
        return new CustomItemStack(Material.CYAN_STAINED_GLASS_PANE, " ");
    }

    protected void constructMenu(@Nonnull BlockMenuPreset preset) {
        for (int i : BACKGROUND) {
            preset.addItem(i, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
        }
        // the recipe button is filled when the menu is opened
        preset.addItem(RECIPE_SLOT, getRecipeButton(NONE_RECIPE), ChestMenuUtils.getEmptyClickHandler());

        for (int i : BORDER_IN) {
            preset.addItem(i, getInputBorder(), ChestMenuUtils.getEmptyClickHandler());
        }
        preset.addItem(PROGRESS_SLOT, getInputBorder(), ChestMenuUtils.getEmptyClickHandler());

        for (int i : BORDER_OUT) {
            preset.addItem(i, new CustomItemStack(Material.ORANGE_STAINED_GLASS_PANE, " "), ChestMenuUtils.getEmptyClickHandler());
        }

        for (int i : getOutputSlots()) {
            preset.addMenuClickHandler(i, new AdvancedMenuClickHandler() {

                @Override
                public boolean onClick(Player p, int slot, ItemStack cursor, ClickAction action) {
                    return false;
                }

                @Override
                public boolean onClick(InventoryClickEvent e, Player p, int slot, ItemStack cursor, ClickAction action) {
                    return cursor == null || cursor.getType() == Material.AIR;
                }
            });
        }
    }
}