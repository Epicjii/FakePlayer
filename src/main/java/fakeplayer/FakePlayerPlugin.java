package fakeplayer;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FakePlayerPlugin extends JavaPlugin {

    private static FakePlayerPlugin plugin;

    public static UUID getRandomUUID(String name) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);

        return offlinePlayer.getUniqueId();
    }

    public static Plugin getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic

        plugin = this;
        getCommand("bot").setExecutor(new FakePlayerSummon());
        getCommand("remove").setExecutor(new FakePlayerRemove());

        File cacheFolder = new File("plugins/FakePlayers/cache");
        if (!cacheFolder.exists()) {
            cacheFolder.mkdir();
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        List<FakePlayer> copyList = new ArrayList<>(FakePlayer.getFakePlayers());
        try {
            BufferedWriter myWriter = new BufferedWriter(new FileWriter("plugins/FakePlayers/cache/cache$1.fpcache"));

            for (FakePlayer player : copyList) {
                myWriter.write(player.getName() + "\n");

                player.removePlayer();
            }

            myWriter.close();
        } catch (IOException e) {
            Bukkit.getLogger().warning("Failed to cache fake players who are currently online. They will not rejoin your server.");
        }

    }

}
