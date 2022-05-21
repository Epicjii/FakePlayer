package fakeplayer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FakePlayerRemove implements CommandExecutor {
    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {

        if (args[0].equalsIgnoreCase("ALL")) {
            List<FakePlayer> copy = new ArrayList<>(FakePlayer.getFakePlayers());
            for (FakePlayer player : copy) {
                player.removePlayer();
            }
            return true;
        } else {
            FakePlayer fakePlayer = FakePlayer.getFakePlayer(args[0]);
            if (fakePlayer != null) {
                fakePlayer.removePlayer();
                return true;
            }
            return false;
        }
    }
}
