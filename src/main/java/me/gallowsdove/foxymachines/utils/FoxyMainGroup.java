package me.gallowsdove.foxymachines.utils;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.NestedItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.SubItemGroup;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.GuideHistory;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.guide.SurvivalSlimefunGuide;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

/**
 * A guide group that lists its sub groups like Slimefun's {@link NestedItemGroup},
 * and can show one extra button at a fixed slot (used for the "Compressed Items" menu).
 */
public class FoxyMainGroup extends NestedItemGroup {
    // the sub groups are listed in the slots 9 to 44, the page buttons are in the last row
    private static final int FIRST_SLOT = 9;
    private static final int LAST_SLOT = 44;
    private static final int PREVIOUS_SLOT = 46;
    private static final int NEXT_SLOT = 52;

    private final List<SubItemGroup> subGroups = new ArrayList<>();
    private int fixedSlot = -1;
    private ItemGroup fixedGroup;

    @ParametersAreNonnullByDefault
    public FoxyMainGroup(NamespacedKey key, ItemStack item) {
        super(key, item);
    }

    /**
     * Shows a button at a fixed slot that opens the group, null removes it.
     */
    public void setFixedGroup(int slot, @Nullable ItemGroup group) {
        this.fixedSlot = slot;
        this.fixedGroup = group;
    }

    @Override
    public void addSubGroup(@Nonnull SubItemGroup group) {
        super.addSubGroup(group);
        if (!subGroups.contains(group)) {
            subGroups.add(group);
        }
    }

    @Override
    public void removeSubGroup(@Nonnull SubItemGroup group) {
        super.removeSubGroup(group);
        subGroups.remove(group);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void open(Player p, PlayerProfile profile, SlimefunGuideMode mode) {
        openPage(p, profile, mode, 1);
    }

    private boolean hasFixedSlotInList() {
        return fixedGroup != null && fixedSlot >= FIRST_SLOT && fixedSlot <= LAST_SLOT;
    }

    @ParametersAreNonnullByDefault
    private void openPage(Player p, PlayerProfile profile, SlimefunGuideMode mode, int page) {
        GuideHistory history = profile.getGuideHistory();
        if (mode == SlimefunGuideMode.SURVIVAL_MODE) {
            history.add(this, page);
        }

        ChestMenu menu = new ChestMenu(Slimefun.getLocalization().getMessage(p, "guide.title.main"));
        SurvivalSlimefunGuide guide = (SurvivalSlimefunGuide) Slimefun.getRegistry().getSlimefunGuide(mode);

        menu.setEmptySlotsClickable(false);
        menu.addMenuOpeningHandler(pl -> pl.playSound(pl.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1));
        guide.createHeader(p, profile, menu);

        menu.addItem(1, ChestMenuUtils.getBackButton(p, "", ChatColor.GRAY + Slimefun.getLocalization().getMessage(p, "guide.back.guide")));
        menu.addMenuClickHandler(1, (pl, s, is, action) -> {
            if (mode == SlimefunGuideMode.SURVIVAL_MODE) {
                history.goBack(guide);
            } else {
                SlimefunGuide.openMainMenu(profile, mode, history.getMainMenuPage());
            }
            return false;
        });

        List<SubItemGroup> visible = new ArrayList<>();
        for (SubItemGroup group : subGroups) {
            if (group.isVisibleInNested(p)) {
                visible.add(group);
            }
        }

        int perPage = LAST_SLOT - FIRST_SLOT + 1 - (hasFixedSlotInList() ? 1 : 0);
        int pages = Math.max(1, (visible.size() + perPage - 1) / perPage);
        int index = (page - 1) * perPage;
        for (int slot = FIRST_SLOT; slot <= LAST_SLOT && index < visible.size(); slot++) {
            if (fixedGroup != null && slot == fixedSlot) {
                continue;
            }
            SubItemGroup group = visible.get(index++);
            menu.addItem(slot, group.getItem(p));
            menu.addMenuClickHandler(slot, (pl, s, item, action) -> {
                SlimefunGuide.openItemGroup(profile, group, mode, 1);
                return false;
            });
        }

        if (fixedGroup != null) {
            ItemGroup group = fixedGroup;
            menu.addItem(fixedSlot, group.getItem(p));
            menu.addMenuClickHandler(fixedSlot, (pl, s, item, action) -> {
                SlimefunGuide.openItemGroup(profile, group, mode, 1);
                return false;
            });
        }

        if (pages > 1) {
            menu.addItem(PREVIOUS_SLOT, ChestMenuUtils.getPreviousButton(p, page, pages));
            menu.addMenuClickHandler(PREVIOUS_SLOT, (pl, s, item, action) -> {
                if (page > 1) {
                    openPage(p, profile, mode, page - 1);
                }
                return false;
            });

            menu.addItem(NEXT_SLOT, ChestMenuUtils.getNextButton(p, page, pages));
            menu.addMenuClickHandler(NEXT_SLOT, (pl, s, item, action) -> {
                if (page < pages) {
                    openPage(p, profile, mode, page + 1);
                }
                return false;
            });
        }

        menu.open(p);
    }
}
