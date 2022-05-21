package fakeplayer;


import com.destroystokyo.paper.PaperConfig;
import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.GameType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R2.CraftServer;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.*;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.UUID;

public class Bot {
    public static ServerPlayer spawn(FakePlayer fakePlayer) {
        ServerLevel serverLevel = ((CraftWorld) Bukkit.getWorlds().get(0)).getHandle();

        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();

        ServerPlayer entityPlayer = createEntityPlayer(fakePlayer.getUUID(), fakePlayer.getName(), serverLevel);

        CraftPlayer bukkitPlayer = entityPlayer.getBukkitEntity();

        entityPlayer.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.CLIENTBOUND), entityPlayer);

        entityPlayer.connection.connection.channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
        entityPlayer.connection.connection.channel.close();

        AsyncPlayerPreLoginEvent asyncPreLoginEvent = new AsyncPlayerPreLoginEvent(fakePlayer.getName(), InetAddress.getLoopbackAddress(), fakePlayer.getUUID());

        new Thread(() -> Bukkit.getPluginManager().callEvent(asyncPreLoginEvent)).start();

        server.getPlayerList().load(entityPlayer);

        Location loc = bukkitPlayer.getLocation();

        entityPlayer.absMoveTo(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

        TranslatableComponent joinMessage = getJoinMessage(entityPlayer);

        playerInitialSpawnEvent(bukkitPlayer);

        entityPlayer.spawnIn(serverLevel);
        entityPlayer.gameMode.setLevel(entityPlayer.getLevel());
        entityPlayer.gameMode.changeGameModeForPlayer(GameType.CREATIVE);

        serverLevel.addNewPlayer(entityPlayer);
        server.getPlayerList().players.add(entityPlayer);

        PlayerJoinEvent playerJoinEvent;

        playerJoinEvent = paperJoinMessageFormat(bukkitPlayer, joinMessage);

        Bukkit.getPluginManager().callEvent(playerJoinEvent);

        try {
            Field didPlayerJoinEvent = entityPlayer.getClass().getDeclaredField("didPlayerJoinEvent");
            didPlayerJoinEvent.set(entityPlayer, true);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {

        }

        Component finalJoinMessage = playerJoinEvent.joinMessage();

        if (finalJoinMessage != null && !finalJoinMessage.equals(Component.empty())) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(finalJoinMessage);
            }
        }

        PlayerResourcePackStatusEvent resourcePackStatusEventAccepted = new PlayerResourcePackStatusEvent(bukkitPlayer, PlayerResourcePackStatusEvent.Status.ACCEPTED);
        PlayerResourcePackStatusEvent resourcePackStatusEventSuccessfullyLoaded = new PlayerResourcePackStatusEvent(bukkitPlayer, PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED);

        Bukkit.getScheduler().scheduleSyncDelayedTask(FakePlayerPlugin.getPlugin(), () -> {
            setResourcePackStatus(bukkitPlayer, PlayerResourcePackStatusEvent.Status.ACCEPTED);
            Bukkit.getPluginManager().callEvent(resourcePackStatusEventAccepted);

        }, 20);
        Bukkit.getScheduler().scheduleSyncDelayedTask(FakePlayerPlugin.getPlugin(), () -> {
            setResourcePackStatus(bukkitPlayer, PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED);
            Bukkit.getPluginManager().callEvent(resourcePackStatusEventSuccessfullyLoaded);

        }, 40);

        for (Player player : Bukkit.getOnlinePlayers()) {
            ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
            connection.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, entityPlayer));
            connection.connection.send(new ClientboundAddPlayerPacket(entityPlayer));
        }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(FakePlayerPlugin.getPlugin(), entityPlayer::tick, 1, 1);

        Bukkit.broadcast(Component.text("Bot Successfully Created"));

        return entityPlayer;
    }

    public static void removePlayer(FakePlayer player) {

        MinecraftServer mcServer = ((CraftServer) Bukkit.getServer()).getServer();

        ServerPlayer entityPlayer = (ServerPlayer) player.getEntityPlayer();

        ServerLevel worldServer = entityPlayer.getLevel().getWorld().getHandle();

        entityPlayer.awardStat(Stats.LEAVE_GAME);

        PlayerQuitEvent playerQuitEvent;

        CraftPlayer craftPlayer = entityPlayer.getBukkitEntity();

        playerQuitEvent = paperQuitMessageFormat(craftPlayer, craftPlayer.getPlayer());

        Bukkit.getPluginManager().callEvent(playerQuitEvent);

        entityPlayer.getBukkitEntity().disconnect(playerQuitEvent.quitMessage().toString());

        if (mcServer.isSameThread()) {
            entityPlayer.doTick();
        }

        entityPlayer.unRide();
        entityPlayer.getAdvancements().stopListening();
        mcServer.getPlayerList().players.remove(entityPlayer);

        mcServer.getPlayerList().remove(entityPlayer);

        FakePlayer.getFakePlayers().remove(player);

        Component finalQuitMessage = playerQuitEvent.quitMessage();

        if (finalQuitMessage != null && !finalQuitMessage.equals(Component.empty())) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                ServerGamePacketListenerImpl connection = ((CraftPlayer) p).getHandle().connection;
                connection.connection.send(new ClientboundRemoveEntitiesPacket(entityPlayer.getId()));
                connection.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, entityPlayer));

                p.sendMessage(playerQuitEvent.quitMessage());
            }
        }

        try {
            Method savePlayerFile = PlayerList.class.getDeclaredMethod("b", ServerPlayer.class);
            savePlayerFile.setAccessible(true);
            savePlayerFile.invoke(mcServer.getPlayerList(), entityPlayer);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
    public static void setResourcePackStatus(CraftPlayer bukkitPlayer, PlayerResourcePackStatusEvent.Status status) {
        bukkitPlayer.setResourcePackStatus(status);
    }

    public static PlayerJoinEvent paperJoinMessageFormat(CraftPlayer player, TranslatableComponent message) {
        return new PlayerJoinEvent(player.getPlayer(), PaperAdventure.asAdventure(message));
    }

    public static void playerInitialSpawnEvent(Player p) {
        PlayerSpawnLocationEvent ev = new PlayerSpawnLocationEvent(p, p.getLocation());
        Bukkit.getPluginManager().callEvent(ev);
    }

    private static ServerPlayer createEntityPlayer(UUID uuid, String name, ServerLevel worldServer) {
        MinecraftServer mcServer = ((CraftServer) Bukkit.getServer()).getServer();
        GameProfile gameProfile = new GameProfile(uuid, name);

        return new ServerPlayer(mcServer, worldServer, gameProfile);
    }

    private static TranslatableComponent getJoinMessage(ServerPlayer entityPlayer) {
        GameProfile gameProfile = entityPlayer.gameProfile;
        GameProfileCache userCache = ((CraftServer) Bukkit.getServer()).getServer().getProfileCache();
        GameProfile gameprofile2 = userCache.getProfileIfCached(gameProfile.getName());

        String s = gameprofile2 == null ? gameProfile.getName() : gameprofile2.getName();

        TranslatableComponent chatMessage;
        if (entityPlayer.gameProfile.getName().equalsIgnoreCase(s)) {
            chatMessage = new TranslatableComponent("multiplayer.player.joined", entityPlayer.getScoreboardName());
        } else {
            chatMessage = new TranslatableComponent("multiplayer.player.joined.renamed", entityPlayer.getScoreboardName(), s);
        }

        chatMessage.withStyle(ChatFormatting.YELLOW);

        return chatMessage;
    }

    public static PlayerQuitEvent paperQuitMessageFormat(CraftPlayer entityPlayer, Player player) {
        return new PlayerQuitEvent(player.getPlayer(), Component.translatable("multiplayer.player.left", NamedTextColor.YELLOW, PaperConfig.useDisplayNameInQuit ? player.displayName() : Component.text(player.getName())), entityPlayer.getHandle().quitReason);
    }
}

