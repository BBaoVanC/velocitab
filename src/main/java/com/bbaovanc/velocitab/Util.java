package com.bbaovanc.velocitab;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.GameProfile;
import net.kyori.adventure.text.Component;

public class Util {
    private final NameFormatter nameFormatter;

    public Util(NameFormatter nameFormatter) {
        this.nameFormatter = nameFormatter;
    }

    public void updateTablist(ProxyServer server) {
        // Update every single person's tab list
        server.getAllPlayers().forEach(onlinePlayer -> {
            TabList tabList = onlinePlayer.getTabList();
            // For every online player, update their entry in everyone's tab list
            server.getAllPlayers().forEach(p -> {
                tabList.addEntry(
                        tabList.removeEntry(p.getUniqueId())
                                .orElse(TabListEntry
                                        .builder()
                                        .tabList(tabList)
                                        .profile(p.getGameProfile())
                                        .build())
                                .setDisplayName(nameFormatter.formatUsername(p, p.getCurrentServer().get().getServerInfo().getName())));
            });

            tabList.addEntry(
                    TabListEntry.builder()
                            .profile(GameProfile.forOfflinePlayer("Anyone"))
                            .tabList(tabList)
                            .displayName(Component.text("xyz"))
                            .build());
        });
    }
}
