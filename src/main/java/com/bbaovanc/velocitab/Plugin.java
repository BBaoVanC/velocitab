package com.bbaovanc.velocitab;

import com.google.common.base.Preconditions;
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
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.group.GroupDataRecalculateEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.types.InheritanceNode;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Optional;

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

        EventBus eventBus = luckPerms.getEventBus();
        eventBus.subscribe(this, GroupDataRecalculateEvent.class, this::onLuckPermsGroupDataRecalculate);
        eventBus.subscribe(this, UserDataRecalculateEvent.class, this::onLuckPermsUserDataRecalculate);

        // Refresh header and footer every so often since ping may change
        server.getScheduler().buildTask(this, this::updateEverythingForEveryone)
                .repeat(Duration.ofSeconds(15))
                .schedule();
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
            updateEntireList(target);
            updateTargetInAllLists(target);
        }).delay(Duration.ofMillis(1000)).schedule();
        refreshAllHeaderFooters();
    }

    public void onLeave(DisconnectEvent event) {
        refreshAllHeaderFooters();
        removeTargetFromAllLists(event.getPlayer());
    }

    private void updatePlayerList(Player player) {
        TabList tabList = player.getTabList();
        server.getAllPlayers().forEach(target -> {
            tabList.removeEntry(target.getUniqueId()).ifPresentOrElse(
                    e -> {
                        tabList.addEntry(
                                e.setDisplayName(
                                        miniMessage.deserialize(
                                                Config.DISPLAY_NAME_FORMAT,
                                                TagResolver.builder()
                                                        .resolver(defaultTagResolver(target))
                                                        .build()
                                        )
                                )
                        );
                    },
                    () -> tabList.addEntry(
                            TabListEntry.builder()
                                    .tabList(tabList)
                                    .profile(target.getGameProfile())
                                    .playerKey(target.getIdentifiedKey())
                                    .latency((int) target.getPing())
                                    .gameMode(0) // proxy does not know gamemode so default to survival
                                    .displayName(miniMessage.deserialize(
                                            Config.DISPLAY_NAME_FORMAT,
                                            TagResolver.builder()
                                                    .resolver(defaultTagResolver(target))
                                                    .build()
                                    ))
                                    .build()
                    )
            );
        });
    }

    private void updateEverythingForEveryone() {
        server.getAllPlayers().forEach(this::updateEntireList);
        refreshAllHeaderFooters();
    }

    private void removeTargetFromAllLists(Player target) {
        server.getAllPlayers().forEach(onlinePlayer -> onlinePlayer.getTabList().removeEntry(target.getUniqueId()));
    }

    private void updateEntireList(Player player) {
        server.getAllPlayers().forEach(onlinePlayer -> updateTargetForPlayer(onlinePlayer, player));
    }

    private void updateTargetInAllLists(Player target) {
        server.getAllPlayers().forEach(onlinePlayer -> updateTargetForPlayer(target, onlinePlayer));
    }

    public TagResolver defaultTagResolver(Player target) {
//        TagResolver luckPermsResolver;
//        if (luckPermsMetaData != null) {
//            luckPermsResolver = TagResolver.builder()
//                    .resolver(Placeholder.parsed("luckperms_prefix",
//                            Optional.ofNullable(luckPermsMetaData.getPrefix()).orElse("")))
//                    .resolver(Placeholder.parsed("luckperms_suffix",
//                            Optional.ofNullable(luckPermsMetaData.getSuffix()).orElse("")))
//                    .build();
//        } else {
//            luckPermsResolver = TagResolver.empty();
//        }

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

    private void updateTargetForPlayer(Player target, Player player) {
        TabList tabList = player.getTabList();
        tabList.removeEntry(
                target.getUniqueId()
        ).ifPresentOrElse(
                e -> {
                    tabList.addEntry(
                            e.setDisplayName(
                                    miniMessage.deserialize(
                                            Config.DISPLAY_NAME_FORMAT,
                                            TagResolver.builder()
                                                    .resolver(defaultTagResolver(target))
                                                    .build()
                                    )
                            )
                    );
                },
                () -> tabList.addEntry(
                        TabListEntry.builder()
                                .tabList(tabList)
                                .profile(target.getGameProfile())
                                .playerKey(target.getIdentifiedKey())
                                .latency((int) target.getPing())
                                .gameMode(0) // proxy does not know gamemode so default to survival
                                .displayName(miniMessage.deserialize(
                                        Config.DISPLAY_NAME_FORMAT,
                                        TagResolver.builder()
                                                .resolver(defaultTagResolver(target))
                                                .build()
                                ))
                                .build()
                )
        );
    }

    private void refreshAllHeaderFooters() {
        server.getAllPlayers().forEach(this::refreshHeaderFooter);
    }

    private void refreshHeaderFooter(Player target) {
        TagResolver tagResolver = defaultTagResolver(target);
        target.sendPlayerListHeaderAndFooter(
                miniMessage.deserialize(Config.HEADER_FORMAT, tagResolver),
                miniMessage.deserialize(Config.FOOTER_FORMAT, tagResolver)
        );
    }
}
