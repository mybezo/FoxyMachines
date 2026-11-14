package me.gallowsdove.foxymachines.commands;

import io.github.mooy1.infinitylib.commands.SubCommand;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import me.gallowsdove.foxymachines.FoxyMachines;
import me.gallowsdove.foxymachines.abstracts.CustomMob;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;

import javax.annotation.Nonnull;
import java.util.List;

public final class ChunkLoaderLimitCommand extends SubCommand {

    private static final NamespacedKey KEY = new NamespacedKey(FoxyMachines.getInstance(), "chunkloaders");

    public ChunkLoaderLimitCommand() {
        super("cllimit", "Set the number of Chunk Loaders a player has placed", "foxymachines.admin");
    }

    @Override
    protected void execute(@Nonnull CommandSender commandSender, @Nonnull String[] args) {
        if (args.length < 1 || args.length > 2) {
            commandSender.sendMessage(ChatColor.LIGHT_PURPLE + "Usage: /foxy cllimit [player] <amount>");
            return;
        }

        Player p = null;
        int limit = 0;
        if (args.length == 1) {
            try {
                limit = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                commandSender.sendMessage(ChatColor.LIGHT_PURPLE + "Invalid amount!");
                return;
            }

            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage(ChatColor.LIGHT_PURPLE + "You must specify a player when running this command from the console!");
                return;
            }

            p = (Player) commandSender;
        } else {
            try {
                limit = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                commandSender.sendMessage(ChatColor.LIGHT_PURPLE + "Invalid amount!");
                return;
            }
            p = FoxyMachines.getInstance().getServer().getPlayer(args[0]);
            if (p == null) {
                commandSender.sendMessage(ChatColor.LIGHT_PURPLE + "Player is not online!");
                return;
            }
        }

        if (limit < 0) {
            commandSender.sendMessage(ChatColor.LIGHT_PURPLE + "Amount can't be less than 0!");
            return;
        }

        Config cfg = new Config(FoxyMachines.getInstance());
        int max = cfg.getInt("max-chunk-loaders");
        if (max != 0 && limit > max) {
            commandSender.sendMessage(ChatColor.LIGHT_PURPLE + "Amount can't be greater than the limit (" + max + ")!");
            return;
        }

        PersistentDataAPI.setInt(p, KEY, limit);
        commandSender.sendMessage(ChatColor.LIGHT_PURPLE + "Set the placed Chunk Loader count of " + p.getName() + " to " + limit);
    }


    @Override
    protected void complete(@Nonnull CommandSender commandSender, @Nonnull String[] args, @Nonnull List<String> tabs) {
    }
}