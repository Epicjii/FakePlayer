package fakeplayer;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class FakePlayerSummon implements CommandExecutor {
    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {

        if (args.length == 1 && FakePlayer.getFakePlayer(args[0]) != null) {
            UUID botID = FakePlayer.getFakePlayer(args[0]).getUUID();
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "tp " + botID + " " + sender.getName());
            
            return true;
        }

        return false;
    }
}
