package com.bbaovanc.velocitab;

import com.google.inject.Inject;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.slf4j.Logger;

import java.util.UUID;

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
    private NameFormatter nameFormatter;

    @Inject
    public Plugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        nameFormatter = new NameFormatter(LuckPermsProvider.get().getUserManager());

//        EventManager manager = server.getEventManager();
//        manager.register(this, new JoinListener(util, server));

//        server.getCommandManager().register("test", new TestCommand());
    }


    @Subscribe
    public void onJoin(ServerPostConnectEvent event) {
        Player target = event.getPlayer();
//        String targetServerName = event.getServer().getServerInfo().getName();
        String targetServerName = target.getCurrentServer().get().getServerInfo().getName();
        modifyListPlayer(target, targetServerName);
    }

//    @Subscribe
//    public void onChat(PlayerChatEvent event) {
//        Player target = event.getPlayer();
//        String targetServerName = "blah";
//        modifyListPlayer(target, targetServerName);
//    }

    private void modifyListPlayer(Player target, String targetServerName) {
        server.getAllPlayers().forEach(p -> {
            TabList tabList = p.getTabList();
            tabList.addEntry(
                    // gamemode should show properly to other people on the same server
                    // since it modifies the existing entry
                    tabList.removeEntry(
                            target.getUniqueId()
                    ).orElse(
                            TabListEntry
                                    .builder()
                                    .tabList(tabList)
//                                    .profile(target.getGameProfile())
                                    .profile(GameProfile.forOfflinePlayer("placeholder"))
                                    .playerKey(target.getIdentifiedKey())
                                    .latency((int) target.getPing())
                                    .gameMode(0) // default to survival since proxy doesn't know gamemode
                                    .displayName(Component.text("stupid"))
                                    .build()
                    )
//                    ).setDisplayName(nameFormatter.formatUsername(target, targetServerName))
            );
        });
    }
}
