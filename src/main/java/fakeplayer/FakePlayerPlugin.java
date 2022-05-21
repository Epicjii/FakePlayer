package fakeplayer;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        getCommand("bot").setExecutor(new FakePlayerSpawn());
        getCommand("remove").setExecutor(new FakePlayerRemove());
        getCommand("teleportbot").setExecutor(new FakePlayerSummon());

        try {
            Files.createDirectory(Paths.get("plugins/FakePlayers/data"));
        }catch (FileAlreadyExistsException e) {
            Bukkit.getLogger().fine("data already exists");

        } catch (IOException e) {
            Bukkit.getLogger().warning("Data Dir Creation Failed");
            e.printStackTrace();
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            File cache = new File("plugins/FakePlayers/data/fakeplayers");
            if (cache.exists()) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader("plugins/FakePlayers/data/fakeplayers"));

                    String line = reader.readLine();

                    while (line != null) {
                        FakePlayer.summon(line);

                        line = reader.readLine();
                    }

                    reader.close();

                } catch (IOException e) {
                    Bukkit.getLogger().warning("Failed to read from cache. Fake players from last server instance won't rejoin.");
                }
                cache.delete();
            }
        }, 100);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        List<FakePlayer> copyList = new ArrayList<>(FakePlayer.getFakePlayers());
        try {
            BufferedWriter myWriter = new BufferedWriter(new FileWriter("plugins/FakePlayers/data/fakeplayers"));

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
