package com.bbaovanc.velocitab;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.proxy.server.RegisteredServer;

public class JoinListener {
    private final ProxyServer server;
    private final NameFormatter nameFormatter;

    public JoinListener(ProxyServer server, NameFormatter nameFormatter) {
        this.server = server;
        this.nameFormatter = nameFormatter;
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player target = event.getPlayer();
        RegisteredServer newServer = event.getServer();
        server.getAllPlayers().forEach(p -> {
            TabListEntry entry = p.getTabList().removeEntry(target.getUniqueId()).orElse(
                    TabListEntry.builder()
                            .profile(target.getGameProfile())
                            .build()
            );
            entry.setDisplayName(nameFormatter.formatUsername(target, newServer.getServerInfo().getName()));
        });
    }
}
