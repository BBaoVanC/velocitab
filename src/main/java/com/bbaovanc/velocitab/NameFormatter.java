package com.bbaovanc.velocitab;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;

import javax.annotation.Nullable;
import java.util.Optional;

public class NameFormatter {
    private final MiniMessage miniMessage;
    private final UserManager userManager;

    public NameFormatter(UserManager userManager, MiniMessage miniMessage) {
        this.userManager = userManager;
        this.miniMessage = miniMessage;
    }

    public TagResolver makePlaceholders(String username, String serverName, @Nullable CachedMetaData luckPermsMetaData, int onlineCount, long ping) {
        TagResolver luckPermsResolver;
        if (luckPermsMetaData != null) {
            luckPermsResolver = TagResolver.builder()
                    .resolver(Placeholder.parsed("luckperms_prefix",
                            Optional.ofNullable(luckPermsMetaData.getPrefix()).orElse("")))
                    .resolver(Placeholder.parsed("luckperms_suffix",
                            Optional.ofNullable(luckPermsMetaData.getSuffix()).orElse("")))
                    .build();
        } else {
            luckPermsResolver = TagResolver.empty();
        }
        return TagResolver.builder()
                .resolver(Placeholder.unparsed("username", username))
                .resolver(Placeholder.unparsed("server", serverName))
                .resolver(Placeholder.unparsed("online_count", String.valueOf(onlineCount)))
                .resolver(Placeholder.unparsed("ping", String.valueOf(ping)))
                .resolver(luckPermsResolver)
                .build();
    }

    public Component formatUsername(Player player, String serverName) {
        User lpUser = userManager.getUser(player.getUniqueId());
        if (lpUser == null) {
            return Component.text(player.getUsername());
        }
        CachedMetaData metaData = lpUser.getCachedData().getMetaData();
        return miniMessage.deserialize(
                Config.DISPLAY_NAME_FORMAT,
                // TODO: fix onlineCount and ping
                makePlaceholders(player.getUsername(), serverName, metaData, 0, 0)
        );
    }
}
