package com.bbaovanc.velocitab;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.TabListEntry;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPermsProvider;

import java.util.stream.Collectors;

public class TestCommand implements SimpleCommand {
    @Override
    public void execute(final Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player player)) {
            return;
        }
//        player.getTabList().getEntries().forEach(e -> {
//            player.sendMessage(e.getDisplayNameComponent().orElse(Component.text("none")));
//        });
    }
}
