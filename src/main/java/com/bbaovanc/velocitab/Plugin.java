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
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.group.GroupDataRecalculateEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.types.InheritanceNode;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@com.velocitypowered.api.plugin.Plugin(
        id = "velocitab",
        name = "velocitab",
        authors = {"bbaovanc"},
        description = "Simple tab list plugin for Velocity",
        url = "https://github.com/BBaoVanC/velocitab",
        version = "1.0",
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
    private NameFormatter nameFormatter;

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
        nameFormatter = new NameFormatter(lpUserManager, miniMessage);

        EventBus eventBus = luckPerms.getEventBus();
        eventBus.subscribe(this, GroupDataRecalculateEvent.class, this::onLuckPermsGroupDataRecalculate);
        eventBus.subscribe(this, UserDataRecalculateEvent.class, this::onLuckPermsUserDataRecalculate);

        // TODO: Refresh header and footer every so often since ping may change
        // can't run getAllPlayers() since no one is online when the initialization happens
//        server.getScheduler().buildTask(this, () -> server.getAllPlayers().forEach(this::refreshHeaderFooter))
//                .delay(Duration.ofSeconds(30)).repeat(30).schedule();
    }

    public void onLuckPermsGroupDataRecalculate(GroupDataRecalculateEvent event) {
        server.getAllPlayers()
                .stream()
                .filter(p -> {
                    User user = lpUserManager.getUser(p.getUniqueId());
                    if (user == null) {
                        return false;
                    }
                    return user.getNodes().contains(InheritanceNode.builder().group(event.getGroup()).build());
                })
                .forEach(this::updateTargetInAllLists);
    }

    public void onLuckPermsUserDataRecalculate(UserDataRecalculateEvent event) {
        server.getPlayer(event.getUser().getUniqueId()).ifPresent(this::updateTargetInAllLists);
    }

    @Subscribe
    public void onJoin(ServerPostConnectEvent event) {
        server.getScheduler().buildTask(this, () -> {
            Player target = event.getPlayer();
            updateEntireList(target.getTabList());
            updateTargetInAllLists(target);
            refreshHeaderFooter(target);
        }).delay(Duration.ofMillis(1000)).schedule();
    }

    public void onLeave(DisconnectEvent event) {
        refreshHeaderFooter(event.getPlayer());
    }

    private void updateEntireList(TabList tabList) {
        server.getAllPlayers().forEach(onlinePlayer -> updateTargetInList(onlinePlayer, tabList));
    }

    private void updateTargetInAllLists(Player target) {
        server.getAllPlayers().forEach(onlinePlayer -> updateTargetInList(target, onlinePlayer.getTabList()));
    }

    private void updateTargetInList(Player target, TabList tabList) {
        tabList.removeEntry(
                target.getUniqueId()
        ).ifPresent(
                e -> {
                    Optional<ServerConnection> targetServer = target.getCurrentServer();
                    targetServer.ifPresentOrElse(
                            serverConnection -> tabList.addEntry(
                                    e.setDisplayName(nameFormatter.formatUsername(
                                            target,
                                            serverConnection.getServerInfo().getName())
                                    )
                            ),
                            () -> tabList.addEntry(
                                    // TODO: replace this with a log warning, this should be impossible
                                    e.setDisplayName(nameFormatter.formatUsername(
                                            target,
                                            "null"
                                    ))
                            )
                    );
                }
        );
    }

    private void refreshHeaderFooter(Player target) {
        Optional<ServerConnection> serverConnection = target.getCurrentServer();
        String serverName;
        if (serverConnection.isPresent()) {
            serverName = serverConnection.get().getServerInfo().getName();
        } else {
            // TODO: replace this with a log warning, this should be impossible
            serverName = "null";
        }

        CachedMetaData cachedMetaData = null;
        User user = lpUserManager.getUser(target.getUniqueId());
        if (user != null) {
            cachedMetaData = user.getCachedData().getMetaData();
        }

        TagResolver tagResolver = nameFormatter.makePlaceholders(
                target.getUsername(), serverName, cachedMetaData, server.getPlayerCount(), target.getPing()
        );
        target.sendPlayerListHeaderAndFooter(
                miniMessage.deserialize(Config.HEADER_FORMAT, tagResolver),
                miniMessage.deserialize(Config.FOOTER_FORMAT, tagResolver)
        );
    }
}
