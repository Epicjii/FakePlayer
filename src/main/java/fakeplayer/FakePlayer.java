package fakeplayer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FakePlayer {
    private static final List<FakePlayer> fakePlayers = new ArrayList<>();
    private final UUID uuid;
    private final String name;
    private Object entityPlayer;


    public FakePlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public static FakePlayer getFakePlayer(String name) {
        for (FakePlayer player : fakePlayers) {
            if (player.getName().equals(name)) {
                return player;
            }
        }

        return null;
    }

    public static List<FakePlayer> getFakePlayers() {
        return fakePlayers;
    }

    public static boolean summon(String name) {
        return new FakePlayer(FakePlayerPlugin.getRandomUUID(name), name).spawn();
    }

    public void removePlayer() {
        Bot.removePlayer(this);
    }

    public UUID getUUID() {
        return uuid;
    }

    public Object getEntityPlayer() {
        return entityPlayer;
    }

    public String getName() {
        return name;
    }

    public boolean spawn() {

        if (name.length() >= 16) {
            return false;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().equals(name)) {
                return false;
            }
        }
        fakePlayers.add(this);

        entityPlayer = Bot.spawn(this);

        return true;
    }
}