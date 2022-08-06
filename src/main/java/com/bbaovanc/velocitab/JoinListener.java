package com.bbaovanc.velocitab;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;

public class JoinListener {
    private final Util util;
    private final ProxyServer server;

    public JoinListener(Util util, ProxyServer server) {
        this.util = util;
        this.server = server;
    }

    @Subscribe(order = PostOrder.LAST)
    public void onServerConnected(ServerConnectedEvent event) {
        util.updateTablist(server);
        /*
        Player target = event.getPlayer();
        RegisteredServer newServer = event.getServer();
        server.getAllPlayers().forEach(p -> {
            TabList tabList = p.getTabList();
            TabListEntry entry = tabList.removeEntry(target.getUniqueId()).orElse(
                    TabListEntry.builder()
                            .profile(target.getGameProfile())
                            .latency(-1)
                            .playerKey(target.getIdentifiedKey())
                            .gameMode(3)
                            .displayName(Component.text("TEMPORARY"))
                            .tabList(tabList)
                            .build()
            );
            entry.setDisplayName(nameFormatter.formatUsername(target, newServer.getServerInfo().getName()));
            tabList.addEntry(entry);
        });
        */
    }
}
