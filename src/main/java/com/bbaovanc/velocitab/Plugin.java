package com.bbaovanc.velocitab;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.TabList;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.group.GroupDataRecalculateEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.user.User;
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
    private LuckPerms luckPerms;
    private NameFormatter nameFormatter;

    @Inject
    public Plugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        luckPerms = LuckPermsProvider.get();
        nameFormatter = new NameFormatter(luckPerms.getUserManager());

        EventBus eventBus = luckPerms.getEventBus();
        eventBus.subscribe(this, GroupDataRecalculateEvent.class, this::onLuckPermsGroupDataRecalculate);
        eventBus.subscribe(this, UserDataRecalculateEvent.class, this::onLuckPermsUserDataRecalculate);
    }

    public void onLuckPermsGroupDataRecalculate(GroupDataRecalculateEvent event) {
        server.getAllPlayers()
                .stream()
                .filter(p -> p.hasPermission("group." + event.getGroup().getName()))
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
        }).delay(Duration.ofMillis(1000)).schedule();
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
                                    // TODO: replace this with a log warning
                                    e.setDisplayName(nameFormatter.formatUsername(
                                            target,
                                            "null"
                                    ))
                            )
                    );
                }
        );
    }
}
