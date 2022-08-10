package com.bbaovanc.velocitab;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Optional;

@com.velocitypowered.api.plugin.Plugin(
        id = "velocitab",
        name = "velocitab",
        authors = {"bbaovanc"},
        description = "Simple tab list plugin for Velocity",
        url = "https://github.com/BBaoVanC/velocitab",
        version = "1.0.1",
        dependencies = {
                @Dependency(id = "luckperms")
        }
)
public class Plugin {
    private final ProxyServer server;
    private final Logger logger;
    private final MiniMessage miniMessage;
    private LuckPerms luckPerms;
    private UserManager lpUserManager;

    @Inject
    public Plugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        luckPerms = LuckPermsProvider.get();
        lpUserManager = luckPerms.getUserManager();

        // EventBus eventBus = luckPerms.getEventBus();
        // eventBus.subscribe(this, GroupDataRecalculateEvent.class, this::onLuckPermsGroupDataRecalculate);
        // eventBus.subscribe(this, UserDataRecalculateEvent.class, this::onLuckPermsUserDataRecalculate);

        // Refresh header and footer every so often since ping may change
        server.getScheduler().buildTask(this, this::update)
                .repeat(Duration.ofSeconds(15))
                .schedule();
    }

    // public void onLuckPermsGroupDataRecalculate(GroupDataRecalculateEvent event) {
    //     update();
    // }

    // public void onLuckPermsUserDataRecalculate(UserDataRecalculateEvent event) {
    //     update();
    // }

    @Subscribe
    public void onJoin(ServerPostConnectEvent event) {
        server.getScheduler().buildTask(this, this::update)
                .delay(Duration.ofMillis(1000))
                .schedule();
    }

    public void onLeave(DisconnectEvent event) {
        update();
    }

    private synchronized void update() {
        server.getAllPlayers().forEach(onlinePlayer -> {
            TagResolver tagResolver = defaultTagResolver(onlinePlayer);
            onlinePlayer.sendPlayerListHeaderAndFooter(
                    miniMessage.deserialize(Config.HEADER_FORMAT, tagResolver),
                    miniMessage.deserialize(Config.FOOTER_FORMAT, tagResolver)
            );

            TabList tabList = onlinePlayer.getTabList();

            // remove all players that aren't online
            tabList.getEntries().stream()
                    .filter(e -> server.getPlayer(e.getProfile().getId()).isEmpty())
                    .forEach(e -> tabList.removeEntry(e.getProfile().getId()));

            // update display name of every player on the proxy
            server.getAllPlayers().forEach(target -> tabList.addEntry(
                    tabList.removeEntry(target.getUniqueId()).orElse(
                            // build tab list entry for player on a different server
                            TabListEntry.builder()
                                    .tabList(tabList)
                                    .profile(target.getGameProfile())
                                    .playerKey(target.getIdentifiedKey())
                                    .latency((int) target.getPing())
                                    .gameMode(0) // proxy does not know gamemode so default to survival
                                    .build()
                    ).setDisplayName(
                            miniMessage.deserialize(
                                    Config.DISPLAY_NAME_FORMAT,
                                    defaultTagResolver(target)
                            )
                    )
            ));
        });
    }

    public TagResolver defaultTagResolver(Player target) {
        Optional<ServerConnection> serverConnection = target.getCurrentServer();
        String serverName = "null";
        if (serverConnection.isPresent()) {
            serverName = serverConnection.get().getServerInfo().getName();
        }

        User user = lpUserManager.getUser(target.getUniqueId());
        TagResolver luckPermsResolver = TagResolver.empty();
        if (user != null) {
            CachedMetaData cachedMetaData = user.getCachedData().getMetaData();
            luckPermsResolver = TagResolver.builder()
                    .resolver(Placeholder.parsed("luckperms_prefix", Optional.ofNullable(cachedMetaData.getPrefix()).orElse("")))
                    .resolver(Placeholder.parsed("luckperms_suffix", Optional.ofNullable(cachedMetaData.getSuffix()).orElse("")))
                    .build();
        }

        return TagResolver.builder()
                .resolver(Placeholder.unparsed("username", target.getUsername()))
                .resolver(Placeholder.unparsed("server", serverName))
                .resolver(Placeholder.unparsed("online_count", String.valueOf(server.getPlayerCount())))
                .resolver(Placeholder.unparsed("ping", String.valueOf(target.getPing())))
                .resolver(luckPermsResolver)
                .build();
    }
}
