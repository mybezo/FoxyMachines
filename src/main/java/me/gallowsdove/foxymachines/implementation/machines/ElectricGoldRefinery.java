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
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.InvUtils;
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
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;


public class ElectricGoldRefinery extends SlimefunItem implements EnergyNetComponent, Compressible, RecipeDisplayItem {
    private static final int[] BORDER = { 2, 3, 4, 5, 6, 7, 8, 14, 23, 32, 41, 47, 48, 49, 50, 51, 52, 53 };
    private static final int[] BORDER_IN = { 11, 12, 13, 20, 22, 29, 31, 38 , 39, 40 };
    private static final int[] BORDER_OUT = { 15, 16, 17, 24, 26, 33, 35, 42, 43, 44 };
    private static final int[] GOLD_INDEXES = { 0, 9, 18, 27, 36, 45, 1, 10, 19, 28, 37, 46 };
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
                for (int i = 0; i < 12; i++) {
                    final int j = i;
                    menu.addMenuClickHandler(GOLD_INDEXES[j], (p, slot, item, action) -> {
                        StorageCacheUtils.setData(b.getLocation(), "gold_recipe", Integer.toString(j));
                        newInstance(menu, b);
                        return false;
                    });
                }

                if (!StorageCacheUtils.hasBlock(b.getLocation()) ||
                        StorageCacheUtils.getData(b.getLocation(), "gold_recipe") == null ||
                        StorageCacheUtils.getData(b.getLocation(), "gold_recipe").equals("11")) {
                    menu.replaceExistingItem(32, new CustomItemStack(Material.RED_STAINED_GLASS_PANE, "&6Current recipe: &cNone", "", "&e> Click the item on the left to change the recipe"));
                } else {
                    switch (StorageCacheUtils.getData(b.getLocation(), "gold_recipe")) {
                        case "0" -> menu.replaceExistingItem(32, new CustomItemStack(Material.GOLD_INGOT, "&6Current recipe: &fGold Ingot &7(4 Carat)", "", "&e> Click the item on the left to change the recipe"));
                        case "1" -> menu.replaceExistingItem(32, new CustomItemStack(Material.GOLD_INGOT, "&6Current recipe: &fGold Ingot &7(6 Carat)", "", "&e> Click the item on the left to change the recipe"));
                        case "2" -> menu.replaceExistingItem(32, new CustomItemStack(Material.GOLD_INGOT, "&6Current recipe: &fGold Ingot &7(8 Carat)", "", "&e> Click the item on the left to change the recipe"));
                        case "3" -> menu.replaceExistingItem(32, new CustomItemStack(Material.GOLD_INGOT, "&6Current recipe: &fGold Ingot &7(10 Carat)", "", "&e> Click the item on the left to change the recipe"));
                        case "4" -> menu.replaceExistingItem(32, new CustomItemStack(Material.GOLD_INGOT, "&6Current recipe: &fGold Ingot &7(12 Carat)", "", "&e> Click the item on the left to change the recipe"));
                        case "5" -> menu.replaceExistingItem(32, new CustomItemStack(Material.GOLD_INGOT, "&6Current recipe: &fGold Ingot &7(14 Carat)", "", "&e> Click the item on the left to change the recipe"));
                        case "6" -> menu.replaceExistingItem(32, new CustomItemStack(Material.GOLD_INGOT, "&6Current recipe: &fGold Ingot &7(16 Carat)", "", "&e> Click the item on the left to change the recipe"));
                        case "7" -> menu.replaceExistingItem(32, new CustomItemStack(Material.GOLD_INGOT, "&6Current recipe: &fGold Ingot &7(18 Carat)", "", "&e> Click the item on the left to change the recipe"));
                        case "8" -> menu.replaceExistingItem(32, new CustomItemStack(Material.GOLD_INGOT, "&6Current recipe: &fGold Ingot &7(20 Carat)", "", "&e> Click the item on the left to change the recipe"));
                        case "9" -> menu.replaceExistingItem(32, new CustomItemStack(Material.GOLD_INGOT, "&6Current recipe: &fGold Ingot &7(22 Carat)", "", "&e> Click the item on the left to change the recipe"));
                        case "10" -> menu.replaceExistingItem(32, new CustomItemStack(Material.GOLD_INGOT, "&6Current recipe: &fGold Ingot &7(24 Carat)", "", "&e> Click the item on the left to change the recipe"));
                        case "11" -> menu.replaceExistingItem(32, new CustomItemStack(Material.RED_STAINED_GLASS_PANE, "&6Current recipe: &cNone", "", "&e> Click the item on the left to change the recipe"));
                    }
                }
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
        return new int[] { 21, 30 };
    }

    public int[] getOutputSlots() {
        return new int[] {25, 34};
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
                ChestMenuUtils.updateProgressbar(inv, 23, timeleft, processing.get(b).getTicks(), getProgressBar());

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
                inv.replaceExistingItem(23, new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " "));

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
        if (g == null || Integer.parseInt(g) == 11) {
            return null;
        }

        int[] inputSlots = getInputSlots();
        ItemStack goldDust1 = menu.getItemInSlot(inputSlots[0]);
        ItemStack goldDust2 = menu.getItemInSlot(inputSlots[1]);
        List<ItemStack> goldDustList = new ArrayList<>();
        if (goldDust1 != null && SlimefunUtils.isItemSimilar(goldDust1, SlimefunItems.GOLD_DUST, true, false)) {
            goldDustList.add(goldDust1);
        } else {
            goldDustList.add(new SlimefunItemStack(SlimefunItems.GOLD_DUST, 0));
        }
        if (goldDust2 != null && SlimefunUtils.isItemSimilar(goldDust2, SlimefunItems.GOLD_DUST, true, false)) {
            goldDustList.add(goldDust2);
        } else {
            goldDustList.add(new SlimefunItemStack(SlimefunItems.GOLD_DUST, 0));
        }
        int goldDusts = 0;
        int goldTier = Integer.parseInt(g);

        for (ItemStack dust : goldDustList) {
            goldDusts = goldDusts + dust.getAmount();
        }



        int costPerIngot = goldTier + 1;
        // refine as many ingots as the gold dust allows, up to the multiplier
        int amount = Math.min(multiplier, goldDusts / costPerIngot);
        if (amount > 0) {
            ItemStack output = GOLDS[goldTier];

            // only refine what fits in the output, so nothing is lost
            while (amount > 0 && !fits(menu, output, amount)) {
                amount--;
            }
            if (amount == 0) {
                return null;
            }

            // the gold dust can come from both input slots
            int goldCost = costPerIngot * amount;
            for (int i = 0; i < goldDustList.size() && goldCost > 0; i++) {
                int available = goldDustList.get(i).getAmount();
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

    private boolean fits(@Nonnull BlockMenu menu, @Nonnull ItemStack output, int amount) {
        // checks all the stacks together, an output above the stack size is split into several stacks
        return InvUtils.fitAll(menu.toInventory(), Compressible.toStacks(output, amount), getOutputSlots());
    }

    protected void constructMenu(@Nonnull BlockMenuPreset preset) {
        for (int i : BORDER) {
            preset.addItem(i, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
        }

        for (int i : BORDER_IN) {
            preset.addItem(i, new SlimefunItemStack("_UI_INPUT_SLOT", Material.CYAN_STAINED_GLASS_PANE, " "), ChestMenuUtils.getEmptyClickHandler());
        }

        for (int i : BORDER_OUT) {
            preset.addItem(i, new SlimefunItemStack("_UI_OUTPUT_SLOT", Material.ORANGE_STAINED_GLASS_PANE, " "), ChestMenuUtils.getEmptyClickHandler());
        }

        preset.addItem(23, new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " "), ChestMenuUtils.getEmptyClickHandler());
        preset.addItem(46, new CustomItemStack(Material.RED_STAINED_GLASS_PANE, "&cNone"), ChestMenuUtils.getEmptyClickHandler());

        NamespacedKey key = new NamespacedKey(FoxyMachines.getInstance(), "nonstackable");

        for (int i = 0; i < GOLDS.length; i++) {
            ItemStack itemStack = GOLDS[i].clone();
            ItemMeta itemMeta = itemStack.getItemMeta();
            Byte value = 1;
            itemMeta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, value);
            itemStack.setItemMeta(itemMeta);
            preset.addItem(GOLD_INDEXES[i], itemStack, ChestMenuUtils.getEmptyClickHandler());
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