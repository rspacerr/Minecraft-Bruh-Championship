package me.kotayka.mbc.commands;

import me.kotayka.mbc.MBC;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class gamenum implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return false;

        if (!(sender.isOp())) {
            sender.sendMessage(ChatColor.RED+"Admin restricted command!");
            return false;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED+"Usage: /gamenum [number (1-6)]");
        }

        int n;
        try {
            n = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED+"Could not parse provided amount " + args[0] + ".");
            return false;
        }

        if (!(n >= 1 && n <= MBC.MAX_TEAMS)) {
            sender.sendMessage(ChatColor.RED+"Invalid number! Use a number 1-6 inclusive!");
            return false;
        }

        MBC.getInstance().setGameNum(n);
        double newMult = switch (n) {
            case 2, 3 -> 1.5;
            case 4, 5 -> 2.0;
            case 6 -> 2.5;
            default -> 1.0;
        };
        MBC.getInstance().setMultiplier(newMult);
        sender.sendMessage(ChatColor.GREEN+"Successfully set game to " + n + ".");
        return true;
    }
}
