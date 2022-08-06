package com.bbaovanc.velocitab;

import com.google.inject.Inject;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.proxy.ProxyServer;
import net.luckperms.api.LuckPermsProvider;
import org.slf4j.Logger;

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
    private Util util;

    @Inject
    public Plugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        util = new Util(new NameFormatter(LuckPermsProvider.get().getUserManager()));
        EventManager manager = server.getEventManager();
        manager.register(this, new JoinListener(util, server));

        server.getCommandManager().register("test", new TestCommand());
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        util.updateTablist(server);
    }
}
